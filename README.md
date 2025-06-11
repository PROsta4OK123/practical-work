# Word Editor - Приложение для форматирования документов с ИИ

Это приложение для автоматического форматирования Word документов с использованием нейросети Mistral-7b-instruct-v0.2 и библиотеки Apache POI.

## Архитектура

### Бекенд (Spring Boot)
- **Spring Boot 3.5.0** - основной фреймворк
- **Apache POI** - для работы с Word документами
- **Mistral-7b-instruct-v0.2 GGUF** - нейросеть для анализа и форматирования текста
- **MySQL** - база данных
- **JWT** - аутентификация
- **Spring Security** - безопасность

### Фронтенд (Next.js)
- **Next.js 14** - React фреймворк
- **TypeScript** - типизация
- **Tailwind CSS** - стилизация
- **Radix UI** - компоненты

## Установка и запуск

### Предварительные требования

1. **Java 17** или выше
2. **Maven 3.8+**
3. **MySQL 8.0+**
4. **Node.js 18+** и **pnpm** (для фронтенда)

### Подготовка базы данных

```sql
CREATE DATABASE word_editor_database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'word_editor'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON word_editor_db.* TO 'word_editor'@'localhost';
FLUSH PRIVILEGES;
```

### Подготовка модели Mistral

1. Создайте директорию `model` в корне проекта
2. Поместите файл `mistral-7b-instruct-v0.2.Q6_K.gguf` в директорию `model/`

### Настройка переменных окружения

Создайте файл `.env` в корне проекта:

```properties
DB_USERNAME=word_editor
DB_PASSWORD=password
JWT_SECRET=myVerySecretJWTKeyThatIsAtLeast256BitsLong123456789
FILE_UPLOAD_DIR=uploads
FILE_PROCESSED_DIR=processed
AI_MODEL_PATH=model/mistral-7b-instruct-v0.2.Q6_K.gguf
```

### Запуск бекенда

```bash
# Установка зависимостей и сборка
mvn clean install

# Запуск приложения
mvn spring-boot:run
```

Бекенд будет доступен по адресу: `http://localhost:8080`

### Запуск фронтенда (опционально, для разработки)

```bash
cd src/main/resources/static

# Установка зависимостей
pnpm install

# Запуск в режиме разработки
pnpm dev
```

Фронтенд будет доступен по адресу: `http://localhost:3000`

## API Endpoints

### Аутентификация

- `POST /api/auth/register` - Регистрация нового пользователя
- `POST /api/auth/login` - Вход в систему
- `POST /api/auth/refresh` - Обновление токенов
- `GET /api/auth/me` - Получение информации о текущем пользователе
- `POST /api/auth/logout` - Выход из системы
- `POST /api/auth/revoke` - Отзыв токенов

### Обработка документов

- `POST /api/upload` - Загрузка Word документа
- `POST /api/format-document` - Загрузка и форматирование документа
- `GET /api/download/{fileId}` - Скачивание обработанного документа
- `GET /api/document/{fileId}/status` - Получение статуса обработки документа
- `GET /api/document/{fileId}/progress` - Получение прогресса обработки в реальном времени
- `GET /api/processing/metrics` - Получение общих метрик производительности

## Структура проекта

```
src/
├── main/
│   ├── java/com/practical/work/
│   │   ├── config/          # Конфигурации
│   │   ├── controller/      # REST контроллеры
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Обработка исключений
│   │   ├── model/          # JPA сущности
│   │   ├── repository/     # JPA репозитории
│   │   ├── service/        # Бизнес логика
│   │   └── WordEditorApplication.java
│   └── resources/
│       ├── static/         # Фронтенд Next.js приложение
│       ├── ai-prompts.yaml # Конфигурация промптов для ИИ
│       ├── application.yml # Основная конфигурация
│       └── data.sql       # Начальные данные
```

## Принципы архитектуры

### SOLID
- **S** - Каждый сервис имеет единственную ответственность
- **O** - Классы открыты для расширения, закрыты для модификации
- **L** - Подклассы могут заменять базовые классы
- **I** - Интерфейсы сегрегированы по назначению
- **D** - Зависимости инвертированы через DI

### DRY (Don't Repeat Yourself)
- Общая логика вынесена в сервисы
- Повторяющийся код в утилитарные классы
- Конфигурации централизованы

