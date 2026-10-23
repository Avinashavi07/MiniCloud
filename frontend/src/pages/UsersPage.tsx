import { useEffect, useState } from 'react'
import { apiErrorMessage, fetchUsers } from '../api/client'
import type { User } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { formatDate } from '../lib/format'

export function UsersPage() {
  const { user } = useAuth()
  const [users, setUsers] = useState<User[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(user?.role === 'ADMIN')

  useEffect(() => {
    if (user?.role !== 'ADMIN') {
      return
    }
    fetchUsers()
      .then(setUsers)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [user?.role])

  return (
    <div>
      <PageHeader title="Users" crumbs={['Security', 'Users']} />
      {user?.role !== 'ADMIN' ? (
        <EmptyState title="Administrator only" description="User management is restricted to the ADMIN role." />
      ) : null}
      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} /> : null}
      {user?.role === 'ADMIN' && !loading && !error ? (
        <div className="overflow-hidden rounded-xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-900/80 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Username</th>
                <th className="px-4 py-3">Display name</th>
                <th className="px-4 py-3">Role</th>
                <th className="px-4 py-3">Created</th>
              </tr>
            </thead>
            <tbody>
              {users.map((item) => (
                <tr key={item.id} className="border-t border-slate-800">
                  <td className="px-4 py-3">{item.username}</td>
                  <td className="px-4 py-3">{item.displayName}</td>
                  <td className="px-4 py-3">{item.role}</td>
                  <td className="px-4 py-3 text-slate-400">{formatDate(item.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}
