import { useEffect, useMemo, useState } from 'react'
import { getProducts, createProduct, updateProduct, deleteProduct } from '../api/productApi'
import { getOrdersByUser, getOrderById, updateOrderStatus } from '../api/orderApi'
import { listCoupons, createCoupon, getAnalyticsSummary } from '../api/orderApi'
import { getFlags, setFlag, bustFlagCache } from '../api/flagsApi'
import { MOCK_USER_ID } from '../constants'
import { formatUSD } from '../utils/format'
import { timeAgo } from '../utils/display'
import { useToast } from '../context/ToastContext'

// NOTE: open route for now — gated behind role===ADMIN in Wave 2 (auth).
const TABS = ['Products', 'Orders', 'Coupons', 'Analytics', 'Reviews', 'Flags']
const STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAYMENT_FAILED']
const PAGE_SIZE = 10

const blankForm = { name: '', description: '', price: '', stockQuantity: '', category: '', imageUrl: '' }

function StatusPill({ status }) {
  const map = {
    PENDING: 'bg-yellow-100 text-yellow-800', CONFIRMED: 'bg-blue-100 text-blue-800',
    PROCESSING: 'bg-indigo-100 text-indigo-800', SHIPPED: 'bg-purple-100 text-purple-800',
    DELIVERED: 'bg-green-100 text-green-700', CANCELLED: 'bg-red-100 text-red-700',
    PAYMENT_FAILED: 'bg-red-100 text-red-700',
  }
  return (
    <span className={`inline-block whitespace-nowrap text-xs font-medium px-2.5 py-1 rounded-full ${map[status] ?? 'bg-gray-100 text-gray-700'}`}>
      {status}
    </span>
  )
}

