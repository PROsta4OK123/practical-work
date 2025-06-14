"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { apiClient } from "@/lib/api-client"

export default function TestApiPage() {
  const [result, setResult] = useState<string>("")
  const [loading, setLoading] = useState(false)

  const testQueueStatus = async () => {
    setLoading(true)
    setResult("Тестируем /api/queue/status...")
    
    try {
      const data = await apiClient.getQueueStatus()
      setResult(`✅ Успех!\n${JSON.stringify(data, null, 2)}`)
    } catch (error) {
      setResult(`❌ Ошибка: ${error instanceof Error ? error.message : String(error)}`)
    } finally {
      setLoading(false)
    }
  }

  const testAuth = async () => {
    setLoading(true)
    setResult("Тестируем /api/auth/me...")
    
    try {
      const data = await apiClient.getProfile()
      setResult(`✅ Успех!\n${JSON.stringify(data, null, 2)}`)
    } catch (error) {
      setResult(`❌ Ошибка: ${error instanceof Error ? error.message : String(error)}`)
    } finally {
      setLoading(false)
    }
  }

  const checkToken = () => {
    const token = localStorage.getItem('authToken')
    setResult(`Токен в localStorage:\n${token ? `✅ Есть (${token.length} символов)\n${token.substring(0, 50)}...` : '❌ Отсутствует'}`)
  }

  return (
    <div className="container py-8">
      <Card>
        <CardHeader>
          <CardTitle>Тест API</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex gap-2">
            <Button onClick={checkToken}>
              Проверить токен
            </Button>
            <Button onClick={testAuth} disabled={loading}>
              Тест /api/auth/me
            </Button>
            <Button onClick={testQueueStatus} disabled={loading}>
              Тест /api/queue/status
            </Button>
          </div>
          
          <div className="bg-gray-100 p-4 rounded-md">
            <pre className="whitespace-pre-wrap text-sm">
              {result || "Выберите тест для выполнения"}
            </pre>
          </div>
        </CardContent>
      </Card>
    </div>
  )
} 