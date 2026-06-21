import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../api/AuthContext'

const navItems = [
  {
    section: 'Main',
    items: [
      { to: '/dashboard', label: 'Dashboard', icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg> },
      { to: '/alerts', label: 'Alerts', icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>, badge: true },
    ]
  },
  {
    section: 'Config',
    items: [
      { to: '/channels', label: 'Channels', icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.15 13a19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 3.06 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 21 16.92z"/></svg> },
      { to: '/settings', label: 'Settings', icon: <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="12" cy="12" r="3"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M4.93 4.93a10 10 0 0 0 0 14.14"/></svg> },
    ]
  }
]

export default function Sidebar() {
  const { pathname } = useLocation()
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  const initials = user?.username?.slice(0, 2).toUpperCase() || 'U'

  return (
    <aside style={{ width: '210px', background: '#0f0f1a', display: 'flex', flexDirection: 'column', padding: '18px 12px', flexShrink: 0, minHeight: '100vh' }}>

      {/* Logo */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '9px', padding: '8px 10px', marginBottom: '20px' }}>
        <div style={{ width: '30px', height: '30px', background: '#4f46e5', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round">
            <circle cx="12" cy="12" r="9"/><polyline points="9,14 11,10 13,13 15,9"/>
            <circle cx="18" cy="6" r="3" fill="#f43f5e" stroke="none"/>
          </svg>
        </div>
        <span style={{ fontSize: '16px', fontWeight: '700', color: '#fff', letterSpacing: '-0.3px' }}>cronitor</span>
      </div>

      {/* Nav */}
      {navItems.map(group => (
        <div key={group.section} style={{ marginBottom: '8px' }}>
          <p style={{ fontSize: '10px', fontWeight: '700', color: '#334155', letterSpacing: '1px', padding: '8px 10px 4px', textTransform: 'uppercase' }}>{group.section}</p>
          {group.items.map(item => {
            const active = pathname === item.to || pathname.startsWith(item.to + '/')
            return (
              <Link key={item.to} to={item.to} style={{
                display: 'flex', alignItems: 'center', gap: '9px', padding: '8px 10px',
                borderRadius: '8px', fontSize: '13px', fontWeight: '500', textDecoration: 'none',
                color: active ? '#a5b4fc' : '#64748b',
                background: active ? 'rgba(99,102,241,0.15)' : 'transparent',
                marginBottom: '2px', transition: 'all .15s'
              }}>
                {item.icon}
                {item.label}
              </Link>
            )
          })}
        </div>
      ))}

      {/* User card at bottom */}
      <div style={{ marginTop: 'auto' }}>
        <div style={{ background: 'rgba(99,102,241,0.08)', borderRadius: '10px', padding: '10px 12px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{ width: '30px', height: '30px', borderRadius: '50%', background: '#4f46e5', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: '700', color: '#fff', flexShrink: 0 }}>
            {initials}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <p style={{ fontSize: '12px', fontWeight: '600', color: '#e2e8f0', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{user?.username}</p>
            <p style={{ fontSize: '10px', color: '#475569', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{user?.email}</p>
          </div>
          <button onClick={handleLogout} title="Sign out"
            style={{ background: 'none', border: 'none', color: '#475569', cursor: 'pointer', padding: '2px', display: 'flex', alignItems: 'center' }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16,17 21,12 16,7"/><line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
          </button>
        </div>
      </div>
    </aside>
  )
}
