import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { placeOrder, validateCoupon } from '../api/orderApi'
import { useFlags } from '../api/flagsApi'
import { formatUSD } from '../utils/format'

const STEPS = ['Cart', 'Shipping', 'Payment']
const METHODS = [
  { id: 'CARD', label: 'Card', hint: 'Credit / debit (simulated)', icon: '💳' },
  { id: 'UPI', label: 'UPI', hint: 'Instant bank transfer (simulated)', icon: '📱' },
  { id: 'COD', label: 'Cash on Delivery', hint: 'Pay at your door', icon: '💵' },
]

export default function CheckoutPage() {
  const { items, total, clear } = useCart()
  const { user, isLoggedIn } = useAuth()
  const { notify } = useToast()
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [name, setName] = useState(user?.name ?? '')
  const [address, setAddress] = useState(user?.address ?? '')
  const [phone, setPhone] = useState(user?.phone ?? '')
  const [shipErrors, setShipErrors] = useState({})
  const [method, setMethod] = useState('CARD')
  const [coupon, setCoupon] = useState('')
  const [couponState, setCouponState] = useState(null) // {valid, discount, reason}
  const [couponBusy, setCouponBusy] = useState(false)
  const [placing, setPlacing] = useState(false)
  const flags = useFlags()
  const couponsOn = flags.coupons !== false
  const [failedOrder, setFailedOrder] = useState(null)
  const [error, setError] = useState('')

  if (items.length === 0 && !failedOrder) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-10 text-center">
        <p className="text-gray-500">Nothing to check out — your cart is empty.</p>
        <Link to="/" className="mt-4 inline-block px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
          Browse products
        </Link>
      </div>
    )
  }

  const validShipping = () => {
    const errs = {}
    if (!name.trim()) errs.name = 'Full name is required'
    if (!address.trim()) errs.address = 'Delivery address is required'
    if (!phone.trim()) errs.phone = 'Phone number is required'
    setShipErrors(errs)
    return Object.keys(errs).length === 0
  }

  const applyCoupon = async () => {
    if (!coupon.trim()) return
    setCouponBusy(true)
    try {
      const res = await validateCoupon(coupon.trim(), total)
      setCouponState({ ...res, code: coupon.trim() })
      if (res.valid) notify(`Coupon applied — you save ${formatUSD(res.discount)}`)
      else notify(res.reason ?? 'Coupon not valid')
    } catch (e) {
      setCouponState({ valid: false, discount: 0, reason: e.message })
      notify(e.message)
    } finally {
      setCouponBusy(false)
    }
  }

  const submitOrder = async () => {
    setError('')
    setPlacing(true)
    try {
      const order = await placeOrder(
        user?.id ?? 1,
        items.map((i) => ({ productId: i.productId, quantity: i.quantity })),
        method,
        couponState?.valid ? couponState.code : null
      )
      if (order.status === 'PAYMENT_FAILED') {
        setFailedOrder(order)
        return
      }
      clear()
      sessionStorage.setItem('omnishop-toast', `Order #${order.id} placed successfully!`)
      navigate(`/order-confirmation/${order.id}`, { state: { order } })
    } catch (e) {
      setError(e.message)
    } finally {
      setPlacing(false)
    }
  }

  // Calm retry screen — payment failures are expected from the mock gateway
  if (failedOrder) {
    return (
      <div className="max-w-xl mx-auto px-4 py-10 text-center animate-fade-up">
        <div className="bg-white rounded-xl shadow p-8">
          <div className="mx-auto w-14 h-14 rounded-full bg-amber-100 flex items-center justify-center">
            <svg className="w-7 h-7 text-amber-600" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M10.3 3.9L1.8 18a2 2 0 001.7 3h17a2 2 0 001.7-3L13.7 3.9a2 2 0 00-3.4 0z" />
            </svg>
          </div>
          <h1 className="mt-3 text-xl font-bold text-gray-900">Payment didn't go through</h1>
          <p className="mt-1 text-sm text-gray-500">
            Our (simulated) payment gateway declined order #{failedOrder.id} — no money moved
            and your items are still reserved in your cart. This happens sometimes with the demo gateway.
          </p>
          <div className="mt-5 flex justify-center gap-2">
            <button
              onClick={() => { setFailedOrder(null); setStep(2) }}
              disabled={placing}
              className="px-5 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro disabled:bg-gray-300"
            >
              {placing ? 'Retrying…' : 'Try a different method'}
            </button>
            <Link to="/cart" className="px-5 py-2 rounded-md text-sm bg-gray-200 hover:bg-gray-300 transition-micro">
              Back to cart
            </Link>
          </div>
        </div>
      </div>
    )
  }

  const inputCls = (bad) =>
    `w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 ${
      bad ? 'border-red-500 focus:ring-red-400' : 'focus:ring-brand-500'
    }`

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      {/* Progress indicator */}
      <ol className="flex items-center gap-1" aria-label="Checkout progress">
        {STEPS.map((s, i) => (
          <li key={s} className="flex items-center flex-1 last:flex-none">
            <span className={`inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold transition-micro ${
              i < step ? 'bg-green-600 text-white' : i === step ? 'bg-brand-600 text-white ring-4 ring-brand-100' : 'bg-gray-200 text-gray-500'
            }`}>
              {i < step ? '✓' : i + 1}
            </span>
            <span className={`ml-1.5 text-xs font-medium hidden sm:block ${i <= step ? 'text-gray-900' : 'text-gray-400'}`}>{s}</span>
            {i < STEPS.length - 1 && <span className={`h-0.5 flex-1 mx-2 rounded ${i < step ? 'bg-green-600' : 'bg-gray-200'}`} />}
          </li>
        ))}
      </ol>

      {error && (
        <div className="mt-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">{error}</div>
      )}

      {step === 1 && (
        <div className="mt-4 bg-white rounded-xl shadow p-6 animate-fade-up">
          <h1 className="text-lg font-bold">Shipping details</h1>
          {!isLoggedIn && (
            <p className="mt-1 text-sm text-gray-500">
              Checking out as guest. <Link to="/login" className="text-brand-600 hover:underline">Log in</Link> to
              attach this order to your account.
            </p>
          )}
          <div className="mt-3 space-y-3">
            <div>
              <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Full name"
                className={inputCls(shipErrors.name)} />
              {shipErrors.name && <p className="mt-1 text-xs text-red-600">{shipErrors.name}</p>}
            </div>
            <div>
              <textarea value={address} onChange={(e) => setAddress(e.target.value)} placeholder="Delivery address" rows={2}
                className={inputCls(shipErrors.address)} />
              {shipErrors.address && <p className="mt-1 text-xs text-red-600">{shipErrors.address}</p>}
            </div>
            <div>
              <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="Phone number"
                className={inputCls(shipErrors.phone)} />
              {shipErrors.phone && <p className="mt-1 text-xs text-red-600">{shipErrors.phone}</p>}
            </div>
          </div>
          <div className="mt-4 flex justify-between">
            <Link to="/cart" className="px-4 py-2 rounded-md text-sm bg-gray-200 hover:bg-gray-300 transition-micro">Back to cart</Link>
            <button onClick={() => validShipping() && setStep(2)}
              className="px-6 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
              Continue to payment
            </button>
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="mt-4 bg-white rounded-xl shadow p-6 animate-fade-up">
          <h1 className="text-lg font-bold">Payment method</h1>
          <p className="text-sm text-gray-500">Demo gateway — no real money moves.</p>
          <div className="mt-3 grid grid-cols-1 sm:grid-cols-3 gap-3" role="radiogroup" aria-label="Payment method">
            {METHODS.map((m) => (
              <button
                key={m.id}
                type="button"
                role="radio"
                aria-checked={method === m.id}
                onClick={() => setMethod(m.id)}
                className={`rounded-xl border-2 p-4 text-left transition-micro ${
                  method === m.id
                    ? 'border-brand-600 bg-brand-50/60 shadow'
                    : 'border-gray-200 hover:border-brand-300'
                }`}
              >
                <span className="text-2xl">{m.icon}</span>
                <p className="mt-1 text-sm font-bold">{m.label}</p>
                <p className="text-xs text-gray-500">{m.hint}</p>
              </button>
            ))}
          </div>
          <div className="mt-4 bg-gray-50 rounded-lg p-4 text-sm space-y-2">
            <div className="flex justify-between">
              <span className="text-gray-500">{items.length} item(s) · Subtotal</span>
              <span className="font-semibold">{formatUSD(total)}</span>
            </div>
            {couponsOn && (
            <div className="flex gap-2">
              <input value={coupon} onChange={(e) => { setCoupon(e.target.value); setCouponState(null) }}
                placeholder="Coupon code (e.g. SAVE10)"
                className="flex-1 border rounded-md px-3 py-1.5 text-sm uppercase focus:outline-none focus:ring-2 focus:ring-brand-500" />
              <button onClick={applyCoupon} disabled={couponBusy || !coupon.trim()}
                className="px-3 py-1.5 rounded-md text-sm bg-gray-200 hover:bg-gray-300 transition-micro disabled:opacity-50">
                {couponBusy ? '…' : 'Apply'}
              </button>
            </div>
            )}
            {couponState && (
              <p className={`text-xs font-medium ${couponState.valid ? 'text-green-600' : 'text-red-600'}`}>
                {couponState.valid
                  ? `Coupon applied — you save ${formatUSD(couponState.discount)}`
                  : couponState.reason}
              </p>
            )}
            <div className="flex justify-between border-t pt-2">
              <span className="text-gray-500">Total</span>
              <span className="font-extrabold text-base">{formatUSD(total)}</span>
            </div>
          </div>
          <div className="mt-4 flex justify-between">
            <button onClick={() => setStep(1)}
              className="px-4 py-2 rounded-md text-sm bg-gray-200 hover:bg-gray-300 transition-micro">Back</button>
            <button onClick={submitOrder} disabled={placing}
              className="px-6 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro disabled:bg-gray-300">
              {placing ? 'Processing payment…' : `Pay ${formatUSD(total)}`}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
