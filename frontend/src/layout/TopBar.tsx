import { LogOut, Menu, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'

interface TopBarProps {
  onRefresh?: () => void
  onMenu: () => void
}

export function TopBar({ onRefresh, onMenu }: TopBarProps) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [confirmLogout, setConfirmLogout] = useState(false)

  return (
    <header className="flex items-center justify-between border-b border-slate-800 bg-[#0d1526] px-4 py-3">
      <div className="flex items-center gap-3">
        <button
          type="button"
          className="rounded-md border border-slate-700 p-2 text-slate-300 lg:hidden"
          onClick={onMenu}
        >
          <Menu size={16} />
        </button>
        <div>
          <p className="text-sm font-medium text-slate-100">Control console</p>
          <p className="text-xs text-slate-500">Ubuntu host · live OS metrics</p>
        </div>
      </div>
      <div className="flex items-center gap-3">
        {onRefresh ? (
          <button
            type="button"
            onClick={onRefresh}
            className="inline-flex items-center gap-2 rounded-md border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-800"
          >
            <RefreshCw size={14} />
            Refresh
          </button>
        ) : null}
        <div className="hidden text-right sm:block">
          <p className="text-sm text-slate-200">{user?.displayName}</p>
          <p className="text-xs text-slate-500">{user?.role}</p>
        </div>
        <button
          type="button"
          onClick={() => setConfirmLogout(true)}
          className="rounded-md border border-slate-700 p-2 text-slate-300 hover:bg-slate-800"
        >
          <LogOut size={16} />
        </button>
      </div>
      <ConfirmDialog
        open={confirmLogout}
        title="Sign out?"
        description="You will need to authenticate again to manage infrastructure."
        confirmLabel="Sign out"
        onCancel={() => setConfirmLogout(false)}
        onConfirm={() => {
          setConfirmLogout(false)
          void logout().then(() => navigate('/login'))
        }}
      />
    </header>
  )
}
