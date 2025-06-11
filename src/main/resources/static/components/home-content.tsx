"use client"

import { Button } from "@/components/ui/button"
import { FileUp, CheckCircle, Clock, Shield, FileText } from "lucide-react"
import Link from "next/link"
import { useLanguage } from "@/components/language-provider"

export function HomeContent() {
  const { t } = useLanguage()

  return (
    <main className="flex-1">
      {/* Hero Section */}
      <section className="py-20 md:py-28 bg-gradient-to-b from-background to-muted/30">
        <div className="container px-4 md:px-6">
          <div className="grid gap-6 lg:grid-cols-2 lg:gap-12 items-center">
            <div className="flex flex-col justify-center space-y-4">
              <div className="space-y-2">
                <h1 className="text-3xl font-bold tracking-tighter sm:text-4xl md:text-5xl lg:text-6xl">
                  {t.home.hero.title}
                </h1>
                <p className="max-w-[600px] text-muted-foreground md:text-xl">{t.home.hero.subtitle}</p>
              </div>
              <div className="flex flex-col sm:flex-row gap-3">
                <Button size="lg" asChild>
                  <Link href="/dashboard">
                    <FileUp className="mr-2 h-5 w-5" />
                    {t.home.hero.formatButton}
                  </Link>
                </Button>
                <Button size="lg" variant="outline" asChild>
                  <Link href="/subscription">{t.home.hero.learnMore}</Link>
                </Button>
              </div>
            </div>
            <div className="mx-auto lg:mx-0 lg:flex lg:justify-center">
              <div className="relative h-[350px] w-[350px] sm:h-[400px] sm:w-[400px] lg:h-[450px] lg:w-[450px]">
                <div className="absolute inset-0 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full blur-3xl opacity-20"></div>
                <div className="relative h-full w-full bg-white dark:bg-gray-900 rounded-xl border shadow-lg overflow-hidden">
                  <div className="p-6 h-full flex flex-col">
                    <div className="flex items-center justify-between mb-6">
                      <div className="flex items-center space-x-2">
                        <div className="h-3 w-3 rounded-full bg-red-500"></div>
                        <div className="h-3 w-3 rounded-full bg-yellow-500"></div>
                        <div className="h-3 w-3 rounded-full bg-green-500"></div>
                      </div>
                      <div className="text-xs text-muted-foreground">document.docx</div>
                    </div>
                    <div className="flex-1 space-y-4">
                      <div className="h-4 bg-muted rounded w-3/4"></div>
                      <div className="h-4 bg-muted rounded"></div>
                      <div className="h-4 bg-muted rounded w-5/6"></div>
                      <div className="h-4 bg-muted rounded w-2/3"></div>
                      <div className="h-4 bg-muted rounded"></div>
                      <div className="h-4 bg-muted rounded w-4/5"></div>
                      <div className="h-4 bg-muted rounded w-3/4"></div>
                      <div className="h-4 bg-muted rounded"></div>
                      <div className="h-4 bg-muted rounded w-5/6"></div>
                      <div className="h-4 bg-muted rounded w-2/3"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 md:py-20">
        <div className="container px-4 md:px-6">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold tracking-tighter sm:text-4xl md:text-5xl">{t.home.features.title}</h2>
            <p className="mt-4 text-muted-foreground md:text-xl">{t.home.features.subtitle}</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="flex flex-col items-center text-center p-6 bg-muted/50 rounded-lg">
              <div className="p-3 bg-primary/10 rounded-full mb-4">
                <CheckCircle className="h-6 w-6 text-primary" />
              </div>
              <h3 className="text-xl font-bold mb-2">{t.home.features.professional.title}</h3>
              <p className="text-muted-foreground">{t.home.features.professional.description}</p>
            </div>
            <div className="flex flex-col items-center text-center p-6 bg-muted/50 rounded-lg">
              <div className="p-3 bg-primary/10 rounded-full mb-4">
                <Clock className="h-6 w-6 text-primary" />
              </div>
              <h3 className="text-xl font-bold mb-2">{t.home.features.saveTime.title}</h3>
              <p className="text-muted-foreground">{t.home.features.saveTime.description}</p>
            </div>
            <div className="flex flex-col items-center text-center p-6 bg-muted/50 rounded-lg">
              <div className="p-3 bg-primary/10 rounded-full mb-4">
                <Shield className="h-6 w-6 text-primary" />
              </div>
              <h3 className="text-xl font-bold mb-2">{t.home.features.secure.title}</h3>
              <p className="text-muted-foreground">{t.home.features.secure.description}</p>
            </div>
          </div>
        </div>
      </section>

      <footer className="border-t py-6 md:py-8">
        <div className="container flex flex-col items-center justify-center gap-4 md:flex-row md:justify-between">
          <div className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-primary" />
            <p className="text-sm leading-loose text-center md:text-left">{t.home.footer.copyright}</p>
          </div>
          <div className="flex gap-4">
            <Link href="#" className="text-sm text-muted-foreground hover:text-foreground">
              {t.home.footer.terms}
            </Link>
            <Link href="#" className="text-sm text-muted-foreground hover:text-foreground">
              {t.home.footer.privacy}
            </Link>
            <Link href="#" className="text-sm text-muted-foreground hover:text-foreground">
              {t.home.footer.contact}
            </Link>
          </div>
        </div>
      </footer>
    </main>
  )
}
