"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Navbar } from "@/components/navbar"
import { Download, CheckCircle, Loader2, XCircle, Clock } from "lucide-react"
import { useToast } from "@/hooks/use-toast"
import { apiClient, type ProcessedDocument } from "@/lib/api-client"

export default function ProcessingPage({ params }: { params: { fileId: string } }) {
  const [document, setDocument] = useState<ProcessedDocument | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [progress, setProgress] = useState<any>(null)
  const router = useRouter()
  const { toast } = useToast()

  useEffect(() => {
    loadDocumentStatus()
    const interval = setInterval(loadDocumentStatus, 2000) // Проверяем каждые 2 секунды

    return () => clearInterval(interval)
  }, [params.fileId])

  const loadDocumentStatus = async () => {
    try {
      const docStatus = await apiClient.getDocumentStatus(params.fileId)
      setDocument(docStatus)
      
      // Если документ обрабатывается, получаем прогресс
      if (docStatus.status === 'processing') {
        try {
          const progressData = await apiClient.getProcessingProgress(params.fileId)
          setProgress(progressData)
        } catch (error) {
          // Прогресс может быть недоступен
        }
      }
      
      setIsLoading(false)
    } catch (error: any) {
      console.error('Failed to load document status:', error)
      toast({
        title: "Ошибка",
        description: "Не удалось загрузить статус документа",
        variant: "destructive",
      })
      setIsLoading(false)
    }
  }

  const handleDownload = async () => {
    if (!document) return
    
    try {
      const blob = await apiClient.downloadDocument(params.fileId)
      const url = window.URL.createObjectURL(blob)
      const a = window.document.createElement('a')
      a.href = url
      a.download = `formatted-${document.originalFilename}`
      window.document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      window.document.body.removeChild(a)
      
      toast({
        title: "Успешно",
        description: "Документ скачан",
      })
    } catch (error) {
    toast({
        title: "Ошибка",
        description: "Не удалось скачать документ",
        variant: "destructive",
    })
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />

      <main className="flex-1 flex items-center justify-center p-4">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 flex flex-col items-center text-center space-y-6">
            {isLoading ? (
              <>
                <div className="relative">
                  <Loader2 className="h-16 w-16 animate-spin text-primary" />
                </div>
                <div className="space-y-2">
                  <h2 className="text-2xl font-bold">Загрузка...</h2>
                  <p className="text-muted-foreground">Получаем информацию о документе...</p>
                </div>
              </>
            ) : !document ? (
              <>
                <div className="bg-red-100 p-3 rounded-full">
                  <XCircle className="h-16 w-16 text-red-500" />
                </div>
                <div className="space-y-2">
                  <h2 className="text-2xl font-bold">Документ не найден</h2>
                  <p className="text-muted-foreground">Не удалось найти информацию о документе</p>
                </div>
                <Button onClick={() => router.push('/dashboard')}>
                  Вернуться к документам
                </Button>
              </>
            ) : document.status === 'processing' || document.status === 'pending' ? (
              <>
                <div className="relative">
                  <Loader2 className="h-16 w-16 animate-spin text-primary" />
                  <div className="absolute inset-0 flex items-center justify-center">
                    <Clock className="h-6 w-6 text-primary" />
                  </div>
                </div>
                <div className="space-y-2">
                  <h2 className="text-2xl font-bold">
                    {document.status === 'pending' ? 'В очереди' : 'Обрабатывается'}
                  </h2>
                  <p className="text-muted-foreground">
                    {document.status === 'pending' 
                      ? 'Документ добавлен в очередь обработки...'
                      : 'Пожалуйста, подождите, пока мы форматируем ваш документ...'
                    }
                  </p>
                  <p className="text-sm text-gray-600">
                    Файл: {document.originalFilename}
                  </p>
                </div>
                {progress && (
                  <div className="w-full space-y-2">
                    <div className="flex justify-between text-sm">
                      <span>Прогресс: {progress.processedChunks || 0} / {progress.totalChunks || 0}</span>
                      <span>{Math.round(progress.progress || 0)}%</span>
                </div>
                <div className="w-full bg-muted rounded-full h-2.5">
                      <div 
                        className="bg-primary h-2.5 rounded-full transition-all duration-300" 
                        style={{ width: `${progress.progress || 0}%` }}
                      ></div>
                    </div>
                </div>
                )}
              </>
            ) : document.status === 'completed' ? (
              <>
                <div className="bg-primary/10 p-3 rounded-full">
                  <CheckCircle className="h-16 w-16 text-primary" />
                </div>
                <div className="space-y-2">
                  <h2 className="text-2xl font-bold">Документ готов!</h2>
                  <p className="text-muted-foreground">Ваш документ успешно отформатирован</p>
                </div>
                <Button size="lg" onClick={handleDownload}>
                  <Download className="mr-2 h-5 w-5" />
                  Скачать документ
                </Button>
                <p className="text-sm text-muted-foreground">
                  Файл: formatted-{document.originalFilename}
                </p>
                <Button variant="outline" onClick={() => router.push('/dashboard')}>
                  Вернуться к документам
                </Button>
              </>
            ) : document.status === 'failed' ? (
              <>
                <div className="bg-red-100 p-3 rounded-full">
                  <XCircle className="h-16 w-16 text-red-500" />
                </div>
                <div className="space-y-2">
                  <h2 className="text-2xl font-bold">Ошибка обработки</h2>
                  <p className="text-muted-foreground">
                    Произошла ошибка при обработке документа
                  </p>
                  {document.errorMessage && (
                    <p className="text-sm text-red-600">
                      {document.errorMessage}
                    </p>
                  )}
                </div>
                <Button onClick={() => router.push('/dashboard')}>
                  Вернуться к документам
                </Button>
              </>
            ) : (
              <>
                <div className="bg-gray-100 p-3 rounded-full">
                  <Clock className="h-16 w-16 text-gray-500" />
                </div>
                <div className="space-y-2">
                  <h2 className="text-2xl font-bold">Неизвестный статус</h2>
                  <p className="text-muted-foreground">Статус: {document.status}</p>
                </div>
                <Button onClick={() => router.push('/dashboard')}>
                  Вернуться к документам
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
