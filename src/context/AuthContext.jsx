import { createContext, useContext, useState } from 'react'
import { api, setToken, clearToken } from '../api/client'

const AuthContext = createContext(null)

const SESSION_KEY = 'chatapp_session'

function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const s     = localStorage.getItem(SESSION_KEY)
      const token = localStorage.getItem('chatapp_token')
      if (!s || !token || isTokenExpired(token)) {
        localStorage.removeItem(SESSION_KEY)
        localStorage.removeItem('chatapp_token')
        return null
      }
      return JSON.parse(s)
    } catch {
      return null
    }
  })

  async function login(email, password) {
    try {
      const data = await api.post('/api/auth/login', { email, password })
      setToken(data.token)
      const session = { id: data.userId, name: data.name, email: data.email, photo: null }
      setUser(session)
      localStorage.setItem(SESSION_KEY, JSON.stringify(session))
      return { ok: true }
    } catch (err) {
      return { ok: false, error: err.message }
    }
  }

  async function signup(name, email, password) {
    try {
      await api.post('/api/auth/register', { name, email, password, confirmPassword: password })
      return { ok: true }
    } catch (err) {
      return { ok: false, error: err.message }
    }
  }

  function logout() {
    setUser(null)
    clearToken()
    localStorage.removeItem(SESSION_KEY)
  }

  async function updateProfile(data) {
    try {
      const updated = await api.put('/api/users/me', {
        name: data.name || undefined,
        profilePhoto: data.photo || data.profilePhoto || undefined,
      })
      const session = { ...user, name: updated.name, photo: updated.profilePhoto }
      setUser(session)
      localStorage.setItem(SESSION_KEY, JSON.stringify(session))
      return { ok: true }
    } catch (err) {
      return { ok: false, error: err.message }
    }
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
