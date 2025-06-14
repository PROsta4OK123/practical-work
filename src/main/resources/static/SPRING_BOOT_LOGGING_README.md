# Система логгирования Spring Boot

## Обзор

В Spring Boot приложение добавлена комплексная система логгирования для мониторинга всех аспектов работы системы.

## 🔧 Компоненты системы логгирования

### 1. RequestLoggingFilter - Фильтр HTTP запросов

**Файл:** `src/main/java/com/practical/work/config/RequestLoggingFilter.java`

#### Функции:
- Логгирование всех входящих HTTP запросов
- Отслеживание времени обработки запросов
- Логгирование IP адресов клиентов
- Скрытие чувствительных заголовков (Authorization, Cookie)
- Предупреждения о медленных запросах (>1000ms)
- Детальное логгирование ошибок с телом ответа

#### Пример логов:
```
[REQ-1234567890-123] --> POST /auth/login (IP: 192.168.1.100, User-Agent: Mozilla/5.0...)
[REQ-1234567890-123] <-- 200 SUCCESS (250ms)
[REQ-1234567890-456] МЕДЛЕННЫЙ ЗАПРОС: 1500ms
```

### 2. LoggingExceptionHandler - Обработчик исключений

**Файл:** `src/main/java/com/practical/work/exception/LoggingExceptionHandler.java`

#### Типы исключений:
- **AsyncRequestNotUsableException** - Разрыв соединения клиентом
- **AuthenticationException** - Ошибки аутентификации
- **AccessDeniedException** - Отказы в доступе
- **MethodArgumentNotValidException** - Ошибки валидации
- **MaxUploadSizeExceededException** - Превышение размера файла
- **RuntimeException** - Бизнес-логика и системные ошибки

#### Пример логов:
```
[ERR-1234567890-789] Клиент закрыл соединение для POST /auth/login: ServletOutputStream failed to flush
[ERR-1234567890-790] Ошибка валидации для POST /auth/register: {email=Некорректный формат email}
```

### 3. Расширенное логгирование AuthService

#### Функции:
- Детальное отслеживание процесса аутентификации
- Логгирование времени выполнения операций
- Предупреждения о неудачных попытках входа
- Отслеживание заблокированных аккаунтов

#### Пример логов:
```
Попытка входа пользователя: test@example.com
Пользователь найден: test@example.com (ID: 123, активен: true)
Пароль валиден для пользователя: test@example.com
Пользователь test@example.com успешно авторизован (150ms)
```

### 4. Расширенное логгирование DocumentService

#### Функции:
- Логгирование загрузки файлов с деталями
- Отслеживание обработки документов
- Мониторинг размеров файлов
- Логгирование ошибок с контекстом

#### Пример логов:
```
Загрузка документа document.docx пользователем test@example.com (размер: 2048 байт)
Документ загружен успешно и добавлен в очередь: abc-123-def (350ms)
```

### 5. PerformanceMonitoringService - Мониторинг производительности

**Файл:** `src/main/java/com/practical/work/service/PerformanceMonitoringService.java`

#### Метрики:
- **Системные метрики** (каждую минуту)
  - Использование памяти (heap/non-heap)
  - Количество потоков
  - Время работы системы
  - Количество процессоров

- **API статистика** (каждые 5 минут)
  - Количество вызовов по эндпоинтам
  - Среднее время ответа
  - Количество ошибок
  - Процент ошибок

- **Ежедневные отчеты** (каждые 24 часа)
  - Общее время работы
  - Пиковое использование памяти

#### Пример логов:
```
SYSTEM_METRICS - Heap: 128MB/512MB (25.0%), NonHeap: 64MB/256MB, Threads: 15, Processors: 4, Uptime: 3600000ms
API_STATISTICS:
  /auth/login - Calls: 45, Avg Response: 180ms, Errors: 2 (4.4%)
  /upload - Calls: 12, Avg Response: 850ms, Errors: 0 (0.0%)
HIGH_MEMORY_USAGE: 85.5% heap memory used
```

## 📋 Конфигурация логгирования

**Файл:** `src/main/resources/application.yml`

```yaml
logging:
  level:
    com.practical.work: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{40}] - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{40}] - %msg%n"
  file:
    name: logs/application.log
    max-size: 100MB
    max-history: 30
```

## 🎯 Типы логов и их назначение

### 1. Безопасность
- Попытки входа с неверными данными
- Доступ к заблокированным аккаунтам
- Подозрительная активность
- JWT токены (без раскрытия содержимого)

### 2. Производительность
- Медленные запросы (>1000ms)
- Высокое использование памяти (>80%)
- Медленные SQL запросы
- Операции с файлами

### 3. Ошибки и исключения
- Разрывы соединений
- Ошибки валидации
- Системные ошибки
- Ошибки обработки файлов

### 4. Пользовательская активность
- Входы/выходы из системы
- Загрузка файлов
- Обработка документов
- Скачивание результатов

### 5. Системные события
- Запуск/остановка приложения
- Очистка очередей
- Мониторинг обработки
- Метрики производительности

## 🔍 Анализ логов

### Поиск конкретных событий

```bash
# Ошибки аутентификации
grep "Ошибка аутентификации" logs/application.log

# Медленные запросы
grep "МЕДЛЕННЫЙ ЗАПРОС" logs/application.log

# Разрывы соединений
grep "Клиент закрыл соединение" logs/application.log

# Системные метрики
grep "SYSTEM_METRICS" logs/application.log
```

### Мониторинг производительности

```bash
# API статистика
grep "API_STATISTICS" logs/application.log

# Высокое использование памяти
grep "HIGH_MEMORY_USAGE" logs/application.log

# Операции с файлами
grep "FILE_OPERATION" logs/application.log
```

## 🚨 Критические события для мониторинга

### 1. Проблемы с соединениями
```
AsyncRequestNotUsableException: ServletOutputStream failed to flush
```
**Причины:** Таймауты клиента, проблемы сети, медленные ответы

### 2. Высокое использование памяти
```
HIGH_MEMORY_USAGE: 85.5% heap memory used
```
**Действия:** Проверить утечки памяти, увеличить heap size

### 3. Медленные запросы
```
МЕДЛЕННЫЙ ЗАПРОС: 2500ms
```
**Действия:** Оптимизировать запросы, проверить нагрузку на БД

### 4. Ошибки аутентификации
```
Неверный пароль для пользователя: user@example.com
```
**Действия:** Мониторить на предмет брутфорса

## 🛠 Настройка для разных сред

### Development
```yaml
logging:
  level:
    com.practical.work: DEBUG
    org.springframework.security: DEBUG
```

### Production
```yaml
logging:
  level:
    com.practical.work: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
```

### Тестирование
```yaml
logging:
  level:
    com.practical.work: TRACE
    org.springframework.web: DEBUG
```

## 📊 Интеграция с мониторингом

Логи можно интегрировать с системами мониторинга:
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Grafana + Loki**
- **Splunk**
- **Azure Monitor / AWS CloudWatch**

## 🔧 Рекомендации по эксплуатации

1. **Регулярно архивируйте логи** (настроено 30 дней)
2. **Мониторьте размер файлов логов** (лимит 100MB)
3. **Настройте алерты** на критические события
4. **Анализируйте тренды** по API статистике
5. **Отслеживайте метрики производительности**

## 🎯 Заключение

Система логгирования обеспечивает:
- **Полную видимость** всех операций в системе
- **Быструю диагностику** проблем
- **Мониторинг производительности** в реальном времени
- **Безопасность** и аудит действий пользователей
- **Проактивное выявление** проблем до их влияния на пользователей 