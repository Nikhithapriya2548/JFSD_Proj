import { createApi } from './client'

const api = createApi(
  import.meta.env.VITE_NOTIFICATION_API_URL || 'http://localhost:8085'
)

export const getNotifications = (userId) =>
  api.get(`/api/v1/notifications/user/${userId}`).then((r) => r.data)
