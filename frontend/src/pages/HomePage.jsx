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
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fade-up bg-[#f4f5f9] min-h-screen">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
        <div className="flex items-center gap-2">
          <div className="relative flex-1 bg-white rounded-full flex items-center px-4 py-3 shadow-sm border border-gray-100">
             <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5 text-gray-400 mr-3">
              <path strokeLinecap="round" strokeLinejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
            </svg>
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Searching here"
              className="bg-transparent border-none outline-none w-full text-gray-900 placeholder-gray-400 text-sm font-medium"
            />
          </div>
          <button className="bg-white rounded-full w-12 h-12 flex items-center justify-center shadow-sm border border-gray-100 text-gray-600 shrink-0">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
              <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 6h9.75M10.5 6a1.5 1.5 0 1 1-3 0m3 0a1.5 1.5 0 1 0-3 0M3.75 6H7.5m3 12h9.75m-9.75 0a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m-3.75 0H7.5m9-6h3.75m-3.75 0a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m-9.75 0h9.75" />
            </svg>
          </button>
        </div>
      </div>

      {/* Top Banner: Congratulations */}
      <div className="bg-white rounded-[2rem] p-4 flex items-center justify-between shadow-sm mb-4">
        <div className="flex items-center gap-3">
          <div className="bg-gray-50 rounded-full p-2">
            <span className="text-xl">👏</span>
          </div>
          <p className="text-sm font-medium text-gray-900 leading-snug max-w-[200px]">
            Congratulations on your first Pay later purchase!
          </p>
        </div>
        <button className="text-gray-400 hover:text-gray-900">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5"><path strokeLinecap="round" strokeLinejoin="round" d="m4.5 19.5 15-15m0 0H8.25m11.25 0v11.25" /></svg>
        </button>
      </div>

      {/* Grid for All Store & Deals */}
      <div className="grid grid-cols-2 gap-4 mb-4">
        {/* All store */}
        <div className="bg-[#f0f4eb] rounded-[2rem] p-4 relative overflow-hidden flex flex-col justify-between">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">All store</h3>
          <button className="bg-[#ff7a45] text-white text-[11px] font-bold px-3 py-2 rounded-2xl w-full flex items-center gap-2 shadow-sm text-left">
            <div className="bg-white/20 p-1 rounded-full"><svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-3 h-3"><path strokeLinecap="round" strokeLinejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" /></svg></div>
            <span className="leading-tight">Buy Now Split<br/>Costs</span>
          </button>
        </div>
        {/* Deals */}
        <div className="bg-white rounded-[2rem] p-4 flex flex-col justify-between">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Deals</h3>
          <button className="bg-black text-white text-[11px] font-bold px-3 py-2 rounded-2xl w-full flex items-center gap-2 shadow-sm text-left">
            <div className="bg-white/20 p-1 rounded-full"><svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-3 h-3"><path strokeLinecap="round" strokeLinejoin="round" d="M9 14.25l6-6m4.5-3.493V21.75l-3.75-1.5-3.75 1.5-3.75-1.5-3.75 1.5V4.757c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0c1.1.128 1.907 1.077 1.907 2.185zM9.75 9h.008v.008H9.75V9zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm4.125 4.5h.008v.008h-.008V13.5zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" /></svg></div>
            <span className="leading-tight">Discount up<br/>to 80%</span>
          </button>
        </div>
      </div>

      {/* PAN Box */}
      <div className="bg-white rounded-[2rem] p-4 flex items-center justify-between shadow-sm mb-8">
        <div className="flex items-center gap-3">
          <div className="bg-gray-100 text-gray-800 font-bold text-xs rounded-full w-12 h-12 flex items-center justify-center shrink-0">
            PAN
          </div>
          <div>
            <div className="font-bold text-gray-900 text-[15px]">Đ 69</div>
            <div className="text-[11px] text-gray-500 font-medium mt-0.5">Due in 3 days . pan home mall</div>
          </div>
        </div>
        <button className="text-gray-400 hover:text-gray-900 shrink-0">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5"><path strokeLinecap="round" strokeLinejoin="round" d="m4.5 19.5 15-15m0 0H8.25m11.25 0v11.25" /></svg>
        </button>
      </div>
      
      <div className="mb-8">
         <div className="flex items-center justify-between mb-4">
             <h2 className="text-xl font-bold text-gray-900">Products</h2>
             <span className="text-sm font-medium text-gray-400 hover:text-gray-900 cursor-pointer">See all</span>
         </div>
         <div className="flex flex-wrap gap-2 mb-6">
            <button
               onClick={() => setCategory('All')}
               className={`px-5 py-2 rounded-full text-sm font-semibold transition-all shadow-sm ${category === 'All' ? 'bg-gray-900 text-white' : 'bg-white text-gray-700 border border-gray-100 hover:bg-gray-50'}`}
            >
               All
            </button>
          {categories.filter(c => c !== 'All').slice(0, 5).map((c) => (
            <button
              key={c}
              onClick={() => setCategory(c)}
              className={`px-5 py-2 rounded-full text-sm font-semibold transition-all shadow-sm ${category === c ? 'bg-gray-900 text-white' : 'bg-white text-gray-700 border border-gray-100 hover:bg-gray-50'}`}
            >
              {c}
            </button>
          ))}
        </div>
        
        {error && (
          <div className="mb-4 bg-red-50 text-red-700 px-4 py-3 rounded-xl text-sm font-medium border border-red-100">
            Couldn't load products: {error}
          </div>
        )}
        
        <ProductList products={visible} loading={loading} />
      </div>
    </div>
  )
}
