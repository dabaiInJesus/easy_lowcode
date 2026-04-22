@echo off
chcp 65001 >nul
echo ========================================
echo   低代码平台 - 快速启动脚本
echo ========================================
echo.

echo [1/3] 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未检测到 Java 环境，请先安装 JDK 21+
    pause
    exit /b 1
)
echo ✅ Java 环境正常
echo.

echo [2/3] 编译项目...
call mvn clean install -DskipTests
if errorlevel 1 (
    echo ❌ 错误: 项目编译失败
    pause
    exit /b 1
)
echo ✅ 项目编译成功
echo.

echo [3/3] 启动应用...
echo.
echo ========================================
echo   应用正在启动...
echo   访问地址: http://localhost:8080
echo   默认账号: admin / admin123
echo   按 Ctrl+C 停止应用
echo ========================================
echo.

cd easy-lowcode-startup
call mvn spring-boot:run

pause
