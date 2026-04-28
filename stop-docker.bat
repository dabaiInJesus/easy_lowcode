@echo off
chcp 65001 >nul
echo ========================================
echo   低代码平台 - Docker 服务停止
echo ========================================
echo.

echo 正在停止所有服务...
docker-compose down

if errorlevel 1 (
    echo ❌ 错误: Docker Compose 停止失败
    pause
    exit /b 1
)

echo.
echo ✅ 所有服务已停止
echo.
echo 提示:
echo   - 如需删除数据卷(清空数据): docker-compose down -v
echo   - 查看容器状态: docker ps -a
echo.
pause
