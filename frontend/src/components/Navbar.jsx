import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { getNotifications } from '../api/notificationApi'
import { MOCK_USER_ID } from '../constants'

export default function Navbar() {
  const { count } = useCart()
  const { user, isLoggedIn, isAdmin, logout } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [q, setQ] = useState('')
  const [unread, setUnread] = useState(0)
  const link = ({ isActive }) =>
    `px-3 py-2 rounded-md text-sm font-medium transition-micro ${
      isActive ? 'bg-brand-600 text-white shadow' : 'text-gray-600 hover:bg-brand-50 hover:text-brand-700'
    }`

  const submitSearch = (e) => {
    e.preventDefault()
    navigate(q.trim() ? `/?q=${encodeURIComponent(q.trim())}` : '/')
  }

  // Alerts badge: inbox size for the current user, refreshed on navigation.
  // (No read-state on the backend, so the badge shows total recent items —
  // newest-first inbox on the Alerts page.)
  useEffect(() => {
    let alive = true
    getNotifications(user?.id ?? MOCK_USER_ID)
      .then((items) => { if (alive) setUnread(Array.isArray(items) ? items.length : 0) })
      .catch(() => {})
    return () => { alive = false }
  }, [pathname, user?.id])

  return (
    <nav className="bg-white/95 backdrop-blur shadow-sm sticky top-0 z-20 border-b">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center gap-3">
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <img src="/logo.svg" alt="OmniShop" className="w-8 h-8 rounded-lg shadow-sm" />
          <span className="text-xl font-extrabold tracking-tight text-brand-700 hidden sm:inline">OmniShop</span>
        </Link>
        <form onSubmit={submitSearch} className="flex-1 max-w-md hidden md:block">
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search laptops, tablets, phones…"
            className="w-full border rounded-full px-4 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </form>
        <div className="flex items-center gap-1 sm:gap-2 ml-auto">
          <NavLink to="/" className={link}>Home</NavLink>
          <NavLink to="/orders" className={link}>My Orders</NavLink>
          <NavLink to="/notifications" className={({ isActive }) =>
            `relative px-3 py-2 rounded-md text-sm font-medium transition-micro flex items-center gap-1.5 ${
              isActive ? 'bg-brand-600 text-white shadow' : 'text-gray-600 hover:bg-brand-50 hover:text-brand-700'
            }`
          }>
            Alerts
            {unread > 0 && (
              <span className="inline-flex items-center justify-center min-w-[1.25rem] h-5 px-1 rounded-full bg-accent-500 text-white text-xs font-semibold">
                {unread > 99 ? '99+' : unread}
              </span>
            )}
          </NavLink>
          <NavLink to="/admin" className={link}>Admin</NavLink>
          {isLoggedIn ? (
            <span className="flex items-center gap-1.5 pl-1">
              <span className="text-sm font-medium text-gray-700 hidden lg:inline" title={user.email}>
                {user.name}{isAdmin ? ' (admin)' : ''}
              </span>
              <button
                onClick={() => { logout(); navigate('/') }}
                className="px-3 py-2 rounded-md text-sm font-medium text-gray-600 hover:bg-brand-50 hover:text-brand-700 transition-micro"
              >
                Logout
              </button>
            </span>
          ) : (
            <NavLink to="/login" className={link}>Login</NavLink>
          )}
          <NavLink to="/cart" className={({ isActive }) =>
            `relative px-3 py-2 rounded-md text-sm font-medium transition-micro flex items-center gap-1.5 ${
              isActive ? 'bg-brand-600 text-white shadow' : 'text-gray-600 hover:bg-brand-50 hover:text-brand-700'
            }`
          }>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.3 4.6a1 1 0 00.9 1.4H19M9 22a1 1 0 100-2 1 1 0 000 2zm8 0a1 1 0 100-2 1 1 0 000 2z" />
            </svg>
            Cart
            <span className="inline-flex items-center justify-center min-w-[1.25rem] h-5 px-1 rounded-full bg-accent-500 text-white text-xs font-semibold">
              {count}
            </span>
          </NavLink>
        </div>
      </div>
    </nav>
  )
}
