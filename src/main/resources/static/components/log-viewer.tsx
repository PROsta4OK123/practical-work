"use client"

import React, { useState, useEffect } from 'react'
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { appLogger, LogLevel, LogEntry } from "@/lib/logger"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Download, Trash2, RefreshCw, Search, Filter } from "lucide-react"

export function LogViewer() {
  const [logs, setLogs] = useState<LogEntry[]>([])
  const [filteredLogs, setFilteredLogs] = useState<LogEntry[]>([])
  const [selectedLevel, setSelectedLevel] = useState<string>('all')
  const [selectedContext, setSelectedContext] = useState<string>('all')
  const [searchTerm, setSearchTerm] = useState('')
  const [refreshInterval, setRefreshInterval] = useState<NodeJS.Timeout | null>(null)
  const [autoRefresh, setAutoRefresh] = useState(false)

  const levelColors = {
    [LogLevel.ERROR]: 'destructive',
    [LogLevel.WARN]: 'secondary',
    [LogLevel.INFO]: 'default',
    [LogLevel.DEBUG]: 'outline',
    [LogLevel.TRACE]: 'outline'
  }

  const levelNames = {
    [LogLevel.ERROR]: 'ERROR',
    [LogLevel.WARN]: 'WARN',
    [LogLevel.INFO]: 'INFO',
    [LogLevel.DEBUG]: 'DEBUG',
    [LogLevel.TRACE]: 'TRACE'
  }

  const loadLogs = () => {
    const allLogs = appLogger.getLogs()
    setLogs(allLogs)
  }

  useEffect(() => {
    loadLogs()
  }, [])

  useEffect(() => {
    if (autoRefresh) {
      const interval = setInterval(loadLogs, 2000)
      setRefreshInterval(interval)
    } else {
      if (refreshInterval) {
        clearInterval(refreshInterval)
        setRefreshInterval(null)
      }
    }

    return () => {
      if (refreshInterval) {
        clearInterval(refreshInterval)
      }
    }
  }, [autoRefresh])

  useEffect(() => {
    let filtered = logs

    // Фильтр по уровню
    if (selectedLevel !== 'all') {
      const level = parseInt(selectedLevel)
      filtered = filtered.filter(log => log.level === level)
    }

    // Фильтр по контексту
    if (selectedContext !== 'all') {
      filtered = filtered.filter(log => log.context === selectedContext)
    }

    // Поиск по тексту
    if (searchTerm) {
      filtered = filtered.filter(log => 
        log.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
        JSON.stringify(log.data || {}).toLowerCase().includes(searchTerm.toLowerCase())
      )
    }

    setFilteredLogs(filtered.sort((a, b) => 
      new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    ))
  }, [logs, selectedLevel, selectedContext, searchTerm])

  const getUniqueContexts = () => {
    return [...new Set(logs.map(log => log.context).filter(Boolean))]
  }

  const clearLogs = () => {
    appLogger.clearLogs()
    setLogs([])
    setFilteredLogs([])
  }

  const exportLogs = () => {
    const exportData = appLogger.exportLogs()
    const blob = new Blob([exportData], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `app_logs_${new Date().toISOString().split('T')[0]}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('ru-RU')
  }

  return (
    <Card className="w-full">
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle className="flex items-center gap-2">
            <Search className="h-5 w-5" />
            Просмотр логов ({filteredLogs.length} из {logs.length})
          </CardTitle>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setAutoRefresh(!autoRefresh)}
            >
              <RefreshCw className={`h-4 w-4 ${autoRefresh ? 'animate-spin' : ''}`} />
              {autoRefresh ? 'Авто' : 'Обновить'}
            </Button>
            <Button variant="outline" size="sm" onClick={loadLogs}>
              <RefreshCw className="h-4 w-4" />
              Обновить
            </Button>
            <Button variant="outline" size="sm" onClick={exportLogs}>
              <Download className="h-4 w-4" />
              Экспорт
            </Button>
            <Button variant="destructive" size="sm" onClick={clearLogs}>
              <Trash2 className="h-4 w-4" />
              Очистить
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* Фильтры */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="text-sm font-medium mb-2 block">Поиск</label>
              <Input
                placeholder="Поиск в логах..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Уровень</label>
              <Select value={selectedLevel} onValueChange={setSelectedLevel}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Все уровни</SelectItem>
                  <SelectItem value="0">ERROR</SelectItem>
                  <SelectItem value="1">WARN</SelectItem>
                  <SelectItem value="2">INFO</SelectItem>
                  <SelectItem value="3">DEBUG</SelectItem>
                  <SelectItem value="4">TRACE</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Контекст</label>
              <Select value={selectedContext} onValueChange={setSelectedContext}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Все контексты</SelectItem>
                  {getUniqueContexts().map(context => (
                    <SelectItem key={context} value={context || ''}>
                      {context}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex items-end">
              <Badge variant="outline" className="h-10 flex items-center">
                <Filter className="h-4 w-4 mr-1" />
                {filteredLogs.length} записей
              </Badge>
            </div>
          </div>

          {/* Список логов */}
          <ScrollArea className="h-[600px] w-full border rounded-md">
            <div className="p-4 space-y-2">
              {filteredLogs.length === 0 ? (
                <div className="text-center text-muted-foreground py-8">
                  Логи не найдены
                </div>
              ) : (
                filteredLogs.map((log, index) => (
                  <div
                    key={`${log.timestamp}-${index}`}
                    className="border rounded-lg p-3 hover:bg-muted/50 transition-colors"
                  >
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="flex items-center gap-2 flex-wrap">
                        <Badge variant={levelColors[log.level] as any}>
                          {levelNames[log.level]}
                        </Badge>
                        {log.context && (
                          <Badge variant="outline">
                            {log.context}
                          </Badge>
                        )}
                        {log.userId && (
                          <Badge variant="secondary">
                            User: {log.userId}
                          </Badge>
                        )}
                        <span className="text-xs text-muted-foreground">
                          {formatTimestamp(log.timestamp)}
                        </span>
                      </div>
                      {log.requestId && (
                        <span className="text-xs text-muted-foreground font-mono">
                          {log.requestId}
                        </span>
                      )}
                    </div>
                    
                    <div className="text-sm mb-2">
                      {log.message}
                    </div>
                    
                    {log.data && (
                      <details className="text-xs">
                        <summary className="cursor-pointer text-muted-foreground hover:text-foreground">
                          Данные (клик для показа)
                        </summary>
                        <pre className="mt-2 p-2 bg-muted rounded text-xs overflow-auto">
                          {JSON.stringify(log.data, null, 2)}
                        </pre>
                      </details>
                    )}
                  </div>
                ))
              )}
            </div>
          </ScrollArea>

          {/* Статистика */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            {Object.entries(LogLevel)
              .filter(([key]) => isNaN(Number(key)))
              .map(([name, level]) => {
                const count = logs.filter(log => log.level === level).length
                return (
                  <div key={name} className="text-center">
                    <div className="text-2xl font-bold">{count}</div>
                    <div className="text-sm text-muted-foreground">{name}</div>
                  </div>
                )
              })}
          </div>
        </div>
      </CardContent>
    </Card>
  )
} 