"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Navbar } from "@/components/navbar"
import { useAuth } from "@/components/auth-provider"
import { useToast } from "@/hooks/use-toast"
import { Check, CreditCard } from "lucide-react"
import { apiClient } from "@/lib/api-client"

const plans = [
  {
    id: "plan_1",
    name: "Basic",
    description: "For occasional document formatting",
    price: 9.99,
    points: 100,
    features: ["100 formatting points", "Standard formatting options", "Email support", "Valid for 30 days"],
  },
  {
    id: "plan_2",
    name: "Professional",
    description: "For regular document formatting needs",
    price: 19.99,
    points: 500,
    features: [
      "500 formatting points",
      "Advanced formatting options",
      "Priority email support",
      "Valid for 30 days",
      "Batch processing",
    ],
    popular: true,
  },
  {
    id: "plan_3",
    name: "Enterprise",
    description: "For teams and businesses",
    price: 49.99,
    points: 1000,
    features: [
      "1000 formatting points",
      "All formatting options",
      "Priority support",
      "Valid for 30 days",
      "Batch processing",
      "API access",
      "Team management",
    ],
  },
]

export default function SubscriptionPage() {
  const router = useRouter()
  const { user, isLoading, addPoints } = useAuth()
  const { toast } = useToast()
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/login")
    }
  }, [user, isLoading, router])

  const handlePurchase = async (planId: string) => {
    const plan = plans.find((p) => p.id === planId)
    if (!plan) return

    try {
      setIsProcessing(true)
      
      const token = localStorage.getItem('authToken')
      if (!token) {
        throw new Error('Not authenticated')
      }

      const response = await apiClient.purchaseSubscription(planId)
      
      if (response.success) {
        addPoints(plan.points)

        toast({
          title: "Подписка оформлена",
          description: `Вы успешно получили ${plan.points} баллов по тарифу ${plan.name}.`,
          variant: "default",
        })

        router.push("/dashboard")
      } else {
        throw new Error(response.message || 'Не удалось оформить подписку')
      }
    } catch (error) {
      console.error('Error purchasing subscription:', error)
      toast({
        title: "Error",
        description: error instanceof Error ? error.message : 'An error occurred while purchasing subscription',
        variant: "destructive",
      })
    } finally {
      setIsProcessing(false)
    }
  }

  if (isLoading || !user) {
    return <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
        <p className="mt-2">Загрузка...</p>
      </div>
    </div>
  }

  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1 container py-8">
        <div className="text-center mb-12">
          <h1 className="text-3xl font-bold">Choose Your Plan</h1>
          <p className="text-muted-foreground mt-2">Select a subscription plan that works for you</p>
          <div className="mt-4 text-lg">
            Current balance: <span className="font-bold text-primary">{user.points} points</span>
          </div>
        </div>

        <div className="grid md:grid-cols-3 gap-6 max-w-6xl mx-auto">
          {plans.map((plan) => (
            <Card key={plan.id} className={`flex flex-col ${plan.popular ? "border-primary shadow-lg" : ""}`}>
              <CardHeader>
                {plan.popular && (
                  <div className="py-1 px-3 bg-primary text-primary-foreground text-xs font-medium rounded-full w-fit mb-2">
                    Most Popular
                  </div>
                )}
                <CardTitle>{plan.name}</CardTitle>
                <CardDescription>{plan.description}</CardDescription>
              </CardHeader>
              <CardContent className="flex-1">
                <div className="mb-4">
                  <span className="text-3xl font-bold">${plan.price}</span>
                  <span className="text-muted-foreground ml-1">/month</span>
                </div>
                <ul className="space-y-2 mb-6">
                  {plan.features.map((feature, i) => (
                    <li key={i} className="flex items-center">
                      <Check className="h-4 w-4 text-primary mr-2 flex-shrink-0" />
                      <span className="text-sm">{feature}</span>
                    </li>
                  ))}
                </ul>
              </CardContent>
              <CardFooter>
                <Button 
                  className="w-full" 
                  variant={plan.popular ? "default" : "outline"}
                  onClick={() => handlePurchase(plan.id)}
                  disabled={isProcessing}
                >
                  {isProcessing ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                      Processing...
                    </>
                  ) : (
                    <>
                      <CreditCard className="mr-2 h-4 w-4" />
                      Subscribe
                    </>
                  )}
                </Button>
              </CardFooter>
            </Card>
          ))}
        </div>

        <div className="mt-16 max-w-3xl mx-auto">
          <h2 className="text-2xl font-bold text-center mb-6">Frequently Asked Questions</h2>
          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">How do points work?</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground">
                  Each document formatting costs 1 point. When you purchase a subscription, you receive a certain number
                  of points that you can use to format documents. Points do not expire as long as your account is
                  active.
                </p>
              </CardContent>
            </Card>
             <Card>
              <CardHeader>
                <CardTitle className="text-lg">Can I cancel my subscription?</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground">
                  Yes, you can cancel your subscription at any time. Your points will remain available for use until
                  they are depleted.
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">What happens if I run out of points?</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground">
                  If you run out of points, you'll need to purchase a new subscription to continue formatting documents.
                  You can check your point balance in your dashboard.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  )
}
