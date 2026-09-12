// Shared axios factory with a centralized error interceptor.
// Normalizes backend GlobalExceptionHandler responses (plain string,
// string[], or {message}) into { message, status } for the UI.
import axios from 'axios'

const TOKEN_KEY = 'omnishop-token'
const REFRESH_KEY = 'omnishop-refresh'
const USER_KEY = 'omnishop-user'

// Single-flight refresh rotation: concurrent 401s share one token request.
let refreshPromise = null

function rotateTokens() {
  if (!refreshPromise) {
    const base = import.meta.env.VITE_USER_API_URL || 'http://localhost:8083'
    let stored = null
    try {
      stored = localStorage.getItem(REFRESH_KEY)
    } catch {
      stored = null
    }
    refreshPromise = (async () => {
      if (!stored) return null
      const r = await axios.post(
        `${base}/api/v1/users/refresh-token`,
        { refreshToken: stored },
        { timeout: 8000 }
      )
      const d = r.data
      try {
        if (d?.token) localStorage.setItem(TOKEN_KEY, d.token)
        if (d?.refreshToken) localStorage.setItem(REFRESH_KEY, d.refreshToken)
        if (d?.user) localStorage.setItem(USER_KEY, JSON.stringify(d.user))
      } catch {
        /* storage blocked — token still returned for this retry */
      }
      return d?.token ?? null
    })()
      .catch(() => {
        // Rotation failed (revoked/reused/expired): drop the dead session so
        // the next navigation lands on login instead of looping 401s.
        try {
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(REFRESH_KEY)
        } catch {
          /* ignore */
        }
        window.dispatchEvent(new Event('omnishop:session-expired'))
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

export function createApi(baseURL) {
  const api = axios.create({ baseURL, timeout: 8000 })

  // Attach JWT when logged in (stored by AuthContext after login/register)
  api.interceptors.request.use((config) => {
    try {
      const token = localStorage.getItem(TOKEN_KEY)
      if (token) config.headers.Authorization = `Bearer ${token}`
    } catch {
      /* storage blocked — requests simply go unauthenticated */
    }
    return config
  })

  api.interceptors.response.use(
    (res) => res,
    async (err) => {
      const status = err.response?.status ?? 0
      const url = err.config?.url ?? ''

      // Access tokens live 15 minutes. On expiry, rotate once with the
      // stored refresh token and replay the original request — this is what
      // keeps order history/notifications working past the first quarter hour.
      // Never intercept the auth endpoints themselves (no refresh loop).
      if (status === 401 && !err.config?._retry
          && !url.includes('/refresh-token') && !url.includes('/login')) {
        err.config._retry = true
        const token = await rotateTokens()
        if (token) {
          err.config.headers.Authorization = `Bearer ${token}`
          return api(err.config)
        }
      }

      const data = err.response?.data
      let message = 'Something went wrong. Please try again.'
      if (status === 401) message = 'Session expired — please log in again.'
      else if (typeof data === 'string' && data.trim()) message = data
      else if (Array.isArray(data) && data.length) message = data.join('; ')
      else if (data?.message) message = data.message
      else if (err.code === 'ECONNABORTED') message = 'Request timed out — is the service running?'
      else if (!err.response) message = 'Service unreachable — is the backend running?'
      return Promise.reject({ message, status, raw: err })
    }
  )
  return api
}
