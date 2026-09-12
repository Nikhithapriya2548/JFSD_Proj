// Shared presentation helpers — single place for currency formatting
// so every price in the UI looks identical.

const usd = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function formatUSD(value) {
  const n = Number(value)
  return Number.isFinite(n) ? usd.format(n) : usd.format(0)
}
