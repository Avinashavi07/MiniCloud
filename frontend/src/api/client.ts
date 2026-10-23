import axios from 'axios'
import type { ApiResponse, AttachDiskPayload, AttachInterfacePayload, AttachOvsInterfacePayload, AttachRbdDiskPayload, AuditEvent, CephListResponse, CephPoolInfo, ConsoleConnectionInfo, ConsoleListResponse, ConsoleSession, CreateCephPoolPayload, CreateNetworkPayload, CreateOvsBridgePayload, CreatePoolPayload, CreateRbdImagePayload, CreateVmPayload, CreateVolumePayload, DashboardSummary, Host, HostMetrics, HypervisorCapabilities, LoginData, NetworkListResponse, OvsBridgeInfo, OvsListResponse, PageResponse, RbdImageInfo, StorageListResponse, StorageVolume, User, VirtualMachineInfo, VirtualNetworkInfo, VirtualStoragePool, VmCatalog, VmListResponse } from './types'

const client = axios.create()

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('minicloud.token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem('minicloud.token')
      localStorage.removeItem('minicloud.user')
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

export async function login(username: string, password: string): Promise<LoginData> {
  const { data } = await client.post<ApiResponse<LoginData>>('/api/v1/auth/login', { username, password })
  return data.data
}

export async function logout(): Promise<void> {
  await client.post('/api/v1/auth/logout')
}

export async function fetchMe(): Promise<User> {
  const { data } = await client.get<ApiResponse<User>>('/api/v1/auth/me')
  return data.data
}

export async function fetchDashboard(): Promise<DashboardSummary> {
  const { data } = await client.get<ApiResponse<DashboardSummary>>('/api/v1/dashboard')
  return data.data
}

export async function fetchHosts(): Promise<Host[]> {
  const { data } = await client.get<ApiResponse<Host[]>>('/api/v1/hosts')
  return data.data
}

export async function fetchHost(id: string): Promise<Host> {
  const { data } = await client.get<ApiResponse<Host>>(`/api/v1/hosts/${id}`)
  return data.data
}

export async function fetchHostMetrics(id: string): Promise<HostMetrics> {
  const { data } = await client.get<ApiResponse<HostMetrics>>(`/api/v1/hosts/${id}/metrics`)
  return data.data
}

export async function fetchVms(): Promise<VmListResponse> {
  const { data } = await client.get<ApiResponse<VmListResponse>>('/api/v1/vms')
  return data.data
}

export async function fetchVm(name: string): Promise<VirtualMachineInfo> {
  const { data } = await client.get<ApiResponse<VirtualMachineInfo>>(`/api/v1/vms/${encodeURIComponent(name)}`)
  return data.data
}

export async function fetchVmCatalog(): Promise<VmCatalog> {
  const { data } = await client.get<ApiResponse<VmCatalog>>('/api/v1/vm-catalog')
  return data.data
}

export async function createVm(payload: CreateVmPayload): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>('/api/v1/vms', payload)
  return data.data
}

export async function startVm(name: string): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>(`/api/v1/vms/${encodeURIComponent(name)}/start`)
  return data.data
}

export async function stopVm(name: string): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>(`/api/v1/vms/${encodeURIComponent(name)}/stop`)
  return data.data
}

export async function restartVm(name: string): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>(`/api/v1/vms/${encodeURIComponent(name)}/restart`)
  return data.data
}

export async function shutdownVm(name: string): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>(`/api/v1/vms/${encodeURIComponent(name)}/shutdown`)
  return data.data
}

export async function deleteVm(name: string): Promise<void> {
  await client.delete(`/api/v1/vms/${encodeURIComponent(name)}`)
}

export async function fetchHypervisorStatus(): Promise<HypervisorCapabilities> {
  const { data } = await client.get<ApiResponse<HypervisorCapabilities>>('/api/v1/hypervisor/status')
  return data.data
}

export async function fetchAuditEvents(page = 0, size = 20): Promise<PageResponse<AuditEvent>> {
  const { data } = await client.get<ApiResponse<PageResponse<AuditEvent>>>('/api/v1/audit-events', {
    params: { page, size },
  })
  return data.data
}

export async function fetchUsers(): Promise<User[]> {
  const { data } = await client.get<ApiResponse<User[]>>('/api/v1/users')
  return data.data
}

