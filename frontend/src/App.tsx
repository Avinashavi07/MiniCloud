import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './layout/AppLayout'
import { AuditLogsPage } from './pages/AuditLogsPage'
import { ConsoleViewerPage } from './pages/ConsoleViewerPage'
import { ConsolesPage } from './pages/ConsolesPage'
import { DashboardPage } from './pages/DashboardPage'
import { HostDetailPage } from './pages/HostDetailPage'
import { HostsPage } from './pages/HostsPage'
import { LoginPage } from './pages/LoginPage'
import { NetworkCreatePage } from './pages/NetworkCreatePage'
import { NetworkDetailPage } from './pages/NetworkDetailPage'
import { NetworksPage } from './pages/NetworksPage'
import { CephCreatePage } from './pages/CephCreatePage'
import { CephDetailPage } from './pages/CephDetailPage'
import { CephPage } from './pages/CephPage'
import { OvsCreatePage } from './pages/OvsCreatePage'
import { OvsDetailPage } from './pages/OvsDetailPage'
import { OvsPage } from './pages/OvsPage'
import { RolesPage } from './pages/RolesPage'
import { SettingsPage } from './pages/SettingsPage'
import { StorageCreatePage } from './pages/StorageCreatePage'
import { StorageDetailPage } from './pages/StorageDetailPage'
import { StoragePage } from './pages/StoragePage'
import { UsersPage } from './pages/UsersPage'
import { VmCreatePage } from './pages/VmCreatePage'
import { VmDetailPage } from './pages/VmDetailPage'
import { VmsPage } from './pages/VmsPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AppLayout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/hosts" element={<HostsPage />} />
        <Route path="/hosts/:id" element={<HostDetailPage />} />
        <Route path="/vms" element={<VmsPage />} />
        <Route path="/vms/create" element={<VmCreatePage />} />
        <Route path="/vms/:id" element={<VmDetailPage />} />
        <Route path="/networks" element={<NetworksPage />} />
        <Route path="/networks/create" element={<NetworkCreatePage />} />
        <Route path="/networks/:name" element={<NetworkDetailPage />} />
        <Route path="/ovs" element={<OvsPage />} />
        <Route path="/ovs/create" element={<OvsCreatePage />} />
        <Route path="/ovs/:name" element={<OvsDetailPage />} />
        <Route path="/storage" element={<StoragePage />} />
        <Route path="/storage/create" element={<StorageCreatePage />} />
        <Route path="/storage/:name" element={<StorageDetailPage />} />
        <Route path="/ceph" element={<CephPage />} />
        <Route path="/ceph/create" element={<CephCreatePage />} />
        <Route path="/ceph/:name" element={<CephDetailPage />} />
        <Route path="/consoles" element={<ConsolesPage />} />
        <Route path="/consoles/:name" element={<ConsoleViewerPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/roles" element={<RolesPage />} />
        <Route path="/audit-logs" element={<AuditLogsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
