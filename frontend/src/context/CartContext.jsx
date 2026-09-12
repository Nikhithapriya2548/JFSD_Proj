import { createContext, useContext, useEffect, useMemo, useReducer } from 'react'
import { CART_STORAGE_KEY } from '../constants'

const CartContext = createContext(null)

function loadInitialCart() {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function cartReducer(state, action) {
  switch (action.type) {
    case 'ADD': {
      const { product, quantity = 1 } = action.payload
      const existing = state.find((i) => i.productId === product.id)
      if (existing) {
        return state.map((i) =>
          i.productId === product.id
            ? { ...i, quantity: Math.min(i.quantity + quantity, product.stockQuantity) }
            : i
        )
      }
      return [
        ...state,
        {
          productId: product.id,
          name: product.name,
          price: product.price,
          stockQuantity: product.stockQuantity,
          quantity: Math.min(quantity, product.stockQuantity),
        },
      ]
    }
    case 'REMOVE':
      return state.filter((i) => i.productId !== action.payload.productId)
    case 'SET_QTY': {
      const { productId, quantity } = action.payload
      if (quantity <= 0) return state.filter((i) => i.productId !== productId)
      return state.map((i) =>
        i.productId === productId
          ? { ...i, quantity: Math.min(quantity, i.stockQuantity) }
          : i
      )
    }
    case 'CLEAR':
      return []
    default:
      return state
  }
}

export function CartProvider({ children }) {
  const [items, dispatch] = useReducer(cartReducer, undefined, loadInitialCart)

  // Persist cart so refresh doesn't wipe it
  useEffect(() => {
    try {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items))
    } catch {
      /* storage full/blocked — cart still works in memory */
    }
  }, [items])

  const value = useMemo(() => {
    // Same functional pattern as the backend total (.reduce, no loops)
    const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0)
    const count = items.reduce((sum, i) => sum + i.quantity, 0)
    return {
      items,
      total,
      count,
      add: (product, quantity) => dispatch({ type: 'ADD', payload: { product, quantity } }),
      remove: (productId) => dispatch({ type: 'REMOVE', payload: { productId } }),
      setQty: (productId, quantity) => dispatch({ type: 'SET_QTY', payload: { productId, quantity } }),
      clear: () => dispatch({ type: 'CLEAR' }),
    }
  }, [items])

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used inside <CartProvider>')
  return ctx
}
