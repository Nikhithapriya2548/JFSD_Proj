import { useCallback, useEffect, useState } from 'react'
import { getOrdersByUser } from '../api/orderApi'
import OrderHistory from '../components/OrderHistory'
import { useAuth } from '../context/AuthContext'
import { MOCK_USER_ID } from '../constants'

export default function OrderHistoryPage() {
  const { user } = useAuth()
  const userId = user?.id ?? MOCK_USER_ID
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    getOrdersByUser(userId)
      .then(setOrders)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [userId])

  useEffect(() => { load() }, [load])

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold text-gray-900">My Orders</h1>
      <p className="text-sm text-gray-500">
        {user ? `Showing orders for ${user.name ?? user.email}` : `Showing orders for guest user #${MOCK_USER_ID}`}
      </p>
      {error && (
        <div className="mt-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
          Couldn't load orders: {error}
        </div>
      )}
      <div className="mt-4">
        {loading ? <p className="text-gray-500">Loading orders…</p>
          : <OrderHistory orders={orders} onChanged={load} />}
      </div>
    </div>
  )
}
