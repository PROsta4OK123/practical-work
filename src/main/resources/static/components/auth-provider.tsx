"use client"

import type React from "react"
import { createContext, useContext, useEffect, useState } from "react"

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
    checkAuthStatus()
  }, [])

  const checkAuthStatus = async () => {
    try {
      const token = localStorage.getItem('authToken')
      if (!token) {
        setIsLoading(false)
        return
      }

      const response = await fetch("http://localhost:8080/auth/me", {
        method: "GET",
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        const data = await response.json()
        setUser(data)
      } else {
        localStorage.removeItem('authToken')
        setUser(null)
      }
    } catch (error) {
      console.error("Auth check failed:", error)
      localStorage.removeItem('authToken')
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }

  const login = async (email: string, password: string) => {
    setIsLoading(true)
    try {
      const response = await fetch("http://localhost:8080/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password }),
      })

      const data = await response.json()

      if (!response.ok) {
        throw new Error(data.error || "Login failed")
      }

      if (data.success && data.token) {
        localStorage.setItem('authToken', data.token)
      setUser(data.user)
      } else {
        throw new Error(data.message || "Login failed")
      }
    } finally {
      setIsLoading(false)
    }
  }

  const register = async (firstName: string, lastName: string, email: string, password: string) => {
    setIsLoading(true)
    try {
      const response = await fetch("http://localhost:8080/auth/register", {
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

      if (data.success && data.token) {
        localStorage.setItem('authToken', data.token)
      setUser(data.user)
      } else {
        throw new Error(data.message || "Registration failed")
      }
    } finally {
      setIsLoading(false)
    }
  }

  const logout = async () => {
    try {
      await fetch("http://localhost:8080/auth/logout", {
        method: "POST",
      })
    } catch (error) {
      console.error("Logout error:", error)
    } finally {
      localStorage.removeItem('authToken')
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
