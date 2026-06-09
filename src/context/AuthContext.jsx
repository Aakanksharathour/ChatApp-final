import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

const SESSION_KEY = 'chatapp_session'
const USERS_KEY   = 'chatapp_users'

/* Seed users always available — not stored in localStorage */
const SEED_USERS = [
  { id: 'seed_1', name: 'Demo User',  email: 'demo@example.com',  password: 'demo123'  },
  { id: 'seed_2', name: 'Alice Johnson', email: 'alice@example.com', password: 'alice123' },
  { id: 'seed_3', name: 'Bob Martinez',  email: 'bob@example.com',   password: 'bob123'   },
]

function loadUsers() {
  try {
    const stored = JSON.parse(localStorage.getItem(USERS_KEY) || '[]')
    const seedEmails = new Set(SEED_USERS.map(u => u.email))
    const extra = stored.filter(u => !seedEmails.has(u.email))
    return [...SEED_USERS, ...extra]
  } catch {
    return [...SEED_USERS]
  }
}

function persistUsers(users) {
  const seedEmails = new Set(SEED_USERS.map(u => u.email))
  const extra = users.filter(u => !seedEmails.has(u.email))
  localStorage.setItem(USERS_KEY, JSON.stringify(extra))
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const s = localStorage.getItem(SESSION_KEY)
      return s ? JSON.parse(s) : null
    } catch {
      return null
    }
  })

  function login(email, password) {
    const users = loadUsers()
    const found = users.find(
      u => u.email.toLowerCase() === email.toLowerCase() && u.password === password
    )
    if (!found) return { ok: false, error: 'Incorrect email or password. Please try again.' }

    const session = { id: found.id, name: found.name, email: found.email }
    setUser(session)
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    return { ok: true }
  }

  function signup(name, email, password) {
    const users = loadUsers()
    if (users.find(u => u.email.toLowerCase() === email.toLowerCase())) {
      return { ok: false, error: 'An account with this email already exists.' }
    }
    const newUser = { id: `u_${Date.now()}`, name, email, password }
    persistUsers([...users, newUser])
    return { ok: true }
  }

  function logout() {
    setUser(null)
    localStorage.removeItem(SESSION_KEY)
  }

  function updateProfile(data) {
    const updated = { ...user, ...data }
    setUser(updated)
    localStorage.setItem(SESSION_KEY, JSON.stringify(updated))

    const users = loadUsers()
    persistUsers(users.map(u => (u.id === updated.id ? { ...u, ...data } : u)))
  }

  return (
    <AuthContext.Provider value={{ user, login, signup, logout, updateProfile }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
