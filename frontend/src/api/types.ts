export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
}

export interface User {
  id: string
  username: string
  displayName: string
  role: 'ADMIN' | 'OPERATOR' | 'VIEWER'
  enabled: boolean
  createdAt: string
}

export interface LoginData {
  token: string
  tokenType: string
  expiresInMs: number
  user: User
}

export interface KvmDetection {
  devicePresent: boolean
  moduleLoaded: boolean
  hardwareVirtualization: boolean
  vendorHint: string
  status: string
  detail: string
}

export interface LibvirtDetection {
  socketPresent: boolean
  socketPath?: string
  serviceState: string
  virshAvailable: boolean
  connectionSucceeded: boolean
  uri: string
  status: string
  detail: string
}

export interface HostMetrics {
  cpuCores?: number
  cpuModel?: string
  cpuUsagePercent?: number
  loadAverage1?: number
  memoryTotalBytes?: number
  memoryAvailableBytes?: number
  memoryUsedBytes?: number
  memoryUsagePercent?: number
  diskTotalBytes?: number
  diskAvailableBytes?: number
  diskUsedBytes?: number
  diskUsagePercent?: number
  diskMount?: string
  kvm?: KvmDetection
  libvirt?: LibvirtDetection
  collectedAt: string
}

export interface Host {
  id: string
  hostname: string
  osName?: string
  osVersion?: string
  kernel?: string
  architecture?: string
  local: boolean
  kvmStatus?: string
  libvirtStatus?: string
  libvirtUri?: string
  lastSeenAt: string
  createdAt: string
  metrics?: HostMetrics
}

export interface LibvirtNodeInfo {
  cpuModel?: string
  cpus?: number
  sockets?: number
  coresPerSocket?: number
  threadsPerCore?: number
  memoryBytes?: number
}

export interface HypervisorCapabilities {
  providerType: string
  uri: string
  kvmAvailable: boolean
  connected: boolean
  status: string
  detail: string
  lifecycleImplemented: boolean
  libvirtVersion?: string
  hypervisorVersion?: string
  node?: LibvirtNodeInfo
}

export interface VmDiskInfo {
  device?: string
  type?: string
  target?: string
  source?: string
}

export interface ConnectedDisk {
  vmName: string
  target?: string
  source?: string
}

export interface StorageVolume {
  name: string
  pool: string
  path?: string
  type?: string
  format?: string
  capacityBytes?: number
  allocationBytes?: number
  usedBy: ConnectedDisk[]
}

export interface VirtualStoragePool {
  name: string
  uuid?: string
  state: string
  active: boolean
  persistent: boolean
  autostart: boolean
  type?: string
  path?: string
  capacityBytes?: number
  allocationBytes?: number
  availableBytes?: number
  volumes: StorageVolume[]
}

export interface HostFilesystem {
  name: string
  mount?: string
  kind: string
  capacityBytes?: number
  availableBytes?: number
  note: string
}

export interface StorageListResponse {
  listingAvailable: boolean
  note: string
  hostFilesystems: HostFilesystem[]
  pools: VirtualStoragePool[]
}

export interface CreatePoolPayload {
  name: string
  path: string
  autostart: boolean
}

export interface CreateVolumePayload {
  name: string
  sizeGb: number
  format: string
}

export interface AttachDiskPayload {
  pool: string
  volume: string
  target?: string
}

export interface VmNicInfo {
  interfaceName?: string
  type?: string
  network?: string
  model?: string
  mac?: string
  ip?: string
  bridge?: string
  hostInterface?: string
}

export interface ConnectedVm {
  vmName: string
  interfaceName?: string
  mac?: string
  ip?: string
}

export interface VirtualNetworkInfo {
  name: string
  uuid?: string
  state: string
  active: boolean
  persistent: boolean
  autostart: boolean
  bridge?: string
  forwardMode?: string
  cidr?: string
  gateway?: string
  dhcpStart?: string
  dhcpEnd?: string
  connectedVms: ConnectedVm[]
}

export interface NetworkListResponse {
  listingAvailable: boolean
  note: string
  networks: VirtualNetworkInfo[]
}

export interface CreateNetworkPayload {
  name: string
  cidr: string
  gateway: string
  dhcpStart: string
  dhcpEnd: string
  autostart: boolean
}

export interface AttachInterfacePayload {
  network: string
  model: string
}

