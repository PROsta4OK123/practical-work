// Система логгирования для приложения
export enum LogLevel {
  ERROR = 0,
  WARN = 1,
  INFO = 2,
  DEBUG = 3,
  TRACE = 4
}

export interface LogEntry {
  timestamp: string
  level: LogLevel
  message: string
  context?: string
  data?: any
  userId?: number
  sessionId?: string
  requestId?: string
}

class Logger {
  private logLevel: LogLevel = LogLevel.INFO
  private context: string = 'App'
  private sessionId: string
  private userId?: number

  constructor(context?: string) {
    if (context) {
      this.context = context
    }
    
    // Генерируем уникальный ID сессии
    this.sessionId = this.generateSessionId()
    
    // Устанавливаем уровень логирования из переменных окружения
    const envLogLevel = process.env.NEXT_PUBLIC_LOG_LEVEL
    if (envLogLevel && LogLevel[envLogLevel as keyof typeof LogLevel] !== undefined) {
      this.logLevel = LogLevel[envLogLevel as keyof typeof LogLevel]
    }

    this.info('Logger initialized', { context: this.context, sessionId: this.sessionId })
  }

  private generateSessionId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  setUserId(userId: number) {
    this.userId = userId
    this.info('User ID set for logging', { userId })
  }

  clearUserId() {
    this.info('User ID cleared from logging', { userId: this.userId })
    this.userId = undefined
  }

  private createLogEntry(level: LogLevel, message: string, data?: any): LogEntry {
    return {
      timestamp: new Date().toISOString(),
      level,
      message,
      context: this.context,
      data,
      userId: this.userId,
      sessionId: this.sessionId,
      requestId: this.generateRequestId()
    }
  }

  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`
  }

  private shouldLog(level: LogLevel): boolean {
    return level <= this.logLevel
  }

  private formatLogMessage(entry: LogEntry): string {
    const levelNames = ['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE']
    const levelName = levelNames[entry.level] || 'UNKNOWN'
    
    let message = `[${entry.timestamp}] [${levelName}] [${entry.context}]`
    
    if (entry.userId) {
      message += ` [User:${entry.userId}]`
    }
    
    if (entry.requestId) {
      message += ` [${entry.requestId}]`
    }
    
    message += ` ${entry.message}`
    
    return message
  }

  private log(entry: LogEntry) {
    if (!this.shouldLog(entry.level)) return

    const formattedMessage = this.formatLogMessage(entry)
    
    // Отправляем в консоль
    switch (entry.level) {
      case LogLevel.ERROR:
        console.error(formattedMessage, entry.data)
        break
      case LogLevel.WARN:
        console.warn(formattedMessage, entry.data)
        break
      case LogLevel.INFO:
        console.info(formattedMessage, entry.data)
        break
      case LogLevel.DEBUG:
        console.debug(formattedMessage, entry.data)
        break
      case LogLevel.TRACE:
        console.trace(formattedMessage, entry.data)
        break
    }

    // Сохраняем в localStorage для отладки (последние 100 записей)
    this.saveToLocalStorage(entry)
    
    // В продакшене можно добавить отправку на сервер
    if (process.env.NODE_ENV === 'production' && entry.level <= LogLevel.WARN) {
      this.sendToServer(entry)
    }
  }

  private saveToLocalStorage(entry: LogEntry) {
    if (typeof window === 'undefined') {
      // Мы находимся на сервере, localStorage недоступен
      return
    }
    
    try {
      const logs = JSON.parse(localStorage.getItem('app_logs') || '[]')
      logs.push(entry)
      
      // Храним только последние 100 записей
      if (logs.length > 100) {
        logs.splice(0, logs.length - 100)
      }
      
      localStorage.setItem('app_logs', JSON.stringify(logs))
    } catch (error) {
      console.error('Failed to save log to localStorage:', error)
    }
  }

  private async sendToServer(entry: LogEntry) {
    if (typeof window === 'undefined') {
      // Мы находимся на сервере, fetch может не работать как ожидается
      return
    }
    
    try {
      const token = localStorage.getItem('authToken')
      await fetch('/api/logs', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token && { 'Authorization': `Bearer ${token}` })
        },
        body: JSON.stringify(entry)
      })
    } catch (error) {
      console.error('Failed to send log to server:', error)
    }
  }

  // Основные методы логгирования
  error(message: string, data?: any) {
    this.log(this.createLogEntry(LogLevel.ERROR, message, data))
  }

  warn(message: string, data?: any) {
    this.log(this.createLogEntry(LogLevel.WARN, message, data))
  }

  info(message: string, data?: any) {
    this.log(this.createLogEntry(LogLevel.INFO, message, data))
  }

  debug(message: string, data?: any) {
    this.log(this.createLogEntry(LogLevel.DEBUG, message, data))
  }

  trace(message: string, data?: any) {
    this.log(this.createLogEntry(LogLevel.TRACE, message, data))
  }

  // Специальные методы для различных типов событий
  userAction(action: string, data?: any) {
    this.info(`User Action: ${action}`, data)
  }

  apiCall(method: string, url: string, data?: any) {
    this.debug(`API Call: ${method} ${url}`, data)
  }

  apiResponse(method: string, url: string, status: number, data?: any) {
    if (status >= 400) {
      this.error(`API Error: ${method} ${url} - Status ${status}`, data)
    } else {
      this.debug(`API Success: ${method} ${url} - Status ${status}`, data)
    }
  }

  performance(operation: string, duration: number, data?: any) {
    if (duration > 1000) {
      this.warn(`Slow Operation: ${operation} took ${duration}ms`, data)
    } else {
      this.debug(`Performance: ${operation} took ${duration}ms`, data)
    }
  }

  security(event: string, data?: any) {
    this.warn(`Security Event: ${event}`, data)
  }

  // Утилиты для получения логов
  getLogs(): LogEntry[] {
    if (typeof window === 'undefined') {
      return []
    }
    
    try {
      return JSON.parse(localStorage.getItem('app_logs') || '[]')
    } catch {
      return []
    }
  }

  clearLogs() {
    if (typeof window === 'undefined') {
      return
    }
    
    localStorage.removeItem('app_logs')
    this.info('Logs cleared')
  }

  exportLogs(): string {
    const logs = this.getLogs()
    return JSON.stringify(logs, null, 2)
  }
}

// Создаем глобальные экземпляры логгеров для разных частей приложения
export const appLogger = new Logger('App')
export const authLogger = new Logger('Auth')
export const apiLogger = new Logger('API')
export const uiLogger = new Logger('UI')
export const uploadLogger = new Logger('Upload')
export const processingLogger = new Logger('Processing')

// Функция для создания логгера с кастомным контекстом
export const createLogger = (context: string) => new Logger(context)

// Экспортируем основной логгер
export default appLogger 