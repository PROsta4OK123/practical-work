@echo off
chcp 65001 >nul
echo ======================================
echo    🧹 ОЧИСТКА И ПЕРЕСБОРКА ПРОЕКТА
echo ======================================
echo.

echo 🏗️  Очистка Maven...
mvn clean
if %errorlevel% neq 0 (
    echo ❌ Ошибка очистки Maven
    pause
    exit /b 1
)

echo.
echo 🟩 Очистка Next.js...
cd src\main\resources\static

echo   📦 Удаление node_modules...
if exist node_modules rmdir /s /q node_modules

echo   🗑️  Удаление кэша...
if exist .next rmdir /s /q .next
if exist .next\cache rmdir /s /q .next\cache

echo   🔧 Очистка package lock...
if exist package-lock.json del package-lock.json

echo   📥 Переустановка зависимостей...
if exist pnpm-lock.yaml (
    pnpm install
) else (
    npm install
)

if %errorlevel% neq 0 (
    echo ❌ Ошибка установки зависимостей
    cd ..\..\..\
    pause
    exit /b 1
)

cd ..\..\..\

echo.
echo ✅ Проект очищен и пересобран!
echo.
echo 💡 Теперь можете запустить:
echo   start-app.bat
echo.
pause 