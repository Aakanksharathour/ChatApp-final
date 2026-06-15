import { useState, useRef } from 'react'
import { useNavigate }  from 'react-router-dom'
import { useAuth }      from '../context/AuthContext'
import { api }          from '../api/client'
import Avatar           from '../components/Avatar'

export default function ProfilePage() {
  const { user, logout, updateProfile } = useAuth()
  const navigate   = useNavigate()
  const fileRef    = useRef(null)

  const [editingName, setEditingName] = useState(false)
  const [nameVal,     setNameVal]     = useState(user?.name || '')
  const [saved,       setSaved]       = useState(false)
  const [saving,      setSaving]      = useState(false)

  async function handleSaveName() {
    const trimmed = nameVal.trim()
    if (!trimmed) return
    setSaving(true)
    const result = await updateProfile({ name: trimmed })
    setSaving(false)
    if (result.ok) {
      setEditingName(false)
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } else {
      alert('Failed to update name: ' + result.error)
    }
  }

  function handleCancelEdit() {
    setNameVal(user?.name || '')
    setEditingName(false)
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  async function handlePhotoChange(e) {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 5 * 1024 * 1024) { alert('Photo must be under 5 MB.'); return }

    setSaving(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const upload = await api.upload('/api/upload', fd)
      await updateProfile({ photo: upload.url })
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } catch (err) {
      alert('Upload failed: ' + err.message)
    } finally {
      setSaving(false)
    }
    e.target.value = ''
  }

  const photoSrc = user?.photo || user?.profilePhoto || null

  return (
    <div className="profile-layout">
      <div className="profile-hd">
        <button
          className="icon-btn icon-btn-light"
          onClick={() => navigate('/chat')}
          title="Back to chats"
        >
          ←
        </button>
        <h1>Profile</h1>
      </div>

      <div className="profile-scroll">
        <div className="profile-body">

          <div className="profile-banner">
            <div
              className="av-edit-wrap"
              onClick={() => !saving && fileRef.current?.click()}
              title="Change photo"
            >
              {photoSrc ? (
                <img
                  src={photoSrc}
                  alt="avatar"
                  style={{ width: 96, height: 96, borderRadius: '50%', objectFit: 'cover' }}
                />
              ) : (
                <Avatar name={user?.name || ''} size={96} />
              )}
              <div className="av-edit-overlay">{saving ? '⏳' : '📷'}</div>
            </div>
            <input
              ref={fileRef}
              type="file"
              accept="image/*"
              className="av-upload"
              onChange={handlePhotoChange}
            />
            <div className="profile-display-name">{user?.name}</div>
            <div className="profile-display-email">{user?.email}</div>
          </div>

          {saved && <div className="alert-success">✓ Profile updated successfully.</div>}

          <div className="info-card">
            <div className="info-card-hd">Account Info</div>

            <div className="info-row">
              <div className="info-row-left">
                <div className="info-row-label">Full Name</div>
                {editingName ? (
                  <input
                    className="edit-inline"
                    value={nameVal}
                    onChange={e => setNameVal(e.target.value)}
                    onKeyDown={e => {
                      if (e.key === 'Enter') handleSaveName()
                      if (e.key === 'Escape') handleCancelEdit()
                    }}
                    autoFocus
                  />
                ) : (
                  <div className="info-row-value">{user?.name}</div>
                )}
              </div>
              {editingName ? (
                <div style={{ display: 'flex', gap: 6 }}>
                  <button className="btn btn-primary btn-sm" onClick={handleSaveName} disabled={saving}>
                    {saving ? '…' : 'Save'}
                  </button>
                  <button className="btn btn-ghost btn-sm" onClick={handleCancelEdit}>Cancel</button>
                </div>
              ) : (
                <button
                  className="btn btn-ghost btn-sm"
                  onClick={() => { setNameVal(user?.name || ''); setEditingName(true) }}
                >
                  Edit
                </button>
              )}
            </div>

            <div className="info-row">
              <div className="info-row-left">
                <div className="info-row-label">Email Address</div>
                <div className="info-row-value">{user?.email}</div>
              </div>
            </div>
          </div>

          <div className="profile-actions">
            <button className="btn btn-danger btn-full" onClick={handleLogout}>
              🚪 Sign Out
            </button>
          </div>

        </div>
      </div>
    </div>
  )
}
