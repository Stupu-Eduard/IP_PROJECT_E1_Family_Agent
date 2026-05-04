import type { ReactNode } from 'react'
import React from 'react'

type Props = {
  children: ReactNode
}

type State = {
  hasError: boolean
  error: unknown
}

const isDev = import.meta.env.DEV

export default class AppErrorBoundary extends React.Component<Props, State> {
  state: State = { hasError: false, error: null }

  static getDerivedStateFromError(error: unknown): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: unknown) {
    // Keep a console trace so the error is still visible in DevTools.
    console.error('App crashed with an uncaught error:', error)
  }

  private handleReload = () => {
    window.location.reload()
  }

  render() {
    if (!this.state.hasError) return this.props.children

    const message =
      this.state.error instanceof Error
        ? this.state.error.message
        : typeof this.state.error === 'string'
          ? this.state.error
          : 'Unknown error'

    const stack = this.state.error instanceof Error ? this.state.error.stack : null

    return (
      <div className="min-h-screen bg-[#FAF8F5] font-sans flex items-center justify-center p-6">
        <div className="w-full max-w-[720px] bg-white border border-[#EDE9E3] rounded-[14px] p-6 shadow-sm">
          <div className="text-[18px] font-medium text-[#2D2926]">A apărut o eroare în aplicație</div>
          <div className="mt-2 text-[13px] text-[#9A8A7C]">Pagina a intrat într-o stare invalidă. Poți reîncărca aplicația.</div>

          <div className="mt-4 text-[13px] text-[#2D2926]">
            <span className="font-medium">Mesaj:</span> {message}
          </div>

          {isDev && stack && (
            <pre className="mt-4 text-[12px] text-[#2D2926] bg-[#FAF8F5] border border-[#EDE9E3] rounded-[10px] p-3 overflow-auto whitespace-pre-wrap">
{stack}
            </pre>
          )}

          <button
            type="button"
            onClick={this.handleReload}
            className="mt-5 h-10 px-4 rounded-[10px] bg-[#2D2926] text-white text-[13px] hover:opacity-90 transition-opacity"
          >
            Reîncarcă
          </button>
        </div>
      </div>
    )
  }
}
