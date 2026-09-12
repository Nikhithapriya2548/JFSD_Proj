import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import * as authApi from '../api/authApi'

const AuthContext = createContext(null)
const TOKEN_KEY = 'omnishop-token'
const REFRESH_KEY = 'omnishop-refresh'
const USER_KEY = 'omnishop-user'

function loadUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadUser)
  const timer = useRef(null)

  const clearTimer = () => {
    if (timer.current) {
      clearTimeout(timer.current)
      timer.current = null
    }
  }

  // Silent refresh: rotate the access token a minute before it expires,
  // so active sessions never hit a 401 mid-checkout.
  const scheduleRefresh = useCallback((accessToken, refreshToken) => {
    clearTimer()
    if (!accessToken || !refreshToken) return
    try {
      const payload = JSON.parse(atob(accessToken.split('.')[1]))
      const delay = payload.exp * 1000 - Date.now() - 60000
      if (delay <= 0) return
      timer.current = setTimeout(() => {
        authApi.refresh(refreshToken)
          .then((d) => saveSession(d))
          .catch(() => logout())
      }, delay)
    } catch {
      /* malformed token — leave session alone */
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => clearTimer, [])

  // Resume silent refresh for sessions restored from storage
  useEffect(() => {
    try {
      scheduleRefresh(
        localStorage.getItem(TOKEN_KEY),
        localStorage.getItem(REFRESH_KEY)
      )
    } catch {
      /* storage blocked */
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const saveSession = useCallback((data) => {
    // user-service login returns { token, tokenType, user }; register returns the user
    const token = data.token ?? null
    const profile = data.user ?? data
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token)
      if (data.refreshToken) localStorage.setItem(REFRESH_KEY, data.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(profile))
    } catch {
      /* storage blocked — session lives in memory only */
    }
    setUser(profile)
    scheduleRefresh(token, data.refreshToken ?? localStorage.getItem(REFRESH_KEY))
  }, [scheduleRefresh])

  const logout = useCallback(() => {
    clearTimer()
    try {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_KEY)
      localStorage.removeItem(USER_KEY)
    } catch {
      /* ignore */
    }
    setUser(null)
  }, [])

  // api/client.js fires this when refresh rotation fails (revoked/reused/
  // expired refresh token): drop to logged-out so the UI stops 401-looping.
  useEffect(() => {
    window.addEventListener('omnishop:session-expired', logout)
    return () => window.removeEventListener('omnishop:session-expired', logout)
  }, [logout])

  const value = useMemo(
    () => ({
      user,
      isLoggedIn: !!user,
      isAdmin: user?.role === 'ADMIN',
      login: (payload) => authApi.login(payload).then((d) => { saveSession(d); return d }),
      register: (payload) => authApi.register(payload).then((d) => { saveSession(d); return d }),
      logout,
    }),
    [user, saveSession, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
