import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../api/AuthContext'

const features = [
  {
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#818cf8" strokeWidth="2" strokeLinecap="round">
        <circle cx="12" cy="12" r="9"/><polyline points="12,7 12,12 15,15"/>
      </svg>
    ),
    bg: 'rgba(99,102,241,0.12)',
    title: 'Missed run detection',
    desc: 'Alerts fire within 60s of a missed schedule',
  },
  {
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4ade80" strokeWidth="2" strokeLinecap="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
    ),
    bg: 'rgba(74,222,128,0.12)',
    title: 'Email & Slack alerts',
    desc: 'Multi-channel with Redis deduplication',
  },
  {
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fbbf24" strokeWidth="2" strokeLinecap="round">
        <polyline points="22,12 18,12 15,21 9,3 6,12 2,12"/>
      </svg>
    ),
    bg: 'rgba(251,191,36,0.12)',
    title: 'Duration anomaly detection',
    desc: 'Flags jobs running 2× longer than average',
  },
  {
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#f472b6" strokeWidth="2" strokeLinecap="round">
        <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
        <rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
      </svg>
    ),
    bg: 'rgba(244,114,182,0.12)',
    title: 'Real-time dashboard',
    desc: 'Live job grid with execution history & p95 charts',
  },
]

export default function Landing() {
  const [tab, setTab] = useState('login')
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login, register } = useAuth()
  const navigate = useNavigate()

  const set = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      if (tab === 'login') {
        await login(form.username, form.password)
      } else {
        await register(form.username, form.email, form.password)
      }
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.detail || 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', minHeight: '100vh', background: '#0f0f1a' }}>

      {/* Left — hero */}
      <div className="landing-left" style={{ padding: '48px', display: 'flex', flexDirection: 'column' }}>
        {/* Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '56px' }}>
          <div style={{ width: '34px', height: '34px', background: '#4f46e5', borderRadius: '9px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round">
              <circle cx="12" cy="12" r="9"/><polyline points="9,14 11,10 13,13 15,9"/>
              <circle cx="18" cy="6" r="3" fill="#f43f5e" stroke="none"/>
            </svg>
          </div>
          <span style={{ fontSize: '18px', fontWeight: '700', color: '#fff', letterSpacing: '-0.3px' }}>cronitor</span>
        </div>

        {/* Hero text */}
        <div style={{ flex: 1 }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', background: 'rgba(99,102,241,0.15)', border: '1px solid rgba(99,102,241,0.3)', color: '#a5b4fc', fontSize: '11px', fontWeight: '600', padding: '4px 10px', borderRadius: '20px', marginBottom: '20px', letterSpacing: '0.5px' }}>
            <span style={{ width: '6px', height: '6px', background: '#4ade80', borderRadius: '50%', display: 'inline-block' }}></span>
            Live monitoring · 99.9% uptime
          </div>

          <h1 style={{ fontSize: '38px', fontWeight: '800', color: '#fff', lineHeight: '1.15', letterSpacing: '-1px', marginBottom: '16px' }}>
            Never miss a<br /><span style={{ color: '#818cf8' }}>cron job</span> again.
          </h1>
          <p style={{ fontSize: '15px', color: '#64748b', lineHeight: '1.7', marginBottom: '40px', maxWidth: '380px' }}>
            Monitor every scheduled job across your infrastructure. Get instant alerts when runs are missed, slow, or failing.
          </p>

          {/* Features */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '48px' }}>
            {features.map((f, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                <div style={{ width: '34px', height: '34px', borderRadius: '9px', background: f.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                  {f.icon}
                </div>
                <div>
                  <p style={{ fontSize: '13px', fontWeight: '600', color: '#e2e8f0', marginBottom: '2px' }}>{f.title}</p>
                  <p style={{ fontSize: '12px', color: '#475569' }}>{f.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Stats */}
        <div style={{ display: 'flex', gap: '32px', paddingTop: '24px', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
          {[['12k+', 'jobs monitored'], ['99.9%', 'uptime'], ['<60s', 'alert latency']].map(([val, label]) => (
            <div key={label}>
              <p style={{ fontSize: '22px', fontWeight: '700', color: '#fff', letterSpacing: '-0.5px' }}>{val}</p>
              <p style={{ fontSize: '11px', color: '#475569' }}>{label}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Right — auth form */}
      <div className="landing-right" style={{ background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '48px', borderRadius: '0 0 0 0' }}>
{/*       <div style={{ background: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '48px', borderRadius: '0 0 0 0' }}> */}
        <div style={{ width: '100%', maxWidth: '360px' }}>
          <h2 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a', marginBottom: '4px' }}>
            {tab === 'login' ? 'Welcome back' : 'Create your account'}
          </h2>
          <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '24px' }}>
            {tab === 'login' ? 'Sign in to your Cronitor dashboard' : 'Start monitoring your cron jobs today'}
          </p>

          {/* Tab switcher */}
          <div style={{ display: 'flex', background: '#f1f5f9', borderRadius: '10px', padding: '3px', marginBottom: '24px' }}>
            {['login', 'register'].map(t => (
              <button key={t} onClick={() => { setTab(t); setError('') }}
                style={{ flex: 1, padding: '8px', borderRadius: '8px', border: 'none', cursor: 'pointer', fontSize: '13px', fontWeight: '600', transition: 'all .15s', background: tab === t ? '#fff' : 'transparent', color: tab === t ? '#4f46e5' : '#64748b', boxShadow: tab === t ? '0 1px 3px rgba(0,0,0,0.08)' : 'none' }}>
                {t === 'login' ? 'Sign in' : 'Create account'}
              </button>
            ))}
          </div>

          {error && (
            <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', padding: '10px 14px', marginBottom: '16px', fontSize: '13px', color: '#b91c1c' }}>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* Username */}
            <div style={{ marginBottom: '14px' }}>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '5px' }}>Username</label>
              <div style={{ position: 'relative' }}>
                <svg style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#9ca3af' }} width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                </svg>
                <input value={form.username} onChange={set('username')} required placeholder="yourname"
                  style={{ width: '100%', border: '1.5px solid #e2e8f0', borderRadius: '10px', padding: '10px 14px 10px 36px', fontSize: '14px', color: '#0f172a', outline: 'none', boxSizing: 'border-box' }} />
              </div>
            </div>

            {/* Email — register only */}
            {tab === 'register' && (
              <div style={{ marginBottom: '14px' }}>
                <label style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '5px' }}>Email address</label>
                <div style={{ position: 'relative' }}>
                  <svg style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#9ca3af' }} width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/>
                  </svg>
                  <input value={form.email} onChange={set('email')} type="email" required placeholder="you@company.com"
                    style={{ width: '100%', border: '1.5px solid #e2e8f0', borderRadius: '10px', padding: '10px 14px 10px 36px', fontSize: '14px', color: '#0f172a', outline: 'none', boxSizing: 'border-box' }} />
                </div>
              </div>
            )}

            {/* Password */}
            <div style={{ marginBottom: tab === 'login' ? '8px' : '20px' }}>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '5px' }}>Password</label>
              <div style={{ position: 'relative' }}>
                <svg style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#9ca3af' }} width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                <input value={form.password} onChange={set('password')} type="password" required placeholder="••••••••"
                  style={{ width: '100%', border: '1.5px solid #e2e8f0', borderRadius: '10px', padding: '10px 14px 10px 36px', fontSize: '14px', color: '#0f172a', outline: 'none', boxSizing: 'border-box' }} />
              </div>
            </div>

            {tab === 'login' && (
              <div style={{ textAlign: 'right', marginBottom: '20px' }}>
                <a href="#" style={{ fontSize: '12px', color: '#6366f1', fontWeight: '600', textDecoration: 'none' }}>Forgot password?</a>
              </div>
            )}

            <button type="submit" disabled={loading}
              style={{ width: '100%', background: loading ? '#818cf8' : '#4f46e5', color: '#fff', border: 'none', borderRadius: '10px', padding: '12px', fontSize: '14px', fontWeight: '600', cursor: loading ? 'not-allowed' : 'pointer', transition: 'background .15s' }}>
              {loading ? 'Please wait...' : tab === 'login' ? 'Sign in →' : 'Create account →'}
            </button>
          </form>

          <p style={{ textAlign: 'center', marginTop: '20px', fontSize: '12px', color: '#94a3b8' }}>
            {tab === 'login' ? "Don't have an account? " : 'Already have an account? '}
            <button onClick={() => { setTab(tab === 'login' ? 'register' : 'login'); setError('') }}
              style={{ background: 'none', border: 'none', color: '#6366f1', fontWeight: '600', cursor: 'pointer', fontSize: '12px' }}>
              {tab === 'login' ? 'Sign up free' : 'Sign in'}
            </button>
          </p>
        </div>
      </div>
    </div>
  )
}
