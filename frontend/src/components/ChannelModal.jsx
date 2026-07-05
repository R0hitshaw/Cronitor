import { useState } from 'react'
import { addChannel } from '../api/client'

const CHANNEL_TYPES = ['EMAIL', 'SLACK']

export default function ChannelModal({ jobId, jobName, onClose }) {
  const [type, setType] = useState('EMAIL')
  const [email, setEmail] = useState('')
  const [webhookUrl, setWebhookUrl] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)

  const buildConfig = () => {
    if (type === 'EMAIL') return JSON.stringify({ to: email })
    if (type === 'SLACK') return JSON.stringify({ webhook_url: webhookUrl })
    return '{}'
  }

  const handleSave = async () => {
    setError('')
    setSaving(true)
    try {
      await addChannel(jobId, { channelType: type, configJson: buildConfig() })
      setSaved(true)
    } catch (err) {
      setError(err.response?.data?.detail || 'Failed to add channel')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div style={{ background: '#fff', borderRadius: '16px', padding: '28px', width: '420px', boxShadow: '0 20px 60px rgba(0,0,0,0.2)' }}>

        {saved ? (
          <div style={{ textAlign: 'center', padding: '16px 0' }}>
            <div style={{ width: '48px', height: '48px', background: '#f0fdf4', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#22c55e" strokeWidth="2.5" strokeLinecap="round"><polyline points="20,6 9,17 4,12"/></svg>
            </div>
            <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginBottom: '6px' }}>Channel added!</h3>
            <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '20px' }}>You'll receive alerts for <strong>{jobName}</strong> via {type}.</p>
            <button onClick={onClose} style={{ background: '#4f46e5', color: '#fff', border: 'none', borderRadius: '9px', padding: '10px 24px', fontSize: '13px', fontWeight: '600', cursor: 'pointer' }}>
              Go to dashboard →
            </button>
          </div>
        ) : (
          <>
            <div style={{ marginBottom: '20px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginBottom: '4px' }}>Set up alerts</h3>
              <p style={{ fontSize: '13px', color: '#64748b' }}>Where should we send alerts for <strong>{jobName}</strong>?</p>
            </div>

            {/* Channel type tabs */}
            <div style={{ display: 'flex', gap: '3px', background: '#f1f5f9', borderRadius: '8px', padding: '3px', marginBottom: '20px' }}>
              {CHANNEL_TYPES.map(t => (
                <button key={t} onClick={() => setType(t)}
                  style={{ flex: 1, padding: '7px', borderRadius: '6px', border: 'none', cursor: 'pointer', fontSize: '12px', fontWeight: '600', background: type === t ? '#fff' : 'transparent', color: type === t ? '#4f46e5' : '#64748b', boxShadow: type === t ? '0 1px 3px rgba(0,0,0,0.08)' : 'none' }}>
                  {t === 'EMAIL' ? '📧 Email' : '💬 Slack'}
                </button>
              ))}
            </div>

            {/* EMAIL config */}
            {type === 'EMAIL' && (
              <div style={{ marginBottom: '20px' }}>
                <label style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '5px' }}>Email address</label>
                <input type="email" placeholder="ops@yourcompany.com" value={email} onChange={e => setEmail(e.target.value)}
                  style={{ width: '100%', border: '1.5px solid #e2e8f0', borderRadius: '9px', padding: '10px 14px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' }} />
              </div>
            )}

            {/* SLACK config */}
            {type === 'SLACK' && (
              <div style={{ marginBottom: '20px' }}>
                <label style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '5px' }}>Slack Webhook URL</label>
                <input type="url" placeholder="https://hooks.slack.com/services/..." value={webhookUrl} onChange={e => setWebhookUrl(e.target.value)}
                  style={{ width: '100%', border: '1.5px solid #e2e8f0', borderRadius: '9px', padding: '10px 14px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' }} />
                <p style={{ fontSize: '11px', color: '#94a3b8', marginTop: '5px' }}>Slack → Apps → Incoming Webhooks → Add new webhook</p>
              </div>
            )}

            {error && (
              <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', padding: '8px 12px', marginBottom: '16px', fontSize: '12px', color: '#b91c1c' }}>{error}</div>
            )}

            <div style={{ display: 'flex', gap: '10px' }}>
              <button onClick={handleSave} disabled={saving || (type === 'EMAIL' && !email) || (type === 'SLACK' && !webhookUrl)}
                style={{ flex: 1, background: '#4f46e5', color: '#fff', border: 'none', borderRadius: '9px', padding: '11px', fontSize: '13px', fontWeight: '600', cursor: 'pointer', opacity: saving ? 0.7 : 1 }}>
                {saving ? 'Saving...' : 'Add channel'}
              </button>
              <button onClick={onClose}
                style={{ padding: '11px 16px', background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '9px', fontSize: '13px', color: '#64748b', cursor: 'pointer' }}>
                Skip
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}