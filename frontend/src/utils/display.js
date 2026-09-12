export function timeAgo(iso) {
  if (!iso) return 'recently'
  const s = Math.floor((Date.now() - new Date(iso).getTime()) / 1000)
  if (s < 60) return 'just now'
  const m = Math.floor(s / 60)
  if (m < 60) return `${m} minute${m > 1 ? 's' : ''} ago`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} hour${h > 1 ? 's' : ''} ago`
  const d = Math.floor(h / 24)
  if (d < 30) return `${d} day${d > 1 ? 's' : ''} ago`
  const mo = Math.floor(d / 30)
  if (mo < 12) return `${mo} month${mo > 1 ? 's' : ''} ago`
  return `${Math.floor(mo / 12)} year${mo >= 24 ? 's' : ''} ago`
}

export function initials(name) {
  return (name || 'U')
    .split(/[\s_@.-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('')
}

const AVATAR_COLORS = [
  'bg-brand-100 text-brand-700', 'bg-amber-100 text-amber-800',
  'bg-green-100 text-green-700', 'bg-purple-100 text-purple-700',
  'bg-rose-100 text-rose-700',
]

export function avatarColor(name) {
  let h = 0
  for (const c of String(name)) h = (h * 31 + c.charCodeAt(0)) % 997
  return AVATAR_COLORS[h % AVATAR_COLORS.length]
}
