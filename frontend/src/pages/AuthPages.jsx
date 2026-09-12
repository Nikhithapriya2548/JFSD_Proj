import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

function PasswordChecklist({ password }) {
  const rules = [
    ['At least 6 characters', password.length >= 6],
    ['Contains a number', /\d/.test(password)],
    ['Contains a letter', /[a-zA-Z]/.test(password)],
  ]
  return (
    <ul className="mt-2 space-y-1 text-xs">
      {rules.map(([label, ok]) => (
        <li key={label} className={`flex items-center gap-1.5 ${ok ? 'text-green-600' : 'text-gray-400'}`}>
          <span className={`inline-flex items-center justify-center w-4 h-4 rounded-full text-[10px] font-bold ${ok ? 'bg-green-600 text-white' : 'bg-gray-200 text-gray-500'}`}>
            {ok ? '✓' : '·'}
          </span>
          {label}
        </li>
      ))}
    </ul>
  )
}

function Shell({ title, subtitle, children }) {
  return (
    <div className="min-h-[70vh] flex items-center justify-center px-4 py-10">
      <div className="w-full max-w-4xl grid grid-cols-1 md:grid-cols-2 rounded-2xl shadow-lift overflow-hidden animate-fade-up">
        <div className="hidden md:flex flex-col justify-center bg-gradient-to-br from-brand-800 via-brand-700 to-brand-500 text-white p-10">
          <img src="/logo.svg" alt="OmniShop" className="w-12 h-12 rounded-xl shadow" />
          <p className="mt-4 text-2xl font-extrabold tracking-tight">OmniShop</p>
          <p className="mt-1 text-brand-100 text-sm">Everything tech, one cart. Laptops, tablets and phones with live stock.</p>
        </div>
        <div className="bg-white p-8">
          <div className="md:hidden flex items-center gap-2">
            <img src="/logo.svg" alt="OmniShop" className="w-8 h-8 rounded-lg" />
            <span className="font-extrabold text-brand-700">OmniShop</span>
          </div>
          <h1 className="mt-2 md:mt-0 text-xl font-extrabold text-gray-900">{title}</h1>
          <p className="text-sm text-gray-500">{subtitle}</p>
          {children}
        </div>
      </div>
    </div>
  )
}

export function LoginPage() {
  const { login } = useAuth()
  const { notify } = useToast()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const data = await login({ email, password })
      notify(`Welcome back, ${data.user?.name ?? 'shopper'}!`)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const inputCls = 'w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500'

  return (
    <Shell title="Welcome back" subtitle="Log in to check out faster and track orders.">
      <form onSubmit={submit} className="mt-4 space-y-3" noValidate>
        <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" placeholder="Email"
          className={inputCls} />
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="Password"
          className={inputCls} />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button disabled={busy}
          className="w-full py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro disabled:bg-gray-300">
          {busy ? 'Logging in…' : 'Log in'}
        </button>
        <p className="text-sm text-gray-500 text-center">
          New here? <Link to="/register" className="text-brand-600 hover:underline font-medium">Create an account</Link>
        </p>
      </form>
    </Shell>
  )
}

export function RegisterPage() {
  const { register } = useAuth()
  const { notify } = useToast()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const emailOk = /.+@.+\..+/.test(email)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (!name.trim()) { setError('Please enter your name.'); return }
    if (!emailOk) { setError('Please enter a valid email address.'); return }
    if (password.length < 6) { setError('Password must be at least 6 characters.'); return }
    setBusy(true)
    try {
      const data = await register({ name, email, password })
      notify(`Account created — welcome, ${data?.name ?? name.trim()}!`)
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const inputCls = (bad) =>
    `w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 ${
      bad ? 'border-red-500 focus:ring-red-400' : 'focus:ring-brand-500'
    }`

  return (
    <Shell title="Create your account" subtitle="One account for checkout, orders and reviews.">
      <form onSubmit={submit} className="mt-4 space-y-3" noValidate>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Full name" className={inputCls(false)} />
        <div>
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" placeholder="Email"
            className={inputCls(email.length > 0 && !emailOk)} />
          {email.length > 0 && !emailOk && <p className="mt-1 text-xs text-red-600">That doesn't look like an email address.</p>}
        </div>
        <div>
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="Password (min 6 chars)"
            className={inputCls(password.length > 0 && password.length < 6)} />
          <PasswordChecklist password={password} />
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button disabled={busy}
          className="w-full py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro disabled:bg-gray-300">
          {busy ? 'Creating account…' : 'Create account'}
        </button>
        <p className="text-sm text-gray-500 text-center">
          Already have one? <Link to="/login" className="text-brand-600 hover:underline font-medium">Log in</Link>
        </p>
      </form>
    </Shell>
  )
}

export function AdminLoginPage() {
  const { login, logout } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const data = await login({ email, password })
      if (data.user?.role !== 'ADMIN') {
        logout()
        setError('This account is not an admin account. Use the customer login instead.')
        return
      }
      navigate('/admin')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const inputCls = 'w-full border rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500'

  return (
    <Shell title="Admin sign in" subtitle="Restricted area — administrators only.">
      <form onSubmit={submit} className="mt-4 space-y-3" noValidate>
        <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" placeholder="Admin email"
          className={inputCls} />
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="Admin password"
          className={inputCls} />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button disabled={busy}
          className="w-full py-2 rounded-md text-sm font-medium text-white bg-gray-900 hover:bg-gray-800 transition-micro disabled:bg-gray-300">
          {busy ? 'Verifying…' : 'Sign in as admin'}
        </button>
        <p className="text-sm text-gray-500 text-center">
          Shopping instead? <Link to="/login" className="text-brand-600 hover:underline font-medium">Customer login</Link>
        </p>
      </form>
    </Shell>
  )
}

export function RequireAdmin({ children }) {
  const { isLoggedIn, isAdmin } = useAuth()
  if (!isLoggedIn || !isAdmin) {
    return <Navigate to="/admin/login" replace />
  }
  return children
}

export function RequireAuth({ children }) {
  const { isLoggedIn } = useAuth()
  if (!isLoggedIn) {
    return <Navigate to="/login" replace />
  }
  return children
}
