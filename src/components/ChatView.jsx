import { useState, useRef, useEffect, useCallback } from 'react'
import Avatar from './Avatar'
import EmojiPicker from './EmojiPicker'
import { formatMsgTime, formatDaySep, AUTO_REPLIES } from '../data/mockData'

function isSameDay(a, b) {
  const da = new Date(a), db = new Date(b)
  return (
    da.getFullYear() === db.getFullYear() &&
    da.getMonth()    === db.getMonth()    &&
    da.getDate()     === db.getDate()
  )
}

/**
 * @param {{
 *   contact: { id: string, name: string, online: boolean },
 *   messages: Array,
 *   onSend: (text: string) => void,
 *   onBack?: () => void,
 *   isTyping: boolean,
 * }} props
 */
export default function ChatView({ contact, messages, onSend, onBack, isTyping }) {
  const [text, setText]           = useState('')
  const [showEmoji, setShowEmoji] = useState(false)
  const bottomRef                 = useRef(null)
  const textareaRef               = useRef(null)
  const fileInputRef              = useRef(null)

  /* Scroll to bottom on new messages */
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  function handleSend() {
    const trimmed = text.trim()
    if (!trimmed) return
    onSend(trimmed)
    setText('')
    textareaRef.current?.focus()
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleEmojiSelect = useCallback((emoji) => {
    setText(prev => prev + emoji)
    setShowEmoji(false)
    textareaRef.current?.focus()
  }, [])

  function handleFileChange(e) {
    const file = e.target.files?.[0]
    if (!file) return
    const tooLarge = file.size > 10 * 1024 * 1024
    if (tooLarge) { alert('File size must be under 10 MB.'); return }
    onSend(`📎 ${file.name}`)
    e.target.value = ''
  }

  /* Build message list with day-separator groups */
  const rows = []
  messages.forEach((msg, i) => {
    const prev = messages[i - 1]
    if (!prev || !isSameDay(prev.ts, msg.ts)) {
      rows.push({ type: 'sep', label: formatDaySep(msg.ts), key: `sep-${i}` })
    }
    rows.push({ type: 'msg', msg, key: msg.id })
  })

  return (
    <>
      {/* Header */}
      <div className="chat-hd">
        {onBack && (
          <button className="icon-btn icon-btn-light" onClick={onBack} title="Back">
            ←
          </button>
        )}
        <Avatar name={contact.name} size={40} online={contact.online} />
        <div className="chat-hd-info">
          <div className="chat-hd-name">{contact.name}</div>
          <div className={`chat-hd-status ${contact.online ? 'is-online' : ''}`}>
            {contact.online ? '● Online' : 'Offline'}
          </div>
        </div>
        <button className="icon-btn icon-btn-light" title="Search in chat">🔍</button>
        <button className="icon-btn icon-btn-light" title="More options">⋮</button>
      </div>

      {/* Messages */}
      <div className="msgs-area">
        {rows.map(row => {
          if (row.type === 'sep') {
            return (
              <div key={row.key} className="day-sep">
                <span>{row.label}</span>
              </div>
            )
          }
          const { msg } = row
          const sent = msg.from === 'me'
          return (
            <div key={row.key} className={`msg-row ${sent ? 'is-sent' : 'is-received'}`}>
              <div className="msg-bubble">
                <div className="msg-text">{msg.text}</div>
                <div className="msg-meta">
                  <span className="msg-time">{formatMsgTime(msg.ts)}</span>
                  {sent && <span style={{ fontSize: 11, color: '#6a9f6a' }}>✓✓</span>}
                </div>
              </div>
            </div>
          )
        })}

        {/* Typing indicator */}
        {isTyping && (
          <div className="typing-row">
            <div className="typing-bubble">
              <span className="typing-dot" />
              <span className="typing-dot" />
              <span className="typing-dot" />
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Input area */}
      <div className="input-area">
        <div className="input-box">
          {/* Emoji */}
          <div className="emoji-wrap">
            <button
              className="inp-icon-btn"
              onClick={() => setShowEmoji(v => !v)}
              title="Emoji"
            >
              😊
            </button>
            {showEmoji && (
              <EmojiPicker
                onSelect={handleEmojiSelect}
                onClose={() => setShowEmoji(false)}
              />
            )}
          </div>

          {/* Textarea */}
          <textarea
            ref={textareaRef}
            className="msg-textarea"
            placeholder="Type a message"
            rows={1}
            value={text}
            onChange={e => setText(e.target.value)}
            onKeyDown={handleKeyDown}
            onInput={e => {
              e.target.style.height = 'auto'
              e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px'
            }}
          />

          {/* Attachment */}
          <button
            className="inp-icon-btn"
            onClick={() => fileInputRef.current?.click()}
            title="Attach file"
          >
            📎
          </button>
          <input
            ref={fileInputRef}
            type="file"
            className="av-upload"
            onChange={handleFileChange}
          />
        </div>

        {/* Send */}
        <button
          className="send-btn"
          onClick={handleSend}
          disabled={!text.trim()}
          title="Send"
        >
          ➤
        </button>
      </div>
    </>
  )
}
