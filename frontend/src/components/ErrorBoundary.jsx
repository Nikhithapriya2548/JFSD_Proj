import { Component } from 'react'
import { Link } from 'react-router-dom'

// Branded safety net: a crashing section shows a retry card, never a white screen.
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { failed: false }
  }

  static getDerivedStateFromError() {
    return { failed: true }
  }

  render() {
    if (this.state.failed) {
      return (
        <div className="max-w-xl mx-auto px-4 py-10 text-center animate-fade-up">
          <div className="bg-white rounded-xl shadow p-8">
            <div className="mx-auto w-14 h-14 rounded-full bg-red-50 flex items-center justify-center">
              <svg className="w-7 h-7 text-red-500" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M10.3 3.9L1.8 18a2 2 0 001.7 3h17a2 2 0 001.7-3L13.7 3.9a2 2 0 00-3.4 0z" />
              </svg>
            </div>
            <h2 className="mt-3 text-lg font-bold text-gray-900">Something went wrong</h2>
            <p className="mt-1 text-sm text-gray-500">
              This section hit an unexpected error. Your cart and data are safe.
            </p>
            <div className="mt-4 flex justify-center gap-2">
              <button
                onClick={() => this.setState({ failed: false })}
                className="px-4 py-2 rounded-md text-sm font-medium text-white bg-brand-600 hover:bg-brand-700 transition-micro"
              >
                Try again
              </button>
              <Link
                to="/"
                className="px-4 py-2 rounded-md text-sm bg-gray-200 hover:bg-gray-300 transition-micro"
              >
                Go home
              </Link>
            </div>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
