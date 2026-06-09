import { useState, useEffect, useCallback } from 'react'
import { useNavigate }   from 'react-router-dom'
import { useAuth }       from '../context/AuthContext'
import Avatar            from '../components/Avatar'
import ChatView          from '../components/ChatView'
import {
  CONTACTS,
  INITIAL_CONVERSATIONS,
  AUTO_REPLIES,
  formatConvTime,
} from '../data/mockData'

/* Deep-clone initial data once on module load */
function cloneConvs() {
  return INITIAL_CONVERSATIONS.map(c => ({
    ...c,
    messages: c.messages.map(m => ({ ...m, ts: new Date(m.ts) })),
  }))
}

export default function ChatListPage() {
  const { user }      = useAuth()
  const navigate      = useNavigate()
  const [convs,       setConvs]       = useState(() => cloneConvs())
  const [activeId,    setActiveId]    = useState(null)   // selected conv id
  const [query,       setQuery]       = useState('')
  const [isTyping,    setIsTyping]    = useState(false)  // contact typing indicator
  const isMobile      = typeof window !== 'undefined' && window.innerWidth <= 768

  /* Helper: find contact by id */
  const contactOf = (cid) => CONTACTS.find(c => c.id === cid) || null

  /* Selected conversation object */
  const activeConv = convs.find(c => c.id === activeId) || null
  const activeContact = activeConv ? contactOf(activeConv.contactId) : null

  /* Mark conversation as read when opened */
  function selectConv(id) {
    setActiveId(id)
    setConvs(prev =>
      prev.map(c => (c.id === id ? { ...c, unread: 0 } : c))
    )
  }

  /* Send a message */
  const handleSend = useCallback((text) => {
    if (!activeId) return
    const newMsg = { id: `m_${Date.now()}`, from: 'me', text, ts: new Date() }
    setConvs(prev =>
      prev.map(c =>
        c.id === activeId
          ? { ...c, messages: [...c.messages, newMsg] }
          : c
      )
    )

    /* Simulate auto-reply */
    setIsTyping(true)
    const delay = 1000 + Math.random() * 1200
    setTimeout(() => {
      setIsTyping(false)
      const reply = {
        id:   `m_${Date.now()}`,
        from: activeConv.contactId,
        text: AUTO_REPLIES[Math.floor(Math.random() * AUTO_REPLIES.length)],
        ts:   new Date(),
      }
      setConvs(prev =>
        prev.map(c =>
          c.id === activeId
            ? { ...c, messages: [...c.messages, reply] }
            : c
        )
      )
    }, delay)
  }, [activeId, activeConv])

  /* Filter conversations by search query */
  const filtered = convs.filter(c => {
    if (!query.trim()) return true
    const contact = contactOf(c.contactId)
    const nameMatch = contact?.name.toLowerCase().includes(query.toLowerCase())
    const lastMsg   = c.messages.at(-1)?.text?.toLowerCase() || ''
    const msgMatch  = lastMsg.includes(query.toLowerCase())
    return nameMatch || msgMatch
  })

  /* Sort by last message time (newest first) */
  const sorted = [...filtered].sort((a, b) => {
    const ta = a.messages.at(-1)?.ts || 0
    const tb = b.messages.at(-1)?.ts || 0
    return new Date(tb) - new Date(ta)
  })

  /* Show chat main when a conv is selected on desktop;
     hide sidebar on mobile when chat is open */
  const showSidebar = !isMobile || !activeId
  const showChat    = !!activeId

  return (
    <div className="app-layout">
      {/* ── Sidebar ── */}
      <div className={`sidebar ${!showSidebar ? 'mob-hidden' : ''}`}>
        {/* Header */}
        <div className="sidebar-hd">
          <div className="sidebar-brand">
            <span>💬 ChatApp</span>
          </div>
          <div className="sidebar-actions">
            <button
              className="icon-btn icon-btn-light"
              onClick={() => navigate('/profile')}
              title="Profile"
            >
              👤
            </button>
          </div>
        </div>

        {/* Search */}
        <div className="search-bar">
          <div className="search-wrap">
            <span className="s-icon">🔍</span>
            <input
              type="search"
              className="search-input"
              placeholder="Search conversations…"
              value={query}
              onChange={e => setQuery(e.target.value)}
            />
          </div>
        </div>

        {/* Conversation list */}
        <div className="conv-list">
          {sorted.length === 0 ? (
            <div className="conv-empty">
              <div className="conv-empty-icon">🔍</div>
              <p>No conversations found</p>
            </div>
          ) : (
            sorted.map(conv => {
              const contact = contactOf(conv.contactId)
              if (!contact) return null
              const lastMsg = conv.messages.at(-1)
              const preview = lastMsg
                ? (lastMsg.from === 'me' ? `You: ${lastMsg.text}` : lastMsg.text)
                : 'No messages yet'
              const ts = lastMsg?.ts

              return (
                <div
                  key={conv.id}
                  className={`conv-item ${conv.id === activeId ? 'is-active' : ''}`}
                  onClick={() => selectConv(conv.id)}
                >
                  <Avatar name={contact.name} size={48} online={contact.online} />

                  <div className="conv-body">
                    <div className="conv-row1">
                      <span className="conv-name">{contact.name}</span>
                      {ts && (
                        <span className={`conv-ts ${conv.unread > 0 ? 'has-unread' : ''}`}>
                          {formatConvTime(ts)}
                        </span>
                      )}
                    </div>
                    <div className="conv-row2">
                      <span className={`conv-last ${conv.unread > 0 ? 'has-unread' : ''}`}>
                        {preview}
                      </span>
                      {conv.unread > 0 && (
                        <span className="unread-badge">{conv.unread}</span>
                      )}
                    </div>
                  </div>
                </div>
              )
            })
          )}
        </div>
      </div>

      {/* ── Chat main ── */}
      <div className={`chat-main ${!showChat ? 'mob-hidden' : ''}`}>
        {showChat && activeContact ? (
          <ChatView
            contact={activeContact}
            messages={activeConv.messages}
            onSend={handleSend}
            onBack={isMobile ? () => setActiveId(null) : undefined}
            isTyping={isTyping}
          />
        ) : (
          <div className="welcome-screen">
            <div className="welcome-icon">💬</div>
            <h2 className="welcome-title">ChatApp</h2>
            <p className="welcome-body">
              Select a conversation from the sidebar to start chatting.
            </p>
            <div className="welcome-secure">
              🔒 Your messages are end-to-end encrypted
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
