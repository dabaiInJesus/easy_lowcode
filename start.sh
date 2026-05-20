#!/bin/bash

echo "========================================"
echo "  低代码平台 - 快速启动脚本"
echo "========================================"
echo ""

echo "[1/3] 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未检测到 Java 环境，请先安装 JDK 21+"
    exit 1
fi
echo "✅ Java 环境正常"
echo ""

echo "[2/3] 编译项目..."
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "❌ 错误: 项目编译失败"
    exit 1
fi
echo "✅ 项目编译成功"
echo ""

echo "[3/3] 启动应用..."
echo ""
echo "========================================"
echo "  应用正在启动..."
echo "  访问地址: http://localhost:8080"
echo "  提示: 请确保 PostgreSQL/Redis/RocketMQ 已启动"
echo "  按 Ctrl+C 停止应用"
echo "========================================"
echo ""

cd easy-lowcode-startup
mvn spring-boot:run
