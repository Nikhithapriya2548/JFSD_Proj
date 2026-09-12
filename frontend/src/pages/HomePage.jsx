import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getProducts, searchProducts } from '../api/productApi'
import ProductList from '../components/ProductList'
import CartDrawer from '../components/CartDrawer'

export default function HomePage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [category, setCategory] = useState('All')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('featured')
  const [params] = useSearchParams()
  const urlQ = params.get('q') ?? ''

  useEffect(() => {
    setLoading(true)
    setError('')
    // Navbar searches hit the backend /search endpoint (JPA Specifications);
    // otherwise the full catalog loads and filters apply client-side.
    const loader = urlQ ? searchProducts({ q: urlQ }) : getProducts()
    loader
      .then((data) => { setProducts(data); setSearch(urlQ) })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [urlQ])

  const categories = useMemo(
    () => ['All', ...new Set(products.map((p) => p.category).filter(Boolean))],
    [products]
  )

  // Client-side filter with Array.filter — mirrors the backend's
  // Streams .filter() in ProductServiceImpl.getAllProducts / getInStockProducts.
  // Sort applied after filtering (Array.sort on a copy to avoid mutating state).
  const visible = useMemo(() => {
    const filtered = products
      .filter((p) => category === 'All' || p.category === category)
      .filter((p) => !search || p.name.toLowerCase().includes(search.toLowerCase()));
    const sorted = [...filtered];
    if (sort === 'price-asc') sorted.sort((a, b) => a.price - b.price);
    else if (sort === 'price-desc') sorted.sort((a, b) => b.price - a.price);
    else if (sort === 'name') sorted.sort((a, b) => a.name.localeCompare(b.name));
    else if (sort === 'stock') sorted.sort((a, b) => (b.stockQuantity ?? 0) - (a.stockQuantity ?? 0));
    return sorted;
  }, [products, category, search, sort]);

  return (
    <div className="max-w-6xl mx-auto px-4 py-6">
      <div className="rounded-2xl bg-gradient-to-r from-brand-700 via-brand-600 to-brand-500 px-6 py-8 sm:px-10 text-white shadow animate-fade-up">
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">Everything tech, one cart.</h1>
        <p className="mt-1 text-sm sm:text-base text-brand-100">
          Laptops, tablets and phones — live stock, honest prices, checkout in seconds.
        </p>
      </div>
      <div className="mt-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div className="lg:col-span-2">
        <div className="flex flex-col sm:flex-row gap-2">
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search products…"
            className="flex-1 border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            className="border rounded-md px-3 py-2 text-sm bg-white"
            aria-label="Sort products"
          >
            <option value="featured">Sort: Featured</option>
            <option value="price-asc">Price: Low to High</option>
            <option value="price-desc">Price: High to Low</option>
            <option value="name">Name: A to Z</option>
            <option value="stock">Availability</option>
          </select>
        </div>
        <div className="mt-3 flex flex-wrap gap-2" role="tablist" aria-label="Filter by category">
          {categories.map((c) => (
            <button
              key={c}
              role="tab"
              aria-selected={category === c}
              onClick={() => setCategory(c)}
              className={`px-3.5 py-1.5 rounded-full text-sm font-medium transition-micro ${
                category === c
                  ? 'bg-brand-600 text-white shadow'
                  : 'bg-white text-gray-600 border hover:border-brand-400 hover:text-brand-700'
              }`}
            >
              {c}
            </button>
          ))}
        </div>
        {error && (
          <div className="mt-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
            Couldn't load products: {error}
          </div>
        )}
        <div className="mt-4">
          <ProductList products={visible} loading={loading} />
        </div>
      </div>
      <div>
        <CartDrawer />
      </div>
      </div>
    </div>
  )
}
