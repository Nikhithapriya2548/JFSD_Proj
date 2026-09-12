import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="max-w-xl mx-auto px-4 py-16 text-center animate-fade-up">
      <p className="text-7xl font-extrabold text-brand-200">404</p>
      <h1 className="mt-2 text-xl font-bold text-gray-900">That page doesn't exist</h1>
      <p className="mt-1 text-sm text-gray-500">
        It may have moved, or you followed a stale link. The catalog is still here.
      </p>
      <div className="mt-6 flex justify-center gap-2">
        <Link
          to="/"
          className="px-5 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro"
        >
          Back to catalog
        </Link>
        <Link
          to="/cart"
          className="px-5 py-2 rounded-md text-sm bg-gray-200 hover:bg-gray-300 transition-micro"
        >
          View cart
        </Link>
      </div>
    </div>
  )
}
