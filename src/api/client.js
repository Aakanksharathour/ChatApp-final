const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export function getToken() {
  return localStorage.getItem('chatapp_token')
}

export function setToken(token) {
  localStorage.setItem('chatapp_token', token)
}

export function clearToken() {
  localStorage.removeItem('chatapp_token')
}

async function request(path, options = {}) {
  const token = getToken()
  const isFormData = options.body instanceof FormData

  const headers = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(!isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  }

  const res = await fetch(BASE_URL + path, { ...options, headers })

  if (res.status === 401) {
    clearToken()
    localStorage.removeItem('chatapp_session')
    window.location.href = '/login'
    return
  }

  if (!res.ok) {
    let errMsg = `Request failed (${res.status})`
    try {
      const j = await res.json()
      errMsg = j.error || j.message || Object.values(j)[0] || errMsg
    } catch {}
    const err = new Error(errMsg)
    err.status = res.status
    throw err
  }

  const text = await res.text()
  return text ? JSON.parse(text) : null
}

export const api = {
  get:    (path)           => request(path),
  post:   (path, body)     => request(path, { method: 'POST',  body: JSON.stringify(body) }),
  put:    (path, body)     => request(path, { method: 'PUT',   body: JSON.stringify(body) }),
  upload: (path, formData) => request(path, { method: 'POST',  body: formData }),
}
