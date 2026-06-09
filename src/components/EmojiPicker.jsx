import { useEffect, useRef } from 'react'

const CATEGORIES = [
  {
    label: 'Smileys',
    emojis: ['😀','😂','😍','🥰','😊','😎','🤔','😴','🥺','😭','😅','🤣','😇','🤩','😋','😏','😤','🙄','😬','🫡'],
  },
  {
    label: 'Gestures',
    emojis: ['👍','👎','👏','🙌','🤝','🙏','👋','✌️','🤞','💪','🫂','👌','🤌','👆','✋'],
  },
  {
    label: 'Hearts',
    emojis: ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','💔','💕','💖','💗','💓','💘','❤️‍🔥'],
  },
  {
    label: 'Objects',
    emojis: ['🎉','🎊','🥳','🎂','🔥','💯','✅','⚡','💡','📱','💻','🎵','🎮','📚','🌟','⭐','🌈','☀️'],
  },
]

/**
 * @param {{ onSelect: (emoji: string) => void, onClose: () => void }} props
 */
export default function EmojiPicker({ onSelect, onClose }) {
  const ref = useRef(null)

  useEffect(() => {
    function handler(e) {
      if (ref.current && !ref.current.contains(e.target)) onClose()
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [onClose])

  return (
    <div className="emoji-panel" ref={ref}>
      {CATEGORIES.map(cat => (
        <div key={cat.label}>
          <div className="emoji-cat-title">{cat.label}</div>
          <div className="emoji-grid">
            {cat.emojis.map(e => (
              <button
                key={e}
                className="emoji-item"
                onClick={() => onSelect(e)}
                title={e}
              >
                {e}
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
