/* ── helpers ── */
const t = (mins) => new Date(Date.now() - mins * 60_000)

export const AVATAR_COLORS = [
  '#e91e63','#9c27b0','#3f51b5','#2196f3',
  '#009688','#4caf50','#ff5722','#795548',
  '#607d8b','#e53935','#f57c00','#6d4c41',
]

export function getAvatarColor(name = '') {
  let h = 0
  for (let i = 0; i < name.length; i++) h = name.charCodeAt(i) + ((h << 5) - h)
  return AVATAR_COLORS[Math.abs(h) % AVATAR_COLORS.length]
}

export function getInitials(name = '') {
  return name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)
}

export function formatConvTime(date) {
  const d    = new Date(date)
  const now  = new Date()
  const diff = Math.floor((now - d) / 86_400_000)  // days

  if (diff === 0) return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  if (diff === 1) return 'Yesterday'
  if (diff < 7)  return d.toLocaleDateString([], { weekday: 'short' })
  return d.toLocaleDateString([], { month: 'short', day: 'numeric' })
}

export function formatMsgTime(date) {
  return new Date(date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

export function formatDaySep(date) {
  const d    = new Date(date)
  const now  = new Date()
  const diff = Math.floor((now - d) / 86_400_000)
  if (diff === 0) return 'Today'
  if (diff === 1) return 'Yesterday'
  return d.toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric' })
}

/* ── Contacts ── */
export const CONTACTS = [
  { id: 'u1', name: 'Alice Johnson',  online: true  },
  { id: 'u2', name: 'Bob Martinez',   online: false },
  { id: 'u3', name: 'Carol Williams', online: true  },
  { id: 'u4', name: 'David Brown',    online: false },
  { id: 'u5', name: 'Emma Davis',     online: true  },
  { id: 'u6', name: 'Frank Wilson',   online: false },
]

/* ── Auto-reply pool ── */
export const AUTO_REPLIES = [
  "That's great! 😊",
  "I agree!",
  "Sounds good to me!",
  "Let me think about that...",
  "Sure, no problem!",
  "Got it, thanks!",
  "Ha, totally! 😄",
  "Really? Tell me more.",
  "Interesting! 🤔",
  "OK, I'll check that.",
  "Thanks for letting me know!",
  "Perfect, see you then!",
  "👍",
  "Absolutely!",
  "I was thinking the same!",
  "Let's do it! 🎉",
  "Makes sense.",
  "Noted!",
  "Haha 😂",
  "That's cool!",
]

/* ── Initial conversations (loaded once; mutated in ChatView state) ── */
function makeId() { return Math.random().toString(36).slice(2, 9) }

export const INITIAL_CONVERSATIONS = [
  {
    id: 'c1', contactId: 'u1', unread: 2,
    messages: [
      { id: makeId(), from: 'u1', text: "Hey! How are you doing?",                            ts: t(120) },
      { id: makeId(), from: 'me', text: "I'm doing great! Just wrapped up the project.",       ts: t(118) },
      { id: makeId(), from: 'u1', text: "That's awesome! I've been grinding on mine too. 💪",  ts: t(115) },
      { id: makeId(), from: 'me', text: "We should catch up soon!",                             ts: t(110) },
      { id: makeId(), from: 'u1', text: "Definitely! Are you free this weekend?",               ts: t(5)   },
      { id: makeId(), from: 'u1', text: "We could grab coffee ☕",                              ts: t(3)   },
    ],
  },
  {
    id: 'c2', contactId: 'u2', unread: 0,
    messages: [
      { id: makeId(), from: 'me', text: "Bob, did you get the files I sent?",                  ts: t(180) },
      { id: makeId(), from: 'u2', text: "Yes! Got them. Looking through everything now.",       ts: t(170) },
      { id: makeId(), from: 'u2', text: "Looks solid, I'll review and get back to you.",        ts: t(165) },
      { id: makeId(), from: 'me', text: "Great, take your time!",                               ts: t(160) },
      { id: makeId(), from: 'u2', text: "Thanks. I'll send feedback by EOD.",                   ts: t(60)  },
    ],
  },
  {
    id: 'c3', contactId: 'u3', unread: 1,
    messages: [
      { id: makeId(), from: 'u3', text: "Morning! Ready for the presentation?",                ts: t(1440) },
      { id: makeId(), from: 'me', text: "Almost! Still polishing the last few slides.",         ts: t(1430) },
      { id: makeId(), from: 'u3', text: "You'll crush it! 💪",                                  ts: t(1425) },
      { id: makeId(), from: 'u3', text: "Let me know if you need help with anything.",           ts: t(30)   },
    ],
  },
  {
    id: 'c4', contactId: 'u4', unread: 0,
    messages: [
      { id: makeId(), from: 'u4', text: "Hey, are you coming to the meetup?",                   ts: t(2880) },
      { id: makeId(), from: 'me', text: "Yes! Registered yesterday.",                            ts: t(2870) },
      { id: makeId(), from: 'u4', text: "Perfect! See you there. 🎉",                            ts: t(2865) },
    ],
  },
  {
    id: 'c5', contactId: 'u5', unread: 0,
    messages: [
      { id: makeId(), from: 'me', text: "Emma, can you share the document?",                    ts: t(4320) },
      { id: makeId(), from: 'u5', text: "Sure! Just sent it to your email.",                    ts: t(4310) },
      { id: makeId(), from: 'u5', text: "Let me know if you can't open it.",                    ts: t(4308) },
      { id: makeId(), from: 'me', text: "Got it! Thanks a lot 🙏",                              ts: t(4300) },
    ],
  },
  {
    id: 'c6', contactId: 'u6', unread: 0,
    messages: [
      { id: makeId(), from: 'u6', text: "Have you seen the latest update?",                     ts: t(7200) },
      { id: makeId(), from: 'me', text: "Not yet, what's new?",                                 ts: t(7195) },
      { id: makeId(), from: 'u6', text: "They added a bunch of cool new features!",              ts: t(7190) },
    ],
  },
]
