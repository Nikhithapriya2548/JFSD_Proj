import { useState } from 'react'
import { cancelOrder } from '../api/orderApi'
import { pay, paymentsByOrder } from '../api/paymentApi'
import { useToast } from '../context/ToastContext'
import { formatUSD } from '../utils/format'
import OrderSummary, { StatusBadge } from './OrderSummary'

const PIPELINE = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED']
const RETRY_METHODS = ['CARD', 'UPI', 'COD']

export function StatusStepper({ status }) {
  if (status === 'CANCELLED' || status === 'PAYMENT_FAILED') {
    return (
      <div className="mt-3 flex items-center gap-2">
        <span className="inline-flex items-center justify-center w-5 h-5 rounded-full bg-red-600 text-white text-xs">✕</span>
        <span className="text-sm font-medium text-red-700">
          {status === 'CANCELLED' ? 'Order cancelled' : 'Payment failed'}
        </span>
      </div>
    )
  }
  const idx = Math.max(0, PIPELINE.indexOf(status))
  return (
    <div className="mt-3 flex items-center" aria-label={`Order status: ${status}`}>
      {PIPELINE.map((s, i) => (
        <div key={s} className="flex items-center flex-1 last:flex-none">
          <div className="flex flex-col items-center">
            <span className={`inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-bold transition-micro ${
              i < idx ? 'bg-green-600 text-white' : i === idx ? 'bg-brand-600 text-white ring-4 ring-brand-100' : 'bg-gray-200 text-gray-500'
            }`}>
              {i < idx ? '✓' : i + 1}
            </span>
            <span className={`mt-1 text-[10px] font-medium hidden sm:block ${i <= idx ? 'text-gray-900' : 'text-gray-400'}`}>
              {s.charAt(0) + s.slice(1).toLowerCase()}
            </span>
          </div>
          {i < PIPELINE.length - 1 && (
            <div className={`h-0.5 flex-1 mx-1 rounded ${i < idx ? 'bg-green-600' : 'bg-gray-200'}`} />
          )}
        </div>
      ))}
    </div>
  )
}

function ReceiptModal({ order, attempts, onClose }) {
  return (
    <div className="fixed inset-0 z-40" role="dialog" aria-modal="true" aria-label={`Receipt for order ${order.id}`}>
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-md bg-white rounded-xl shadow-xl p-6 animate-fade-up">
        <h2 className="text-lg font-bold">Receipt — order #{order.id}</h2>
        <p className="text-sm text-gray-500">Every gateway attempt is recorded, successes and declines.</p>
        <ul className="mt-4 space-y-2 max-h-80 overflow-auto">
          {attempts.map((p) => (
            <li key={p.id} className="border rounded-lg px-3 py-2 text-sm">
              <div className="flex items-center justify-between gap-2">
                <span className="font-mono font-semibold">{p.transactionId}</span>
                <StatusBadge status={p.status} />
              </div>
              <p className="mt-1 text-xs text-gray-500">
                {p.method} · {formatUSD(p.amount)} · {p.createdAt ? new Date(p.createdAt).toLocaleString() : ''}
              </p>
            </li>
          ))}
          {attempts.length === 0 && (
            <p className="text-sm text-gray-500">No payment attempts recorded yet.</p>
          )}
        </ul>
        <button onClick={onClose}
          className="mt-4 w-full py-2 rounded-md text-sm font-medium bg-gray-200 hover:bg-gray-300 transition-micro">
          Close
        </button>
      </div>
    </div>
  )
}

export default function OrderHistory({ orders, onChanged }) {
  const { notify } = useToast()
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [retryMethod, setRetryMethod] = useState({})
  const [receipt, setReceipt] = useState(null) // {order, attempts}

  const handleCancel = async (id) => {
    setError('')
    setBusyId(id)
    try {
      await cancelOrder(id)
      notify(`Order #${id} cancelled`)
      onChanged?.()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusyId(null)
    }
  }

  // Retry posts a NEW gateway attempt against the same order (the mock
  // gateway declines ~15%, so a second attempt usually succeeds).
  const handleRetry = async (order) => {
    setError('')
    setBusyId(order.id)
    try {
      const res = await pay(order.id, order.totalAmount, retryMethod[order.id] ?? 'CARD')
      notify(res.status === 'SUCCESS'
        ? `Payment for order #${order.id} succeeded (${res.transactionId})`
        : `Payment for order #${order.id} declined again — no money moved`)
      onChanged?.()
    } catch (e) {
      setError(e.message)
    } finally {
      setBusyId(null)
    }
  }

  const openReceipt = async (order) => {
    try {
      const attempts = await paymentsByOrder(order.id)
      setReceipt({ order, attempts })
    } catch (e) {
      setError(e.message)
    }
  }

  if (!orders.length) {
    return (
      <div className="text-center py-10 animate-fade-up">
        <div className="mx-auto w-16 h-16 rounded-full bg-brand-50 flex items-center justify-center">
          <svg className="w-8 h-8 text-brand-400" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
        </div>
        <p className="mt-3 font-semibold text-gray-900">No orders yet</p>
        <p className="text-sm text-gray-500">Place your first order and it will show up here.</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {error && <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">{error}</div>}
      {orders.map((o) => (
        <div key={o.id}>
          <OrderSummary order={o} />
          <StatusStepper status={o.status} />
          <div className="flex items-center gap-3 mt-1 text-sm text-gray-500">
            <span>{o.orderDate ? new Date(o.orderDate).toLocaleString() : ''}</span>
            <StatusBadge status={o.status} />
            {o.status === 'PENDING' && (
              <button
                disabled={busyId === o.id}
                onClick={() => handleCancel(o.id)}
                className="ml-auto px-3 py-1 rounded-md text-xs font-medium bg-red-600 text-white hover:bg-red-700 disabled:bg-gray-300"
              >
                {busyId === o.id ? 'Cancelling…' : 'Cancel order'}
              </button>
            )}
            {o.status === 'PAYMENT_FAILED' && (
              <span className="ml-auto flex items-center gap-1.5">
                <select
                  value={retryMethod[o.id] ?? 'CARD'}
                  onChange={(e) => setRetryMethod((m) => ({ ...m, [o.id]: e.target.value }))}
                  className="border rounded-md px-2 py-1 text-xs bg-white"
                  aria-label="Retry payment method"
                >
                  {RETRY_METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
                </select>
                <button
                  disabled={busyId === o.id}
                  onClick={() => handleRetry(o)}
                  className="px-3 py-1 rounded-md text-xs font-medium text-white bg-brand-600 hover:bg-brand-700 disabled:bg-gray-300"
                >
                  {busyId === o.id ? 'Retrying…' : 'Retry payment'}
                </button>
              </span>
            )}
          </div>
          <button onClick={() => openReceipt(o)}
            className="mt-1 text-xs text-brand-600 hover:underline">
            View receipt
          </button>
        </div>
      ))}
      {receipt && (
        <ReceiptModal order={receipt.order} attempts={receipt.attempts} onClose={() => setReceipt(null)} />
      )}
    </div>
  )
}
