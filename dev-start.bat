@echo off
echo 🚀 Быстрый запуск для разработчиков...
echo.

:: Запуск бекенда
echo 🟦 Запуск Spring Boot...
start "Backend" cmd /k "mvn spring-boot:run"

:: Небольшая задержка
timeout /t 2 /nobreak >nul

:: Запуск фронтенда
echo 🟩 Запуск Next.js...
start "Frontend" cmd /k "cd src\main\resources\static && npm run dev"

echo.
echo ✅ Серверы запущены!
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:3000
echo.
pause 