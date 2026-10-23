export function formatBytes(bytes?: number | null): string {
  if (bytes == null) {
    return 'n/a'
  }
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let value = bytes
  let unit = 0
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024
    unit += 1
  }
  return `${value.toFixed(value >= 10 || unit === 0 ? 0 : 1)} ${units[unit]}`
}

export function formatPercent(value?: number | null): string {
  if (value == null || Number.isNaN(value)) {
    return 'n/a'
  }
  return `${value.toFixed(1)}%`
}

export function formatDate(value?: string | null): string {
  if (!value) {
    return 'n/a'
  }
  return new Date(value).toLocaleString()
}
