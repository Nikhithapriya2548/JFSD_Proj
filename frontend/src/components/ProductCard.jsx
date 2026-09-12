import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useToast } from '../context/ToastContext'
import { formatUSD } from '../utils/format'

const PLACEHOLDER = '/placeholder-product.png'

export function stockTier(stockQuantity) {
  const q = stockQuantity ?? 0
  if (q <= 0) return { label: 'Out of Stock', cls: 'bg-red-100 text-red-700' }
  if (q < 10) return { label: `Low Stock (${q})`, cls: 'bg-amber-100 text-amber-800' }
  return { label: `In Stock (${q})`, cls: 'bg-green-100 text-green-700' }
}

export function StarRating({ value, count }) {
  if (value == null) return <span className="text-xs text-gray-400">No ratings yet</span>
  const full = Math.round(value)
  return (
    <span className="inline-flex items-center gap-1" title={`${value} average from ${count ?? 0} reviews`}>
      <span className="text-amber-400 text-sm tracking-tight">
        {'★'.repeat(full)}{'☆'.repeat(Math.max(0, 5 - full))}
      </span>
      <span className="text-xs text-gray-500">{value}{count != null ? ` (${count})` : ''}</span>
    </span>
  )
}

export default function ProductCard({ product }) {
  const { add } = useCart()
  const { notify } = useToast()
  const outOfStock = (product.stockQuantity ?? 0) <= 0
  const [imgSrc, setImgSrc] = useState(product.imageUrl || PLACEHOLDER)
  const tier = stockTier(product.stockQuantity)

  const handleAdd = (e) => {
    e.preventDefault()
    e.stopPropagation()
    add(product)
    notify(`${product.name} added to cart`)
  }

  return (
    <div className="bg-white rounded-xl shadow hover:shadow-lift hover:-translate-y-0.5 transition-micro flex flex-col overflow-hidden">
      <Link to={`/product/${product.id}`} className="block h-44 bg-gray-100 overflow-hidden">
        <img
          src={imgSrc}
          alt={product.name}
          loading="lazy"
          onError={() => { if (imgSrc !== PLACEHOLDER) setImgSrc(PLACEHOLDER) }}
          className="h-full w-full object-cover hover:scale-105 transition-transform duration-200"
        />
      </Link>
      <div className="p-4 flex flex-col flex-1">
        <Link to={`/product/${product.id}`} className="font-semibold text-gray-900 truncate hover:text-brand-600 transition-micro">
          {product.name}
        </Link>
        <p className="text-sm text-gray-500 line-clamp-2 flex-1">{product.description}</p>
        <p className="mt-2 text-lg font-bold text-gray-900">{formatUSD(product.price)}</p>
        <StarRating value={product.averageRating} count={product.reviewCount} />
        <span className={`mt-1 inline-block w-fit whitespace-nowrap text-xs font-medium px-2.5 py-1 rounded-full ${tier.cls}`}>
          {tier.label}
        </span>
        <button
          disabled={outOfStock}
          onClick={handleAdd}
          className="mt-3 w-full py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 active:scale-[0.98] transition-micro disabled:bg-gray-300 disabled:cursor-not-allowed"
        >
          {outOfStock ? 'Unavailable' : 'Add to Cart'}
        </button>
      </div>
    </div>
  )
}
