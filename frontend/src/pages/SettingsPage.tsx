import { useEffect, useState } from 'react'
import { apiErrorMessage, fetchSystemInfo } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ErrorState } from '../components/ErrorState'
import { PageHeader } from '../components/PageHeader'

export function SettingsPage() {
  const { user } = useAuth()
  const [info, setInfo] = useState<Record<string, unknown> | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchSystemInfo()
      .then(setInfo)
      .catch((err) => setError(apiErrorMessage(err)))
  }, [])

  return (
    <div>
      <PageHeader title="Settings" crumbs={['System', 'Settings']} description="Control-plane identity and current implementation phase." />
      {error ? <ErrorState message={error} /> : null}
      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 text-sm">
          <h2 className="font-medium text-slate-200">Signed-in operator</h2>
          <p className="mt-3 text-slate-300">{user?.displayName} ({user?.username})</p>
          <p className="text-slate-500">Role {user?.role}</p>
        </section>
        <section className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 text-sm">
          <h2 className="font-medium text-slate-200">Control plane</h2>
          <pre className="mt-3 overflow-auto text-xs text-slate-300">{JSON.stringify(info, null, 2)}</pre>
        </section>
      </div>
    </div>
  )
}
