@echo off
chcp 65001 >nul
echo ======================================
echo    🚀 ЗАПУСК ПРИЛОЖЕНИЯ ОБРАБОТКИ ДОКУМЕНТОВ
echo ======================================
echo.

echo 📋 Проверка зависимостей...

:: Проверка Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Java не найдена! Установите Java 17 или выше
    pause
    exit /b 1
)

:: Проверка Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Maven не найден! Установите Apache Maven
    pause
    exit /b 1
)

:: Проверка Node.js
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Node.js не найден! Установите Node.js
    pause
    exit /b 1
)

:: Проверка npm/pnpm
cd src\main\resources\static 2>nul
if %errorlevel% neq 0 (
    echo ❌ Папка static не найдена!
    pause
    exit /b 1
)

if exist pnpm-lock.yaml (
    where pnpm >nul 2>nul
    if %errorlevel% neq 0 (
        echo ❌ pnpm не найден! Установите pnpm: npm install -g pnpm
        pause
        exit /b 1
    )
    set PACKAGE_MANAGER=pnpm
) else (
    set PACKAGE_MANAGER=npm
)

cd ..\..\..\..\

echo ✅ Все зависимости найдены!
echo.

echo 🏗️  Установка зависимостей фронтенда...
cd src\main\resources\static
%PACKAGE_MANAGER% install
if %errorlevel% neq 0 (
    echo ❌ Ошибка установки зависимостей фронтенда
    pause
    exit /b 1
)

echo 🧹 Очистка кэша Next.js...
if exist .next rmdir /s /q .next
if exist .next\cache rmdir /s /q .next\cache

cd ..\..\..\..\

echo.
echo 🔥 Запуск серверов...
echo.

:: Запуск бекенда в новой консоли
echo 🟦 Запускаю Spring Boot бекенд...
start "🟦 Spring Boot Backend" cmd /k "title 🟦 Spring Boot Backend (Port 8080) && echo Запуск Spring Boot приложения... && echo. && mvn spring-boot:run"

:: Небольшая задержка перед запуском фронтенда
timeout /t 3 /nobreak >nul

:: Запуск фронтенда в новой консоли  
echo 🟩 Запускаю Next.js фронтенд...
start "🟩 Next.js Frontend" cmd /k "title 🟩 Next.js Frontend (Port 3000) && cd src\main\resources\static && echo Запуск Next.js приложения... && echo. && %PACKAGE_MANAGER% run dev"

echo.
echo ✅ Серверы запущены!
echo.
echo 📋 Информация о приложении:
echo   🟦 Backend:  http://localhost:8080
echo   🟩 Frontend: http://localhost:3000  
echo   📖 API Docs: http://localhost:8080/swagger-ui.html
echo.
echo 💡 Подсказки:
echo   • Дождитесь полной загрузки обоих серверов
echo   • Spring Boot запускается ~30-60 секунд
echo   • Next.js запускается ~10-20 секунд
echo   • Ctrl+C в любой консоли остановит сервер
echo.
echo 🎯 Откройте http://localhost:3000 для начала работы!
echo.

:: Опция быстрого открытия браузера
set /p "OPEN_BROWSER=Открыть браузер автоматически? (y/n): "
if /i "%OPEN_BROWSER%"=="y" (
    timeout /t 10 /nobreak >nul
    start http://localhost:3000
)

echo.
echo ✨ Приложение запущено! Нажмите любую клавишу для выхода...
pause >nul 