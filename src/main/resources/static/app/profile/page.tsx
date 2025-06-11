"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { useToast } from "@/hooks/use-toast"
import { useAuth } from "@/components/auth-provider"
import { ProtectedRoute } from "@/components/protected-route"
import { Navbar } from "@/components/navbar"

interface UserProfile {
  firstName: string
  lastName: string
  email: string
  company: string
  phone: string
}

export default function ProfilePage() {
  const { user, updateUser, isLoading } = useAuth()
  const { toast } = useToast()
  const [mounted, setMounted] = useState(false)
  const [userProfile, setUserProfile] = useState<UserProfile>({
    firstName: "",
    lastName: "",
    email: "",
    company: "",
    phone: "",
  })
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    if (mounted && !isLoading && user) {
      setUserProfile({
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        email: user.email || "",
        company: "",
        phone: "",
      })
    }
  }, [mounted, isLoading, user])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSaving(true)

    try {
    updateUser({
        firstName: userProfile.firstName,
        lastName: userProfile.lastName,
      email: userProfile.email,
    })

      toast({
        title: "Успешно",
        description: "Профиль обновлен успешно",
      })
    } catch (error) {
    toast({
        title: "Ошибка",
        description: "Не удалось обновить профиль",
        variant: "destructive",
    })
    } finally {
      setIsSaving(false)
    }
  }

  const handleInputChange = (field: keyof UserProfile, value: string) => {
    setUserProfile(prev => ({
      ...prev,
      [field]: value
    }))
  }

    return (
    <ProtectedRoute>
      <div className="min-h-screen flex flex-col">
        <Navbar />

        <main className="flex-1 container mx-auto px-4 py-8">
          <div className="max-w-2xl mx-auto space-y-8">
            <div>
              <h1 className="text-3xl font-bold">Профиль</h1>
              <p className="text-muted-foreground mt-2">
                Управляйте настройками вашего аккаунта
              </p>
        </div>

          <Card>
            <CardHeader>
                <CardTitle>Личная информация</CardTitle>
                <CardDescription>
                  Обновите свою личную информацию и контактные данные
                </CardDescription>
            </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmit} className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                      <Label htmlFor="firstName">Имя</Label>
                <Input
                        id="firstName"
                        value={userProfile.firstName}
                        onChange={(e) => handleInputChange("firstName", e.target.value)}
                        disabled={isSaving}
                        required
                />
              </div>
              <div className="space-y-2">
                      <Label htmlFor="lastName">Фамилия</Label>
                      <Input
                        id="lastName"
                        value={userProfile.lastName}
                        onChange={(e) => handleInputChange("lastName", e.target.value)}
                        disabled={isSaving}
                        required
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={userProfile.email}
                      onChange={(e) => handleInputChange("email", e.target.value)}
                      disabled={isSaving}
                      required
                />
              </div>

              <div className="space-y-2">
                    <Label htmlFor="company">Компания</Label>
                <Input
                  id="company"
                  value={userProfile.company}
                      onChange={(e) => handleInputChange("company", e.target.value)}
                      disabled={isSaving}
                      placeholder="Название вашей компании"
                />
              </div>

              <div className="space-y-2">
                    <Label htmlFor="phone">Телефон</Label>
                <Input
                  id="phone"
                  value={userProfile.phone}
                      onChange={(e) => handleInputChange("phone", e.target.value)}
                      disabled={isSaving}
                      placeholder="+380123456789"
                />
              </div>

                  <Button type="submit" disabled={isSaving}>
                    {isSaving ? "Сохранение..." : "Сохранить изменения"}
                  </Button>
                </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
                <CardTitle>Информация об аккаунте</CardTitle>
                <CardDescription>
                  Основная информация о вашем аккаунте
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <Label className="text-sm font-medium">Баллы</Label>
                    <p className="text-2xl font-bold text-primary">{user?.points || 0}</p>
                    <p className="text-sm text-muted-foreground">
                      Доступно для обработки документов
                    </p>
                  </div>
                  <div>
                    <Label className="text-sm font-medium">Статус аккаунта</Label>
                    <p className="text-lg font-semibold">
                      {user?.isActive ? (
                        <span className="text-green-600">Активен</span>
                      ) : (
                        <span className="text-red-600">Неактивен</span>
                      )}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      Состояние вашего аккаунта
                    </p>
                  </div>
              </div>

                <Separator />

                <div>
                  <Label className="text-sm font-medium">Роль</Label>
                  <p className="text-lg capitalize">{user?.role?.toLowerCase()}</p>
                  <p className="text-sm text-muted-foreground">
                    Ваша роль в системе
                  </p>
                </div>

                <div>
                  <Label className="text-sm font-medium">Дата регистрации</Label>
                  <p className="text-lg">
                    {user?.createdAt ? new Date(user.createdAt).toLocaleDateString('ru-RU') : "—"}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Когда был создан аккаунт
                  </p>
                </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
    </ProtectedRoute>
  )
}



