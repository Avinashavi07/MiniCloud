import RFB from '@novnc/novnc'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiErrorMessage, fetchConsole, openConsoleSession } from '../api/client'
import type { ConsoleConnectionInfo } from '../api/types'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingSkeleton } from '../components/LoadingSkeleton'
import { PageHeader } from '../components/PageHeader'
import { StatusBadge } from '../components/StatusBadge'

export function ConsoleViewerPage() {
  const { name } = useParams()
  const screenRef = useRef<HTMLDivElement>(null)
  const rfbRef = useRef<RFB | null>(null)
  const [info, setInfo] = useState<ConsoleConnectionInfo | null>(null)
  const [status, setStatus] = useState('Connecting')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!name) {
      return
    }
    const vmName = name
    let cancelled = false
    async function connect() {
      setLoading(true)
      setError(null)
      try {
        const consoleInfo = await fetchConsole(vmName)
        if (cancelled) {
          return
        }
        setInfo(consoleInfo)
        if (!consoleInfo.available) {
          setStatus(consoleInfo.status)
          return
        }
        const session = await openConsoleSession(vmName)
        if (cancelled || !screenRef.current) {
          return
        }
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        const url = `${protocol}//${window.location.host}${session.websocketPath}`
        const rfb = new RFB(screenRef.current, url)
        rfb.scaleViewport = true
        rfb.addEventListener('connect', () => setStatus('Connected'))
        rfb.addEventListener('disconnect', () => setStatus('Disconnected'))
        rfb.addEventListener('securityfailure', () => setStatus('VNC security failure'))
        rfbRef.current = rfb
        setStatus('Handshaking')
      } catch (err) {
        if (!cancelled) {
          setError(apiErrorMessage(err, 'Could not open the libvirt console'))
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    void connect()
    return () => {
      cancelled = true
      rfbRef.current?.disconnect()
      rfbRef.current = null
    }
  }, [name])

  if (!name) {
    return <EmptyState title="No VM selected" description="Pick a domain from the consoles list." action={<Link to="/consoles" className="text-sky-300">Back to consoles</Link>} />
  }

  return (
    <div>
      <PageHeader
        title={`Console · ${name}`}
        crumbs={['VDI', 'Consoles', name]}
        description="noVNC in the browser → MiniCloud WebSocket → QEMU VNC on 127.0.0.1. MiniCloud does not decode RFB."
        actions={
          <Link to={`/vms/${encodeURIComponent(name)}`} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:bg-slate-800">
            VM detail
          </Link>
        }
      />
      {info ? (
        <div className="mb-4 flex flex-wrap items-center gap-3 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-300">
          <StatusBadge status={status} />
          <span>{info.protocol ?? 'vnc'} {info.host}:{info.port ?? '—'}</span>
          <span className="text-slate-500">{info.detail}</span>
        </div>
      ) : null}
      {loading ? <LoadingSkeleton /> : null}
      {error ? <ErrorState message={error} /> : null}
      {info && !info.available ? (
        <EmptyState
          title="Console is not available"
          description={info.detail}
          action={<Link to="/consoles" className="text-sky-300">Back to consoles</Link>}
        />
      ) : (
        <div ref={screenRef} className="min-h-[480px] overflow-hidden rounded-xl border border-slate-800 bg-black" />
      )}
    </div>
  )
}