export default function AdminPage() {
  const { notify } = useToast()
  const [tab, setTab] = useState('Products')
  const [products, setProducts] = useState([])
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [msg, setMsg] = useState('')
  // products table state
  const [query, setQuery] = useState('')
  const [sortKey, setSortKey] = useState('id')
  const [sortDir, setSortDir] = useState(1)
  const [page, setPage] = useState(0)
  // slide-over form state
  const [formOpen, setFormOpen] = useState(false)
  const [form, setForm] = useState(blankForm)
  const [editingId, setEditingId] = useState(null)
  const [formErrors, setFormErrors] = useState({})
  // orders tab state
  const [orderLookup, setOrderLookup] = useState('')
  const [newStatus, setNewStatus] = useState('SHIPPED')
  // coupons tab state
  const [coupons, setCoupons] = useState([])
  const [couponForm, setCouponForm] = useState({ code: '', discountType: 'PERCENTAGE', value: '', minOrderAmount: '', usageLimit: '' })
  // flags tab state
  const [flags, setFlags] = useState({})
  const [summary, setSummary] = useState(null)

  const reload = () => {
    setLoading(true)
    Promise.all([
      getProducts().then(setProducts),
      getOrdersByUser(MOCK_USER_ID).then(setOrders),
      listCoupons().then(setCoupons).catch(() => {}),
      getFlags().then(setFlags).catch(() => {}),
      getAnalyticsSummary().then(setSummary).catch(() => setSummary(null)),
    ])
      .catch((e) => setMsg(e.message))
      .finally(() => setLoading(false))
  }
  useEffect(reload, [])

  const submitCoupon = async (e) => {
    e.preventDefault()
    setMsg('')
    try {
      await createCoupon({
        code: couponForm.code.trim().toUpperCase(),
        discountType: couponForm.discountType,
        value: Number(couponForm.value),
        minOrderAmount: couponForm.minOrderAmount === '' ? null : Number(couponForm.minOrderAmount),
        usageLimit: couponForm.usageLimit === '' ? null : Number(couponForm.usageLimit),
      })
      notify('Coupon created')
      setCouponForm({ code: '', discountType: 'PERCENTAGE', value: '', minOrderAmount: '', usageLimit: '' })
      reload()
    } catch (err) {
      setMsg(err.message)
    }
  }

  const filteredProducts = useMemo(() => {
    const f = products.filter((p) =>
      !query || p.name.toLowerCase().includes(query.toLowerCase()));
    return [...f].sort((a, b) => {
      const av = a[sortKey] ?? '', bv = b[sortKey] ?? ''
      return (typeof av === 'number' ? av - bv : String(av).localeCompare(String(bv))) * sortDir
    });
  }, [products, query, sortKey, sortDir])

  const pageCount = Math.max(1, Math.ceil(filteredProducts.length / PAGE_SIZE))
  const pageRows = filteredProducts.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)

  const toggleSort = (key) => {
    if (sortKey === key) setSortDir((d) => -d)
    else { setSortKey(key); setSortDir(1) }
  }

  // Analytics computed client-side from existing endpoints
  const stats = useMemo(() => {
    const valid = orders.filter((o) => o.status !== 'CANCELLED' && o.status !== 'PAYMENT_FAILED')
    const revenue = valid.reduce((s, o) => s + Number(o.totalAmount ?? 0), 0)
    const byStatus = orders.reduce((m, o) => ({ ...m, [o.status]: (m[o.status] ?? 0) + 1 }), {})
    const byDay = valid.reduce((m, o) => {
      const day = (o.orderDate || '').slice(0, 10) || 'unknown'
      m[day] = (m[day] ?? 0) + Number(o.totalAmount ?? 0)
      return m
    }, {})
    const days = Object.entries(byDay).sort(([a], [b]) => (a < b ? -1 : 1)).slice(-14)
    const maxDay = Math.max(1, ...days.map(([, v]) => v))
    const topRated = [...products]
      .filter((p) => p.reviewCount > 0)
      .sort((a, b) => (b.averageRating ?? 0) - (a.averageRating ?? 0))
      .slice(0, 5)
    const aov = valid.length ? revenue / valid.length : 0
    return { revenue, byStatus, days, maxDay, topRated, count: orders.length, aov }
  }, [orders, products])

  const reviewedProducts = useMemo(
    () => [...products].filter((p) => p.reviewCount > 0)
      .sort((a, b) => (b.reviewCount ?? 0) - (a.reviewCount ?? 0)).slice(0, 20),
    [products]
  )

  const validateForm = () => {
    const errs = {}
    if (!form.name.trim()) errs.name = 'Name is required'
    if (!(Number(form.price) > 0)) errs.price = 'Price must be positive'
    if (form.stockQuantity === '' || Number(form.stockQuantity) < 0) errs.stockQuantity = 'Stock cannot be negative'
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  const submitProduct = async (e) => {
    e.preventDefault()
    if (!validateForm()) return
    setMsg('')
    try {
      const payload = {
        ...form, price: Number(form.price), stockQuantity: Number(form.stockQuantity),
        imageUrl: form.imageUrl || null,
      }
      if (editingId) {
        await updateProduct(editingId, payload)
        notify(`Product #${editingId} updated`)
      } else {
        await createProduct(payload)
        notify('Product created')
      }
      closeForm()
      reload()
    } catch (err) {
      setMsg(err.message)
    }
  }

  const openCreate = () => {
    setEditingId(null); setForm(blankForm); setFormErrors({}); setFormOpen(true)
  }
  const closeForm = () => {
    setFormOpen(false); setEditingId(null); setForm(blankForm); setFormErrors({})
  }
  const startEdit = (p) => {
    setEditingId(p.id)
    setForm({
      name: p.name, description: p.description ?? '', price: p.price,
      stockQuantity: p.stockQuantity, category: p.category ?? '', imageUrl: p.imageUrl ?? '',
    })
    setFormErrors({})
    setFormOpen(true)
  }

  const removeProduct = async (id) => {
    if (!window.confirm(`Delete product #${id}?`)) return
    try {
      await deleteProduct(id)
      notify(`Product #${id} deleted`, 'info')
      reload()
    } catch (err) {
      setMsg(err.message)
    }
  }

  const applyStatus = async () => {
    setMsg('')
    const trimmed = orderLookup.trim()
    if (!trimmed) {
      setMsg('Enter an Order ID first.')
      return
    }
    try {
      await updateOrderStatus(trimmed, newStatus)
      notify(`Order #${trimmed} → ${newStatus}`)
      setOrderLookup('')
      reload()
    } catch (err) {
      setMsg(err.message)
    }
  }

  const fieldCls = (bad) =>
    `w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 ${
      bad ? 'border-red-500 focus:ring-red-400' : 'focus:ring-brand-500'
    }`

  const th = (label, key) => (
    <th className="text-left px-3 py-2 font-semibold">
      <button onClick={() => toggleSort(key)} className="inline-flex items-center gap-1 hover:text-brand-700 transition-micro">
        {label}
        <span className="text-gray-400 text-xs">{sortKey === key ? (sortDir === 1 ? '▲' : '▼') : '△'}</span>
      </button>
    </th>
  )

  return (
    <div className="max-w-6xl mx-auto px-4 py-6 flex flex-col md:flex-row gap-6">
      {/* Sidebar — visually distinct mode from the customer store */}
      <aside className="md:w-52 shrink-0">
        <div className="bg-gray-900 text-white rounded-xl p-4 md:sticky md:top-20">
          <p className="text-xs uppercase tracking-widest text-gray-400">Admin mode</p>
          <p className="font-extrabold">Dashboard</p>
          <nav className="mt-3 flex md:flex-col gap-1 overflow-x-auto">
            {TABS.map((t) => (
              <button
                key={t}
                onClick={() => setTab(t)}
                className={`px-3 py-2 rounded-md text-sm font-medium text-left whitespace-nowrap transition-micro ${
                  tab === t ? 'bg-brand-600 text-white' : 'text-gray-300 hover:bg-gray-800'
                }`}
              >
                {t}
              </button>
            ))}
          </nav>
        </div>
      </aside>

      <div className="flex-1 min-w-0">
        <h1 className="text-2xl font-extrabold text-gray-900">{tab}</h1>
        <p className="text-sm text-gray-500">Open access for now — role gate lands in Wave 2.</p>
        {msg && <div className="mt-3 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">{msg}</div>}

        {loading ? (
          <div className="mt-4 bg-white rounded-xl shadow p-4 animate-pulse space-y-3">
            {[0, 1, 2, 3].map((i) => <div key={i} className="h-10 bg-gray-100 rounded-md" />)}
          </div>
        ) : (
          <>
            {tab === 'Products' && (
              <div className="mt-4 bg-white rounded-xl shadow overflow-hidden animate-fade-up">
                <div className="p-4 flex flex-col sm:flex-row gap-2 border-b">
                  <input value={query} onChange={(e) => { setQuery(e.target.value); setPage(0) }}
                    placeholder="Filter products…" className="flex-1 border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500" />
                  <button onClick={openCreate}
                    className="px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
                    + Add product
                  </button>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm min-w-[640px]">
                    <thead className="bg-gray-50 text-gray-600">
                      <tr>{th('ID', 'id')}{th('Name', 'name')}{th('Price', 'price')}{th('Stock', 'stockQuantity')}
                        <th className="text-left px-3 py-2 font-semibold">Rating</th>
                        <th className="text-right px-3 py-2 font-semibold">Actions</th></tr>
                    </thead>
                    <tbody className="divide-y">
                      {pageRows.map((p) => (
                        <tr key={p.id} className="hover:bg-brand-50/40 transition-micro">
                          <td className="px-3 py-2 text-gray-400">#{p.id}</td>
                          <td className="px-3 py-2 font-medium max-w-[220px] truncate">{p.name}</td>
                          <td className="px-3 py-2">{formatUSD(p.price)}</td>
                          <td className="px-3 py-2">{p.stockQuantity}</td>
                          <td className="px-3 py-2 text-amber-500">
                            {p.averageRating != null ? `★ ${p.averageRating} (${p.reviewCount})` : <span className="text-gray-300">—</span>}
                          </td>
                          <td className="px-3 py-2 text-right whitespace-nowrap">
                            <button onClick={() => startEdit(p)} className="text-brand-600 hover:underline mr-2">Edit</button>
                            <button onClick={() => removeProduct(p.id)} className="text-red-600 hover:underline">Delete</button>
                          </td>
                        </tr>
                      ))}
                      {pageRows.length === 0 && (
                        <tr><td colSpan={6} className="px-3 py-10 text-center text-gray-500">
                          No products match “{query}”. Try a different filter.
                        </td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
                <div className="p-3 flex items-center justify-between border-t text-sm">
                  <span className="text-gray-500">Page {page + 1} of {pageCount} · {filteredProducts.length} products</span>
                  <div className="flex gap-2">
                    <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}
                      className="px-3 py-1 rounded-md border disabled:opacity-40 hover:border-brand-400 transition-micro">Prev</button>
                    <button disabled={page + 1 >= pageCount} onClick={() => setPage((p) => p + 1)}
                      className="px-3 py-1 rounded-md border disabled:opacity-40 hover:border-brand-400 transition-micro">Next</button>
                  </div>
                </div>
              </div>
            )}

            {tab === 'Orders' && (
              <div className="mt-4 bg-white rounded-xl shadow overflow-hidden animate-fade-up">
                <div className="p-4 flex flex-col sm:flex-row gap-2 border-b">
                  <input value={orderLookup} onChange={(e) => setOrderLookup(e.target.value)}
                    placeholder="Order ID" className="border rounded-md px-3 py-2 text-sm" />
                  <select value={newStatus} onChange={(e) => setNewStatus(e.target.value)}
                    className="border rounded-md px-3 py-2 text-sm bg-white">
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                  <button onClick={applyStatus}
                    className="px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
                    Apply status
                  </button>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm min-w-[560px]">
                    <thead className="bg-gray-50 text-gray-600">
                      <tr>
                        <th className="text-left px-3 py-2 font-semibold">Order</th>
                        <th className="text-left px-3 py-2 font-semibold">User</th>
                        <th className="text-left px-3 py-2 font-semibold">Total</th>
                        <th className="text-left px-3 py-2 font-semibold">Status</th>
                        <th className="text-left px-3 py-2 font-semibold">Placed</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {orders.map((o) => (
                        <tr key={o.id} className="hover:bg-brand-50/40 transition-micro">
                          <td className="px-3 py-2 font-medium">#{o.id}</td>
                          <td className="px-3 py-2">User #{o.userId}</td>
                          <td className="px-3 py-2">{formatUSD(o.totalAmount)}</td>
                          <td className="px-3 py-2"><StatusPill status={o.status} /></td>
                          <td className="px-3 py-2 text-gray-500">{timeAgo(o.orderDate)}</td>
                        </tr>
                      ))}
                      {orders.length === 0 && (
                        <tr><td colSpan={5} className="px-3 py-10 text-center text-gray-500">No orders yet.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {tab === 'Coupons' && (
              <div className="mt-4 space-y-4 animate-fade-up">
                <form onSubmit={submitCoupon} className="bg-white rounded-xl shadow p-4 grid grid-cols-2 sm:grid-cols-3 gap-2">
                  <input value={couponForm.code} onChange={(e) => setCouponForm({ ...couponForm, code: e.target.value })}
                    placeholder="CODE *" required className="border rounded-md px-3 py-2 text-sm uppercase" />
                  <select value={couponForm.discountType} onChange={(e) => setCouponForm({ ...couponForm, discountType: e.target.value })}
                    className="border rounded-md px-3 py-2 text-sm bg-white">
                    <option value="PERCENTAGE">% percent</option>
                    <option value="FIXED">$ fixed</option>
                  </select>
                  <input value={couponForm.value} onChange={(e) => setCouponForm({ ...couponForm, value: e.target.value })}
                    placeholder="Value *" required type="number" step="0.01" min="0.01" className="border rounded-md px-3 py-2 text-sm" />
                  <input value={couponForm.minOrderAmount} onChange={(e) => setCouponForm({ ...couponForm, minOrderAmount: e.target.value })}
                    placeholder="Min order (optional)" type="number" step="0.01" min="0" className="border rounded-md px-3 py-2 text-sm" />
                  <input value={couponForm.usageLimit} onChange={(e) => setCouponForm({ ...couponForm, usageLimit: e.target.value })}
                    placeholder="Usage limit (optional)" type="number" min="1" className="border rounded-md px-3 py-2 text-sm" />
                  <button className="px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
                    Create coupon
                  </button>
                </form>
                <div className="bg-white rounded-xl shadow overflow-hidden">
                  <table className="w-full text-sm min-w-[520px]">
                    <thead className="bg-gray-50 text-gray-600">
                      <tr>
                        <th className="text-left px-3 py-2 font-semibold">Code</th>
                        <th className="text-left px-3 py-2 font-semibold">Discount</th>
                        <th className="text-left px-3 py-2 font-semibold">Min order</th>
                        <th className="text-left px-3 py-2 font-semibold">Used</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {coupons.map((c) => (
                        <tr key={c.id} className="hover:bg-brand-50/40 transition-micro">
                          <td className="px-3 py-2 font-mono font-semibold">{c.code}</td>
                          <td className="px-3 py-2">
                            {c.discountType === 'PERCENTAGE' ? `${c.value}%` : formatUSD(c.value)}
                          </td>
                          <td className="px-3 py-2">{c.minOrderAmount != null ? formatUSD(c.minOrderAmount) : '—'}</td>
                          <td className="px-3 py-2">{c.timesUsed ?? 0}{c.usageLimit != null ? ` / ${c.usageLimit}` : ''}</td>
                        </tr>
                      ))}
                      {coupons.length === 0 && (
                        <tr><td colSpan={4} className="px-3 py-10 text-center text-gray-500">No coupons yet — create SAVE10 above.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {tab === 'Analytics' && (
              <div className="mt-4 space-y-4 animate-fade-up">
                {summary && (
                  <div className="bg-white rounded-xl shadow p-5">
                    <h2 className="font-semibold">Server summary <span className="ml-1 text-xs font-normal text-green-600">· live from /analytics/summary</span></h2>
                    <div className="mt-2 grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
                      <div><p className="text-gray-500 text-xs">Total orders</p><p className="text-xl font-bold">{summary.totalOrders}</p></div>
                      <div><p className="text-gray-500 text-xs">Revenue (30d)</p><p className="text-xl font-bold">{formatUSD(Number(summary.revenueTotal ?? 0))}</p></div>
                      <div className="col-span-2"><p className="text-gray-500 text-xs">By status</p>
                        <p className="mt-0.5 text-xs">{Object.entries(summary.statusDistribution ?? {}).map(([s, n]) => `${s}: ${n}`).join(' · ')}</p>
                      </div>
                    </div>
                  </div>
                )}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  {[
                    ['Total orders', String(stats.count), `${Object.keys(stats.byStatus).length} statuses`],
                    ['Total revenue', formatUSD(stats.revenue), 'excl. cancelled / failed · client-side'],
                    ['Avg. order value', formatUSD(stats.aov), 'per successful order'],
                  ].map(([label, big, sub]) => (
                    <div key={label} className="bg-white rounded-xl shadow p-5">
                      <p className="text-xs uppercase tracking-wide text-gray-500">{label}</p>
                      <p className="mt-1 text-3xl font-extrabold text-gray-900">{big}</p>
                      <p className="text-xs text-gray-400">{sub}</p>
                    </div>
                  ))}
                </div>
                <div className="bg-white rounded-xl shadow p-5">
                  <h2 className="font-semibold">Revenue — last 14 days</h2>
                  {stats.days.length === 0 ? (
                    <p className="mt-2 text-sm text-gray-500">No successful orders yet.</p>
                  ) : (
                    <div className="mt-4">
                      <div className="flex items-end gap-1.5 h-40">
                        {stats.days.map(([day, v]) => (
                          <div key={day} className="flex-1 flex flex-col items-center gap-1" title={`${day}: ${formatUSD(v)}`}>
                            <div
                              className="w-full rounded-t bg-gradient-to-t from-brand-700 to-brand-400 transition-micro hover:opacity-80"
                              style={{ height: `${Math.max(4, (v / stats.maxDay) * 100)}%` }}
                            />
                          </div>
                        ))}
                      </div>
                      <div className="flex gap-1.5 mt-1">
                        {stats.days.map(([day]) => (
                          <span key={day} className="flex-1 text-center text-[10px] text-gray-400 truncate">
                            {day.slice(5)}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
                {stats.topRated.length > 0 && (
                  <div className="bg-white rounded-xl shadow p-5">
                    <h2 className="font-semibold">Top rated products</h2>
                    <ul className="mt-2 text-sm space-y-1.5">
                      {stats.topRated.map((p) => (
                        <li key={p.id} className="flex justify-between gap-2">
                          <span className="truncate">{p.name}</span>
                          <span className="text-amber-500 font-medium shrink-0">★ {p.averageRating} ({p.reviewCount})</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}

            {tab === 'Reviews' && (
              <div className="mt-4 bg-white rounded-xl shadow overflow-hidden animate-fade-up">                <div className="overflow-x-auto">
                  <table className="w-full text-sm min-w-[480px]">
                    <thead className="bg-gray-50 text-gray-600">
                      <tr>
                        <th className="text-left px-3 py-2 font-semibold">Product</th>
                        <th className="text-left px-3 py-2 font-semibold">Average</th>
                        <th className="text-left px-3 py-2 font-semibold">Count</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {reviewedProducts.map((p) => (
                        <tr key={p.id} className="hover:bg-brand-50/40 transition-micro">
                          <td className="px-3 py-2 font-medium max-w-[280px] truncate">{p.name}</td>
                          <td className="px-3 py-2 text-amber-500">★ {p.averageRating}</td>
                          <td className="px-3 py-2">{p.reviewCount}</td>
                        </tr>
                      ))}
                      {reviewedProducts.length === 0 && (
                        <tr><td colSpan={3} className="px-3 py-10 text-center text-gray-500">No reviews yet.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {tab === 'Flags' && (
              <div className="mt-4 bg-white rounded-xl shadow p-5 animate-fade-up">
                <h2 className="font-semibold">Feature flags</h2>
                <p className="mt-1 text-sm text-gray-500">
                  Runtime toggles — no redeploy needed. Turning <span className="font-mono">reviews</span> off
                  hides the Reviews tab; turning <span className="font-mono">coupons</span> off hides the
                  checkout coupon field.
                </p>
                <div className="mt-4 space-y-3">
                  {Object.keys(flags).length === 0 && (
                    <p className="text-sm text-gray-500">No flags found.</p>
                  )}
                  {Object.entries(flags).map(([key, enabled]) => (
                    <label key={key} className="flex items-center justify-between gap-3 border rounded-lg px-4 py-3 cursor-pointer hover:bg-gray-50">
                      <span className="font-mono text-sm font-semibold">{key}</span>
                      <button
                        type="button"
                        role="switch"
                        aria-checked={enabled}
                        onClick={async () => {
                          try {
                            const updated = await setFlag(key, !enabled)
                            setFlags((f) => ({ ...f, [updated.key ?? key]: updated.enabled ?? !enabled }))
                            notify(`Flag ${key} ${!enabled ? 'enabled' : 'disabled'}`)
                          } catch (err) {
                            setMsg(err.message)
                          }
                        }}
                        className={`relative w-11 h-6 rounded-full transition-micro ${enabled ? 'bg-green-500' : 'bg-gray-300'}`}
                      >
                        <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-micro ${enabled ? 'left-[22px]' : 'left-0.5'}`} />
                      </button>
                    </label>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Slide-over product form */}
      {formOpen && (
        <div className="fixed inset-0 z-40">
          <div className="absolute inset-0 bg-black/40" onClick={closeForm} />
          <div className="absolute right-0 top-0 bottom-0 w-full max-w-md bg-white shadow-xl p-6 overflow-auto animate-drawer-in">
            <h2 className="text-lg font-bold">{editingId ? `Edit product #${editingId}` : 'Add product'}</h2>
            <form onSubmit={submitProduct} className="mt-4 space-y-3" noValidate>
              <div>
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="Name *" className={fieldCls(formErrors.name)} />
                {formErrors.name && <p className="mt-1 text-xs text-red-600">{formErrors.name}</p>}
              </div>
              <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Description" rows={3} className={fieldCls(false)} />
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <input value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })}
                    placeholder="Price *" type="number" step="0.01" min="0.01" className={fieldCls(formErrors.price)} />
                  {formErrors.price && <p className="mt-1 text-xs text-red-600">{formErrors.price}</p>}
                </div>
                <div>
                  <input value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })}
                    placeholder="Stock *" type="number" min="0" className={fieldCls(formErrors.stockQuantity)} />
                  {formErrors.stockQuantity && <p className="mt-1 text-xs text-red-600">{formErrors.stockQuantity}</p>}
                </div>
              </div>
              <input value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}
                placeholder="Category" className={fieldCls(false)} />
              <input value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                placeholder="Image URL (optional)" className={fieldCls(false)} />
              <div className="flex gap-2 pt-2">
                <button className="flex-1 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro">
                  {editingId ? 'Save changes' : 'Create product'}
                </button>
                <button type="button" onClick={closeForm}
                  className="px-4 py-2 rounded-md text-sm bg-gray-200 hover:bg-gray-300 transition-micro">
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
