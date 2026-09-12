export default function Footer() {
  return (
    <footer className="mt-12 border-t bg-white">
      <div className="max-w-6xl mx-auto px-4 py-8 grid grid-cols-1 sm:grid-cols-3 gap-6 text-sm">
        <div>
          <p className="font-extrabold text-brand-700 text-base">OmniShop</p>
          <p className="mt-1 text-gray-500">
            Microservices e-commerce demo — Spring Boot + React. Built as a
            Java Full Stack (ACSD30) course project.
          </p>
        </div>
        <div>
          <p className="font-semibold text-gray-900">Stack</p>
          <ul className="mt-1 text-gray-500 space-y-1">
            <li>product-service :8081 · order-service :8082</li>
            <li>PostgreSQL · Docker Compose</li>
            <li>API docs at /swagger-ui.html on each service</li>
          </ul>
        </div>
        <div>
          <p className="font-semibold text-gray-900">Attribution</p>
          <p className="mt-1 text-gray-500">
            Catalog data via webscraper.io sandbox. Product photos via
            Unsplash and LoremFlickr.
          </p>
        </div>
      </div>
      <div className="border-t py-3 text-center text-xs text-gray-400">
        OmniShop demo storefront — items listed here are not for sale.
      </div>
    </footer>
  )
}
