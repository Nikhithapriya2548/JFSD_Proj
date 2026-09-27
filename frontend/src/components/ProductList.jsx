import ProductCard from './ProductCard'

export function ProductSkeleton() {
  return (
    <div className="bg-white rounded-lg shadow p-4 animate-pulse">
      <div className="h-32 bg-gray-200 rounded" />
      <div className="h-4 bg-gray-200 rounded mt-4 w-3/4" />
      <div className="h-4 bg-gray-200 rounded mt-2 w-1/2" />
    </div>
  )
}

export default function ProductList({ products, loading }) {
  if (loading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
        {Array.from({ length: 8 }).map((_, i) => <ProductSkeleton key={i} />)}
      </div>
    )
  }
  if (!products.length) {
    return <p className="text-center text-gray-500 py-10">No products match your filters.</p>
  }
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
      {products.map((p) => <ProductCard key={p.id} product={p} />)}
    </div>
  )
}