export interface VirtualMachineInfo {
  name: string
  uuid?: string
  hypervisorId?: string
  state: string
  normalizedState: string
  cpuCores?: number
  memoryBytes?: number
  memoryUsedBytes?: number
  osType?: string
  persistent?: boolean
  autostart?: boolean
  disks: VmDiskInfo[]
  nics: VmNicInfo[]
}

export interface AuditEvent {
  id: string
  username?: string
  action: string
  resourceType?: string
  resourceId?: string
  timestamp: string
  status: string
  message?: string
}

export interface DashboardSummary {
  phase: number
  totalHosts: number
  totalVms: number
  runningVms: number
  stoppedVms: number
  totalCpuCores?: number
  allocatedCpuCores?: number
  totalMemoryBytes?: number
  allocatedMemoryBytes?: number
  storageCapacityBytes?: number
  storageUsedBytes?: number
  cpuUsagePercent?: number
  memoryUsagePercent?: number
  storageUsagePercent?: number
  vmStatusDistribution: Record<string, number>
  hypervisor: HypervisorCapabilities
  localHost?: Host
  vms: VirtualMachineInfo[]
  recentEvents: AuditEvent[]
}

export interface VmListResponse {
  listingAvailable: boolean
  note: string
  hypervisor: HypervisorCapabilities
  vms: VirtualMachineInfo[]
}

export interface VmCatalog {
  connected: boolean
  hypervisorStatus: string
  hostCpuCores?: number
  hostMemoryAvailableBytes?: number
  storagePool?: {
    name: string
    state?: string
    capacityBytes?: number
    allocationBytes?: number
    availableBytes?: number
  }
  networks: Array<{ name: string; state: string; autostart: boolean }>
  operatingSystems: Array<{ id: string; label: string; isoAvailable: boolean }>
  isoImages: Array<{ fileName: string; path: string; sizeBytes: number }>
}

export interface CreateVmPayload {
  name: string
  cpuCores: number
  memoryMb: number
  diskGb: number
  operatingSystem: string
  network: string
  startAfterCreate: boolean
}

export interface ConsoleConnectionInfo {
  vmName: string
  protocol?: string
  host?: string
  port?: number
  display?: number
  available: boolean
  status: string
  detail: string
}

export interface ConsoleListResponse {
  listingAvailable: boolean
  note: string
  consoles: ConsoleConnectionInfo[]
}

export interface ConsoleSession {
  console: ConsoleConnectionInfo
  ticket: string
  websocketPath: string
  expiresAtEpochMs: number
}

export interface OvsCapabilities {
  installed: boolean
  connected: boolean
  status: string
  detail: string
  ovsVersion?: string
  dbVersion?: string
}

export interface OvsPortInfo {
  name: string
  uuid?: string
  type?: string
  interfaces: string[]
  ofport?: string
}

export interface OvsBridgeInfo {
  name: string
  uuid?: string
  managedByMiniCloud: boolean
  ports: OvsPortInfo[]
  datapathId?: string
  detail?: string
}

export interface OvsListResponse {
  capabilities: OvsCapabilities
  note: string
  bridges: OvsBridgeInfo[]
}

export interface CreateOvsBridgePayload {
  name: string
}

export interface AttachOvsInterfacePayload {
  bridge: string
  model: string
}

export interface CephCapabilities {
  installed: boolean
  connected: boolean
  status: string
  detail: string
  cephVersion?: string
  clusterFsId?: string
}

export interface CephClusterStatus {
  health: string
  healthDetail: string
  monCount: number
  osdCount: number
  osdUp: number
  osdIn: number
  totalBytes: number
  usedBytes: number
  availableBytes: number
  services: string[]
  rawStatus: string
}

export interface RbdImageInfo {
  name: string
  pool: string
  sizeBytes: number
  usedBytes: number
  format?: string
  usedByVms: string[]
  detail?: string
}

export interface CephPoolInfo {
  name: string
  id?: number
  pgNum?: number
  application?: string
  managedByMiniCloud: boolean
  storedBytes: number
  maxAvailableBytes: number
  objectCount: number
  images: RbdImageInfo[]
  detail?: string
}

export interface CephListResponse {
  capabilities: CephCapabilities
  status?: CephClusterStatus | null
  note: string
  pools: CephPoolInfo[]
}

export interface CreateCephPoolPayload {
  name: string
  pgNum?: number
}

export interface CreateRbdImagePayload {
  name: string
  sizeGb: number
}

export interface AttachRbdDiskPayload {
  pool: string
  image: string
  target?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
