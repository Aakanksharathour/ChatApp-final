import { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate }    from 'react-router-dom'
import { useAuth }        from '../context/AuthContext'
import { api, getToken }  from '../api/client'
import Avatar             from '../components/Avatar'
import ChatView           from '../components/ChatView'
import { formatConvTime } from '../data/mockData'

/* Convert backend message to ChatView format */
function convertMessage(msg, myId) {
  return {
    id:   msg.messageId,
    from: msg.senderId === myId ? 'me' : msg.senderId,
    text: msg.text,
    ts:   new Date(msg.timestamp),
  }
}

export default function ChatListPage() {
  const { user, logout } = useAuth()
  const navigate      = useNavigate()

  const [chats,         setChats]         = useState([])
  const [activeChatId,  setActiveChatId]  = useState(null)
  const [messages,      setMessages]      = useState([])
  const [loadingChats,  setLoadingChats]  = useState(true)
  const [loadingMsgs,   setLoadingMsgs]   = useState(false)
  const [onlineUsers,   setOnlineUsers]   = useState(new Set())
  const [query,         setQuery]         = useState('')
  const [showNewChat,    setShowNewChat]    = useState(false)
  const [userSearch,     setUserSearch]     = useState('')
  const [userResults,    setUserResults]    = useState([])
  const [searchingUsers, setSearchingUsers] = useState(false)
  const [searchError,    setSearchError]    = useState('')
  const [sidebarUsers,   setSidebarUsers]   = useState([])
  const [searchingSidebar, setSearchingSidebar] = useState(false)

  const wsRef           = useRef(null)
  const activeChatIdRef = useRef(null)
  const isMobile        = typeof window !== 'undefined' && window.innerWidth <= 768

  /* Keep ref in sync with state for use inside WS callback */
  useEffect(() => { activeChatIdRef.current = activeChatId }, [activeChatId])

  /* ── Load chats on mount ── */
  useEffect(() => {
    fetchChats()
    connectWS()
    return () => {
      if (wsRef.current) {
        wsRef.current.onclose = null   // prevent reconnect on intentional close
        wsRef.current.close()
      }
    }
  }, [])

  async function fetchChats() {
    try {
      const data = await api.get('/api/chats')
      setChats(data.chats || [])
    } catch (err) {
      console.error('Failed to load chats:', err)
    } finally {
      setLoadingChats(false)
    }
  }

  /* ── WebSocket connection ── */
  function connectWS() {
    const token = getToken()
    if (!token) return

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
    const wsUrl  = apiUrl.replace(/^http/, 'ws')
    const ws = new WebSocket(`${wsUrl}/ws/chat?token=${token}`)
    wsRef.current = ws

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)

        if (data.type === 'new_message') {
          const { chatId, message } = data

          /* Add to messages list if this chat is open AND sender is not me */
          if (chatId === activeChatIdRef.current && message.senderId !== user.id) {
            setMessages(prev => [...prev, convertMessage(message, user.id)])
          }

          /* Update chat list preview + unread count */
          setChats(prev => prev.map(c =>
            c.chatId === chatId
              ? {
                  ...c,
                  lastMessageText:     message.text,
                  lastMessageSenderId: message.senderId,
                  lastMessageTime:     message.timestamp,
                  unreadCount:
                    chatId === activeChatIdRef.current
                      ? 0
                      : (c.unreadCount || 0) + 1,
                }
              : c
          ))
        } else if (data.type === 'user_online') {
          setOnlineUsers(prev => new Set([...prev, data.userId]))
        } else if (data.type === 'user_offline') {
          setOnlineUsers(prev => { const s = new Set(prev); s.delete(data.userId); return s })
        }
      } catch {}
    }

    ws.onclose = () => {
      /* Auto-reconnect after 3 s if not intentionally closed */
      if (wsRef.current === ws) {
        setTimeout(connectWS, 3000)
      }
    }

    ws.onerror = () => {}
  }

  /* ── Select & open a chat ── */
  async function selectChat(chatId) {
    setActiveChatId(chatId)
    setMessages([])
    setLoadingMsgs(true)

    /* Reset unread count optimistically */
    setChats(prev => prev.map(c => c.chatId === chatId ? { ...c, unreadCount: 0 } : c))

    try {
      const data = await api.get(`/api/chats/${chatId}/messages`)
      setMessages((data.messages || []).map(m => convertMessage(m, user.id)))
    } catch (err) {
      console.error('Failed to load messages:', err)
    } finally {
      setLoadingMsgs(false)
    }
  }

  /* ── Send a message ── */
  const handleSend = useCallback(async (text) => {
    if (!activeChatId || !text.trim()) return
    try {
      const data = await api.post(`/api/chats/${activeChatId}/messages`, { text })
      const msg = convertMessage(data.message, user.id)
      setMessages(prev => [...prev, msg])
      setChats(prev => prev.map(c =>
        c.chatId === activeChatId
          ? {
              ...c,
              lastMessageText:     text,
              lastMessageSenderId: user.id,
              lastMessageTime:     new Date().toISOString(),
            }
          : c
      ))
    } catch (err) {
      alert('Failed to send message: ' + err.message)
    }
  }, [activeChatId, user.id])

  /* ── Send a file ── */
  const handleSendFile = useCallback(async (file) => {
    if (!activeChatId) return
    if (file.size > 10 * 1024 * 1024) { alert('File size must be under 10 MB.'); return }
    try {
      const fd = new FormData()
      fd.append('file', file)
      const upload = await api.upload('/api/upload', fd)
      await handleSend(upload.url)
    } catch (err) {
      alert('Upload failed: ' + err.message)
    }
  }, [activeChatId, handleSend])

  /* ── Sidebar search: also search users when query is active ── */
  useEffect(() => {
    if (!query.trim()) { setSidebarUsers([]); return }
    const timer = setTimeout(async () => {
      setSearchingSidebar(true)
      try {
        const results = await api.get(`/api/users/search?q=${encodeURIComponent(query)}`)
        setSidebarUsers(Array.isArray(results) ? results : [])
      } catch {
        setSidebarUsers([])
      } finally {
        setSearchingSidebar(false)
      }
    }, 300)
    return () => clearTimeout(timer)
  }, [query])

  /* ── Modal search for new chat ── */
  useEffect(() => {
    if (!showNewChat || !userSearch.trim()) {
      setUserResults([])
      setSearchError('')
      return
    }
    const timer = setTimeout(async () => {
      setSearchingUsers(true)
      setSearchError('')
      try {
        const results = await api.get(`/api/users/search?q=${encodeURIComponent(userSearch)}`)
        setUserResults(Array.isArray(results) ? results : [])
      } catch (err) {
        setSearchError(err.message || 'Search failed')
        setUserResults([])
      } finally {
        setSearchingUsers(false)
      }
    }, 300)
    return () => clearTimeout(timer)
  }, [userSearch, showNewChat])

  /* ── Start a new chat ── */
  function closeNewChat() {
    setShowNewChat(false)
    setUserSearch('')
    setUserResults([])
    setSearchError('')
  }

  async function startChat(participantId) {
    closeNewChat()
    try {
      const data = await api.post('/api/chats', { participantId })
      const chat = data.chat
      setChats(prev =>
        prev.find(c => c.chatId === chat.chatId)
          ? prev
          : [chat, ...prev]
      )
      await selectChat(chat.chatId)
    } catch (err) {
      alert('Failed to start chat: ' + err.message)
    }
  }

  /* ── Derived data ── */
  const activeChat = chats.find(c => c.chatId === activeChatId) || null

  const filteredChats = chats.filter(c => {
    if (!query.trim()) return true
    const q = query.toLowerCase()
    return (
      c.contactName?.toLowerCase().includes(q) ||
      c.lastMessageText?.toLowerCase().includes(q)
    )
  })

  const sortedChats = [...filteredChats].sort((a, b) =>
    new Date(b.lastMessageTime || b.updatedAt || 0) -
    new Date(a.lastMessageTime || a.updatedAt || 0)
  )

  const showSidebar = !isMobile || !activeChatId
  const showChat    = !!activeChatId

  return (
    <div className="app-layout">

      {/* ── Sidebar ── */}
      <div className={`sidebar ${!showSidebar ? 'mob-hidden' : ''}`}>
        <div className="sidebar-hd">
          <div className="sidebar-brand">
            <span>💬 ChatApp</span>
          </div>
          <div className="sidebar-actions">
            <button
              className="icon-btn icon-btn-light"
              onClick={() => setShowNewChat(true)}
              title="New chat"
            >
              ✏️
            </button>
            <button
              className="icon-btn icon-btn-light"
              onClick={() => navigate('/profile')}
              title="Profile"
            >
              👤
            </button>
            <button
              className="icon-btn icon-btn-light"
              onClick={() => { logout(); navigate('/login') }}
              title="Sign out"
            >
              🚪
            </button>
          </div>
        </div>

        {/* Chat search */}
        <div className="search-bar">
          <div className="search-wrap">
            <span className="s-icon">🔍</span>
            <input
              type="search"
              className="search-input"
              placeholder="Search chats or people…"
              value={query}
              onChange={e => setQuery(e.target.value)}
            />
          </div>
        </div>

        {/* Conversation list */}
        <div className="conv-list">
          {loadingChats ? (
            <div className="conv-empty"><p>Loading chats…</p></div>
          ) : (
            <>
              {/* Existing conversations */}
              {sortedChats.length === 0 && !query && (
                <div className="conv-empty">
                  <div className="conv-empty-icon">💬</div>
                  <p>No chats yet</p>
                  <p style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 4 }}>
                    Use 🔍 above to find people
                  </p>
                </div>
              )}

              {sortedChats.map(chat => {
                const isActive = chat.chatId === activeChatId
                const isOnline = onlineUsers.has(chat.contactId)
                const lastTime = chat.lastMessageTime || chat.updatedAt
                const preview  = chat.lastMessageText
                  ? (chat.lastMessageSenderId === user.id
                      ? `You: ${chat.lastMessageText}`
                      : chat.lastMessageText)
                  : 'No messages yet'

                return (
                  <div
                    key={chat.chatId}
                    className={`conv-item ${isActive ? 'is-active' : ''}`}
                    onClick={() => selectChat(chat.chatId)}
                  >
                    <Avatar name={chat.contactName || '?'} size={48} online={isOnline} />
                    <div className="conv-body">
                      <div className="conv-row1">
                        <span className="conv-name">{chat.contactName}</span>
                        {lastTime && (
                          <span className={`conv-ts ${chat.unreadCount > 0 ? 'has-unread' : ''}`}>
                            {formatConvTime(lastTime)}
                          </span>
                        )}
                      </div>
                      <div className="conv-row2">
                        <span className={`conv-last ${chat.unreadCount > 0 ? 'has-unread' : ''}`}>
                          {preview}
                        </span>
                        {chat.unreadCount > 0 && (
                          <span className="unread-badge">{chat.unreadCount}</span>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })}

              {/* People section — shows when searching */}
              {query.trim() && (
                <>
                  <div style={{ padding: '8px 16px 4px', fontSize: 11, fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.05em', borderTop: sortedChats.length ? '1px solid var(--border)' : 'none' }}>
                    People
                  </div>
                  {searchingSidebar && (
                    <div style={{ padding: '8px 16px', color: 'var(--text-3)', fontSize: 13 }}>Searching…</div>
                  )}
                  {!searchingSidebar && sidebarUsers.length === 0 && (
                    <div style={{ padding: '8px 16px', color: 'var(--text-3)', fontSize: 13 }}>No people found</div>
                  )}
                  {sidebarUsers.map(u => (
                    <div
                      key={u.id}
                      className="conv-item"
                      onClick={() => startChat(u.id)}
                    >
                      <Avatar name={u.name} size={48} />
                      <div className="conv-body">
                        <div className="conv-row1">
                          <span className="conv-name">{u.name}</span>
                        </div>
                        <div className="conv-row2">
                          <span className="conv-last">{u.email}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </>
              )}
            </>
          )}
        </div>
      </div>

      {/* ── Chat main ── */}
      <div className={`chat-main ${!showChat ? 'mob-hidden' : ''}`}>
        {showChat && activeChat ? (
          loadingMsgs ? (
            <div className="welcome-screen">
              <p>Loading messages…</p>
            </div>
          ) : (
            <ChatView
              contact={{
                id:     activeChat.contactId,
                name:   activeChat.contactName,
                photo:  activeChat.contactProfilePhoto,
                online: onlineUsers.has(activeChat.contactId),
              }}
              messages={messages}
              onSend={handleSend}
              onSendFile={handleSendFile}
              onBack={isMobile ? () => setActiveChatId(null) : undefined}
              isTyping={false}
            />
          )
        ) : (
          <div className="welcome-screen">
            <div className="welcome-icon">💬</div>
            <h2 className="welcome-title">ChatApp</h2>
            <p className="welcome-body">
              Select a conversation or start a new one.
            </p>
            <div className="welcome-secure">
              🔒 Your messages are end-to-end encrypted
            </div>
          </div>
        )}
      </div>

      {/* ── New Chat Modal ── */}
      {showNewChat && (
        <div className="modal-backdrop" onClick={closeNewChat}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <div className="modal-hd">
              <span>New Chat</span>
              <button className="icon-btn icon-btn-light" onClick={closeNewChat}>✕</button>
            </div>
            <div className="search-bar" style={{ padding: '8px 12px' }}>
              <div className="search-wrap">
                <span className="s-icon">🔍</span>
                <input
                  type="search"
                  className="search-input"
                  placeholder="Search by name or email…"
                  value={userSearch}
                  onChange={e => setUserSearch(e.target.value)}
                  autoFocus
                />
              </div>
            </div>
            <div style={{ maxHeight: 320, overflowY: 'auto' }}>
              {searchingUsers && (
                <p style={{ padding: '12px 16px', color: '#666', margin: 0 }}>Searching…</p>
              )}
              {searchError && (
                <p style={{ padding: '12px 16px', color: '#c00', margin: 0 }}>{searchError}</p>
              )}
              {!searchingUsers && !searchError && userSearch && userResults.length === 0 && (
                <p style={{ padding: '12px 16px', color: '#666', margin: 0 }}>No users found</p>
              )}
              {userResults.map(u => (
                <div
                  key={u.id}
                  style={{ display:'flex', alignItems:'center', padding:'12px 16px', gap:12, cursor:'pointer', borderBottom:'1px solid #eee' }}
                  onClick={() => startChat(u.id)}
                >
                  <Avatar name={u.name} size={40} />
                  <div>
                    <div style={{ fontWeight: 600 }}>{u.name}</div>
                    <div style={{ fontSize: 13, color: '#666' }}>{u.email}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
