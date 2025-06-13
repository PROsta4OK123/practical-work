"use client"

import type React from "react"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { FileUp, AlertCircle } from "lucide-react"
import { useToast } from "@/hooks/use-toast"
import { useAuth } from "@/components/auth-provider"
import { useLanguage } from "@/components/language-provider"
import { apiClient } from "@/lib/api-client"
import { uploadLogger, uiLogger } from "@/lib/logger"
import Link from "next/link"

export function UploadForm() {
  const [file, setFile] = useState<File | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const router = useRouter()
  const { toast } = useToast()
  const { user, usePoints } = useAuth()
  const { t } = useLanguage()

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0]
    uiLogger.userAction('file_selected', { 
      fileName: selectedFile?.name,
      fileSize: selectedFile?.size,
      fileType: selectedFile?.type 
    })
    
    if (selectedFile) {
      uploadLogger.info('File selected for upload', {
        fileName: selectedFile.name,
        fileSize: selectedFile.size,
        fileType: selectedFile.type,
        lastModified: new Date(selectedFile.lastModified).toISOString()
      })
      
      // Check if file is a Word document
      if (
        selectedFile.type === "application/msword" ||
        selectedFile.type === "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      ) {
        setFile(selectedFile)
        uploadLogger.info('Valid Word document selected', { fileName: selectedFile.name })
      } else {
        uploadLogger.warn('Invalid file type selected', { 
          fileName: selectedFile.name,
          fileType: selectedFile.type,
          expectedTypes: ['application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
        })
        toast({
          title: t.common.error,
          description: t.upload.errors.invalidFileType,
          variant: "destructive",
        })
      }
    }
  }

  const handleUpload = async () => {
    if (!file) {
      uploadLogger.warn('Upload attempted without selected file')
      return
    }

    const startTime = Date.now()
    uploadLogger.info('Starting document upload process', {
      fileName: file.name,
      fileSize: file.size,
      userId: user?.id
    })
    uiLogger.userAction('upload_initiated', { fileName: file.name, fileSize: file.size })

    // Check if user is logged in
    if (!user) {
      uploadLogger.security('Upload attempted without authentication', { fileName: file.name })
      toast({
        title: t.common.error,
        description: t.upload.errors.authRequired,
        variant: "destructive",
      })
      router.push("/login")
      return
    }

    // Check if user has enough points
    if (user.points < 1) {
      uploadLogger.warn('Upload attempted with insufficient points', { 
        userId: user.id,
        userPoints: user.points,
        requiredPoints: 1,
        fileName: file.name
      })
      toast({
        title: t.common.error,
        description: t.upload.errors.insufficientPoints,
        variant: "destructive",
      })
      router.push("/subscription")
      return
    }

    setIsUploading(true)

    try {
      uploadLogger.debug('Sending document for formatting', { 
        fileName: file.name,
        fileSize: file.size,
        userId: user.id
      })
      
      // Используем реальный API для форматирования документа
      const response = await apiClient.formatDocument(file)
      const duration = Date.now() - startTime

      if (response.success && response.fileId) {
        uploadLogger.info('Document upload successful', {
          fileName: file.name,
          fileId: response.fileId,
          userId: user.id,
          duration,
          queuePosition: response.queuePosition
        })
        
        toast({
          title: "Успешно!",
          description: response.message || "Файл добавлен в очередь обработки",
          variant: "default",
        })

        // Перенаправляем на страницу обработки с реальным fileId
        router.push(`/processing/${response.fileId}`)
      } else {
        uploadLogger.error('Document upload failed - server response', {
          fileName: file.name,
          userId: user.id,
          error: response.error,
          message: response.message,
          duration
        })
        
        toast({
          title: "Ошибка",
          description: response.error || "Не удалось обработать файл",
          variant: "destructive",
        })
        setIsUploading(false)
      }
    } catch (error: any) {
      const duration = Date.now() - startTime
      const errorMessage = error instanceof Error ? error.message : String(error)
      
      uploadLogger.error('Document upload failed - exception', {
        fileName: file.name,
        userId: user.id,
        error: errorMessage,
        duration
      })
      
      toast({
        title: "Ошибка",
        description: errorMessage || "Не удалось загрузить файл",
        variant: "destructive",
      })
      setIsUploading(false)
    }
  }

  if (!user) {
    return (
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
    )
  }

  if (user.points < 1) {
    return (
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col items-center justify-center space-y-4 text-center">
            <AlertCircle className="h-12 w-12 text-muted-foreground" />
            <div className="space-y-2">
              <h3 className="text-lg font-semibold">{t.upload.insufficientPoints.title}</h3>
              <p className="text-sm text-muted-foreground">{t.upload.insufficientPoints.description}</p>
            </div>
            <Button asChild>
              <Link href="/subscription">{t.upload.insufficientPoints.purchaseButton}</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardContent className="pt-6">
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="file">{t.upload.title}</Label>
            <Input
              id="file"
              type="file"
              accept=".doc,.docx,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              onChange={handleFileChange}
              disabled={isUploading}
            />
            <p className="text-xs text-muted-foreground">{t.upload.supportedFormats}</p>
          </div>

          {file && (
            <div className="text-sm">
              <p className="font-medium">{file.name}</p>
              <p className="text-muted-foreground">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
            </div>
          )}

          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">{t.upload.cost}</p>
            <p className="text-sm">
              {t.upload.yourPoints} <span className="font-medium">{user.points}</span>
            </p>
          </div>

          <Button className="w-full" onClick={handleUpload} disabled={!file || isUploading}>
            {isUploading ? (
              <>
                <div className="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"></div>
                {t.upload.uploading}
              </>
            ) : (
              <>
                <FileUp className="mr-2 h-4 w-4" />
                {t.upload.formatButton}
              </>
            )}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
