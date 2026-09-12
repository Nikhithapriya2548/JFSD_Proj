import { useState } from 'react'

// Interactive star input with hover-to-preview. Controlled: value + onChange.
export default function StarInput({ value = 5, onChange }) {
  const [hover, setHover] = useState(0)
  const shown = hover || value
  return (
    <div className="inline-flex items-center gap-0.5" role="radiogroup" aria-label="Choose a rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} star${n > 1 ? 's' : ''}`}
          onMouseEnter={() => setHover(n)}
          onMouseLeave={() => setHover(0)}
          onFocus={() => setHover(n)}
          onBlur={() => setHover(0)}
          onClick={() => onChange(n)}
          className="p-0.5 transition-micro hover:scale-125"
        >
          <span className={`text-2xl leading-none ${n <= shown ? 'text-amber-400' : 'text-gray-300'}`}>
            ★
          </span>
        </button>
      ))}
    </div>
  )
}
