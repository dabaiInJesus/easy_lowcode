@echo off
REM Easy Lowcode AI 模块启动脚本 (Windows)

echo ========================================
echo   Easy Lowcode AI 模块启动脚本
echo ========================================

REM 检查 Java 版本
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到 Java，请先安装 Java 21 或更高版本
    pause
    exit /b 1
)

echo ✓ Java 已安装

REM 设置默认 profile
set PROFILE=%1
if "%PROFILE%"=="" set PROFILE=dev
echo ✓ 使用环境: %PROFILE%

echo.
echo 正在启动 Easy Lowcode AI 模块...
echo 访问地址: http://localhost:8081
echo 健康检查: http://localhost:8081/api/ai/health
echo 服务信息: http://localhost:8081/api/ai/info
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=%PROFILE%

pause
