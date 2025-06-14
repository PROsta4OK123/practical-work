"use client"

import { useState, useEffect } from "react"
import { Progress } from "@/components/ui/progress"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Clock, CheckCircle, XCircle, FileText, Zap, Download, ExternalLink, Loader2 } from "lucide-react"
import { apiClient } from "@/lib/api-client"
import { useToast } from "@/hooks/use-toast"
import Link from "next/link"

interface DocumentProgressProps {
  fileId: string
  filename: string
  status: string
  onStatusChange?: (newStatus: string) => void
}

interface ProcessingProgress {
  fileId: string
  totalChunks: number
  processedChunks: number
  progress: number
  startTime: string
}

export function DocumentProgress({ fileId, filename, status, onStatusChange }: DocumentProgressProps) {
  const [progress, setProgress] = useState<ProcessingProgress | null>(null)
  const [loading, setLoading] = useState(false)
  const [documentInfo, setDocumentInfo] = useState<any>(null)
  const { toast } = useToast()

  useEffect(() => {
    // Загружаем информацию о документе
    const loadDocumentInfo = async () => {
      try {
        const info = await apiClient.getDocumentStatus(fileId)
        setDocumentInfo(info)
      } catch (error) {
        console.error('Failed to load document info:', error)
      }
    }
    
    loadDocumentInfo()
  }, [fileId])

  useEffect(() => {
    // Только для документов в статусе 'processing' запрашиваем прогресс
    if (status === 'processing') {
      const interval = setInterval(async () => {
        try {
          setLoading(true)
          const progressData = await apiClient.getProcessingProgress(fileId)
          setProgress(progressData)
          
          // Проверяем статус документа
          const statusData = await apiClient.getDocumentStatus(fileId)
          if (statusData.status !== status && onStatusChange) {
            onStatusChange(statusData.status)
          }
        } catch (error) {
          // Если получаем 404, значит документ не обрабатывается
          // Запрашиваем актуальный статус
          const errorObj = error as { status?: number }
          if (errorObj.status === 404) {
            try {
              const statusData = await apiClient.getDocumentStatus(fileId)
              if (statusData.status !== status && onStatusChange) {
                onStatusChange(statusData.status)
              }
            } catch (statusError) {
              console.error('Failed to fetch status after 404:', statusError)
            }
          } else {
            console.error('Failed to fetch progress:', error)
          }
        } finally {
          setLoading(false)
        }
      }, 2000) // Обновляем каждые 2 секунды

      return () => clearInterval(interval)
    } else if (status === 'uploaded' || status === 'pending') {
      // Для загруженных и ожидающих документов периодически проверяем статус
      const interval = setInterval(async () => {
        try {
          const statusData = await apiClient.getDocumentStatus(fileId)
          if (statusData.status !== status && onStatusChange) {
            onStatusChange(statusData.status)
          }
        } catch (error) {
          console.error('Failed to fetch status:', error)
        }
      }, 5000) // Проверяем каждые 5 секунд

      return () => clearInterval(interval)
    }
    // Для документов в статусе 'completed', 'failed', 'cancelled' не делаем запросы
  }, [fileId, status, onStatusChange])

  const downloadDocument = async () => {
    try {
      const blob = await apiClient.downloadDocument(fileId)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `formatted-${filename}`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      toast({
        title: "Успех",
        description: "Файл успешно скачан",
      })
    } catch (error) {
      toast({
        title: "Ошибка",
        description: "Не удалось скачать файл",
        variant: "destructive",
      })
    }
  }

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return new Intl.DateTimeFormat("ru-RU", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    }).format(date)
  }

  const getStatusVariant = (status: string): "default" | "secondary" | "destructive" | "outline" => {
    switch (status?.toLowerCase()) {
      case 'completed':
        return 'default' // зеленый
      case 'processing':
        return 'default' // синий
      case 'pending':
        return 'secondary' // желтый
      case 'uploaded':
        return 'outline' // голубой
      case 'failed':
        return 'destructive' // красный
      case 'cancelled':
        return 'secondary' // серый
      default:
        return 'outline'
    }
  }

  const getStatusColorClass = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'completed':
        return 'bg-green-100 text-green-800 border-green-200'
      case 'processing':
        return 'bg-blue-100 text-blue-800 border-blue-200'
      case 'pending':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200'
      case 'uploaded':
        return 'bg-blue-50 text-blue-700 border-blue-200'
      case 'failed':
        return 'bg-red-100 text-red-800 border-red-200'
      case 'cancelled':
        return 'bg-gray-100 text-gray-800 border-gray-200'
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'completed':
        return <CheckCircle className="h-4 w-4 text-green-500" />
      case 'processing':
        return <Zap className="h-4 w-4 text-blue-500 animate-pulse" />
      case 'pending':
        return <Clock className="h-4 w-4 text-yellow-500" />
      case 'uploaded':
        return <FileText className="h-4 w-4 text-blue-400" />
      case 'failed':
        return <XCircle className="h-4 w-4 text-red-500" />
      case 'cancelled':
        return <XCircle className="h-4 w-4 text-gray-500" />
      default:
        return <FileText className="h-4 w-4 text-gray-500" />
    }
  }

  const formatTime = (seconds: number) => {
    if (seconds < 60) return `${Math.round(seconds)}с`
    if (seconds < 3600) return `${Math.round(seconds / 60)}м`
    return `${Math.round(seconds / 3600)}ч`
  }

  const getStatusText = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'completed':
        return 'Завершено'
      case 'processing':
        return 'Обрабатывается'
      case 'pending':
        return 'В очереди'
      case 'uploaded':
        return 'Загружен'
      case 'failed':
        return 'Ошибка'
      case 'cancelled':
        return 'Отменен'
      default:
        return status || 'Неизвестно'
    }
  }

  const calculateProcessingSpeed = (progress: ProcessingProgress) => {
    if (progress.processedChunks > 0) {
      const elapsedTime = new Date().getTime() - new Date(progress.startTime).getTime()
      const blocksPerSecond = progress.processedChunks / (elapsedTime / 1000)
      const blocksPerMinute = blocksPerSecond * 60
      return blocksPerMinute.toFixed(1)
    }
    return '0'
  }

  return (
    <Card className="w-full">
      <CardContent className="p-4">
        <div className="space-y-3">
          {/* Заголовок с именем файла и статусом */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 flex-1 min-w-0">
              <FileText className="h-5 w-5 text-primary flex-shrink-0" />
              <span className="font-medium truncate">{filename}</span>
            </div>
            <div className="flex items-center gap-2 flex-shrink-0">
              {getStatusIcon(status)}
              <Badge variant="outline" className={getStatusColorClass(status)}>
                {getStatusText(status)}
              </Badge>
              {loading && status === 'processing' && (
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
              )}
              {status === 'processing' && (
                <Button variant="ghost" size="sm" asChild>
                  <Link href={`/processing/${fileId}`}>
                    <ExternalLink className="h-4 w-4" />
                  </Link>
                </Button>
              )}
              {status === 'completed' && (
                <Button 
                  variant="ghost" 
                  size="sm"
                  onClick={downloadDocument}
                  className="ml-2"
                >
                  <Download className="h-4 w-4" />
                </Button>
              )}
            </div>
          </div>

          {/* Дополнительная информация о файле */}
          {documentInfo && (
            <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
              <span>Размер: {formatFileSize(documentInfo.originalSize || 0)}</span>
              {documentInfo.processingStartedAt && (
                <span>Начато: {formatDate(documentInfo.processingStartedAt)}</span>
              )}
              {documentInfo.processingCompletedAt && (
                <span>Завершено: {formatDate(documentInfo.processingCompletedAt)}</span>
              )}
            </div>
          )}

          {/* Прогресс-бар для обрабатывающихся документов */}
          {status === 'processing' && (
            <div className="space-y-2">
              <div className="flex justify-between text-sm text-muted-foreground">
                <span>
                  {progress ? `Блок ${progress.processedChunks} из ${progress.totalChunks}` : 'Загрузка...'}
                </span>
                <span>
                  {progress ? `${Math.round(progress.progress)}%` : '0%'}
                </span>
              </div>
              
              <Progress 
                value={progress?.progress || 0} 
                className="h-2"
              />
              
              {progress && (
                <div className="flex justify-between text-xs text-muted-foreground">
                  <span>Обработка блоков</span>
                  <div className="flex gap-4">
                    <span>Начато: {formatDate(progress.startTime)}</span>
                    {progress.processedChunks > 0 && (
                      <span>
                        Скорость: {calculateProcessingSpeed(progress)} блоков/мин
                      </span>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Статическая информация для других статусов */}
          {status === 'pending' && (
            <div className="text-sm text-muted-foreground">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4" />
                <span>Документ ожидает обработки в очереди</span>
              </div>
            </div>
          )}

          {status === 'uploaded' && (
            <div className="text-sm text-blue-600">
              <div className="flex items-center gap-2">
                <FileText className="h-4 w-4" />
                <span>Документ загружен и готов к обработке</span>
              </div>
            </div>
          )}

          {status === 'completed' && (
            <div className="text-sm text-green-600">
              <div className="flex items-center gap-2">
                <CheckCircle className="h-4 w-4" />
                <span>Документ успешно обработан и готов к скачиванию</span>
              </div>
            </div>
          )}

          {status === 'failed' && (
            <div className="text-sm text-red-600">
              <div className="flex items-center gap-2">
                <XCircle className="h-4 w-4" />
                <span>
                  Произошла ошибка при обработке документа
                  {documentInfo?.errorMessage && (
                    <span className="block text-xs mt-1">
                      Ошибка: {documentInfo.errorMessage}
                    </span>
                  )}
                </span>
              </div>
            </div>
          )}

          {status === 'cancelled' && (
            <div className="text-sm text-gray-600">
              <div className="flex items-center gap-2">
                <XCircle className="h-4 w-4" />
                <span>Обработка документа была отменена</span>
              </div>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
} 