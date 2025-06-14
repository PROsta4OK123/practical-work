# Система логгирования приложения

## Обзор

В приложение добавлена комплексная система логгирования, которая отслеживает различные события и ошибки в реальном времени.

## Компоненты системы

### 1. Основной модуль логгирования (`/lib/logger.ts`)

#### Уровни логгирования:
- **ERROR** (0) - Критические ошибки
- **WARN** (1) - Предупреждения
- **INFO** (2) - Информационные сообщения
- **DEBUG** (3) - Отладочная информация
- **TRACE** (4) - Детальная трассировка

#### Функции:
- Автоматическое сохранение в localStorage (последние 100 записей)
- Отправка критических ошибок на сервер в продакшене
- Генерация уникальных ID для сессий и запросов
- Контекстное логгирование для разных частей приложения

### 2. Специализированные логгеры

- `authLogger` - Аутентификация и авторизация
- `apiLogger` - API запросы и ответы
- `uiLogger` - События пользовательского интерфейса
- `uploadLogger` - Загрузка файлов
- `processingLogger` - Обработка документов

### 3. Компонент просмотра логов (`/components/log-viewer.tsx`)

#### Возможности:
- Фильтрация по уровню логгирования
- Фильтрация по контексту
- Поиск по содержимому
- Автоматическое обновление
- Экспорт логов в JSON
- Очистка логов
- Статистика по типам событий

### 4. Провайдер логгирования (`/components/logging-provider.tsx`)

#### Отслеживает:
- Ошибки JavaScript
- Необработанные Promise rejections
- Изменения видимости страницы
- Сетевое состояние (online/offline)
- Метрики производительности
- Переходы между страницами

### 5. Административная страница (`/app/admin/logs/page.tsx`)

Доступна только администраторам для просмотра всех логов системы.

## Использование

### Базовое логгирование

```typescript
import { appLogger, authLogger, apiLogger } from '@/lib/logger'

// Информационное сообщение
appLogger.info('Пользователь вошел в систему', { userId: 123 })

// Предупреждение
authLogger.warn('Неудачная попытка входа', { email: 'user@example.com' })

// Ошибка
apiLogger.error('Ошибка API запроса', { url: '/api/users', status: 500 })
```

### Специальные методы

```typescript
// Действия пользователя
appLogger.userAction('file_upload', { fileName: 'document.docx' })

// API запросы
apiLogger.apiCall('POST', '/api/upload', { fileSize: 1024 })
apiLogger.apiResponse('POST', '/api/upload', 200, { fileId: 'abc123' })

// Производительность
appLogger.performance('database_query', 250, { query: 'SELECT * FROM users' })

// Безопасность
authLogger.security('Suspicious login attempt', { ip: '192.168.1.1' })
```

### Работа с пользователями

```typescript
// Установка ID пользователя для последующих логов
authLogger.setUserId(123)

// Очистка ID пользователя
authLogger.clearUserId()
```

## Конфигурация

### Переменные окружения

```env
# Уровень логгирования (ERROR, WARN, INFO, DEBUG, TRACE)
NEXT_PUBLIC_LOG_LEVEL=INFO
```

### Хранение логов

- **localStorage**: Последние 100 записей для отладки
- **Сервер**: Критические ошибки в продакшене (ERROR и WARN)
- **Консоль**: Все логи согласно установленному уровню

## Интеграция с компонентами

### Auth Provider
```typescript
// Логгирование попыток входа
authLogger.userAction('login_attempt', { email })

// Отслеживание успешной аутентификации
authLogger.info('User authenticated successfully', { userId, email, role })
```

### Upload Form
```typescript
// Логгирование выбора файла
uploadLogger.info('File selected for upload', { fileName, fileSize, fileType })

// Отслеживание процесса загрузки
uploadLogger.debug('Sending document for formatting', { fileName, fileSize, userId })
```

### API Client
```typescript
// Логгирование API запросов с метриками производительности
apiLogger.apiCall('POST', '/auth/login', { email })
apiLogger.apiResponse('POST', '/auth/login', 200, { success: true, duration: 150 })
```

## Мониторинг и отладка

### Просмотр логов в браузере

1. Откройте консоль разработчика
2. Логи отображаются с форматированием: `[timestamp] [level] [context] [user] [request] message`

### Доступ к сохраненным логам

```javascript
// В консоли браузера
const logs = JSON.parse(localStorage.getItem('app_logs') || '[]')
console.table(logs)
```

### Административная панель

Перейдите по адресу `/admin/logs` (только для администраторов) для полного интерфейса управления логами.

## Рекомендации

1. **Используйте подходящие уровни логгирования**:
   - ERROR: только для критических ошибок
   - WARN: для потенциальных проблем
   - INFO: для важных событий
   - DEBUG: для отладочной информации

2. **Включайте контекстную информацию**:
   ```typescript
   logger.error('Database connection failed', {
     host: 'localhost',
     port: 5432,
     database: 'myapp',
     attempt: 3
   })
   ```

3. **Не логгируйте чувствительные данные**:
   - Пароли
   - Токены
   - Персональные данные

4. **Используйте специализированные логгеры** для разных частей приложения

5. **Регулярно очищайте логи** в localStorage для экономии памяти

## Производительность

- Логи сохраняются асинхронно
- localStorage ограничен 100 записями
- Отправка на сервер только для критических событий
- Минимальное влияние на производительность приложения 