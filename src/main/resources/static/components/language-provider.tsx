"use client"

import type React from "react"
import { createContext, useContext, useEffect, useState } from "react"
import type { Language, Translations } from "@/lib/i18n"
import { getTranslation } from "@/lib/i18n"

type LanguageContextType = {
  language: Language
  setLanguage: (language: Language) => void
  t: Translations
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined)

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguageState] = useState<Language>("en")
  const [t, setT] = useState<Translations>(getTranslation("en"))

  useEffect(() => {
    // Load language from localStorage on mount
    const savedLanguage = localStorage.getItem("language") as Language
    if (savedLanguage && (savedLanguage === "en" || savedLanguage === "uk")) {
      setLanguageState(savedLanguage)
      setT(getTranslation(savedLanguage))
    }
  }, [])

  const setLanguage = (newLanguage: Language) => {
    setLanguageState(newLanguage)
    setT(getTranslation(newLanguage))
    localStorage.setItem("language", newLanguage)
  }

  return <LanguageContext.Provider value={{ language, setLanguage, t }}>{children}</LanguageContext.Provider>
}

export function useLanguage() {
  const context = useContext(LanguageContext)
  if (context === undefined) {
    throw new Error("useLanguage must be used within a LanguageProvider")
  }
  return context
}
