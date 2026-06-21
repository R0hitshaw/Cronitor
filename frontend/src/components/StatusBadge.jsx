const colors = {
  HEALTHY:  'bg-green-100 text-green-800',
  RUNNING:  'bg-blue-100 text-blue-800',
  MISSED:   'bg-red-100 text-red-800',
  FAILED:   'bg-red-100 text-red-800',
  PENDING:  'bg-gray-100 text-gray-600',
}

const dots = {
  HEALTHY: 'bg-green-500',
  RUNNING: 'bg-blue-500 animate-pulse',
  MISSED:  'bg-red-500',
  FAILED:  'bg-red-500',
  PENDING: 'bg-gray-400',
}

export default function StatusBadge({ status }) {
  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${colors[status] || colors.PENDING}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dots[status] || dots.PENDING}`} />
      {status}
    </span>
  )
}
