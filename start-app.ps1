# PowerShell скрипт для запуска приложения обработки документов
# Запускает Spring Boot и Next.js в отдельных консолях

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   🚀 ЗАПУСК ПРИЛОЖЕНИЯ ОБРАБОТКИ ДОКУМЕНТОВ" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "📋 Проверка зависимостей..." -ForegroundColor Yellow

# Проверка Java
try {
    $null = Get-Command java -ErrorAction Stop
    Write-Host "✅ Java найдена" -ForegroundColor Green
} catch {
    Write-Host "❌ Java не найдена! Установите Java 17 или выше" -ForegroundColor Red
    Read-Host "Нажмите Enter для выхода"
    exit 1
}

# Проверка Maven
try {
    $null = Get-Command mvn -ErrorAction Stop
    Write-Host "✅ Maven найден" -ForegroundColor Green
} catch {
    Write-Host "❌ Maven не найден! Установите Apache Maven" -ForegroundColor Red
    Read-Host "Нажмите Enter для выхода"
    exit 1
}

# Проверка Node.js
try {
    $null = Get-Command node -ErrorAction Stop
    Write-Host "✅ Node.js найден" -ForegroundColor Green
} catch {
    Write-Host "❌ Node.js не найден! Установите Node.js" -ForegroundColor Red
    Read-Host "Нажмите Enter для выхода"
    exit 1
}

# Проверка папки static
if (-not (Test-Path "src\main\resources\static")) {
    Write-Host "❌ Папка static не найдена!" -ForegroundColor Red
    Read-Host "Нажмите Enter для выхода"
    exit 1
}

# Определение пакетного менеджера
if (Test-Path "src\main\resources\static\pnpm-lock.yaml") {
    try {
        $null = Get-Command pnpm -ErrorAction Stop
        $packageManager = "pnpm"
        Write-Host "✅ pnpm найден" -ForegroundColor Green
    } catch {
        Write-Host "❌ pnpm не найден! Установите pnpm: npm install -g pnpm" -ForegroundColor Red
        Read-Host "Нажмите Enter для выхода"
        exit 1
    }
} else {
    $packageManager = "npm"
    Write-Host "✅ npm будет использован" -ForegroundColor Green
}

Write-Host ""
Write-Host "🏗️  Установка зависимостей фронтенда..." -ForegroundColor Yellow

Push-Location "src\main\resources\static"
try {
    & $packageManager install
    if ($LASTEXITCODE -ne 0) {
        throw "Ошибка установки зависимостей"
    }
    Write-Host "✅ Зависимости фронтенда установлены" -ForegroundColor Green
} catch {
    Write-Host "❌ Ошибка установки зависимостей фронтенда" -ForegroundColor Red
    Pop-Location
    Read-Host "Нажмите Enter для выхода"
    exit 1
}
Pop-Location

Write-Host ""
Write-Host "🔥 Запуск серверов..." -ForegroundColor Yellow
Write-Host ""

# Запуск Spring Boot в новом окне PowerShell
Write-Host "🟦 Запускаю Spring Boot бекенд..." -ForegroundColor Blue
Start-Process powershell -ArgumentList "-NoExit", "-Command", "& {
    `$Host.UI.RawUI.WindowTitle = '🟦 Spring Boot Backend (Port 8080)'
    Write-Host 'Запуск Spring Boot приложения...' -ForegroundColor Blue
    Write-Host ''
    mvn spring-boot:run
}"

# Небольшая задержка
Start-Sleep -Seconds 3

# Запуск Next.js в новом окне PowerShell
Write-Host "🟩 Запускаю Next.js фронтенд..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "& {
    `$Host.UI.RawUI.WindowTitle = '🟩 Next.js Frontend (Port 3000)'
    Set-Location 'src\main\resources\static'
    Write-Host 'Запуск Next.js приложения...' -ForegroundColor Green
    Write-Host ''
    & '$packageManager' run dev
}"

Write-Host ""
Write-Host "✅ Серверы запущены!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Информация о приложении:" -ForegroundColor Cyan
Write-Host "  🟦 Backend:  http://localhost:8080" -ForegroundColor Blue
Write-Host "  🟩 Frontend: http://localhost:3000" -ForegroundColor Green
Write-Host "  📖 API Docs: http://localhost:8080/swagger-ui.html" -ForegroundColor Magenta
Write-Host ""
Write-Host "💡 Подсказки:" -ForegroundColor Yellow
Write-Host "  • Дождитесь полной загрузки обоих серверов" -ForegroundColor White
Write-Host "  • Spring Boot запускается ~30-60 секунд" -ForegroundColor White
Write-Host "  • Next.js запускается ~10-20 секунд" -ForegroundColor White
Write-Host "  • Ctrl+C в любой консоли остановит сервер" -ForegroundColor White
Write-Host ""
Write-Host "🎯 Откройте http://localhost:3000 для начала работы!" -ForegroundColor Green
Write-Host ""

# Опция автоматического открытия браузера
$openBrowser = Read-Host "Открыть браузер автоматически? (y/n)"
if ($openBrowser -eq "y" -or $openBrowser -eq "Y") {
    Start-Sleep -Seconds 10
    Start-Process "http://localhost:3000"
}

Write-Host ""
Write-Host "✨ Приложение запущено! Нажмите любую клавишу для выхода..." -ForegroundColor Green
Read-Host 