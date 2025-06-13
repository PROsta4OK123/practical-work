"use client"

import type React from "react"
import { createContext, useContext, useEffect, useState } from "react"
import { authLogger } from "@/lib/logger"

type User = {
  id: number
  email: string
  firstName: string
  lastName: string
  role: string
  points: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

type AuthContextType = {
  user: User | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (firstName: string, lastName: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  updateUser: (userData: Partial<User>) => void
  addPoints: (points: number) => void
  usePoints: (points: number) => boolean
  refreshToken: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // Check authentication status on mount
  useEffect(() => {
    authLogger.info('AuthProvider initializing')
    checkAuthStatus()
  }, [])

  const checkAuthStatus = async () => {
    const startTime = Date.now()
    authLogger.debug('Checking authentication status')
    
    try {
      const token = localStorage.getItem('authToken')
      if (!token) {
        authLogger.info('No auth token found, user not authenticated')
        setIsLoading(false)
        return
      }

      authLogger.debug('Auth token found, verifying with server')
      const response = await fetch("/api/auth/me", {
        method: "GET",
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      const duration = Date.now() - startTime
      
      if (response.ok) {
        const data = await response.json()
        setUser(data)
        authLogger.setUserId(data.id)
        authLogger.info('User authenticated successfully', { 
          userId: data.id, 
          email: data.email,
          role: data.role,
          duration 
        })
      } else {
        authLogger.warn('Auth token verification failed', { 
          status: response.status,
          duration 
        })
        localStorage.removeItem('authToken')
        localStorage.removeItem('refreshToken')
        setUser(null)
      }
    } catch (error) {
      const duration = Date.now() - startTime
      const errorMessage = error instanceof Error ? error.message : String(error)
      authLogger.error('Auth check failed', { error: errorMessage, duration })
      localStorage.removeItem('authToken')
      localStorage.removeItem('refreshToken')
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }

  const login = async (email: string, password: string) => {
    const startTime = Date.now()
    setIsLoading(true)
    authLogger.userAction('login_attempt', { email })
    
    try {
      authLogger.debug('Sending login request', { url: '/api/auth/login', email })
      
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password }),
      })

      authLogger.debug('Login response received', { 
        status: response.status,
        ok: response.ok,
        statusText: response.statusText 
      })

      const data = await response.json()
      const duration = Date.now() - startTime

      authLogger.debug('Login response data', { 
        hasAccessToken: !!data.accessToken,
        hasUser: !!data.user,
        hasError: !!data.error,
        data: data 
      })

      if (!response.ok) {
        authLogger.warn('Login failed - server error', { 
          email, 
          status: response.status,
          error: data.error || data.message,
          fullResponse: data,
          duration 
        })
        throw new Error(data.error || data.message || "Login failed")
      }

      // Обновляем под новую структуру ответа API
      if (data.accessToken && data.user) {
        localStorage.setItem('authToken', data.accessToken)
        if (data.refreshToken) {
          localStorage.setItem('refreshToken', data.refreshToken)
        }
        setUser(data.user)
        authLogger.setUserId(data.user.id)
        authLogger.info('Login successful', { 
          userId: data.user.id,
          email: data.user.email,
          role: data.user.role,
          tokenLength: data.accessToken.length,
          hasRefreshToken: !!data.refreshToken,
          duration 
        })
        
        // Проверяем что токен действительно сохранился
        const savedToken = localStorage.getItem('authToken')
        authLogger.debug('Token verification after save', {
          tokenSaved: !!savedToken,
          tokenMatches: savedToken === data.accessToken,
          savedTokenLength: savedToken?.length || 0
        })
      } else {
        authLogger.warn('Login failed - invalid response structure', { 
          email,
          hasAccessToken: !!data.accessToken,
          hasUser: !!data.user,
          responseKeys: Object.keys(data),
          fullResponse: data,
          duration 
        })
        throw new Error("Login failed - invalid response structure")
      }
    } catch (error) {
      const duration = Date.now() - startTime
      const errorMessage = error instanceof Error ? error.message : String(error)
      authLogger.error('Login error', { 
        email, 
        error: errorMessage, 
        errorType: error instanceof TypeError ? 'TypeError' : 'Other',
        duration 
      })
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  const register = async (firstName: string, lastName: string, email: string, password: string) => {
    setIsLoading(true)
    try {
      const response = await fetch("/api/auth/register", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ firstName, lastName, email, password }),
      })

      const data = await response.json()

      if (!response.ok) {
        throw new Error(data.error || "Registration failed")
      }

      if (data.accessToken && data.user) {
        localStorage.setItem('authToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)
      setUser(data.user)
      } else {
        throw new Error("Registration failed - invalid response")
      }
    } finally {
      setIsLoading(false)
    }
  }

  const logout = async () => {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
      })
    } catch (error) {
      console.error("Logout error:", error)
    } finally {
      localStorage.removeItem('authToken')
      localStorage.removeItem('refreshToken')
      setUser(null)
    }
  }

  const refreshToken = async () => {
    try {
      // В текущей реализации используем только access token без refresh
      localStorage.removeItem('authToken')
        setUser(null)
    } catch (error) {
      console.error("Token refresh failed:", error)
      setUser(null)
    }
  }

  const updateUser = (userData: Partial<User>) => {
    if (!user) return
    setUser({ ...user, ...userData })
  }

  const addPoints = (points: number) => {
    if (!user) return
    setUser({ ...user, points: user.points + points })
  }

  const usePoints = (points: number): boolean => {
    if (!user || user.points < points) return false
    setUser({ ...user, points: user.points - points })
    return true
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        login,
        register,
        logout,
        updateUser,
        addPoints,
        usePoints,
        refreshToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
