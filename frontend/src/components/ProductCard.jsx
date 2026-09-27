import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useToast } from '../context/ToastContext'
import { formatUSD } from '../utils/format'

const PLACEHOLDER = '/placeholder-product.png'

const getProductImage = (product) => {
  const text = `${product.name} ${product.category || ''}`.toLowerCase();
  if (text.includes('laptop')) return '/laptop.png';
  if (text.includes('tablet')) return '/tablet.png';
  if (text.includes('phone') || text.includes('iphone')) return '/phone.png';
  if (text.includes('smartwatch') || text.includes('watch')) return '/smartwatch.png';
  if (text.includes('monitor') || text.includes('display')) return '/monitor.png';
  if (text.includes('keyboard')) return '/keyboard.png';
  if (text.includes('mouse') || text.includes('mice')) return '/mouse.png';
  if (text.includes('console') || text.includes('xbox') || text.includes('playstation')) return '/console.png';
  if (text.includes('camera') || text.includes('dslr')) return '/camera.png';
  if (text.includes('headphone') || text.includes('audio')) return '/headphones.png';
  return PLACEHOLDER;
};

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
  const categoryImage = getProductImage(product);
  const [imgSrc, setImgSrc] = useState(product.imageUrl || categoryImage)
  const tier = stockTier(product.stockQuantity)

  const handleAdd = (e) => {
    e.preventDefault()
    e.stopPropagation()
    add(product)
    notify(`${product.name} added to cart`)
  }

  return (
    <div className="flex flex-col h-full bg-white rounded-[2rem] overflow-hidden transition-all duration-200 hover:shadow-sm border-0 p-2 pb-4">
      <div className="relative bg-[#f4f5f9] rounded-3xl p-4 flex justify-center items-center h-44 mb-3 group">
        <button 
          onClick={handleAdd}
          className="absolute top-3 right-3 text-gray-400 hover:text-red-500 transition-colors bg-white rounded-full p-1.5 shadow-sm opacity-100 z-10"
        >
           <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-4 h-4">
             <path strokeLinecap="round" strokeLinejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
           </svg>
        </button>
        <Link to={`/product/${product.id}`} className="block h-full w-full flex justify-center items-center">
          <img
            src={imgSrc}
            alt={product.name}
            loading="lazy"
            onError={() => { if (imgSrc !== categoryImage) setImgSrc(categoryImage) }}
            className="max-h-full max-w-full object-contain mix-blend-multiply transition-transform duration-300 group-hover:scale-105"
          />
        </Link>
      </div>
      
      <div className="flex items-center justify-between px-2">
        <Link to={`/product/${product.id}`} className="text-[13px] font-medium text-gray-700 truncate hover:text-black mr-2">
          {product.name}
        </Link>
        <span className="text-[13px] font-bold text-gray-900 shrink-0">
          ${Math.round(product.price)}
        </span>
      </div>
    </div>
  )
}
