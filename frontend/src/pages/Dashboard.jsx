import { useEffect, useState } from 'react'
import ChannelModal from '../components/ChannelModal'
import { Link, useNavigate } from 'react-router-dom'
import { getJobs, registerJob, deleteJob, pingStart, pingFinish, pingFail } from '../api/client'
import StatusBadge from '../components/StatusBadge'

const statusBorderColor = {
  HEALTHY: '#22c55e', RUNNING: '#3b82f6', MISSED: '#ef4444', FAILED: '#f59e0b', PENDING: '#94a3b8',
}
const emptyForm = { name: '', slug: '', cronExpression: '', gracePeriodSeconds: 300 }

export default function Dashboard() {
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [filter, setFilter] = useState('ALL')
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState('')
  const [pinging, setPinging] = useState(null)
  const [activity, setActivity] = useState([])
  const [channelModal, setChannelModal] = useState(null)
  const navigate = useNavigate()

  const load = async () => {
    try {
      const res = await getJobs()
      const data = res.data
      setJobs(data)
      setActivity([...data].filter(j => j.lastPingAt).sort((a,b) => new Date(b.lastPingAt)-new Date(a.lastPingAt)).slice(0,5))
    } finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleSubmit = async (e) => {
    e.preventDefault(); setError('')
    try {
      const res = await registerJob({ ...form, gracePeriodSeconds: Number(form.gracePeriodSeconds) })

//       console.log("Backend Response Payload:", res.data)
      const newJob = res.data  // capture the response
      setShowForm(false)
      setForm(emptyForm)
      load()
      setChannelModal({ id: newJob.id, name: newJob.name })
    } catch (err) { setError(err.response?.data?.detail || 'Failed to register job') }
  }

  const handleDelete = async (id, name) => {
    if (!confirm(`Delete "${name}"?`)) return
    await deleteJob(id); load()
  }

  const handlePing = async (type, slug) => {
    setPinging(slug + type)
    try {
      if (type === 'start') await pingStart(slug)
      if (type === 'finish') await pingFinish(slug)
      if (type === 'fail') await pingFail(slug)
      await load()
    } finally { setPinging(null) }
  }

  const fmt = (ts) => {
    if (!ts) return '—'
    const diff = Date.now() - new Date(ts)
    const mins = Math.floor(diff / 60000)
    if (mins < 1) return 'just now'
    if (mins < 60) return `${mins}m ago`
    const hrs = Math.floor(mins / 60)
    if (hrs < 24) return `${hrs}h ago`
    return `${Math.floor(hrs / 24)}d ago`
  }

  const filtered = filter === 'ALL' ? jobs
    : filter === 'HEALTHY' ? jobs.filter(j => j.status === 'HEALTHY')
    : jobs.filter(j => ['MISSED','FAILED'].includes(j.status))

  const total = jobs.length
  const healthy = jobs.filter(j => j.status === 'HEALTHY').length
  const missed = jobs.filter(j => j.status === 'MISSED').length
  const failed = jobs.filter(j => j.status === 'FAILED').length
  const running = jobs.filter(j => j.status === 'RUNNING').length
  const issues = missed + failed

  const actIconData = (status) => {
    if (status === 'HEALTHY') return { bg: '#f0fdf4', color: '#22c55e' }
    if (status === 'MISSED')  return { bg: '#fef2f2', color: '#ef4444' }
    if (status === 'RUNNING') return { bg: '#eff6ff', color: '#3b82f6' }
    if (status === 'FAILED')  return { bg: '#fffbeb', color: '#f59e0b' }
    return { bg: '#f8fafc', color: '#94a3b8' }
  }

  return (
    <div style={{ flex:1, padding:'28px', overflowY:'auto', background:'#f8fafc' }}>

      {issues > 0 && (
        <div style={{ background:'#fef2f2', border:'1px solid #fecaca', borderRadius:'10px', padding:'11px 16px', display:'flex', alignItems:'center', gap:'10px', marginBottom:'24px' }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ef4444" strokeWidth="2" strokeLinecap="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <p style={{ fontSize:'13px', color:'#991b1b', fontWeight:'500' }}>
            {issues} job{issues > 1 ? 's' : ''} need attention.{' '}
            <Link to="/alerts" style={{ color:'#dc2626', fontWeight:'700', textDecoration:'none' }}>View alerts →</Link>
          </p>
        </div>
      )}

      <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:'24px' }}>
        <div>
          <h1 style={{ fontSize:'20px', fontWeight:'700', color:'#0f172a' }}>Dashboard</h1>
          <p style={{ fontSize:'12px', color:'#94a3b8', marginTop:'2px' }}>{new Date().toLocaleDateString('en-US', { weekday:'long', month:'long', day:'numeric' })}</p>
        </div>
        <button onClick={() => setShowForm(!showForm)} style={{ background:'#4f46e5', color:'#fff', border:'none', borderRadius:'9px', padding:'9px 16px', fontSize:'13px', fontWeight:'600', cursor:'pointer', display:'flex', alignItems:'center', gap:'6px' }}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Register job
        </button>
      </div>

      {showForm && (
        <div style={{ background:'#fff', border:'1px solid #e2e8f0', borderRadius:'14px', padding:'20px 24px', marginBottom:'24px', boxShadow:'0 2px 8px rgba(0,0,0,0.05)' }}>
          <h2 style={{ fontSize:'14px', fontWeight:'700', color:'#0f172a', marginBottom:'16px' }}>Register a new job</h2>
          {error && <div style={{ background:'#fef2f2', border:'1px solid #fecaca', borderRadius:'8px', padding:'8px 12px', marginBottom:'12px', fontSize:'12px', color:'#b91c1c' }}>{error}</div>}
          <form onSubmit={handleSubmit} style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'12px' }}>
            {[['Name','name','text','Nightly billing job'],['Slug','slug','text','nightly-billing-job'],['Cron Expression','cronExpression','text','0 0 2 * * *'],['Grace Period (s)','gracePeriodSeconds','number','300']].map(([label,key,type,ph]) => (
              <div key={key}>
                <label style={{ fontSize:'11px', fontWeight:'600', color:'#374151', display:'block', marginBottom:'4px' }}>{label}</label>
                <input type={type} placeholder={ph} value={form[key]} onChange={e => setForm({...form,[key]:e.target.value})} required style={{ width:'100%', border:'1.5px solid #e2e8f0', borderRadius:'8px', padding:'8px 12px', fontSize:'13px', outline:'none', boxSizing:'border-box' }}/>
              </div>
            ))}
            <div style={{ gridColumn:'span 2', display:'flex', gap:'10px', paddingTop:'4px' }}>
              <button type="submit" style={{ background:'#4f46e5', color:'#fff', border:'none', borderRadius:'8px', padding:'9px 20px', fontSize:'13px', fontWeight:'600', cursor:'pointer' }}>Register</button>
              <button type="button" onClick={() => setShowForm(false)} style={{ background:'none', border:'none', color:'#64748b', fontSize:'13px', cursor:'pointer' }}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:'12px', marginBottom:'28px' }}>
        {[
          { label:'Total jobs', value:total, sub:`${running} currently running`, color:'#0f172a' },
          { label:'Healthy', value:healthy, sub:total ? `${Math.round(healthy/total*100)}% success rate` : '—', color:'#22c55e' },
          { label:'Missed', value:missed, sub:missed > 0 ? 'needs attention' : 'all clear', color:missed > 0 ? '#ef4444' : '#22c55e' },
          { label:'Active issues', value:issues, sub:`${failed} failed · ${missed} missed`, color:issues > 0 ? '#f59e0b' : '#22c55e' },
        ].map(card => (
          <div key={card.label} style={{ background:'#fff', border:'1px solid #f1f5f9', borderRadius:'12px', padding:'16px 18px', boxShadow:'0 1px 3px rgba(0,0,0,0.04)' }}>
            <p style={{ fontSize:'11px', fontWeight:'600', color:'#94a3b8', textTransform:'uppercase', letterSpacing:'0.5px', marginBottom:'8px' }}>{card.label}</p>
            <p style={{ fontSize:'28px', fontWeight:'700', color:card.color, letterSpacing:'-1px' }}>{card.value}</p>
            <p style={{ fontSize:'11px', color:'#94a3b8', marginTop:'3px' }}>{card.sub}</p>
          </div>
        ))}
      </div>

      <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:'14px' }}>
        <h2 style={{ fontSize:'14px', fontWeight:'700', color:'#0f172a' }}>All jobs</h2>
        <div style={{ display:'flex', gap:'3px', background:'#f1f5f9', borderRadius:'8px', padding:'3px' }}>
          {[['ALL','All'],['HEALTHY','Healthy'],['FAILING','Failing']].map(([f,label]) => (
            <button key={f} onClick={() => setFilter(f)} style={{ padding:'5px 12px', borderRadius:'6px', border:'none', cursor:'pointer', fontSize:'11px', fontWeight:'600', background:filter===f?'#fff':'transparent', color:filter===f?'#4f46e5':'#64748b', boxShadow:filter===f?'0 1px 3px rgba(0,0,0,0.08)':'none' }}>{label}</button>
          ))}
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign:'center', padding:'60px', color:'#94a3b8' }}>Loading...</div>
      ) : filtered.length === 0 ? (
        <div style={{ textAlign:'center', padding:'60px', color:'#94a3b8' }}>
          <p style={{ fontSize:'32px', marginBottom:'8px' }}>⏱</p>
          <p style={{ fontWeight:'600', fontSize:'14px' }}>{filter !== 'ALL' ? 'No jobs match this filter' : 'No jobs registered yet'}</p>
          <p style={{ fontSize:'12px', marginTop:'4px' }}>{filter !== 'ALL' ? 'Try switching the filter above' : 'Click "Register job" to get started'}</p>
        </div>
      ) : (
        <div style={{ display:'grid', gridTemplateColumns:'repeat(2,1fr)', gap:'12px', marginBottom:'28px' }}>
          {filtered.map(job => {
            const bc = statusBorderColor[job.status] || '#94a3b8'
            return (
              <div key={job.id} style={{ background:'#fff', border:'1px solid #f1f5f9', borderLeft:`3px solid ${bc}`, borderRadius:'12px', padding:'16px 18px', boxShadow:'0 1px 3px rgba(0,0,0,0.04)', transition:'box-shadow .15s' }}
                onMouseEnter={e => e.currentTarget.style.boxShadow='0 4px 12px rgba(0,0,0,0.08)'}
                onMouseLeave={e => e.currentTarget.style.boxShadow='0 1px 3px rgba(0,0,0,0.04)'}>
                <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', marginBottom:'10px' }}>
                  <div>
                    <p style={{ fontSize:'13px', fontWeight:'600', color:'#0f172a' }}>{job.name}</p>
                    <p style={{ fontSize:'11px', color:'#94a3b8', fontFamily:'monospace', marginTop:'2px' }}>{job.slug}</p>
                  </div>
                  <StatusBadge status={job.status} />
                </div>
                <div style={{ display:'flex', justifyContent:'space-between', fontSize:'11px', color:'#94a3b8', marginBottom:'10px' }}>
                  <span style={{ fontFamily:'monospace' }}>{job.cronExpression}</span>
                  <span>Last: {fmt(job.lastPingAt)}</span>
                </div>
                <div style={{ background:'#f1f5f9', borderRadius:'4px', height:'4px', marginBottom:'12px', overflow:'hidden' }}>
                  <div style={{ height:'4px', borderRadius:'4px', width:job.status==='HEALTHY'?'92%':job.status==='RUNNING'?'60%':'35%', background:bc, transition:'width .3s' }}/>
                </div>
                <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between' }}>
                  <span style={{ fontSize:'11px', color:'#cbd5e1' }}>Next: {fmt(job.nextExpectedAt)}</span>
                  <div style={{ display:'flex', gap:'5px' }}>
                    {['start','finish','fail'].map(type => (
                      <button key={type} onClick={() => handlePing(type, job.slug)} disabled={pinging===job.slug+type}
                        style={{ fontSize:'10px', fontWeight:'600', padding:'3px 7px', borderRadius:'6px', border:'1px solid #e2e8f0', background:'#fff', color:'#64748b', cursor:'pointer', opacity:pinging===job.slug+type?0.5:1 }}>
                        {pinging===job.slug+type?'...':type}
                      </button>
                    ))}
                    <button onClick={() => navigate(`/jobs/${job.id}`)} style={{ fontSize:'10px', fontWeight:'600', padding:'3px 8px', borderRadius:'6px', border:'1px solid #e0e7ff', background:'#eef2ff', color:'#4f46e5', cursor:'pointer' }}>Details →</button>
                    <button onClick={() => handleDelete(job.id, job.name)} style={{ fontSize:'10px', fontWeight:'600', padding:'3px 8px', borderRadius:'6px', border:'1px solid #fee2e2', background:'#fff', color:'#ef4444', cursor:'pointer' }}>✕</button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {activity.length > 0 && (
        <>
          <h2 style={{ fontSize:'14px', fontWeight:'700', color:'#0f172a', marginBottom:'12px' }}>Recent activity</h2>
          <div style={{ background:'#fff', border:'1px solid #f1f5f9', borderRadius:'12px', overflow:'hidden', boxShadow:'0 1px 3px rgba(0,0,0,0.04)' }}>
            {activity.map((a, i) => {
              const ic = actIconData(a.status)
              return (
                <div key={a.id} style={{ display:'flex', alignItems:'center', gap:'12px', padding:'11px 16px', borderBottom:i<activity.length-1?'1px solid #f8fafc':'none' }}>
                  <div style={{ width:'28px', height:'28px', borderRadius:'8px', background:ic.bg, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
                    <div style={{ width:'8px', height:'8px', borderRadius:'50%', background:ic.color }}/>
                  </div>
                  <div style={{ flex:1, fontSize:'13px', color:'#374151' }}>
                    <strong style={{ color:'#0f172a' }}>{a.slug}</strong> · {a.status.toLowerCase()}
                  </div>
                  <span style={{ fontSize:'11px', color:'#cbd5e1', whiteSpace:'nowrap' }}>{fmt(a.time)}</span>
                </div>
              )
            })}
          </div>
        </>
      )}
    {channelModal && (
      <ChannelModal
        jobId={channelModal.id}
        jobName={channelModal.name}
        onClose={() => setChannelModal(null)}
      />
    )}
    </div>
  )
}
