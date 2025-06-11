export type Language = "en" | "uk"

export interface Translations {
  // Navigation
  nav: {
    home: string
    dashboard: string
    subscription: string
    login: string
    signup: string
    logout: string
    points: string
    profile: string
  }

  // Authentication
  auth: {
    login: {
      title: string
      subtitle: string
      email: string
      password: string
      forgotPassword: string
      loginButton: string
      loggingIn: string
      noAccount: string
      signUp: string
      continueWithGoogle: string
      orContinueWith: string
    }
    register: {
      title: string
      subtitle: string
      name: string
      email: string
      password: string
      confirmPassword: string
      createButton: string
      creatingAccount: string
      haveAccount: string
      logIn: string
      continueWithGoogle: string
      orContinueWith: string
    }
    errors: {
      fillAllFields: string
      passwordsNoMatch: string
      invalidCredentials: string
      createAccountFailed: string
    }
    success: {
      loggedIn: string
      accountCreated: string
    }
  }

  // Home page
  home: {
    hero: {
      title: string
      subtitle: string
      formatButton: string
      learnMore: string
    }
    features: {
      title: string
      subtitle: string
      professional: {
        title: string
        description: string
      }
      saveTime: {
        title: string
        description: string
      }
      secure: {
        title: string
        description: string
      }
    }
    upload: {
      title: string
      subtitle: string
    }
    footer: {
      copyright: string
      terms: string
      privacy: string
      contact: string
    }
  }

  // Upload form
  upload: {
    title: string
    supportedFormats: string
    cost: string
    yourPoints: string
    formatButton: string
    uploading: string
    authRequired: {
      title: string
      description: string
    }
    insufficientPoints: {
      title: string
      description: string
      purchaseButton: string
    }
    errors: {
      invalidFileType: string
      authRequired: string
      insufficientPoints: string
      uploadFailed: string
      processingFailed: string
    }
  }

  // Processing page
  processing: {
    title: string
    subtitle: string
    ready: {
      title: string
      subtitle: string
      downloadButton: string
      newFileButton: string
    }
    messages: {
      documentReady: string
      downloadStarted: string
    }
  }

  // Dashboard
  dashboard: {
    title: string
    tabs: {
      upload: string
      documents: string
      profile: string
    }
    documents: {
      title: string
      formatNewButton: string
      noDocuments: string
      original: string
      formatted: string
    }
    profile: {
      title: string
      subtitle: string
      editButton: string
      saveButton: string
      name: string
      email: string
      company: string
      phone: string
      subscription: {
        title: string
        subtitle: string
        currentPlan: string
        pointsAvailable: string
        expiresOn: string
        manageButton: string
        purchaseButton: string
        free: string
        premium: string
      }
    }
    messages: {
      profileUpdated: string
      profileUpdateSuccess: string
    }
  }

  // Subscription
  subscription: {
    title: string
    subtitle: string
    plans: {
      basic: {
        name: string
        description: string
        features: string[]
      }
      pro: {
        name: string
        description: string
        features: string[]
      }
      enterprise: {
        name: string
        description: string
        features: string[]
      }
    }
    mostPopular: string
    subscribeButton: string
    faq: {
      title: string
      howPointsWork: {
        question: string
        answer: string
      }
      cancelSubscription: {
        question: string
        answer: string
      }
      runOutOfPoints: {
        question: string
        answer: string
      }
    }
    messages: {
      purchased: string
      purchaseSuccess: string
    }
  }

  // Common
  common: {
    loading: string
    error: string
    success: string
    cancel: string
    save: string
    edit: string
    delete: string
    download: string
    upload: string
    back: string
    next: string
    previous: string
    close: string
  }
}

