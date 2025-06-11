@echo off
chcp 65001 >nul
echo ======================================
echo    🛑 ОСТАНОВКА ПРИЛОЖЕНИЯ
echo ======================================
echo.

echo 🔍 Поиск запущенных процессов...

:: Остановка Spring Boot (Java процессы на порту 8080)
echo 🟦 Остановка Spring Boot...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":8080" ^| find "LISTENING"') do (
    echo   Завершение процесса PID: %%a
    taskkill /F /PID %%a >nul 2>nul
)

:: Остановка Next.js (Node процессы на порту 3000)
echo 🟩 Остановка Next.js...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":3000" ^| find "LISTENING"') do (
    echo   Завершение процесса PID: %%a
    taskkill /F /PID %%a >nul 2>nul
)

:: Остановка всех Java процессов с Maven
echo 🧹 Очистка Maven процессов...
taskkill /F /IM java.exe /FI "WINDOWTITLE eq *mvn*" >nul 2>nul

:: Остановка всех Node процессов
echo 🧹 Очистка Node процессов...
taskkill /F /IM node.exe /FI "WINDOWTITLE eq *Next.js*" >nul 2>nul

echo.
echo ✅ Приложение остановлено!
echo.
echo 💡 Все серверы завершены. Можете безопасно закрыть консоли.
echo.
pause 