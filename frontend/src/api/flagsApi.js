import { useEffect, useState } from 'react'
import { createApi } from './client'

const api = createApi(import.meta.env.VITE_PRODUCT_API_URL)

let cache = null
let cacheAt = 0
const TTL_MS = 5 * 60 * 1000

// Feature flags with a 5-minute in-memory cache (toggles are rare,
// and the admin Flags tab busts this by reloading the page after a change).
export function useFlags() {
  const [flags, setFlags] = useState(cache ?? { reviews: true, coupons: true })

  useEffect(() => {
    if (cache && Date.now() - cacheAt < TTL_MS) {
      setFlags(cache)
      return
    }
    api.get('/api/v1/flags')
      .then((r) => {
        cache = { reviews: true, coupons: true, ...r.data }
        cacheAt = Date.now()
        setFlags(cache)
      })
      .catch(() => {})
  }, [])

  return flags
}

export function bustFlagCache() {
  cache = null
}

export const getFlags = () =>
  api.get('/api/v1/flags').then((r) => r.data)

export const setFlag = (key, enabled) =>
  api.put(`/api/v1/flags/${key}`, { enabled })
    .then((r) => {
      bustFlagCache()
      return r.data
    })
