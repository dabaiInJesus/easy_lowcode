#!/bin/bash

echo "========================================"
echo "  低代码平台 - 环境检查"
echo "========================================"
echo ""

echo "正在检查必需的环境..."
echo ""

# 检查 Java
echo "[1/5] 检查 Java..."
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装或未配置环境变量"
    echo "   请安装 JDK 21+"
else
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo "✅ Java 已安装: $JAVA_VERSION"
fi
echo ""

# 检查 Maven
echo "[2/5] 检查 Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装或未配置环境变量"
    echo "   请安装 Maven 3.6+"
else
    MAVEN_VERSION=$(mvn -version 2>&1 | head -n 1 | awk '{print $3}')
    echo "✅ Maven 已安装: $MAVEN_VERSION"
fi
echo ""

# 检查 PostgreSQL
echo "[3/5] 检查 PostgreSQL..."
if ! command -v psql &> /dev/null; then
    echo "⚠️  PostgreSQL 客户端未找到"
    echo "   请确保 PostgreSQL 17 已安装并运行"
else
    echo "✅ PostgreSQL 客户端已安装"
fi
echo ""

# 检查 Redis
echo "[4/5] 检查 Redis..."
if redis-cli ping &> /dev/null; then
    echo "✅ Redis 运行正常"
else
    echo "⚠️  Redis 未运行或无法连接"
    echo "   请启动 Redis 服务"
fi
echo ""

# 检查 RocketMQ
echo "[5/5] 检查 RocketMQ..."
echo "⚠️  RocketMQ 需要手动检查"
echo "   请确保 NameServer 和 Broker 已启动"
echo ""

echo "========================================"
echo "  环境检查完成"
echo "========================================"
echo ""
echo "下一步:"
echo "1. 确保 PostgreSQL 已启动并创建数据库: psql -U postgres -c \"CREATE DATABASE thinker WITH ENCODING 'UTF8';\""
echo "2. Liquibase 将自动执行数据库初始化（无需手动执行 SQL）"
echo "3. 启动应用: ./start.sh"
echo ""
