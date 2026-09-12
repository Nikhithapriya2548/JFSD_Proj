import { createApi } from './client'

const api = createApi(import.meta.env.VITE_PRODUCT_API_URL)

export const getProducts = () => api.get('/api/v1/products').then((r) => r.data)
export const getProductById = (id) => api.get(`/api/v1/products/${id}`).then((r) => r.data)
export const getInStockProducts = () => api.get('/api/v1/products/in-stock').then((r) => r.data)
export const createProduct = (payload) => api.post('/api/v1/products', payload).then((r) => r.data)
export const searchProducts = (params) =>
  api.get('/api/v1/products/search', { params }).then((r) => r.data)
export const updateProduct = (id, payload) => api.put(`/api/v1/products/${id}`, payload).then((r) => r.data)
export const deleteProduct = (id) => api.delete(`/api/v1/products/${id}`).then((r) => r.data)
