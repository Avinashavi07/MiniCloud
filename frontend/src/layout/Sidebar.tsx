import {
  ClipboardList,
  Cpu,
  HardDrive,
  LayoutDashboard,
  MonitorPlay,
  Network,
  Database,
  Server,
  Settings,
  Share2,
  Shield,
  Users,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'

const sections = [
  {
    title: 'Overview',
    items: [{ to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard }],
  },
  {
    title: 'Infrastructure',
    items: [
      { to: '/hosts', label: 'Hosts', icon: Server },
      { to: '/vms', label: 'Virtual Machines', icon: Cpu },
      { to: '/networks', label: 'Networks', icon: Network },
      { to: '/ovs', label: 'Open vSwitch', icon: Share2 },
      { to: '/storage', label: 'Storage', icon: HardDrive },
      { to: '/ceph', label: 'Ceph', icon: Database },
    ],
  },
  {
    title: 'VDI',
    items: [{ to: '/consoles', label: 'Consoles', icon: MonitorPlay }],
  },
  {
    title: 'Security',
    items: [
      { to: '/users', label: 'Users', icon: Users },
      { to: '/roles', label: 'Roles', icon: Shield },
    ],
  },
  {
    title: 'Operations',
    items: [{ to: '/audit-logs', label: 'Audit Logs', icon: ClipboardList }],
  },
  {
    title: 'System',
    items: [{ to: '/settings', label: 'Settings', icon: Settings }],
  },
]

export function Sidebar({ className = 'hidden lg:flex' }: { className?: string }) {
  return (
    <aside className={`${className} w-64 shrink-0 flex-col border-r border-slate-800 bg-[#0b1220]`}>
      <div className="border-b border-slate-800 px-5 py-4">
        <p className="text-xs font-medium uppercase tracking-[0.2em] text-sky-400">MiniCloud</p>
        <p className="mt-1 text-sm text-slate-400">Infrastructure control plane</p>
      </div>
      <nav className="flex-1 overflow-y-auto px-3 py-4">
        {sections.map((section) => (
          <div key={section.title} className="mb-5">
            <p className="mb-2 px-2 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
              {section.title}
            </p>
            <div className="space-y-1">
              {section.items.map((item) => {
                const Icon = item.icon
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      `flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm ${
                        isActive
                          ? 'bg-sky-500/15 text-sky-200'
                          : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                      }`
                    }
                  >
                    <Icon size={16} />
                    {item.label}
                  </NavLink>
                )
              })}
            </div>
          </div>
        ))}
      </nav>
      <div className="border-t border-slate-800 px-4 py-3 text-xs text-slate-500">Phase 8 · Ceph / RBD</div>
    </aside>
  )
}
