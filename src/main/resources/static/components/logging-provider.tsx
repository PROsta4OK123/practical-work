"use client"

import React, { useEffect } from 'react'
import { appLogger, uiLogger } from '@/lib/logger'

interface LoggingProviderProps {
  children: React.ReactNode
}

export function LoggingProvider({ children }: LoggingProviderProps) {
  useEffect(() => {
    // Проверяем что мы в браузере
    if (typeof window === 'undefined') {
      return
    }

    // Глобальная инициализация логгирования
    appLogger.info('Application started', {
      userAgent: navigator?.userAgent || 'unknown',
      language: navigator?.language || 'unknown',
      platform: navigator?.platform || 'unknown',
      cookieEnabled: navigator?.cookieEnabled || false,
      onLine: navigator?.onLine || false,
      timestamp: new Date().toISOString()
    })

    // Логируем ошибки JavaScript
    const originalConsoleError = console.error
    console.error = (...args) => {
      appLogger.error('Console Error', { args })
      originalConsoleError.apply(console, args)
    }

    // Логируем необработанные ошибки
    const handleUnhandledError = (event: ErrorEvent) => {
      appLogger.error('Unhandled Error', {
        message: event.message,
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        error: event.error?.toString()
      })
    }

    // Логируем отклоненные промисы
    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      appLogger.error('Unhandled Promise Rejection', {
        reason: event.reason?.toString(),
        stack: event.reason?.stack
      })
    }

    // Логируем переходы по страницам
    const handleBeforeUnload = () => {
      appLogger.info('Page unloading', {
        url: window.location.href,
        timestamp: new Date().toISOString()
      })
    }

    // Логируем изменения видимости страницы
    const handleVisibilityChange = () => {
      if (typeof document !== 'undefined') {
        uiLogger.info('Page visibility changed', {
          hidden: document.hidden,
          visibilityState: document.visibilityState,
          timestamp: new Date().toISOString()
        })
      }
    }

    // Логируем изменения сетевого состояния
    const handleOnline = () => {
      appLogger.info('Network status: online')
    }

    const handleOffline = () => {
      appLogger.warn('Network status: offline')
    }

    // Регистрируем обработчики событий только в браузере
    if (typeof window !== 'undefined') {
      window.addEventListener('error', handleUnhandledError)
      window.addEventListener('unhandledrejection', handleUnhandledRejection)
      window.addEventListener('beforeunload', handleBeforeUnload)
      window.addEventListener('online', handleOnline)
      window.addEventListener('offline', handleOffline)
    }

    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', handleVisibilityChange)
    }

    // Логируем производительность
    if (typeof window !== 'undefined' && 'performance' in window && 'getEntriesByType' in performance) {
      const perfObserver = new PerformanceObserver((list) => {
        list.getEntries().forEach((entry) => {
          if (entry.entryType === 'navigation') {
            const navEntry = entry as PerformanceNavigationTiming
            appLogger.performance('page_load', navEntry.loadEventEnd - navEntry.fetchStart, {
              domContentLoaded: navEntry.domContentLoadedEventEnd - navEntry.fetchStart,
              domInteractive: navEntry.domInteractive - navEntry.fetchStart,
              responseStart: navEntry.responseStart - navEntry.fetchStart
            })
          }
        })
      })
      
      try {
        perfObserver.observe({ entryTypes: ['navigation'] })
      } catch (error) {
        appLogger.debug('Performance observer not supported', { error })
      }
    }

    // Cleanup функция
    return () => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('error', handleUnhandledError)
        window.removeEventListener('unhandledrejection', handleUnhandledRejection)
        window.removeEventListener('beforeunload', handleBeforeUnload)
        window.removeEventListener('online', handleOnline)
        window.removeEventListener('offline', handleOffline)
      }

      if (typeof document !== 'undefined') {
        document.removeEventListener('visibilitychange', handleVisibilityChange)
      }
      
      // Восстанавливаем оригинальный console.error
      console.error = originalConsoleError
    }
  }, [])

  return <>{children}</>
} 