import { Link, useLocation } from 'react-router-dom'

export default function Navbar() {
  const { pathname } = useLocation()

  const link = (to, label) => (
    <Link
      to={to}
      className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
        pathname === to
          ? 'bg-indigo-700 text-white'
          : 'text-indigo-100 hover:bg-indigo-700'
      }`}
    >
      {label}
    </Link>
  )

  return (
    <nav className="bg-indigo-800 shadow-md">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <Link to="/" className="text-white font-bold text-xl tracking-tight">
          ⏱ Cronitor
        </Link>
        <div className="flex gap-2">
          {link('/', 'Dashboard')}
          {link('/alerts', 'Alerts')}
        </div>
      </div>
    </nav>
  )
}
