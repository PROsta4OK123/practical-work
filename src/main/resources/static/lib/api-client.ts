// API клиент для интеграции с Spring Boot backend
import { apiLogger } from './logger'

// Используем относительные пути для работы с Next.js rewrites
const API_BASE_URL = ''

export interface ApiResponse<T = any> {
  success?: boolean
  data?: T
  message?: string
  error?: string
  fileId?: string
  originalName?: string
  size?: number
  status?: string
  queuePosition?: number | null
}

export interface User {
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

export interface AuthResponse {
  success: boolean
  user: User
  token: string
  message: string
}

export interface ProcessedDocument {
  fileId: string
  originalFilename: string
  status: string
  originalSize: number
  processedSize?: number
  processingStartedAt?: string
  processingCompletedAt?: string
  errorMessage?: string
}

export interface QueueStatistics {
  pendingCount: number
  processingCount: number
  completedCount: number
  failedCount: number
  totalInQueue: number
}

export interface ProcessingStatus {
  eventDrivenMode: boolean
  isCurrentlyProcessing: boolean
  activeProcessings: number
  totalUsedThreads: number
  maxAvailableThreads: number
  queueStatistics: QueueStatistics
  systemInfo: {
    mode: string
    description: string
    threadAllocation: {
      small: string
      medium: string
      large: string
    }
  }
}

class ApiClient {
  private getAuthHeaders(): HeadersInit {
    if (typeof window === 'undefined') {
      apiLogger.debug('getAuthHeaders: SSR mode, no token available')
      return { 'Content-Type': 'application/json' }
    }
    
    const token = localStorage.getItem('authToken')
    apiLogger.debug('getAuthHeaders: checking token', {
      hasToken: !!token,
      tokenLength: token?.length || 0,
      tokenPrefix: token?.substring(0, 20) + '...' || 'none'
    })
    
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    }
  }

  private getAuthHeadersForFormData(): HeadersInit {
    if (typeof window === 'undefined') {
      return {}
    }
    
    const token = localStorage.getItem('authToken')
    return {
      ...(token && { 'Authorization': `Bearer ${token}` })
    }
  }

  // === AUTH ENDPOINTS ===

