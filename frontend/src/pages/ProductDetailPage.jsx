import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getProductById, getProducts } from '../api/productApi'
import { useCart } from '../context/CartContext'
import { useToast } from '../context/ToastContext'
import ProductCard, { stockTier, StarRating } from '../components/ProductCard'
import StarInput from '../components/StarInput'
import { formatUSD } from '../utils/format'
import { timeAgo, initials, avatarColor } from '../utils/display'
import { getReviews, addReview } from '../api/reviewApi'
import { useFlags } from '../api/flagsApi'
import { MOCK_USER_ID } from '../constants'

const PLACEHOLDER = '/placeholder-product.png'
const TABS = ['Description', 'Reviews', 'Specifications']

export default function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { add } = useCart()
  const { notify } = useToast()
  const [product, setProduct] = useState(null)
  const [related, setRelated] = useState([])
  const [qty, setQty] = useState(1)
  const [tab, setTab] = useState('Description')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [imgSrc, setImgSrc] = useState(PLACEHOLDER)
  const [reviews, setReviews] = useState([])
  const [myRating, setMyRating] = useState(5)
  const [myComment, setMyComment] = useState('')
  const [reviewMsg, setReviewMsg] = useState('')
  const [reviewBusy, setReviewBusy] = useState(false)
  const flags = useFlags()
  const showReviews = flags.reviews !== false
  const visibleTabs = TABS.filter((t) => t !== 'Reviews' || showReviews)

  useEffect(() => {
    setLoading(true)
    setError('')
    setTab('Description')
    getProductById(id)
      .then((p) => {
        setProduct(p)
        setImgSrc(p.imageUrl || PLACEHOLDER)
        setQty(1)
        getReviews(p.id).then(setReviews).catch(() => {})
        // Related: same category, excluding itself (client-side filter,
        // same functional style as the backend Streams filters)
        getProducts()
          .then((all) =>
            setRelated(
              all.filter((x) => x.category === p.category && x.id !== p.id).slice(0, 3)
            )
          )
          .catch(() => {})
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-6 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/3" />
        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="h-80 bg-gray-200 rounded-xl" />
          <div className="space-y-3">
            <div className="h-6 bg-gray-200 rounded w-3/4" />
            <div className="h-4 bg-gray-200 rounded w-full" />
            <div className="h-4 bg-gray-200 rounded w-2/3" />
          </div>
        </div>
      </div>
    )
  }

  if (error || !product) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-10 text-center">
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
          Couldn't load this product: {error || 'not found'}
        </div>
        <Link to="/" className="mt-4 inline-block px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
          Back to catalog
        </Link>
      </div>
    )
  }

  const outOfStock = (product.stockQuantity ?? 0) <= 0
  const tier = stockTier(product.stockQuantity)

  const handleAdd = () => {
    add(product, qty)
    notify(`${product.name} × ${qty} added to cart`)
  }

  const submitReview = async (e) => {
    e.preventDefault()
    setReviewMsg('')
    setReviewBusy(true)
    try {
      const saved = await addReview(product.id, { userId: MOCK_USER_ID, rating: myRating, comment: myComment })
      setReviews([...reviews, saved])
      setMyComment('')
      setReviewMsg('Thanks! Your review was posted.')
      // Refresh the product to update its average rating
      getProductById(product.id).then(setProduct).catch(() => {})
    } catch (err) {
      setReviewMsg(err.message)
    } finally {
      setReviewBusy(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-6 pb-28 md:pb-6">
      <button onClick={() => navigate(-1)} className="text-sm text-brand-600 hover:text-brand-700 hover:underline transition-micro">
        &larr; Back
      </button>

      <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Gallery column — sticky on desktop, structured for future thumbnails */}
        <div className="md:sticky md:top-20 self-start">
          <div className="bg-white rounded-xl shadow overflow-hidden">
            <img
              src={imgSrc}
              alt={product.name}
              loading="lazy"
              onError={() => { if (imgSrc !== PLACEHOLDER) setImgSrc(PLACEHOLDER) }}
              className="w-full h-72 sm:h-96 object-cover"
            />
          </div>
          <div className="mt-2 flex gap-2">
            <button className="w-16 h-16 rounded-lg overflow-hidden ring-2 ring-brand-600 transition-micro" aria-label="Main image">
              <img src={imgSrc} alt="" className="w-full h-full object-cover"
                onError={(e) => { e.currentTarget.src = PLACEHOLDER }} />
            </button>
          </div>
        </div>

        {/* Details column */}
        <div className="bg-white rounded-xl shadow p-6 flex flex-col">
          <p className="text-xs uppercase tracking-wide text-gray-500">{product.category}</p>
          <h1 className="mt-1 text-2xl font-extrabold tracking-tight text-gray-900">{product.name}</h1>
          <div className="mt-2 flex items-center gap-3">
            <StarRating value={product.averageRating} count={product.reviewCount} />
            <span className={`inline-block text-xs font-medium px-2.5 py-1 rounded-full ${tier.cls}`}>
              {tier.label}
            </span>
          </div>
          <p className="mt-3 text-3xl font-extrabold text-gray-900">{formatUSD(product.price)}</p>

          <div className="mt-4 hidden md:flex items-center gap-3">
            <label className="text-sm text-gray-600">Qty</label>
            <input
              type="number" min="1" max={Math.max(product.stockQuantity ?? 1, 1)}
              value={qty} onChange={(e) => setQty(Math.max(1, Number(e.target.value)))}
              disabled={outOfStock}
              className="w-20 border rounded-md px-2 py-2 text-sm"
            />
            <button
              disabled={outOfStock}
              onClick={handleAdd}
              className="flex-1 py-2.5 rounded-md text-sm font-semibold text-white bg-brand-600 hover:bg-brand-700 active:scale-[0.98] transition-micro disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {outOfStock ? 'Unavailable' : 'Add to Cart'}
            </button>
          </div>
          <p className="mt-3 text-xs text-gray-400 hidden md:block">
            Free delivery over $50 · 30-day returns · 1-year warranty
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="mt-6 bg-white rounded-xl shadow overflow-hidden">
        <div className="flex border-b" role="tablist">
          {visibleTabs.map((t) => (
            <button
              key={t}
              role="tab"
              aria-selected={tab === t}
              onClick={() => setTab(t)}
              className={`flex-1 px-4 py-3 text-sm font-semibold transition-micro ${
                tab === t
                  ? 'text-brand-700 border-b-2 border-brand-600 bg-brand-50/50'
                  : 'text-gray-500 hover:text-gray-800'
              }`}
            >
              {t}
              {t === 'Reviews' && reviews.length > 0 && (
                <span className="ml-1.5 text-xs font-medium bg-gray-100 text-gray-600 rounded-full px-2 py-0.5">
                  {reviews.length}
                </span>
              )}
            </button>
          ))}
        </div>
        <div className="p-6">
          {tab === 'Description' && (
            <p className="text-sm text-gray-700 leading-relaxed animate-fade-up">
              {product.description || 'No description provided for this product.'}
            </p>
          )}
          {tab === 'Specifications' && (
            <dl className="text-sm grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-3 animate-fade-up">
              {[
                ['Category', product.category ?? '—'],
                ['Price', formatUSD(product.price)],
                ['Availability', tier.label],
                ['Average rating', product.averageRating != null ? `${product.averageRating} / 5 (${product.reviewCount} reviews)` : 'Not rated yet'],
                ['Product ID', `#${product.id}`],
              ].map(([k, v]) => (
                <div key={k} className="flex justify-between border-b pb-2">
                  <dt className="text-gray-500">{k}</dt>
                  <dd className="font-medium text-gray-900 text-right">{v}</dd>
                </div>
              ))}
            </dl>
          )}
          {tab === 'Reviews' && showReviews && (
            <div className="animate-fade-up">
              {reviews.length === 0 ? (
                <p className="text-sm text-gray-500">No reviews yet — be the first to rate this product.</p>
              ) : (
                <ul className="space-y-4">
                  {reviews.map((r) => (
                    <li key={r.id} className="flex gap-3">
                      <span className={`shrink-0 inline-flex items-center justify-center w-9 h-9 rounded-full text-sm font-bold ${avatarColor(r.userId)}`}>
                        {initials(`user ${r.userId}`)}
                      </span>
                      <div className="flex-1 border-b pb-3">
                        <div className="flex items-center gap-2">
                          <StarRating value={r.rating} />
                          <span className="text-xs text-gray-400">{timeAgo(r.createdAt)}</span>
                        </div>
                        <p className="mt-1 text-sm text-gray-700">{r.comment || <em className="text-gray-400">No comment</em>}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <form onSubmit={submitReview} className="mt-5 border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-900">Write a review</h3>
                <div className="mt-2">
                  <StarInput value={myRating} onChange={setMyRating} />
                </div>
                <div className="mt-2 flex flex-col sm:flex-row gap-2">
                  <input value={myComment} onChange={(e) => setMyComment(e.target.value)}
                    placeholder="What did you think? (optional)" maxLength={2000}
                    className="flex-1 border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
                  <button disabled={reviewBusy}
                    className="px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro disabled:bg-gray-300">
                    {reviewBusy ? 'Posting…' : 'Post review'}
                  </button>
                </div>
                {reviewMsg && <p className="mt-2 text-sm text-gray-600">{reviewMsg}</p>}
              </form>
            </div>
          )}
        </div>
      </div>

      {related.length > 0 && (
        <div className="mt-8">
          <h2 className="text-lg font-bold text-gray-900">You may also like</h2>
          <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {related.map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        </div>
      )}

      {/* Sticky mobile CTA */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-30 bg-white/95 backdrop-blur border-t px-4 py-3 flex items-center gap-3">
        <div className="flex-1">
          <p className="text-lg font-extrabold text-gray-900 leading-none">{formatUSD(product.price)}</p>
          <p className="text-xs text-gray-500">{tier.label}</p>
        </div>
        <input
          type="number" min="1" max={Math.max(product.stockQuantity ?? 1, 1)}
          value={qty} onChange={(e) => setQty(Math.max(1, Number(e.target.value)))}
          disabled={outOfStock} aria-label="Quantity"
          className="w-14 border rounded-md px-2 py-2 text-sm"
        />
        <button
          disabled={outOfStock}
          onClick={handleAdd}
          className="flex-1 py-2.5 rounded-md text-sm font-semibold text-white bg-brand-600 hover:bg-brand-700 transition-micro disabled:bg-gray-300"
        >
          {outOfStock ? 'Unavailable' : 'Add to Cart'}
        </button>
      </div>
    </div>
  )
}
