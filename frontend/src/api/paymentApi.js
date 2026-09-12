import { createApi } from './client'

const api = createApi(import.meta.env.VITE_PAYMENT_API_URL || 'http://localhost:8084')

// POST /api/v1/payments  body: { orderId, amount, method? }
export const pay = (orderId, amount, method) =>
  api.post('/api/v1/payments', { orderId, amount, method }).then((r) => r.data)

export const paymentsByOrder = (orderId) =>
  api.get(`/api/v1/payments/order/${orderId}`).then((r) => r.data)

export const receipt = (id) =>
  api.get(`/api/v1/payments/${id}`).then((r) => r.data)
