#!/bin/bash

# Easy Lowcode AI 模块启动脚本

echo "========================================"
echo "  Easy Lowcode AI 模块启动脚本"
echo "========================================"

# 检查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "错误: 需要 Java 21 或更高版本"
    echo "当前版本: Java $JAVA_VERSION"
    exit 1
fi

echo "✓ Java 版本检查通过: Java $JAVA_VERSION"

# 设置默认 profile
PROFILE=${1:-dev}
echo "✓ 使用环境: $PROFILE"

# 启动应用
echo ""
echo "正在启动 Easy Lowcode AI 模块..."
echo "访问地址: http://localhost:8081"
echo "健康检查: http://localhost:8081/api/ai/health"
echo "服务信息: http://localhost:8081/api/ai/info"
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE
