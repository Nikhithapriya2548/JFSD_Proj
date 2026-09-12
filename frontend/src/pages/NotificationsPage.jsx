import { useEffect, useState } from 'react'
import { getNotifications } from '../api/notificationApi'
import { useAuth } from '../context/AuthContext'
import { MOCK_USER_ID } from '../constants'
import { timeAgo } from '../utils/display'

const TYPE_STYLE = {
  'order.created': 'bg-blue-100 text-blue-800',
  'order.status.changed': 'bg-indigo-100 text-indigo-800',
  'payment.completed': 'bg-green-100 text-green-700',
  'payment.failed': 'bg-red-100 text-red-700',
  'order.payment_failed': 'bg-amber-100 text-amber-800',
  'stock.low': 'bg-orange-100 text-orange-800',
}

export default function NotificationsPage() {
  const { user } = useAuth()
  const userId = user?.id ?? MOCK_USER_ID
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getNotifications(userId)
      .then(setItems)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [userId])

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-extrabold text-gray-900">Notifications</h1>
      <p className="text-sm text-gray-500">
        Order updates, payment results and stock alerts — newest first.
      </p>
      {error && (
        <div className="mt-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
          Couldn't load notifications: {error}
        </div>
      )}
      <div className="mt-4">
        {loading ? (
          <div className="space-y-3 animate-pulse">
            {[0, 1, 2].map((i) => <div key={i} className="h-20 bg-white rounded-lg shadow" />)}
          </div>
        ) : items.length === 0 ? (
          <div className="text-center py-10 animate-fade-up">
            <div className="mx-auto w-16 h-16 rounded-full bg-brand-50 flex items-center justify-center">
              <svg className="w-8 h-8 text-brand-400" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2a2 2 0 01-.6 1.4L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <p className="mt-3 font-semibold text-gray-900">All quiet</p>
            <p className="text-sm text-gray-500">Place an order and updates will land here.</p>
          </div>
        ) : (
          <ul className="space-y-3">
            {items.map((n) => (
              <li key={n.id} className="bg-white rounded-lg shadow p-4 animate-fade-up">
                <div className="flex items-center gap-2">
                  <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${TYPE_STYLE[n.eventType] ?? 'bg-gray-100 text-gray-700'}`}>
                    {n.eventType}
                  </span>
                  <span className="ml-auto text-xs text-gray-400">{timeAgo(n.createdAt)}</span>
                </div>
                <p className="mt-1.5 text-sm text-gray-700">{n.message}</p>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
