import { useState, useEffect } from 'react'
import { useAuth } from '../api/AuthContext'
import api from '../api/client'

export default function Setting() {
  const { user } = useAuth()
  const [tab, setTab] = useState('apikeys')

  // API Keys state
  const [apiKeys, setApiKeys] = useState([])
  const [newKeyName, setNewKeyName] = useState('')
  const [generatedKey, setGeneratedKey] = useState(null)
  const [keyLoading, setKeyLoading] = useState(false)
  const [keyError, setKeyError] = useState('')
  const [copied, setCopied] = useState(false)

  // Account state
  const [password, setPassword] = useState({ current: '', next: '', confirm: '' })
  const [accountMsg, setAccountMsg] = useState('')
  const [accountError, setAccountError] = useState('')
  const [accountLoading, setAccountLoading] = useState(false)

  const loadKeys = async () => {
    try {
      const res = await api.get('/settings/api-keys')
      setApiKeys(res.data)
    } catch {
      setApiKeys([])
    }
  }

  useEffect(() => { loadKeys() }, [])

  const handleGenerateKey = async (e) => {
    e.preventDefault()
    setKeyError('')
    setGeneratedKey(null)
    setKeyLoading(true)
    try {
      const res = await api.post('/settings/api-keys', { name: newKeyName })
      setGeneratedKey(res.data.rawKey)
      setNewKeyName('')
      loadKeys()
    } catch (err) {
      setKeyError(err.response?.data?.detail || 'Failed to generate key')
    } finally {
      setKeyLoading(false)
    }
  }

  const handleRevokeKey = async (id) => {
    if (!confirm('Revoke this API key? This cannot be undone.')) return
    await api.delete(`/settings/api-keys/${id}`)
    loadKeys()
  }

  const handleCopy = () => {
    navigator.clipboard.writeText(generatedKey)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleChangePassword = async (e) => {
    e.preventDefault()
    setAccountMsg('')
    setAccountError('')
    if (password.next !== password.confirm) {
      setAccountError('New passwords do not match')
      return
    }
    setAccountLoading(true)
    try {
      await api.post('/settings/change-password', {
        currentPassword: password.current,
        newPassword: password.next,
      })
      setAccountMsg('Password changed successfully')
      setPassword({ current: '', next: '', confirm: '' })
    } catch (err) {
      setAccountError(err.response?.data?.detail || 'Failed to change password')
    } finally {
      setAccountLoading(false)
    }
  }

  const fmt = (ts) => ts ? new Date(ts).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'

  return (
    <div style={{ flex: 1, padding: '28px', overflowY: 'auto', background: '#f8fafc' }}>

      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '20px', fontWeight: '700', color: '#0f172a' }}>Settings</h1>
        <p style={{ fontSize: '12px', color: '#94a3b8', marginTop: '2px' }}>Manage your account and API keys</p>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '0', borderBottom: '1px solid #e2e8f0', marginBottom: '28px' }}>
        {[['apikeys', '🔑 API Keys'], ['account', '👤 Account']].map(([t, label]) => (
          <button key={t} onClick={() => setTab(t)}
            style={{ padding: '10px 20px', border: 'none', borderBottom: tab === t ? '2px solid #4f46e5' : '2px solid transparent', background: 'none', fontSize: '13px', fontWeight: '600', color: tab === t ? '#4f46e5' : '#64748b', cursor: 'pointer', marginBottom: '-1px' }}>
            {label}
          </button>
        ))}
      </div>

      {/* API Keys tab */}
      {tab === 'apikeys' && (
        <div>
          <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '14px', padding: '20px 24px', marginBottom: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <h2 style={{ fontSize: '14px', fontWeight: '700', color: '#0f172a', marginBottom: '4px' }}>Generate API Key</h2>
            <p style={{ fontSize: '12px', color: '#64748b', marginBottom: '16px' }}>
              Use API keys to authenticate ping requests from your cron jobs via the <code style={{ background: '#f1f5f9', padding: '1px 5px', borderRadius: '4px', fontSize: '11px' }}>X-API-Key</code> header.
            </p>

            {/* Generated key banner */}
            {generatedKey && (
              <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '10px', padding: '14px 16px', marginBottom: '16px' }}>
                <p style={{ fontSize: '12px', fontWeight: '600', color: '#15803d', marginBottom: '8px' }}>
                  ✓ Key generated — copy it now. It won't be shown again.
                </p>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <code style={{ flex: 1, background: '#fff', border: '1px solid #d1fae5', borderRadius: '7px', padding: '8px 12px', fontSize: '12px', color: '#065f46', wordBreak: 'break-all' }}>
                    {generatedKey}
                  </code>
                  <button onClick={handleCopy}
                    style={{ background: copied ? '#22c55e' : '#4f46e5', color: '#fff', border: 'none', borderRadius: '7px', padding: '8px 14px', fontSize: '12px', fontWeight: '600', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                    {copied ? 'Copied!' : 'Copy'}
                  </button>
                </div>
              </div>
            )}

            <form onSubmit={handleGenerateKey} style={{ display: 'flex', gap: '10px' }}>
              <input value={newKeyName} onChange={e => setNewKeyName(e.target.value)} required
                placeholder="e.g. Production server"
                style={{ flex: 1, border: '1.5px solid #e2e8f0', borderRadius: '9px', padding: '9px 14px', fontSize: '13px', outline: 'none' }} />
              <button type="submit" disabled={keyLoading}
                style={{ background: '#4f46e5', color: '#fff', border: 'none', borderRadius: '9px', padding: '9px 18px', fontSize: '13px', fontWeight: '600', cursor: 'pointer', opacity: keyLoading ? 0.7 : 1 }}>
                {keyLoading ? 'Generating...' : 'Generate key'}
              </button>
            </form>
            {keyError && <p style={{ fontSize: '12px', color: '#b91c1c', marginTop: '8px' }}>{keyError}</p>}
          </div>

          {/* Active keys list */}
          <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '14px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <div style={{ padding: '14px 20px', borderBottom: '1px solid #f1f5f9' }}>
              <h2 style={{ fontSize: '13px', fontWeight: '700', color: '#0f172a' }}>Active keys</h2>
            </div>
            {apiKeys.length === 0 ? (
              <div style={{ padding: '32px', textAlign: 'center', color: '#94a3b8', fontSize: '13px' }}>
                No API keys yet. Generate one above.
              </div>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ fontSize: '11px', fontWeight: '600', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    <th style={{ padding: '10px 20px', textAlign: 'left' }}>Name</th>
                    <th style={{ padding: '10px 20px', textAlign: 'left' }}>Created</th>
                    <th style={{ padding: '10px 20px', textAlign: 'left' }}>Last used</th>
                    <th style={{ padding: '10px 20px', textAlign: 'left' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {apiKeys.map((key, i) => (
                    <tr key={key.id} style={{ borderTop: '1px solid #f8fafc', fontSize: '13px' }}>
                      <td style={{ padding: '12px 20px', color: '#0f172a', fontWeight: '500' }}>{key.name}</td>
                      <td style={{ padding: '12px 20px', color: '#64748b' }}>{fmt(key.createdAt)}</td>
                      <td style={{ padding: '12px 20px', color: '#64748b' }}>{key.lastUsedAt ? fmt(key.lastUsedAt) : 'Never'}</td>
                      <td style={{ padding: '12px 20px' }}>
                        <button onClick={() => handleRevokeKey(key.id)}
                          style={{ fontSize: '11px', fontWeight: '600', color: '#ef4444', background: '#fff', border: '1px solid #fee2e2', borderRadius: '6px', padding: '4px 10px', cursor: 'pointer' }}>
                          Revoke
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* Usage example */}
          <div style={{ background: '#0f0f1a', borderRadius: '12px', padding: '16px 20px', marginTop: '20px' }}>
            <p style={{ fontSize: '11px', fontWeight: '600', color: '#64748b', marginBottom: '10px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Usage example</p>
            <code style={{ fontSize: '12px', color: '#a5b4fc', lineHeight: '1.8', display: 'block' }}>
              curl -X POST https://yourapp.com/api/ping/your-job-slug/finish \<br />
              &nbsp;&nbsp;-H "X-API-Key: your-api-key-here"
            </code>
          </div>
        </div>
      )}

      {/* Account tab */}
      {tab === 'account' && (
        <div>
          {/* User info card */}
          <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '14px', padding: '20px 24px', marginBottom: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <h2 style={{ fontSize: '14px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>Account info</h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              {[['Username', user?.username], ['Email', user?.email]].map(([label, val]) => (
                <div key={label}>
                  <p style={{ fontSize: '11px', fontWeight: '600', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '4px' }}>{label}</p>
                  <p style={{ fontSize: '14px', color: '#0f172a', fontWeight: '500' }}>{val}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Change password */}
          <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '14px', padding: '20px 24px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <h2 style={{ fontSize: '14px', fontWeight: '700', color: '#0f172a', marginBottom: '4px' }}>Change password</h2>
            <p style={{ fontSize: '12px', color: '#64748b', marginBottom: '20px' }}>Must be at least 8 characters.</p>

            {accountMsg && <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '8px', padding: '10px 14px', marginBottom: '16px', fontSize: '12px', color: '#15803d' }}>{accountMsg}</div>}
            {accountError && <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', padding: '10px 14px', marginBottom: '16px', fontSize: '12px', color: '#b91c1c' }}>{accountError}</div>}

            <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {[['Current password', 'current'], ['New password', 'next'], ['Confirm new password', 'confirm']].map(([label, key]) => (
                <div key={key}>
                  <label style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '5px' }}>{label}</label>
                  <input type="password" value={password[key]} onChange={e => setPassword({ ...password, [key]: e.target.value })} required
                    style={{ width: '100%', border: '1.5px solid #e2e8f0', borderRadius: '9px', padding: '10px 14px', fontSize: '13px', outline: 'none', boxSizing: 'border-box', maxWidth: '400px' }} />
                </div>
              ))}
              <div>
                <button type="submit" disabled={accountLoading}
                  style={{ background: '#4f46e5', color: '#fff', border: 'none', borderRadius: '9px', padding: '10px 24px', fontSize: '13px', fontWeight: '600', cursor: 'pointer', opacity: accountLoading ? 0.7 : 1 }}>
                  {accountLoading ? 'Updating...' : 'Update password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}