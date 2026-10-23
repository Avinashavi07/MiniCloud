import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function LoginPage() {
  const { token, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('ChangeMe!Admin')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (token) {
    return <Navigate to="/dashboard" replace />
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await login(username, password)
      navigate('/dashboard')
    } catch (err) {
      setError(apiErrorMessage(err, 'Invalid username or password'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#070b14] px-4">
      <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-[#0d1526] p-8 shadow-xl">
        <p className="text-xs font-medium uppercase tracking-[0.25em] text-sky-400">MiniCloud</p>
        <h1 className="mt-2 text-2xl font-semibold text-white">Sign in to the control plane</h1>
        <p className="mt-2 text-sm text-slate-400">
          Phase 1 authenticates operators before they can inspect the local Ubuntu host.
        </p>
        <form className="mt-6 space-y-4" onSubmit={onSubmit}>
          <label className="block text-sm">
            <span className="text-slate-300">Username</span>
            <input
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
            />
          </label>
          <label className="block text-sm">
            <span className="text-slate-300">Password</span>
            <input
              type="password"
              className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-sky-500"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
            />
          </label>
          {error ? <p className="text-sm text-red-300">{error}</p> : null}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-sky-600 py-2.5 text-sm font-medium text-white hover:bg-sky-500 disabled:opacity-60"
          >
            {loading ? 'Authenticating…' : 'Sign in'}
          </button>
        </form>
        <p className="mt-4 text-xs text-slate-500">
          Seeded accounts: admin / operator / viewer. Change these passwords before any shared use.
        </p>
      </div>
    </div>
  )
}