  async login(email: string, password: string): Promise<AuthResponse> {
    const startTime = Date.now()
    apiLogger.apiCall('POST', '/api/auth/login', { email })
    
    try {
      const response = await fetch(`/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      })

      const data = await response.json()
      const duration = Date.now() - startTime
      
      apiLogger.apiResponse('POST', '/api/auth/login', response.status, { 
        success: data.success,
        hasToken: !!data.accessToken,
        duration 
      })
      
      if (response.ok && data.accessToken) {
        localStorage.setItem('authToken', data.accessToken)
        apiLogger.info('Login successful, token stored', { email })
        return { success: true, user: data.user, token: data.accessToken, message: 'Login successful' }
      } else {
        apiLogger.warn('Login failed', { email, error: data.error || data.message })
        return { success: false, user: null as any, token: '', message: data.message || 'Login failed' }
      }
      
    } catch (error) {
      const duration = Date.now() - startTime
      const errorMessage = error instanceof Error ? error.message : String(error)
      apiLogger.error('Login request failed', { email, error: errorMessage, duration })
      throw error
    }
  }

  async register(firstName: string, lastName: string, email: string, password: string): Promise<AuthResponse> {
    const startTime = Date.now()
    apiLogger.apiCall('POST', '/api/auth/register', { firstName, lastName, email })
    
    try {
      const response = await fetch(`/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ firstName, lastName, email, password })
      })

      const data = await response.json()
      const duration = Date.now() - startTime
      
      apiLogger.apiResponse('POST', '/api/auth/register', response.status, { 
        success: data.success,
        hasToken: !!data.accessToken,
        duration 
      })
      
      if (response.ok && data.accessToken) {
        localStorage.setItem('authToken', data.accessToken)
        apiLogger.info('Registration successful, token stored', { firstName, lastName, email })
        return { success: true, user: data.user, token: data.accessToken, message: 'Registration successful' }
      } else {
        apiLogger.warn('Registration failed', { firstName, lastName, email, error: data.error || data.message })
        return { success: false, user: null as any, token: '', message: data.message || 'Registration failed' }
      }
      
    } catch (error) {
      const duration = Date.now() - startTime
      const errorMessage = error instanceof Error ? error.message : String(error)
      apiLogger.error('Registration request failed', { firstName, lastName, email, error: errorMessage, duration })
      throw error
    }
  }

  async logout(): Promise<void> {
    if (typeof window !== 'undefined') {
    localStorage.removeItem('authToken')
    }
  }

  async getProfile(): Promise<User> {
    const response = await fetch(`/api/auth/me`, {
      headers: this.getAuthHeaders()
    })
    
    if (!response.ok) {
      throw new Error('Failed to get profile')
    }
    
    return response.json()
  }

  // === DOCUMENT ENDPOINTS ===

  async uploadDocument(file: File): Promise<ApiResponse> {
    const startTime = Date.now()
    apiLogger.apiCall('POST', '/api/upload', { 
      fileName: file.name, 
      fileSize: file.size, 
      fileType: file.type 
    })
    
    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch(`/api/upload`, {
        method: 'POST',
        headers: this.getAuthHeadersForFormData(),
        body: formData
      })

      const data = await response.json()
      const duration = Date.now() - startTime
      
      apiLogger.apiResponse('POST', '/api/upload', response.status, { 
        fileName: file.name,
        fileSize: file.size,
        success: data.success,
        fileId: data.fileId,
        duration 
      })
      
      if (data.success) {
        apiLogger.info('Document uploaded successfully', { 
          fileName: file.name, 
          fileId: data.fileId,
          originalName: data.originalName
        })
      } else {
        apiLogger.warn('Document upload failed', { 
          fileName: file.name, 
          error: data.error || data.message 
        })
      }
      
      apiLogger.performance('uploadDocument', duration, { fileSize: file.size })
      return data
    } catch (error) {
      const duration = Date.now() - startTime
      const errorMessage = error instanceof Error ? error.message : String(error)
      apiLogger.error('Document upload request failed', { 
        fileName: file.name, 
        fileSize: file.size,
        error: errorMessage, 
        duration 
      })
      throw error
    }
  }

  async formatDocument(file: File): Promise<ApiResponse> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(`/api/format-document`, {
      method: 'POST',
      headers: this.getAuthHeadersForFormData(),
      body: formData
    })

    return response.json()
  }

  async downloadDocument(fileId: string): Promise<Blob> {
    const response = await fetch(`/api/download/${fileId}`, {
      headers: this.getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to download document')
    }

    return response.blob()
  }

  async getDocumentStatus(fileId: string): Promise<ProcessedDocument> {
    const response = await fetch(`/api/document/${fileId}/status`, {
      headers: this.getAuthHeaders()
    })

    return response.json()
  }

  async getProcessingProgress(fileId: string): Promise<any> {
    const response = await fetch(`/api/document/${fileId}/progress`, {
      headers: this.getAuthHeaders()
    })

    if (!response.ok) {
      const error: any = new Error('Failed to fetch progress')
      error.status = response.status
      throw error
    }

    return response.json()
  }

  // === QUEUE ENDPOINTS ===

  async getQueueStatus(): Promise<{ queueStatistics: QueueStatistics, userFiles: any[] }> {
    const startTime = Date.now()
    apiLogger.apiCall('GET', '/api/queue/status', {})
    
    try {
      const response = await fetch(`/api/queue/status`, {
      headers: this.getAuthHeaders()
    })

      const duration = Date.now() - startTime
      
      apiLogger.apiResponse('GET', '/api/queue/status', response.status, { 
        ok: response.ok,
        statusText: response.statusText,
        duration 
      })

      if (!response.ok) {
        const errorText = await response.text()
        apiLogger.error('Queue status request failed', { 
          status: response.status,
          statusText: response.statusText,
          errorText,
          duration 
        })
        throw new Error(`Failed to get queue status: ${response.status} ${response.statusText}`)
      }

      const data = await response.json()
      apiLogger.info('Queue status loaded successfully', { 
        hasQueueStatistics: !!data.queueStatistics,
        hasUserFiles: !!data.userFiles,
        userFilesCount: data.userFiles?.length || 0,
        duration 
      })

      return data
    } catch (error) {
      const duration = Date.now() - startTime
      const errorMessage = error instanceof Error ? error.message : String(error)
      apiLogger.error('Queue status request failed', { error: errorMessage, duration })
      throw error
    }
  }

  async getProcessingStatus(): Promise<ProcessingStatus> {
    const response = await fetch(`/api/queue/processing-status`, {
      headers: this.getAuthHeaders()
    })

    return response.json()
  }

  // === METRICS ENDPOINTS ===

  async getGlobalMetrics(): Promise<any> {
    const response = await fetch(`/api/metrics/global`, {
      headers: this.getAuthHeaders()
    })
    return response.json()
  }

  async purchaseSubscription(planId: string): Promise<ApiResponse> {
    const startTime = Date.now()
    apiLogger.apiCall('POST', '/api/subscription/purchase', { planId })
    
    try {
      const response = await fetch(`/api/subscription/purchase`, {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ planId })
      })

      const data = await response.json()
      const duration = Date.now() - startTime
      
      apiLogger.apiResponse('POST', '/api/subscription/purchase', response.status, { 
        success: data.success,
        planId,
        duration 
      })
      
      if (data.success) {
        apiLogger.info('Subscription purchased successfully', { planId })
      } else {
        apiLogger.warn('Subscription purchase failed', { 
          planId, 
          error: data.error || data.message 
        })
      }
      
      return data
    } catch (error) {
      const duration = Date.now() - startTime
      const errorMessage = error instanceof Error ? error.message : String(error)
      apiLogger.error('Subscription purchase request failed', { 
        planId, 
        error: errorMessage, 
        duration 
      })
      throw error
    }
  }
}

export const apiClient = new ApiClient() 