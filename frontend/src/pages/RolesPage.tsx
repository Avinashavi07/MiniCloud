import { useEffect, useState } from 'react'
import { apiErrorMessage, fetchRoles } from '../api/client'
import { ErrorState } from '../components/ErrorState'
import { PageHeader } from '../components/PageHeader'

export function RolesPage() {
  const [roles, setRoles] = useState<Array<{ name: string; description: string; permissions: string[] }>>([])
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchRoles()
      .then(setRoles)
      .catch((err) => setError(apiErrorMessage(err)))
  }, [])

  return (
    <div>
      <PageHeader title="Roles" crumbs={['Security', 'Roles']} description="Phase 1 RBAC used by the control plane." />
      {error ? <ErrorState message={error} /> : null}
      <div className="grid gap-4 md:grid-cols-3">
        {roles.map((role) => (
          <section key={role.name} className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
            <h2 className="text-lg font-medium text-slate-100">{role.name}</h2>
            <p className="mt-2 text-sm text-slate-400">{role.description}</p>
            <ul className="mt-4 space-y-1 text-sm text-slate-300">
              {role.permissions.map((permission) => (
                <li key={permission}>{permission}</li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  )
}
