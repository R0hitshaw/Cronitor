import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer
} from 'recharts'
import { getJob, getStats, getExecutions, pingStart, pingFinish, pingFail } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import StatCard from '../components/StatCard'

export default function JobDetail() {
  const { id } = useParams()
  const [job, setJob] = useState(null)
  const [stats, setStats] = useState(null)
  const [executions, setExecutions] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [pinging, setPinging] = useState(null)

  const load = async () => {
    try {
      const [jobRes, statsRes, execRes] = await Promise.all([
        getJob(id),
        getStats(id),
        getExecutions(id, page),
      ])
      setJob(jobRes.data)
      setStats(statsRes.data)
      setExecutions(execRes.data.content)
      setTotalPages(execRes.data.totalPages)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [id, page])

  const handlePing = async (type) => {
    setPinging(type)
    try {
      if (type === 'start') await pingStart(job.slug)
      if (type === 'finish') await pingFinish(job.slug)
      if (type === 'fail') await pingFail(job.slug)
      await load()
    } finally {
      setPinging(null)
    }
  }

  const fmt = (ts) => ts ? new Date(ts).toLocaleString() : '—'
  const ms = (v) => v != null ? `${(v / 1000).toFixed(1)}s` : '—'

  // Build chart data from execution history (reverse so oldest first)
  const chartData = [...executions]
    .filter(e => e.status === 'SUCCESS' && e.durationMs)
    .reverse()
    .map((e, i) => ({
      run: `#${i + 1}`,
      duration: parseFloat((e.durationMs / 1000).toFixed(2)),
    }))

  if (loading) return <div className="p-8 text-gray-400">Loading...</div>
  if (!job) return <div className="p-8 text-red-500">Job not found</div>

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">

      {/* Breadcrumb */}
      <div className="text-sm text-gray-400 mb-4">
        <Link to="/" className="hover:text-indigo-600">Dashboard</Link>
        <span className="mx-2">›</span>
        <span className="text-gray-700">{job.name}</span>
      </div>

      {/* Job header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-gray-900">{job.name}</h1>
            <StatusBadge status={job.status} />
          </div>
          <p className="text-sm font-mono text-gray-400 mt-1">{job.slug}</p>
        </div>

        {/* Manual ping buttons — for testing */}
        <div className="flex gap-2">
          {['start', 'finish', 'fail'].map(type => (
            <button
              key={type}
              onClick={() => handlePing(type)}
              disabled={pinging !== null}
              className={`text-xs font-medium px-3 py-1.5 rounded-lg border transition-colors disabled:opacity-50 ${
                type === 'start'  ? 'border-blue-200 text-blue-600 hover:bg-blue-50' :
                type === 'finish' ? 'border-green-200 text-green-600 hover:bg-green-50' :
                                    'border-red-200 text-red-600 hover:bg-red-50'
              }`}
            >
              {pinging === type ? '...' : `Ping /${type}`}
            </button>
          ))}
        </div>
      </div>

      {/* Stats cards */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          <StatCard
            label="Success Rate"
            value={`${stats.successRatePercent}%`}
            sub={`${stats.successfulRuns} / ${stats.totalRuns} runs`}
            color={stats.successRatePercent >= 90 ? 'text-green-600' : 'text-red-600'}
          />
          <StatCard
            label="Avg Duration"
            value={ms(stats.avgDurationMs)}
            sub="recent successful runs"
          />
          <StatCard
            label="p95 Duration"
            value={ms(stats.p95DurationMs)}
            sub="95% of runs finish within"
            color="text-indigo-600"
          />
          <StatCard
            label="Active Alerts"
            value={stats.activeAlertCount}
            sub="unresolved"
            color={stats.activeAlertCount > 0 ? 'text-red-600' : 'text-green-600'}
          />
        </div>
      )}

      {/* Duration trend chart */}
      {chartData.length > 1 && (
        <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm mb-8">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Duration trend (seconds)</h2>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="run" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip
                formatter={(v) => [`${v}s`, 'Duration']}
                contentStyle={{ fontSize: 12 }}
              />
              <Line
                type="monotone"
                dataKey="duration"
                stroke="#6366f1"
                strokeWidth={2}
                dot={{ r: 3 }}
                activeDot={{ r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Execution history table */}
      <div className="bg-white border border-gray-200 rounded-xl shadow-sm">
        <div className="px-5 py-4 border-b border-gray-100">
          <h2 className="text-sm font-semibold text-gray-700">Execution history</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-xs text-gray-400 uppercase border-b border-gray-100">
                <th className="px-5 py-3 text-left">Status</th>
                <th className="px-5 py-3 text-left">Started</th>
                <th className="px-5 py-3 text-left">Finished</th>
                <th className="px-5 py-3 text-left">Duration</th>
                <th className="px-5 py-3 text-left">Message</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {executions.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-5 py-8 text-center text-gray-400">No executions yet</td>
                </tr>
              ) : executions.map(e => (
                <tr key={e.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-5 py-3">
                    <StatusBadge status={e.status} />
                  </td>
                  <td className="px-5 py-3 text-gray-600">{fmt(e.startedAt)}</td>
                  <td className="px-5 py-3 text-gray-600">{fmt(e.finishedAt)}</td>
                  <td className="px-5 py-3 text-gray-600">{ms(e.durationMs)}</td>
                  <td className="px-5 py-3 text-gray-400 text-xs truncate max-w-xs">{e.exitMessage || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-5 py-3 border-t border-gray-100 flex items-center justify-between">
            <p className="text-xs text-gray-400">Page {page + 1} of {totalPages}</p>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="text-xs px-3 py-1.5 border border-gray-200 rounded-lg disabled:opacity-40 hover:bg-gray-50"
              >
                Previous
              </button>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="text-xs px-3 py-1.5 border border-gray-200 rounded-lg disabled:opacity-40 hover:bg-gray-50"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Job meta */}
      <div className="mt-6 text-xs text-gray-400 space-y-1">
        <p>Cron: <span className="font-mono text-gray-600">{job.cronExpression}</span></p>
        <p>Grace period: <span className="text-gray-600">{job.gracePeriodSeconds}s</span></p>
        <p>Registered: <span className="text-gray-600">{fmt(job.createdAt)}</span></p>
      </div>
    </div>
  )
}
