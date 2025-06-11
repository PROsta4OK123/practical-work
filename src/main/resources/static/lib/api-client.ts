// API клиент для интеграции с Spring Boot backend

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

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
    const token = localStorage.getItem('authToken')
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    }
  }

  private getAuthHeadersForFormData(): HeadersInit {
    const token = localStorage.getItem('authToken')
    return {
      ...(token && { 'Authorization': `Bearer ${token}` })
    }
  }

  // === AUTH ENDPOINTS ===

  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    })

    const data = await response.json()
    
    if (data.success && data.token) {
      localStorage.setItem('authToken', data.token)
    }
    
    return data
  }

  async register(firstName: string, lastName: string, email: string, password: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ firstName, lastName, email, password })
    })

    const data = await response.json()
    
    if (data.success && data.token) {
      localStorage.setItem('authToken', data.token)
    }
    
    return data
  }

  async logout(): Promise<void> {
    localStorage.removeItem('authToken')
  }

  async getProfile(): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: this.getAuthHeaders()
    })
    
    if (!response.ok) {
      throw new Error('Failed to get profile')
    }
    
    return response.json()
  }

  // === DOCUMENT ENDPOINTS ===

  async uploadDocument(file: File): Promise<ApiResponse> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(`${API_BASE_URL}/upload`, {
      method: 'POST',
      headers: this.getAuthHeadersForFormData(),
      body: formData
    })

    return response.json()
  }

  async formatDocument(file: File): Promise<ApiResponse> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(`${API_BASE_URL}/format-document`, {
      method: 'POST',
      headers: this.getAuthHeadersForFormData(),
      body: formData
    })

    return response.json()
  }

  async downloadDocument(fileId: string): Promise<Blob> {
    const response = await fetch(`${API_BASE_URL}/download/${fileId}`, {
      headers: this.getAuthHeaders()
    })

    if (!response.ok) {
      throw new Error('Failed to download document')
    }

    return response.blob()
  }

  async getDocumentStatus(fileId: string): Promise<ProcessedDocument> {
    const response = await fetch(`${API_BASE_URL}/document/${fileId}/status`, {
      headers: this.getAuthHeaders()
    })

    return response.json()
  }

  async getProcessingProgress(fileId: string): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/document/${fileId}/progress`, {
      headers: this.getAuthHeaders()
    })

    return response.json()
  }

  // === QUEUE ENDPOINTS ===

  async getQueueStatus(): Promise<{ queueStatistics: QueueStatistics, userFiles: any[] }> {
    const response = await fetch(`${API_BASE_URL}/queue/status`, {
      headers: this.getAuthHeaders()
    })

    return response.json()
  }

  async getProcessingStatus(): Promise<ProcessingStatus> {
    const response = await fetch(`${API_BASE_URL}/queue/processing-status`, {
      headers: this.getAuthHeaders()
    })

    return response.json()
  }

  // === METRICS ENDPOINTS ===

  async getGlobalMetrics(): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/processing/metrics`, {
      headers: this.getAuthHeaders()
    })

    return response.json()
  }
}

export const apiClient = new ApiClient() 