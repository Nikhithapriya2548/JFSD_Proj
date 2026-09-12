import { formatUSD } from '../utils/format'

export function StatusBadge({ status }) {
  const colors = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    CONFIRMED: 'bg-blue-100 text-blue-800',
    CANCELLED: 'bg-red-100 text-red-700',
    PLACED: 'bg-yellow-100 text-yellow-800',
    SHIPPED: 'bg-blue-100 text-blue-800',
    DELIVERED: 'bg-green-100 text-green-700',
  }
  return (
    <span className={`inline-block text-xs font-medium px-2 py-1 rounded-full ${colors[status] ?? 'bg-gray-100 text-gray-700'}`}>
      {status}
    </span>
  )
}

export default function OrderSummary({ order }) {
  return (
    <div className="bg-white rounded-lg shadow p-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold">Order #{order.id}</h3>
        <StatusBadge status={order.status} />
      </div>
      <ul className="mt-2 text-sm space-y-1">
        {order.items?.map((i, idx) => (
          <li key={idx} className="flex justify-between">
            <span>Product #{i.productId} × {i.quantity}</span>
            <span>{formatUSD((i.priceAtPurchase ?? 0) * i.quantity)}</span>
          </li>
        ))}
      </ul>
      <p className="mt-2 font-bold">Total: {formatUSD(order.totalAmount)}</p>
    </div>
  )
}
