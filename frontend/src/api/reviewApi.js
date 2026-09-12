import { createApi } from './client'

const api = createApi(import.meta.env.VITE_PRODUCT_API_URL)

export const getReviews = (productId) =>
  api.get(`/api/v1/products/${productId}/reviews`).then((r) => r.data)

export const addReview = (productId, { userId, rating, comment }) =>
  api.post(`/api/v1/products/${productId}/reviews`, { userId, rating, comment }).then((r) => r.data)
