import { createApi } from './client'

const api = createApi(import.meta.env.VITE_ORDER_API_URL)

// POST /api/v1/orders  body: { userId, items, paymentMethod?, couponCode? }
export const placeOrder = (userId, items, paymentMethod, couponCode) =>
  api.post('/api/v1/orders', { userId, items, paymentMethod, couponCode }).then((r) => r.data)

export const getOrderById = (id) =>
  api.get(`/api/v1/orders/${id}`).then((r) => r.data)

export const getOrdersByUser = (userId) =>
  api.get(`/api/v1/orders/user/${userId}`).then((r) => r.data)

export const cancelOrder = (id) =>
  api.put(`/api/v1/orders/${id}/cancel`).then((r) => r.data)

export const updateOrderStatus = (id, status) =>
  api.put(`/api/v1/orders/${id}/status`, { status }).then((r) => r.data)

export const validateCoupon = (code, subtotal) =>
  api.get('/api/v1/coupons/validate', { params: { code, subtotal } }).then((r) => r.data)

export const listCoupons = () =>
  api.get('/api/v1/coupons').then((r) => r.data)

export const createCoupon = (payload) =>
  api.post('/api/v1/coupons', payload).then((r) => r.data)

export const getAnalyticsSummary = () =>
  api.get('/api/v1/orders/analytics/summary').then((r) => r.data)
