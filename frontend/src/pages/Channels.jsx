import { useState, useEffect } from 'react'
import { getJobs, getChannels, addChannel } from '../api/client'
import api from '../api/client'

export default function Channels() {
  const [jobs, setJobs] = useState([])
  const [selectedJob, setSelectedJob] = useState(null)
  const [channels, setChannels] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [type, setType] = useState('EMAIL')
  const [email, setEmail] = useState('')
  const [webhookUrl, setWebhookUrl] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getJobs().then(res => {
      setJobs(res.data)
      if (res.data.length > 0) {
        setSelectedJob(res.data[0])
      }
    }).finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (selectedJob) loadChannels()
  }, [selectedJob])

  const loadChannels = async () => {
    try {
      const res = await getChannels(selectedJob.id)
      setChannels(res.data)
    } catch {
      setChannels([])
    }
  }

  const buildConfig = () => {
    if (type === 'EMAIL') return JSON.stringify({ to: email })
    if (type === 'SLACK') return JSON.stringify({ webhook_url: webhookUrl })
    return '{}'
  }

  const handleAdd = async (e) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      await addChannel(selectedJob.id, { channelType: type, configJson: buildConfig() })
      setShowForm(false)
      setEmail('')
      setWebhookUrl('')
      loadChannels()
    } catch (err) {
      setError(err.response?.data?.detail || 'Failed to add channel')
    } finally {
      setSaving(false)
    }
  }

  const handleDeactivate = async (channelId) => {
    if (!confirm('Remove this channel?')) return
    await api.delete(`/jobs/${selectedJob.id}/channels/${channelId}`)
    loadChannels()
  }

  const channelIcon = (type) => {
    if (type === 'EMAIL') return '📧'
    if (type === 'SLACK') return '💬'
    if (type === 'SNS') return '📣'
    return '🔔'
  }

  const channelDetail = (ch) => {
    try {
      const config = JSON.parse(ch.configJson)
      if (ch.channelType === 'EMAIL') return config.to
      if (ch.channelType === 'SLACK') return config.webhook_url?.substring(0, 40) + '...'
    } catch { return '' }
  }

  if (loading) return <div style={{ flex: 1, padding: '28px', color: '#94a3b8' }}>Loading...</div>

  return (
    <div style={{ flex: 1, display: 'flex', overflow: 'hidden', background: '#f8fafc' }}>

      {/* Left — job list */}
      <div style={{ width: '240px', borderRight: '1px solid #e2e8f0', background: '#fff', overflowY: 'auto', flexShrink: 0 }}>
        <div style={{ padding: '16px', borderBottom: '1px solid #f1f5f9' }}>
          <p style={{ fontSize: '12px', fontWeight: '700', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Jobs</p>
        </div>
        {jobs.length === 0 ? (
          <p style={{ padding: '16px', fontSize: '13px', color: '#94a3b8' }}>No jobs registered yet</p>
        ) : jobs.map(job => (
          <div key={job.id} onClick={() => { setSelectedJob(job); setShowForm(false) }}
            style={{ padding: '12px 16px', cursor: 'pointer', borderBottom: '1px solid #f8fafc', background: selectedJob?.id === job.id ? '#eef2ff' : 'transparent', transition: 'background .15s' }}>
            <p style={{ fontSize: '13px', fontWeight: '600', color: selectedJob?.id === job.id ? '#4f46e5' : '#0f172a', marginBottom: '2px' }}>{job.name}</p>
            <p style={{ fontSize: '11px', color: '#94a3b8', fontFamily: 'monospace' }}>{job.slug}</p>
          </div>
        ))}
      </div>

      {/* Right — channels for selected job */}
      <div style={{ flex: 1, padding: '28px', overflowY: 'auto' }}>
        {!selectedJob ? (
          <div style={{ textAlign: 'center', padding: '60px', color: '#94a3b8' }}>
            <p style={{ fontSize: '32px', marginBottom: '8px' }}>📡</p>
            <p style={{ fontWeight: '600' }}>Select a job to manage its channels</p>
          </div>
        ) : (
          <>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
              <div>
                <h1 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a' }}>{selectedJob.name}</h1>
                <p style={{ fontSize: '12px', color: '#94a3b8', marginTop: '2px' }}>Notification channels</p>
              </div>
              <button onClick={() => setShowForm(!showForm)}
                style={{ background: '#4f46e5', color: '#fff', border: 'none', borderRadius: '9px', padding: '9px 16px', fontSize: '13px', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                Add channel
              </button>
            </div>

            {/* Add channel form */}
            {showForm && (
              <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '14px', padding: '20px 24px', marginBottom: '20px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
                <h2 style={{ fontSize: '14px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>Add notification channel</h2>

                <div style={{ display: 'flex', gap: '3px', background: '#f1f5f9', borderRadius: '8px', padding: '3px', marginBottom: '16px', width: 'fit-content' }}>
                  {['EMAIL', 'SLACK'].map(t => (
                    <button key={t} onClick={() => setType(t)}
                      style={{ padding: '6px 16px', borderRadius: '6px', border: 'none', cursor: 'pointer', fontSize: '12px', fontWeight: '600', background: type === t ? '#fff' : 'transparent', color: type === t ? '#4f46e5' : '#64748b', boxShadow: type === t ? '0 1px 3px rgba(0,0,0,0.08)' : 'none' }}>
                      {t === 'EMAIL' ? '📧 Email' : '💬 Slack'}
                    </button>
                  ))}
                </div>

                <form onSubmit={handleAdd}>
                  {type === 'EMAIL' && (
                    <div style={{ marginBottom: '16px' }}>
                      <label style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '5px' }}>Email address</label>
                      <input type="email" value={email} onChange={e => setEmail(e.target.value)} required placeholder="ops@yourcompany.com"
                        style={{ width: '100%', maxWidth: '380px', border: '1.5px solid #e2e8f0', borderRadius: '9px', padding: '9px 14px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' }} />
                    </div>
                  )}
                  {type === 'SLACK' && (
                    <div style={{ marginBottom: '16px' }}>
                      <label style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '5px' }}>Slack Webhook URL</label>
                      <input type="url" value={webhookUrl} onChange={e => setWebhookUrl(e.target.value)} required placeholder="https://hooks.slack.com/services/..."
                        style={{ width: '100%', maxWidth: '460px', border: '1.5px solid #e2e8f0', borderRadius: '9px', padding: '9px 14px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' }} />
                    </div>
                  )}
                  {error && <p style={{ fontSize: '12px', color: '#b91c1c', marginBottom: '12px' }}>{error}</p>}
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button type="submit" disabled={saving}
                      style={{ background: '#4f46e5', color: '#fff', border: 'none', borderRadius: '8px', padding: '9px 20px', fontSize: '13px', fontWeight: '600', cursor: 'pointer', opacity: saving ? 0.7 : 1 }}>
                      {saving ? 'Saving...' : 'Add channel'}
                    </button>
                    <button type="button" onClick={() => setShowForm(false)}
                      style={{ background: 'none', border: 'none', color: '#64748b', fontSize: '13px', cursor: 'pointer' }}>
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* Channels list */}
            {channels.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '48px', color: '#94a3b8', background: '#fff', borderRadius: '14px', border: '1px solid #e2e8f0' }}>
                <p style={{ fontSize: '28px', marginBottom: '8px' }}>🔕</p>
                <p style={{ fontWeight: '600', fontSize: '14px' }}>No channels configured</p>
                <p style={{ fontSize: '12px', marginTop: '4px' }}>Add a channel so you get alerted when this job fails or is missed</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {channels.map(ch => (
                  <div key={ch.id} style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '14px 18px', display: 'flex', alignItems: 'center', gap: '14px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
                    <div style={{ width: '36px', height: '36px', background: '#eef2ff', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px', flexShrink: 0 }}>
                      {channelIcon(ch.channelType)}
                    </div>
                    <div style={{ flex: 1 }}>
                      <p style={{ fontSize: '13px', fontWeight: '600', color: '#0f172a' }}>{ch.channelType}</p>
                      <p style={{ fontSize: '12px', color: '#64748b', marginTop: '1px' }}>{channelDetail(ch)}</p>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <span style={{ fontSize: '11px', fontWeight: '600', background: '#f0fdf4', color: '#15803d', padding: '3px 8px', borderRadius: '20px' }}>Active</span>
                      <button onClick={() => handleDeactivate(ch.id)}
                        style={{ fontSize: '11px', fontWeight: '600', color: '#ef4444', background: '#fff', border: '1px solid #fee2e2', borderRadius: '6px', padding: '4px 10px', cursor: 'pointer' }}>
                        Remove
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}