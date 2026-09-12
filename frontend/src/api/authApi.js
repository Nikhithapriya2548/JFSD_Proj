import { createApi } from './client'

const api = createApi(
  import.meta.env.VITE_USER_API_URL || 'http://localhost:8083'
)

export const register = (payload) =>
  api.post('/api/v1/users/register', payload).then((r) => r.data)

export const login = (payload) =>
  api.post('/api/v1/users/login', payload).then((r) => r.data)

export const refresh = (refreshToken) =>
  api.post('/api/v1/users/refresh-token', { refreshToken }).then((r) => r.data)
