import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { login as loginRequest, logout as logoutRequest } from '../api/client'
import type { User } from '../api/types'

interface AuthContextValue {
  user: User | null
  token: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function readStoredUser(): User | null {
  const raw = localStorage.getItem('minicloud.user')
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('minicloud.token'))
  const [user, setUser] = useState<User | null>(readStoredUser)

  const value = useMemo<AuthContextValue>(() => ({
    user,
    token,
    login: async (username, password) => {
      const result = await loginRequest(username, password)
      localStorage.setItem('minicloud.token', result.token)
      localStorage.setItem('minicloud.user', JSON.stringify(result.user))
      setToken(result.token)
      setUser(result.user)
    },
    logout: async () => {
      try {
        if (localStorage.getItem('minicloud.token')) {
          await logoutRequest()
        }
      } finally {
        localStorage.removeItem('minicloud.token')
        localStorage.removeItem('minicloud.user')
        setToken(null)
        setUser(null)
      }
    },
  }), [token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
