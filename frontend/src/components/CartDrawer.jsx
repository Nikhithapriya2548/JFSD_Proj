import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { formatUSD } from '../utils/format'

// Mini-cart preview shown on the Home page sidebar
export default function CartDrawer() {
  const { items, total, count } = useCart()

  return (
    <div className="bg-white rounded-lg shadow p-4 sticky top-20 animate-drawer-in">
      <h2 className="font-semibold text-gray-900">Cart ({count})</h2>
      {items.length === 0 ? (
        <p className="text-sm text-gray-500 mt-2">Your cart is empty.</p>
      ) : (
        <>
          <ul className="mt-2 space-y-1 text-sm max-h-48 overflow-auto">
            {items.map((i) => (
              <li key={i.productId} className="flex justify-between">
                <span className="truncate">{i.name} × {i.quantity}</span>
                <span className="font-medium">{formatUSD(i.price * i.quantity)}</span>
              </li>
            ))}
          </ul>
          <p className="mt-2 font-bold">Total: {formatUSD(total)}</p>
          <Link to="/cart" className="mt-3 block text-center py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
            Review & Checkout
          </Link>
        </>
      )}
    </div>
  )
}
