import { Link, useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useToast } from '../context/ToastContext'
import { formatUSD } from '../utils/format'

export default function CartPage() {
  const { items, total, setQty, remove } = useCart()
  const { notify } = useToast()
  const navigate = useNavigate()

  // Optimistic cart: reducer updates instantly (local-first) and persists to
  // localStorage; the only feedback needed is when stock caps a quantity.
  const changeQty = (item, qty) => {
    const capped = Math.min(Math.max(1, qty || 1), item.stockQuantity)
    if (qty > item.stockQuantity) notify(`Only ${item.stockQuantity} in stock — quantity capped`)
    setQty(item.productId, capped)
  }

  const drop = (item) => {
    remove(item.productId)
    notify(`${item.name} removed from cart`)
  }

  if (!items.length) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-10 text-center animate-fade-up">
        <div className="mx-auto w-20 h-20 rounded-full bg-brand-50 flex items-center justify-center">
          <svg className="w-10 h-10 text-brand-400" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.3 4.6a1 1 0 00.9 1.4H19M9 22a1 1 0 100-2 1 1 0 000 2zm8 0a1 1 0 100-2 1 1 0 000 2z" />
          </svg>
        </div>
        <h1 className="mt-4 text-xl font-bold text-gray-900">Your cart is empty</h1>
        <p className="mt-1 text-sm text-gray-500">Add some tech to get started — checkout takes seconds.</p>
        <Link to="/" className="mt-4 inline-block px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
          Browse products
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold text-gray-900">Your Cart</h1>
      <div className="mt-4 space-y-3">
        {items.map((i) => (
          <div key={i.productId} className="bg-white rounded-lg shadow p-4 flex items-center gap-3">
            <div className="flex-1">
              <p className="font-medium">{i.name}</p>
              <p className="text-sm text-gray-500">{formatUSD(i.price)} each</p>
            </div>
            <input
              type="number" min="1" max={i.stockQuantity} value={i.quantity}
              onChange={(e) => changeQty(i, Number(e.target.value))}
              className="w-16 border rounded-md px-2 py-1 text-sm"
            />
            <p className="font-semibold w-24 text-right">{formatUSD(i.price * i.quantity)}</p>
            <button onClick={() => drop(i)} className="text-sm text-red-600 hover:underline">Remove</button>
          </div>
        ))}
      </div>
      <div className="mt-4 bg-white rounded-lg shadow p-4 flex items-center justify-between">
        <p className="text-lg font-bold">Total: {formatUSD(total)}</p>
        <button
          onClick={() => navigate('/checkout')}
          className="px-6 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 disabled:bg-gray-300 transition-micro"
        >
          Proceed to Checkout
        </button>
      </div>
    </div>
  )
}
