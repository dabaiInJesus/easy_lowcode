# Easy Lowcode - 多阶段构建 Dockerfile
# 用法: docker build -t easy-lowcode .

# =============================================
# 阶段1: 构建
# =============================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 复制 pom.xml 和源码，利用 Maven 缓存层
COPY pom.xml .
COPY easy-lowcode-common/pom.xml easy-lowcode-common/
COPY easy-lowcode-auth/pom.xml easy-lowcode-auth/
COPY easy-lowcode-ai/pom.xml easy-lowcode-ai/
COPY easy-lowcode-collector/pom.xml easy-lowcode-collector/
COPY easy-lowcode-resource/pom.xml easy-lowcode-resource/
COPY easy-lowcode-dashboard/pom.xml easy-lowcode-dashboard/
COPY easy-lowcode-etl/pom.xml easy-lowcode-etl/
COPY easy-lowcode-database/pom.xml easy-lowcode-database/
COPY easy-lowcode-gateway/pom.xml easy-lowcode-gateway/
COPY easy-lowcode-startup/pom.xml easy-lowcode-startup/

# 下载依赖（提前执行，利用 Maven 缓存）
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY . .
RUN mvn clean package -DskipTests -B

# =============================================
# 阶段2: 运行
# =============================================
FROM eclipse-temurin:21-jre-alpine

# 安全: 非 root 运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# 复制 jar 包（取最后一个）
COPY --from=builder /app/easy-lowcode-startup/target/*.jar app.jar

# 复制 Liquibase SQL 文件（如果有外部 SQL 脚本）
COPY --from=builder /app/easy-lowcode-database/src/main/resources/db ./db

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q --spider http://localhost:8081/actuator/health/ || exit 1

EXPOSE 8081

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
