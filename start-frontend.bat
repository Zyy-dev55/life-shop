@echo off
chcp 65001 >nul
title 悦家生活 - 前端
setlocal
cd /d "%~dp0frontend"

where node >nul 2>nul
if errorlevel 1 (
    echo.
    echo 未检测到 Node.js，请先安装 Node 16 及以上版本。
    echo.
    pause
    exit /b 1
)

if not exist "node_modules" (
    echo 首次运行，正在安装前端依赖（可能需要几分钟）……
    call npm install --no-audit --no-fund
)

echo.
echo ============================================
echo   前端启动中，稍后会自动打开浏览器
echo   地址：http://localhost:5173
echo   注意：请先运行 start-demo.bat 启动后端
echo ============================================
echo.

call npm run dev

pause
