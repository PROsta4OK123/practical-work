"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Navbar } from "@/components/navbar"
import { useAuth } from "@/components/auth-provider"
import { useLanguage } from "@/components/language-provider"
import { useToast } from "@/hooks/use-toast"
import { FileText, Download, AlertCircle, Clock, CheckCircle, XCircle } from "lucide-react"
import { UploadForm } from "@/components/upload-form"
import { DocumentProgress } from "@/components/document-progress"
import { apiClient, type QueueStatistics } from "@/lib/api-client"
import Link from "next/link"

export default function DashboardPage() {
  const [documents, setDocuments] = useState<any[]>([])
  const [queueData, setQueueData] = useState<{ queueStatistics: QueueStatistics, userFiles: any[] } | null>(null)
  const [mounted, setMounted] = useState(false)
  const [isLoadingDocs, setIsLoadingDocs] = useState(false)
  const router = useRouter()
  const { user, isLoading } = useAuth()
  const { t } = useLanguage()
  const { toast } = useToast()

  // Ensure component is mounted before checking auth
  useEffect(() => {
    setMounted(true)
  }, [])

  // Load user documents and queue data
  useEffect(() => {
    if (user && mounted) {
      loadUserData()
    }
  }, [user, mounted])

  // Автоматическое обновление данных каждые 10 секунд
  useEffect(() => {
    if (user && mounted) {
      const interval = setInterval(() => {
        loadUserData()
      }, 10000) // Обновляем каждые 10 секунд

      return () => clearInterval(interval)
    }
  }, [user, mounted])

  const loadUserData = async () => {
    setIsLoadingDocs(true)
    try {
      console.log('🔍 Загружаем данные пользователя...')
      
      // Проверяем токен
      const token = localStorage.getItem('authToken')
      console.log('🔑 Токен:', token ? `Есть (${token.length} символов)` : 'Отсутствует')
      
      // Load queue status and user files
      console.log('📡 Отправляем запрос к /api/queue/status...')
      const queueResponse = await apiClient.getQueueStatus()
      console.log('✅ Ответ получен:', queueResponse)
      
      setQueueData(queueResponse)
      setDocuments(queueResponse.userFiles || [])
      
      console.log('📊 Данные загружены успешно')
    } catch (error) {
      console.error('❌ Ошибка загрузки данных:', error)
      console.error('📋 Детали ошибки:', {
        message: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
        type: typeof error,
        error
      })
      
      toast({
        title: "Ошибка",
        description: `Не удалось загрузить данные: ${error instanceof Error ? error.message : String(error)}`,
        variant: "destructive",
      })
    } finally {
      setIsLoadingDocs(false)
    }
  }

  // Show loading while checking authentication
  if (!mounted || isLoading) {
    return (
      <div className="min-h-screen flex flex-col">
        <Navbar />
        <main className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
            <p>{t.common.loading}</p>
          </div>
        </main>
      </div>
    )
  }

  // Show login prompt if not authenticated
  if (!user) {
    return (
      <div className="min-h-screen flex flex-col">
        <Navbar />
        <main className="flex-1 container py-8">
          <div className="max-w-md mx-auto">
            <Card>
              <CardContent className="pt-6">
                <div className="flex flex-col items-center justify-center space-y-4 text-center">
                  <AlertCircle className="h-12 w-12 text-muted-foreground" />
                  <div className="space-y-2">
                    <h3 className="text-lg font-semibold">{t.upload.authRequired.title}</h3>
                    <p className="text-sm text-muted-foreground">{t.upload.authRequired.description}</p>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" asChild>
                      <Link href="/login">{t.nav.login}</Link>
                    </Button>
                    <Button asChild>
                      <Link href="/register">{t.nav.signup}</Link>
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
    )
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

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const getStatusIcon = (status: string) => {
    switch (status.toLowerCase()) {
      case 'completed':
        return <CheckCircle className="h-4 w-4 text-green-500" />
      case 'processing':
        return <Clock className="h-4 w-4 text-blue-500" />
      case 'pending':
        return <Clock className="h-4 w-4 text-yellow-500" />
      case 'uploaded':
        return <FileText className="h-4 w-4 text-blue-400" />
      case 'failed':
        return <XCircle className="h-4 w-4 text-red-500" />
      case 'cancelled':
        return <XCircle className="h-4 w-4 text-gray-500" />
      default:
        return <Clock className="h-4 w-4 text-gray-500" />
    }
  }

  const downloadDocument = async (fileId: string, filename: string) => {
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
    } catch (error) {
      toast({
        title: "Ошибка",
        description: "Не удалось скачать файл",
        variant: "destructive",
      })
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />

      <main className="flex-1 container py-8">
        <h1 className="text-3xl font-bold mb-6">{t.dashboard.title}</h1>

        <Tabs defaultValue="upload">
          <TabsList className="mb-6">
            <TabsTrigger value="upload">{t.dashboard.tabs.upload}</TabsTrigger>
            <TabsTrigger value="documents">{t.dashboard.tabs.documents}</TabsTrigger>
          </TabsList>

          <TabsContent value="upload">
            <div className="space-y-6">
              <div className="text-center">
                <h2 className="text-2xl font-bold mb-4">{t.home.upload.title}</h2>
                <p className="text-muted-foreground mb-8">{t.home.upload.subtitle}</p>
              </div>
              <div className="max-w-md mx-auto">
                <UploadForm />
              </div>
            </div>
          </TabsContent>

          <TabsContent value="documents">
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold">{t.dashboard.documents.title}</h2>
                <Button variant="outline" onClick={() => router.push("/dashboard")}>
                  {t.dashboard.documents.formatNewButton}
                </Button>
              </div>

              {isLoadingDocs ? (
                <div className="flex items-center justify-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                  <span className="ml-2">Загрузка документов...</span>
                </div>
              ) : (
              <div className="grid gap-4">
                {documents.length > 0 ? (
                    documents.map((doc: any) => (
                      <DocumentProgress
                        key={doc.fileId}
                        fileId={doc.fileId}
                        filename={doc.originalFilename}
                        status={doc.status}
                        onStatusChange={(newStatus) => {
                          // Обновляем статус документа в локальном состоянии
                          setDocuments(prev => 
                            prev.map(d => 
                              d.fileId === doc.fileId 
                                ? { ...d, status: newStatus }
                                : d
                            )
                          )
                          
                          // Если документ завершен, перезагружаем данные
                          if (newStatus === 'completed') {
                            loadUserData()
                          }
                        }}
                      />
                  ))
                ) : (
                  <Card>
                    <CardContent className="p-6 text-center">
                        <p className="text-muted-foreground">Документы не найдены</p>
                    </CardContent>
                  </Card>
                )}
              </div>
              )}

              {queueData && (
                <Card className="mt-6">
                  <CardContent className="p-4">
                    <h3 className="font-semibold mb-3">Статистика очереди</h3>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <p className="text-muted-foreground">В очереди</p>
                        <p className="font-medium">{queueData.queueStatistics.pendingCount}</p>
                      </div>
                      <div>
                        <p className="text-muted-foreground">Обрабатывается</p>
                        <p className="font-medium">{queueData.queueStatistics.processingCount}</p>
                      </div>
                      <div>
                        <p className="text-muted-foreground">Завершено</p>
                        <p className="font-medium">{queueData.queueStatistics.completedCount}</p>
                      </div>
                      <div>
                        <p className="text-muted-foreground">Ошибки</p>
                        <p className="font-medium">{queueData.queueStatistics.failedCount}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  )
}