### Слои архитектуры
1. **Controller** - Только HTTP обработка и валидация
2. **Service** - Бизнес логика
3. **Repository** - Доступ к данным
4. **Model** - Доменные сущности

## Тестовые пользователи

После запуска приложения доступны тестовые аккаунты:

- **Admin**: admin@example.com / password
- **User**: test@example.com / password

## Обработка документов

### Ресурсосберегающая многопоточная архитектура

1. Пользователь загружает Word документ
2. Система анализирует размер файла и определяет количество потоков (1-3)
3. Текст извлекается по абзацам и разбивается на блоки
4. **Умное распределение**: Блоки распределяются по очередям (queues) между потоками
5. **Ограниченное количество потоков**: Каждый поток обрабатывает свою очередь блоков последовательно
6. **Параллельная обработка очередей**: Потоки работают параллельно, но каждый обрабатывает свои блоки
7. **Сохранение порядка**: Результаты собираются и сортируются по индексу
8. Форматирование применяется к абзацам в правильном порядке с помощью Apache POI
9. Обработанный документ сохраняется и становится доступен для скачивания

### Преимущества ресурсосберегающей архитектуры

- **Контролируемое потребление ресурсов**: Максимум 2-6 потоков одновременно
- **Адаптивность**: Количество потоков зависит от размера файла
  - Маленькие файлы (<1MB): 1 поток
  - Средние файлы (1-5MB): 2 потока  
  - Большие файлы (>5MB): 3 потока
- **Защита от перегрузки**: Система отклоняет новые файлы при превышении лимита
- **Эффективность**: Один большой файл не блокирует обработку других
- **Мониторинг**: Отслеживание загрузки системы и прогресса в реальном времени
- **Надежность**: Обработка ошибок не влияет на другие блоки
- **Порядок**: Гарантированное сохранение исходной последовательности абзацев

## База данных

### Flyway миграции

Проект использует Flyway для управления миграциями базы данных.

#### Первоначальная настройка

1. **Создание базы данных**:
```sql
CREATE DATABASE word_editor_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **Применение миграций**:
```bash
# Применить все миграции
mvn flyway:migrate

# Проверить статус
mvn flyway:info
```

#### Структура миграций

- **V001** - Создание таблицы очереди обработки файлов
- **V002** - Дополнительные индексы и ограничения
- **V003** - Конфигурация системы и представления

#### Команды для работы с миграциями

```bash
# Применить миграции
mvn flyway:migrate

# Проверить статус
mvn flyway:info

# Валидация
mvn flyway:validate

# Очистка (только для разработки!)
mvn flyway:clean
```

⚠️ **Важно**: Всегда делайте резервные копии перед применением миграций в production!

### Таблица очереди обработки файлов

```sql
CREATE TABLE file_processing_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    estimated_threads INT,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_priority_created (priority DESC, created_at ASC),
    INDEX idx_user_status (user_id, status),
    INDEX idx_created_at (created_at)
);
```

### Статусы очереди
- **PENDING**: В очереди, ожидает обработки
- **PROCESSING**: Обрабатывается
- **COMPLETED**: Обработка завершена успешно
- **FAILED**: Обработка завершена с ошибкой
- **CANCELLED**: Отменена

### Приоритеты очереди
- **HIGH**: Маленькие файлы (<1MB) - высокий приоритет
- **NORMAL**: Средние файлы (1-5MB) - обычный приоритет  
- **LOW**: Большие файлы (>5MB) - низкий приоритет

## Разработка

### Добавление новых функций

1. Создайте соответствующие DTO в пакете `dto`
2. Добавьте бизнес логику в сервисы
3. Создайте или обновите контроллеры
4. Обновите конфигурацию безопасности при необходимости

### Настройка промптов ИИ

Отредактируйте файл `src/main/resources/ai-prompts.yaml` для изменения поведения нейросети.

## Производственное развертывание

1. Настройте внешнюю базу данных MySQL
2. Установите переменные окружения
3. Соберите приложение: `mvn clean package`
4. Запустите JAR файл: `java -jar target/work-0.0.1-SNAPSHOT.jar`

## Лицензия

Этот проект создан в учебных целях. 