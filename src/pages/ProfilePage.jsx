import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth }     from '../context/AuthContext'
import Avatar          from '../components/Avatar'

export default function ProfilePage() {
  const { user, logout, updateProfile } = useAuth()
  const navigate   = useNavigate()
  const fileRef    = useRef(null)

  const [editingName, setEditingName] = useState(false)
  const [nameVal,     setNameVal]     = useState(user?.name || '')
  const [saved,       setSaved]       = useState(false)

  function handleSaveName() {
    const trimmed = nameVal.trim()
    if (!trimmed) return
    updateProfile({ name: trimmed })
    setEditingName(false)
    setSaved(true)
    setTimeout(() => setSaved(false), 2500)
  }

  function handleCancelEdit() {
    setNameVal(user?.name || '')
    setEditingName(false)
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  /* Avatar photo upload (UI only — stores as data URL in session) */
  function handlePhotoChange(e) {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 5 * 1024 * 1024) { alert('Photo must be under 5 MB.'); return }
    const reader = new FileReader()
    reader.onload = () => updateProfile({ photo: reader.result })
    reader.readAsDataURL(file)
    e.target.value = ''
  }

  return (
    <div className="profile-layout">
      {/* Header */}
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

          {/* Banner with avatar */}
          <div className="profile-banner">
            <div
              className="av-edit-wrap"
              onClick={() => fileRef.current?.click()}
              title="Change photo"
            >
              {user?.photo ? (
                <img
                  src={user.photo}
                  alt="avatar"
                  style={{ width: 96, height: 96, borderRadius: '50%', objectFit: 'cover' }}
                />
              ) : (
                <Avatar name={user?.name || ''} size={96} />
              )}
              <div className="av-edit-overlay">📷</div>
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

          {/* Info card */}
          <div className="info-card">
            <div className="info-card-hd">Account Info</div>

            {/* Name row */}
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
                  <button className="btn btn-primary btn-sm" onClick={handleSaveName}>Save</button>
                  <button className="btn btn-ghost   btn-sm" onClick={handleCancelEdit}>Cancel</button>
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

            {/* Email row */}
            <div className="info-row">
              <div className="info-row-left">
                <div className="info-row-label">Email Address</div>
                <div className="info-row-value">{user?.email}</div>
              </div>
            </div>
          </div>

          {/* Actions */}
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