export async function fetchRoles(): Promise<Array<{ name: string; description: string; permissions: string[] }>> {
  const { data } = await client.get<ApiResponse<Array<{ name: string; description: string; permissions: string[] }>>>('/api/v1/roles')
  return data.data
}

export async function fetchNetworks(): Promise<NetworkListResponse> {
  const { data } = await client.get<ApiResponse<NetworkListResponse>>('/api/v1/networks')
  return data.data
}

export async function fetchNetwork(name: string): Promise<VirtualNetworkInfo> {
  const { data } = await client.get<ApiResponse<VirtualNetworkInfo>>(`/api/v1/networks/${encodeURIComponent(name)}`)
  return data.data
}

export async function createNetwork(payload: CreateNetworkPayload): Promise<VirtualNetworkInfo> {
  const { data } = await client.post<ApiResponse<VirtualNetworkInfo>>('/api/v1/networks', payload)
  return data.data
}

export async function startNetwork(name: string): Promise<VirtualNetworkInfo> {
  const { data } = await client.post<ApiResponse<VirtualNetworkInfo>>(`/api/v1/networks/${encodeURIComponent(name)}/start`)
  return data.data
}

export async function stopNetwork(name: string): Promise<VirtualNetworkInfo> {
  const { data } = await client.post<ApiResponse<VirtualNetworkInfo>>(`/api/v1/networks/${encodeURIComponent(name)}/stop`)
  return data.data
}

export async function setNetworkAutostart(name: string, autostart: boolean): Promise<VirtualNetworkInfo> {
  const { data } = await client.post<ApiResponse<VirtualNetworkInfo>>(`/api/v1/networks/${encodeURIComponent(name)}/autostart`, { autostart })
  return data.data
}

export async function deleteNetwork(name: string): Promise<void> {
  await client.delete(`/api/v1/networks/${encodeURIComponent(name)}`)
}

export async function attachVmInterface(vmName: string, payload: AttachInterfacePayload): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>(`/api/v1/vms/${encodeURIComponent(vmName)}/interfaces`, payload)
  return data.data
}

export async function detachVmInterface(vmName: string, mac: string): Promise<VirtualMachineInfo> {
  const { data } = await client.delete<ApiResponse<VirtualMachineInfo>>(
    `/api/v1/vms/${encodeURIComponent(vmName)}/interfaces/${encodeURIComponent(mac)}`,
  )
  return data.data
}

export async function fetchStorage(): Promise<StorageListResponse> {
  const { data } = await client.get<ApiResponse<StorageListResponse>>('/api/v1/storage')
  return data.data
}

export async function fetchStoragePool(name: string): Promise<VirtualStoragePool> {
  const { data } = await client.get<ApiResponse<VirtualStoragePool>>(`/api/v1/storage/${encodeURIComponent(name)}`)
  return data.data
}

export async function createStoragePool(payload: CreatePoolPayload): Promise<VirtualStoragePool> {
  const { data } = await client.post<ApiResponse<VirtualStoragePool>>('/api/v1/storage', payload)
  return data.data
}

export async function startStoragePool(name: string): Promise<VirtualStoragePool> {
  const { data } = await client.post<ApiResponse<VirtualStoragePool>>(`/api/v1/storage/${encodeURIComponent(name)}/start`)
  return data.data
}

export async function stopStoragePool(name: string): Promise<VirtualStoragePool> {
  const { data } = await client.post<ApiResponse<VirtualStoragePool>>(`/api/v1/storage/${encodeURIComponent(name)}/stop`)
  return data.data
}

export async function setStorageAutostart(name: string, autostart: boolean): Promise<VirtualStoragePool> {
  const { data } = await client.post<ApiResponse<VirtualStoragePool>>(`/api/v1/storage/${encodeURIComponent(name)}/autostart`, { autostart })
  return data.data
}

export async function deleteStoragePool(name: string): Promise<void> {
  await client.delete(`/api/v1/storage/${encodeURIComponent(name)}`)
}

export async function createVolume(pool: string, payload: CreateVolumePayload): Promise<StorageVolume> {
  const { data } = await client.post<ApiResponse<StorageVolume>>(`/api/v1/storage/${encodeURIComponent(pool)}/volumes`, payload)
  return data.data
}

export async function deleteVolume(pool: string, volume: string): Promise<void> {
  await client.delete(`/api/v1/storage/${encodeURIComponent(pool)}/volumes/${encodeURIComponent(volume)}`)
}

