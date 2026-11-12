@echo off
chcp 65001 >nul
title 悦家生活 - 后端服务（演示模式 / H2 内存库）
setlocal
cd /d "%~dp0"

set "JAVA_EXE=java"
where java >nul 2>nul
if errorlevel 1 (
    if exist "%USERPROFILE%\.workbuddy\binaries\jdk\jdk-17.0.2\bin\java.exe" (
        set "JAVA_EXE=%USERPROFILE%\.workbuddy\binaries\jdk\jdk-17.0.2\bin\java.exe"
    ) else (
        echo.
        echo 未检测到 Java。请先安装 JDK 8 及以上版本，并把 bin 目录加入 PATH。
        echo.
        pause
        exit /b 1
    )
)

if not exist "backend\target\life-shop-server.jar" (
    echo 未找到已编译的 jar，尝试用 Maven 打包……
    pushd backend
    call mvn -DskipTests package
    popd
)

echo.
echo ============================================
echo   悦家生活后端启动中（演示模式，无需 MySQL）
echo   商城地址：http://localhost:8080
echo   接口自检：http://localhost:8080/api/auth/health
echo   账号：admin/admin123  zhangsan/123456
echo   关闭本窗口即停止服务
echo.
echo   说明：前端产物已打进 jar，8080 直接就能打开整套系统；
echo         只有需要改前端、要热更新时才运行 start-frontend.bat。
echo ============================================
echo.

"%JAVA_EXE%" -jar "backend\target\life-shop-server.jar" --spring.profiles.active=demo --server.port=8080

echo.
echo 服务已退出。
pause
