import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getOrderById } from '../api/orderApi'
import OrderSummary from '../components/OrderSummary'

export default function OrderConfirmationPage() {
  const { id } = useParams()
  const location = useLocation()
  const [order, setOrder] = useState(location.state?.order ?? null)
  const [toast, setToast] = useState('')
  const [error, setError] = useState('')

  const deliveryEstimate = () => {
    const d = new Date(Date.now() + 4 * 864e5)
    return d.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })
  }

  useEffect(() => {
    setToast(sessionStorage.getItem('omnishop-toast') ?? '')
    sessionStorage.removeItem('omnishop-toast')
    if (!order) {
      getOrderById(id).then(setOrder).catch((e) => setError(e.message))
      return
    }
    // SAGA: payment settles asynchronously — poll until the order leaves
    // PENDING (CONFIRMED / PAYMENT_FAILED) instead of assuming it's done.
    if (order.status !== 'PENDING') return
    const started = Date.now()
    const timer = setInterval(() => {
      if (Date.now() - started > 60000) {
        clearInterval(timer)
        return
      }
      getOrderById(id)
        .then((fresh) => {
          setOrder(fresh)
          if (fresh.status !== 'PENDING') clearInterval(timer)
        })
        .catch(() => {})
    }, 2000)
    return () => clearInterval(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, order?.status])

  return (
    <div className="max-w-2xl mx-auto px-4 py-6 animate-fade-up">
      <div className="text-center">
        <div className="mx-auto w-20 h-20 rounded-full bg-green-100 flex items-center justify-center animate-pop">
          <svg className="w-10 h-10 text-green-600 check-draw" fill="none" stroke="currentColor" strokeWidth="3" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h1 className="mt-3 text-2xl font-extrabold text-gray-900">Order confirmed!</h1>
        <p className="text-sm text-gray-500">Thanks for shopping — your order is being prepared.</p>
        {order && (
          <p className="mt-1 text-sm text-gray-500">
            Estimated delivery: <span className="font-semibold text-gray-700">{deliveryEstimate()}</span>
          </p>
        )}
      </div>
      {toast && (
        <div className="mt-4 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-md text-sm">{toast}</div>
      )}
      {error && (
        <div className="mt-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">{error}</div>
      )}
      {!order && !error && <p className="mt-4 text-gray-500">Loading order…</p>}
      {order && order.status === 'PENDING' && (
        <div className="mt-4 bg-blue-50 border border-blue-200 text-blue-800 px-4 py-3 rounded-md text-sm flex items-center gap-2">
          <span className="inline-block w-4 h-4 rounded-full border-2 border-blue-400 border-t-transparent animate-spin" />
          Processing payment… this page updates automatically.
        </div>
      )}
      {order && order.status === 'PAYMENT_FAILED' && (
        <div className="mt-4 bg-amber-50 border border-amber-200 text-amber-800 px-4 py-3 rounded-md text-sm">
          Payment didn't go through — your order is on hold, nothing was charged.
          <Link to="/cart" className="ml-2 font-medium underline">Back to cart to retry</Link>
        </div>
      )}
      {order && (
        <div className="mt-4">
          <OrderSummary order={order} />
        </div>
      )}
      <div className="mt-4 flex gap-2">
        <Link to={order ? `/orders` : '/'} className="px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
          View Order
        </Link>
        <Link to="/" className="px-4 py-2 rounded-md text-sm font-medium bg-gray-200 hover:bg-gray-300 transition-micro">
          Continue Shopping
        </Link>
      </div>
    </div>
  )
}
