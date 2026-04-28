@echo off
chcp 65001 >nul
echo ========================================
echo   低代码平台 - Docker 服务启动
echo ========================================
echo.

echo [1/2] 检查 Docker 环境...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未检测到 Docker，请先安装 Docker Desktop
    pause
    exit /b 1
)
echo ✅ Docker 环境正常
echo.

echo [2/2] 启动 Docker Compose 服务...
echo    - PostgreSQL 17 (端口 5432)
echo    - Redis 7 (端口 6379)
echo    - RocketMQ NameServer (端口 9876)
echo    - RocketMQ Broker (端口 10909, 10911, 10912)
echo    - RocketMQ Console (端口 8088)
echo.

docker-compose up -d

if errorlevel 1 (
    echo ❌ 错误: Docker Compose 启动失败
    pause
    exit /b 1
)

echo.
echo ✅ 所有服务启动成功!
echo.
echo ========================================
echo   服务访问信息:
echo   PostgreSQL: localhost:5432
echo     用户名: postgres
echo     密码: thinker
echo     数据库: easy_lowcode
echo.
echo   Redis: localhost:6379
echo.
echo   RocketMQ NameServer: localhost:9876
echo   RocketMQ Console: http://localhost:8088
echo ========================================
echo.
echo 等待服务完全启动...(约30秒)
timeout /t 30 /nobreak >nul

echo.
echo 查看服务状态:
docker-compose ps

echo.
echo ========================================
echo   提示:
echo   - 停止服务: docker-compose down
echo   - 查看日志: docker-compose logs -f
echo   - 重启服务: docker-compose restart
echo ========================================
echo.
pause
