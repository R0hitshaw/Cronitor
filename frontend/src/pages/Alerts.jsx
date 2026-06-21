import { useEffect, useState } from 'react'
import { getAlertHistory, resolveAlert } from '../api/client'

const severityColor = {
  CRITICAL: 'text-red-600 bg-red-50 border-red-200',
  WARNING:  'text-yellow-700 bg-yellow-50 border-yellow-200',
  INFO:     'text-blue-600 bg-blue-50 border-blue-200',
}

export default function Alerts() {
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [resolving, setResolving] = useState(null)

  const load = () => {
    setLoading(true)
    getAlertHistory(page)
      .then(r => {
        setAlerts(r.data.content)
        setTotalPages(r.data.totalPages)
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [page])

  const handleResolve = async (id) => {
    setResolving(id)
    try {
      await resolveAlert(id)
      load()
    } finally {
      setResolving(null)
    }
  }

  const fmt = (ts) => ts ? new Date(ts).toLocaleString() : '—'

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Alert History</h1>
        <p className="text-sm text-gray-500 mt-0.5">All alerts across all jobs, newest first</p>
      </div>

      <div className="bg-white border border-gray-200 rounded-xl shadow-sm">
        {loading ? (
          <p className="px-5 py-8 text-center text-gray-400 text-sm">Loading...</p>
        ) : alerts.length === 0 ? (
          <div className="px-5 py-16 text-center text-gray-400">
            <p className="text-3xl mb-2">✅</p>
            <p className="font-medium">No alerts yet</p>
            <p className="text-sm mt-1">All your jobs are healthy</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-gray-400 uppercase border-b border-gray-100">
                  <th className="px-5 py-3 text-left">Severity</th>
                  <th className="px-5 py-3 text-left">Message</th>
                  <th className="px-5 py-3 text-left">Fired at</th>
                  <th className="px-5 py-3 text-left">Status</th>
                  <th className="px-5 py-3 text-left">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {alerts.map(alert => (
                  <tr key={alert.id} className={`hover:bg-gray-50 transition-colors ${alert.resolved ? 'opacity-50' : ''}`}>
                    <td className="px-5 py-3">
                      <span className={`text-xs font-semibold px-2 py-1 rounded border ${severityColor[alert.severity] || severityColor.INFO}`}>
                        {alert.severity}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-gray-700 max-w-md">
                      <p className="truncate">{alert.message}</p>
                    </td>
                    <td className="px-5 py-3 text-gray-500 whitespace-nowrap">{fmt(alert.firedAt)}</td>
                    <td className="px-5 py-3">
                      {alert.resolved ? (
                        <span className="text-xs text-green-600 font-medium">✓ Resolved</span>
                      ) : (
                        <span className="text-xs text-red-500 font-medium">● Firing</span>
                      )}
                    </td>
                    <td className="px-5 py-3">
                      {!alert.resolved && (
                        <button
                          onClick={() => handleResolve(alert.id)}
                          disabled={resolving === alert.id}
                          className="text-xs text-indigo-600 hover:text-indigo-800 border border-indigo-200 hover:border-indigo-400 rounded px-2 py-1 transition-colors disabled:opacity-50"
                        >
                          {resolving === alert.id ? 'Resolving...' : 'Resolve'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

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
    </div>
  )
}