export const translations: Record<Language, Translations> = {
  en: {
    nav: {
      home: "Home",
      dashboard: "Dashboard",
      subscription: "Subscription",
      login: "Log in",
      signup: "Sign up",
      logout: "Log out",
      points: "points",
      profile: "Profile",
    },
    auth: {
      login: {
        title: "Log in",
        subtitle: "Enter your email and password to log in to your account",
        email: "Email",
        password: "Password",
        forgotPassword: "Forgot password?",
        loginButton: "Log in",
        loggingIn: "Logging in...",
        noAccount: "Don't have an account?",
        signUp: "Sign up",
        continueWithGoogle: "Continue with Google",
        orContinueWith: "Or continue with",
      },
      register: {
        title: "Create an account",
        subtitle: "Enter your information to create an account",
        name: "Name",
        email: "Email",
        password: "Password",
        confirmPassword: "Confirm Password",
        createButton: "Create account",
        creatingAccount: "Creating account...",
        haveAccount: "Already have an account?",
        logIn: "Log in",
        continueWithGoogle: "Continue with Google",
        orContinueWith: "Or continue with",
      },
      errors: {
        fillAllFields: "Please fill in all fields",
        passwordsNoMatch: "Passwords do not match",
        invalidCredentials: "Invalid email or password",
        createAccountFailed: "Failed to create account",
      },
      success: {
        loggedIn: "You have been logged in",
        accountCreated: "Your account has been created",
      },
    },
    home: {
      hero: {
        title: "Professional Word Document Formatting",
        subtitle:
          "Upload your Word documents and get them professionally formatted in seconds. Save time and ensure consistency across all your documents.",
        formatButton: "Format Document",
        learnMore: "Learn More",
      },
      features: {
        title: "Why Choose DocFormat?",
        subtitle: "Our document formatting service offers several advantages",
        professional: {
          title: "Professional Results",
          description: "Get perfectly formatted documents that follow industry standards and best practices.",
        },
        saveTime: {
          title: "Save Time",
          description: "Format documents in seconds instead of spending hours doing it manually.",
        },
        secure: {
          title: "Secure Processing",
          description: "Your documents are processed securely and never stored on our servers.",
        },
      },
      upload: {
        title: "Format Your Document",
        subtitle: "Upload your Word document and we'll format it for you",
      },
      footer: {
        copyright: "© 2023 DocFormat. All rights reserved.",
        terms: "Terms",
        privacy: "Privacy",
        contact: "Contact",
      },
    },
    upload: {
      title: "Upload Word Document",
      supportedFormats: "Supported formats: .doc, .docx",
      cost: "Cost: 1 point",
      yourPoints: "Your points:",
      formatButton: "Format Document",
      uploading: "Uploading...",
      authRequired: {
        title: "Authentication Required",
        description: "Please log in or create an account to format documents",
      },
      insufficientPoints: {
        title: "Insufficient Points",
        description: "You need at least 1 point to format a document",
        purchaseButton: "Purchase Subscription",
      },
      errors: {
        invalidFileType: "Please upload a Word document (.doc or .docx)",
        authRequired: "Please log in to format documents",
        insufficientPoints: "You need at least 1 point to format a document",
        uploadFailed: "There was an error uploading your document",
        processingFailed: "Failed to process document. Please try again.",
      },
    },
    processing: {
      title: "Processing Document",
      subtitle: "Please wait while we format your document...",
      ready: {
        title: "Document Ready",
        subtitle: "Your document has been formatted successfully",
        downloadButton: "Download Document",
        newFileButton: "Upload New File",
      },
      messages: {
        documentReady: "Your document has been formatted successfully",
        downloadStarted: "Your document is being downloaded",
      },
    },
    dashboard: {
      title: "Dashboard",
      tabs: {
        upload: "Upload",
        documents: "Documents",
        profile: "Profile",
      },
      documents: {
        title: "Document History",
        formatNewButton: "Format New Document",
        noDocuments: "No documents found",
        original: "Original:",
        formatted: "Formatted:",
      },
      profile: {
        title: "Profile Information",
        subtitle: "Update your account information",
        editButton: "Edit",
        saveButton: "Save",
        name: "Name",
        email: "Email",
        company: "Company (Optional)",
        phone: "Phone (Optional)",
        subscription: {
          title: "Subscription Status",
          subtitle: "Manage your subscription and points",
          currentPlan: "Current Plan",
          pointsAvailable: "Points Available",
          expiresOn: "Expires On",
          manageButton: "Manage Subscription",
          purchaseButton: "Purchase Subscription",
          free: "Free",
          premium: "Premium",
        },
      },
      messages: {
        profileUpdated: "Profile Updated",
        profileUpdateSuccess: "Your profile has been updated successfully",
      },
    },
    subscription: {
      title: "Choose Your Plan",
      subtitle: "Select a subscription plan that works for you",
      plans: {
        basic: {
          name: "Basic",
          description: "For occasional document formatting",
          features: ["30 formatting points", "Standard formatting options", "Email support", "Valid for 30 days"],
        },
        pro: {
          name: "Professional",
          description: "For regular document formatting needs",
          features: [
            "100 formatting points",
            "Advanced formatting options",
            "Priority email support",
            "Valid for 30 days",
            "Batch processing",
          ],
        },
        enterprise: {
          name: "Enterprise",
          description: "For teams and businesses",
          features: [
            "300 formatting points",
            "All formatting options",
            "Priority support",
            "Valid for 30 days",
            "Batch processing",
            "API access",
            "Team management",
          ],
        },
      },
      mostPopular: "Most Popular",
      subscribeButton: "Subscribe",
      faq: {
        title: "Frequently Asked Questions",
        howPointsWork: {
          question: "How do points work?",
          answer:
            "Each document formatting costs 1 point. When you purchase a subscription, you receive a certain number of points that you can use to format documents. Points do not expire as long as your account is active.",
        },
        cancelSubscription: {
          question: "Can I cancel my subscription?",
          answer:
            "Yes, you can cancel your subscription at any time. Your points will remain available for use until they are depleted.",
        },
        runOutOfPoints: {
          question: "What happens if I run out of points?",
          answer:
            "If you run out of points, you'll need to purchase a new subscription to continue formatting documents. You can check your point balance in your dashboard.",
        },
      },
      messages: {
        purchased: "Subscription Purchased",
        purchaseSuccess: "You have successfully purchased the {plan} plan and received {points} points.",
      },
    },
    common: {
      loading: "Loading...",
      error: "Error",
      success: "Success",
      cancel: "Cancel",
      save: "Save",
      edit: "Edit",
      delete: "Delete",
      download: "Download",
      upload: "Upload",
      back: "Back",
      next: "Next",
      previous: "Previous",
      close: "Close",
    },
  },
  uk: {
    nav: {
      home: "Головна",
      dashboard: "Панель керування",
      subscription: "Підписка",
      login: "Увійти",
      signup: "Реєстрація",
      logout: "Вийти",
      points: "балів",
      profile: "Профіль",
    },
    auth: {
      login: {
        title: "Вхід в систему",
        subtitle: "Введіть свій email та пароль для входу в акаунт",
        email: "Email",
        password: "Пароль",
        forgotPassword: "Забули пароль?",
        loginButton: "Увійти",
        loggingIn: "Вхід...",
        noAccount: "Немає акаунту?",
        signUp: "Зареєструватися",
        continueWithGoogle: "Продовжити з Google",
        orContinueWith: "Або продовжити з",
      },
      register: {
        title: "Створити акаунт",
        subtitle: "Введіть свою інформацію для створення акаунту",
        name: "Ім'я",
        email: "Email",
        password: "Пароль",
        confirmPassword: "Підтвердіть пароль",
        createButton: "Створити акаунт",
        creatingAccount: "Створення акаунту...",
        haveAccount: "Вже є акаунт?",
        logIn: "Увійти",
        continueWithGoogle: "Продовжити з Google",
        orContinueWith: "Або продовжити з",
      },
      errors: {
        fillAllFields: "Будь ласка, заповніть всі поля",
        passwordsNoMatch: "Паролі не співпадають",
        invalidCredentials: "Невірний email або пароль",
        createAccountFailed: "Не вдалося створити акаунт",
      },
      success: {
        loggedIn: "Ви успішно увійшли в систему",
        accountCreated: "Ваш акаунт було створено",
      },
    },
    home: {
      hero: {
        title: "Професійне форматування Word документів",
        subtitle:
          "Завантажте свої Word документи та отримайте їх професійно відформатованими за секунди. Заощаджуйте час та забезпечуйте узгодженість усіх ваших документів.",
        formatButton: "Форматувати документ",
        learnMore: "Дізнатися більше",
      },
      features: {
        title: "Чому обрати DocFormat?",
        subtitle: "Наш сервіс форматування документів пропонує кілька переваг",
        professional: {
          title: "Професійні результати",
          description:
            "Отримайте ідеально відформатовані документи, які відповідають галузевим стандартам та найкращим практикам.",
        },
        saveTime: {
          title: "Заощадьте час",
          description: "Форматуйте документи за секунди замість того, щоб витрачати години на ручне форматування.",
        },
        secure: {
          title: "Безпечна обробка",
          description: "Ваші документи обробляються безпечно і ніколи не зберігаються на наших серверах.",
        },
      },
      upload: {
        title: "Відформатуйте свій документ",
        subtitle: "Завантажте свій Word документ, і ми відформатуємо його для вас",
      },
      footer: {
        copyright: "© 2023 DocFormat. Всі права захищені.",
        terms: "Умови",
        privacy: "Конфіденційність",
        contact: "Контакти",
      },
    },
    upload: {
      title: "Завантажити Word документ",
      supportedFormats: "Підтримувані формати: .doc, .docx",
      cost: "Вартість: 1 бал",
      yourPoints: "Ваші бали:",
      formatButton: "Форматувати документ",
      uploading: "Завантаження...",
      authRequired: {
        title: "Потрібна автентифікація",
        description: "Будь ласка, увійдіть або створіть акаунт для форматування документів",
      },
      insufficientPoints: {
        title: "Недостатньо балів",
        description: "Вам потрібен принаймні 1 бал для форматування документа",
        purchaseButton: "Придбати підписку",
      },
      errors: {
        invalidFileType: "Будь ласка, завантажте Word документ (.doc або .docx)",
        authRequired: "Будь ласка, увійдіть для форматування документів",
        insufficientPoints: "Вам потрібен принаймні 1 бал для форматування документа",
        uploadFailed: "Сталася помилка при завантаженні вашого документа",
        processingFailed: "Не вдалося обробити документ. Спробуйте ще раз.",
      },
    },
    processing: {
      title: "Обробка документа",
      subtitle: "Будь ласка, зачекайте, поки ми форматуємо ваш документ...",
      ready: {
        title: "Документ готовий",
        subtitle: "Ваш документ було успішно відформатовано",
        downloadButton: "Завантажити документ",
        newFileButton: "Завантажити новий файл",
      },
      messages: {
        documentReady: "Ваш документ було успішно відформатовано",
        downloadStarted: "Ваш документ завантажується",
      },
    },
    dashboard: {
      title: "Панель керування",
      tabs: {
        upload: "Завантаження",
        documents: "Документи",
        profile: "Профіль",
      },
      documents: {
        title: "Історія документів",
        formatNewButton: "Форматувати новий документ",
        noDocuments: "Документи не знайдено",
        original: "Оригінал:",
        formatted: "Відформатований:",
      },
      profile: {
        title: "Інформація профілю",
        subtitle: "Оновіть інформацію вашого акаунту",
        editButton: "Редагувати",
        saveButton: "Зберегти",
        name: "Ім'я",
        email: "Email",
        company: "Компанія (необов'язково)",
        phone: "Телефон (необов'язково)",
        subscription: {
          title: "Статус підписки",
          subtitle: "Керуйте своєю підпискою та балами",
          currentPlan: "Поточний план",
          pointsAvailable: "Доступні бали",
          expiresOn: "Діє до",
          manageButton: "Керувати підпискою",
          purchaseButton: "Придбати підписку",
          free: "Безкоштовний",
          premium: "Преміум",
        },
      },
      messages: {
        profileUpdated: "Профіль оновлено",
        profileUpdateSuccess: "Ваш профіль було успішно оновлено",
      },
    },
    subscription: {
      title: "Оберіть свій план",
      subtitle: "Виберіть план підписки, який вам підходить",
      plans: {
        basic: {
          name: "Базовий",
          description: "Для періодичного форматування документів",
          features: [
            "30 балів для форматування",
            "Стандартні опції форматування",
            "Підтримка по email",
            "Дійсний 30 днів",
          ],
        },
        pro: {
          name: "Професійний",
          description: "Для регулярних потреб форматування документів",
          features: [
            "100 балів для форматування",
            "Розширені опції форматування",
            "Пріоритетна підтримка по email",
            "Дійсний 30 днів",
            "Пакетна обробка",
          ],
        },
        enterprise: {
          name: "Корпоративний",
          description: "Для команд та бізнесу",
          features: [
            "300 балів для форматування",
            "Всі опції форматування",
            "Пріоритетна підтримка",
            "Дійсний 30 днів",
            "Пакетна обробка",
            "Доступ до API",
            "Керування командою",
          ],
        },
      },
      mostPopular: "Найпопулярніший",
      subscribeButton: "Підписатися",
      faq: {
        title: "Часті запитання",
        howPointsWork: {
          question: "Як працюють бали?",
          answer:
            "Кожне форматування документа коштує 1 бал. Коли ви купуєте підписку, ви отримуєте певну кількість балів, які можете використовувати для форматування документів. Бали не закінчуються, поки ваш акаунт активний.",
        },
        cancelSubscription: {
          question: "Чи можу я скасувати підписку?",
          answer:
            "Так, ви можете скасувати підписку в будь-який час. Ваші бали залишаться доступними для використання, поки вони не закінчаться.",
        },
        runOutOfPoints: {
          question: "Що станеться, якщо у мене закінчаться бали?",
          answer:
            "Якщо у вас закінчаться бали, вам потрібно буде придбати нову підписку, щоб продовжити форматування документів. Ви можете перевірити баланс балів у своїй панелі керування.",
        },
      },
      messages: {
        purchased: "Підписку придбано",
        purchaseSuccess: "Ви успішно придбали план {plan} та отримали {points} балів.",
      },
    },
    common: {
      loading: "Завантаження...",
      error: "Помилка",
      success: "Успіх",
      cancel: "Скасувати",
      save: "Зберегти",
      edit: "Редагувати",
      delete: "Видалити",
      download: "Завантажити",
      upload: "Завантажити",
      back: "Назад",
      next: "Далі",
      previous: "Попередній",
      close: "Закрити",
    },
  },
}

export function getTranslation(language: Language): Translations {
  return translations[language] || translations.en
}