export async function attachVmDisk(vmName: string, payload: AttachDiskPayload): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>(`/api/v1/vms/${encodeURIComponent(vmName)}/disks`, payload)
  return data.data
}

export async function detachVmDisk(vmName: string, target: string): Promise<VirtualMachineInfo> {
  const { data } = await client.delete<ApiResponse<VirtualMachineInfo>>(
    `/api/v1/vms/${encodeURIComponent(vmName)}/disks/${encodeURIComponent(target)}`,
  )
  return data.data
}

export async function fetchConsoles(): Promise<ConsoleListResponse> {
  const { data } = await client.get<ApiResponse<ConsoleListResponse>>('/api/v1/consoles')
  return data.data
}

export async function fetchConsole(name: string): Promise<ConsoleConnectionInfo> {
  const { data } = await client.get<ApiResponse<ConsoleConnectionInfo>>(`/api/v1/vms/${encodeURIComponent(name)}/console`)
  return data.data
}

export async function openConsoleSession(name: string): Promise<ConsoleSession> {
  const { data } = await client.post<ApiResponse<ConsoleSession>>(`/api/v1/vms/${encodeURIComponent(name)}/console/sessions`)
  return data.data
}

export async function fetchOvs(): Promise<OvsListResponse> {
  const { data } = await client.get<ApiResponse<OvsListResponse>>('/api/v1/ovs')
  return data.data
}

export async function fetchOvsBridge(name: string): Promise<OvsBridgeInfo> {
  const { data } = await client.get<ApiResponse<OvsBridgeInfo>>(`/api/v1/ovs/bridges/${encodeURIComponent(name)}`)
  return data.data
}

export async function createOvsBridge(payload: CreateOvsBridgePayload): Promise<OvsBridgeInfo> {
  const { data } = await client.post<ApiResponse<OvsBridgeInfo>>('/api/v1/ovs/bridges', payload)
  return data.data
}

export async function deleteOvsBridge(name: string): Promise<void> {
  await client.delete(`/api/v1/ovs/bridges/${encodeURIComponent(name)}`)
}

export async function attachVmOvsInterface(vmName: string, payload: AttachOvsInterfacePayload): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>(
    `/api/v1/vms/${encodeURIComponent(vmName)}/ovs-interfaces`,
    payload,
  )
  return data.data
}

export async function fetchCeph(): Promise<CephListResponse> {
  const { data } = await client.get<ApiResponse<CephListResponse>>('/api/v1/ceph')
  return data.data
}

export async function fetchCephPool(name: string): Promise<CephPoolInfo> {
  const { data } = await client.get<ApiResponse<CephPoolInfo>>(`/api/v1/ceph/pools/${encodeURIComponent(name)}`)
  return data.data
}

export async function createCephPool(payload: CreateCephPoolPayload): Promise<CephPoolInfo> {
  const { data } = await client.post<ApiResponse<CephPoolInfo>>('/api/v1/ceph/pools', payload)
  return data.data
}

export async function deleteCephPool(name: string): Promise<void> {
  await client.delete(`/api/v1/ceph/pools/${encodeURIComponent(name)}`)
}

export async function createRbdImage(pool: string, payload: CreateRbdImagePayload): Promise<RbdImageInfo> {
  const { data } = await client.post<ApiResponse<RbdImageInfo>>(
    `/api/v1/ceph/pools/${encodeURIComponent(pool)}/images`,
    payload,
  )
  return data.data
}

export async function deleteRbdImage(pool: string, image: string): Promise<void> {
  await client.delete(`/api/v1/ceph/pools/${encodeURIComponent(pool)}/images/${encodeURIComponent(image)}`)
}

export async function attachVmRbdDisk(vmName: string, payload: AttachRbdDiskPayload): Promise<VirtualMachineInfo> {
  const { data } = await client.post<ApiResponse<VirtualMachineInfo>>(
    `/api/v1/vms/${encodeURIComponent(vmName)}/rbd-disks`,
    payload,
  )
  return data.data
}

export async function fetchSystemInfo(): Promise<Record<string, unknown>> {
  const { data } = await client.get<ApiResponse<Record<string, unknown>>>('/api/v1/system/info')
  return data.data
}

export function apiErrorMessage(error: unknown, fallback = 'Request failed'): string {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === 'string' && message.length > 0) {
      return message
    }
  }
  return fallback
}
