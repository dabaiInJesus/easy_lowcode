#!/bin/bash
# ============================================================
#  低代码平台 - 快速启动脚本（前后端一体 / 增量构建版）
#
#  用法:
#    ./start.sh           前后端一起启动 + 增量构建（默认，日常开发用）
#    ./start.sh clean     前后端一起启动 + 全量重建（依赖变化/排查构建问题时用）
#    ./start.sh code      前后端一起启动 + 跳过构建（代码没变时最快）
#    ./start.sh back      只启动后端（前端不需要时）
#    ./start.sh help      查看热加载使用说明
#
#  访问地址:
#    前端(Vite):  http://localhost:6173   （/api 自动代理到后端 8081）
#    后端:        http://localhost:8081   （Swagger: /swagger-ui.html）
#
#  ⚡ 热加载说明:
#    [后端 spring-boot-devtools] 应用启动后保持本终端运行，另开终端触发增量编译，
#    应用会自动快速重启（不重连数据库）:
#      - 改了 easy-lowcode-startup 内的代码:
#          cd easy-lowcode-startup && mvn compile
#      - 改了其他业务模块（如 easy-lowcode-auth）的代码（在项目根目录执行）:
#          mvn install -pl easy-lowcode-auth -DskipTests
#    [前端 Vite]  直接改 easy-lowcode-frontend/src 下的代码，页面即时热更新，
#    无需任何操作。
# ============================================================

MODE="${1:-incremental}"
FRONTEND_DIR="easy-lowcode-frontend"
BACKEND_DIR="easy-lowcode-startup"
FRONT_PORT=6173
BACK_PORT=8081

# ============================================================
# 中文乱码修复：JVM/Maven 输出统一为 UTF-8，并把 Windows 终端代码页切到 65001(UTF-8)
# （Java 日志是 UTF-8 字节，conhost 默认 GBK(936) 渲染会得到 "娉ㄥ唽鑺傜偣" 之类乱码）
# ============================================================
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
export MAVEN_OPTS="-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 ${MAVEN_OPTS}"
RUN_JVM_ARGS="-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
chcp.com 65001 >/dev/null 2>&1 || true

usage() {
    echo "用法:"
    echo "  ./start.sh           前后端一起启动 + 增量构建（默认）"
    echo "  ./start.sh clean     前后端一起启动 + 全量重建"
    echo "  ./start.sh code      前后端一起启动 + 跳过构建"
    echo "  ./start.sh back      只启动后端"
    echo "  ./start.sh help      本帮助"
}

case "${MODE}" in
    incremental|clean|code|back) ;;
    help) usage; exit 0 ;;
    *) echo "❌ 未知模式: ${MODE}"; usage; exit 1 ;;
esac

# 是否启动前端（back 模式不启动）
START_FRONTEND=1
[ "${MODE}" = "back" ] && START_FRONTEND=0

echo "========================================"
echo "  低代码平台 - 快速启动脚本"
echo "  模式: ${MODE}  |  前端: $([ ${START_FRONTEND} = 1 ] && echo 启动 || echo 跳过)"
echo "========================================"
echo ""

echo "[1/3] 检查环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未检测到 Java 环境，请先安装 JDK 21+"
    exit 1
fi
if [ ${START_FRONTEND} = 1 ] && ! command -v npm &> /dev/null; then
    echo "❌ 错误: 未检测到 npm，请先安装 Node.js 18+"
    exit 1
fi
echo "✅ 环境检查通过"
echo ""

# ============================================================
# 远程数据库预检：从 .env 提取 DB 地址，端口可达才继续（DB_SKIP_WAIT=1 跳过）
# 背景：远程（Tailscale）场景下数据库偶发抖动，先等窗口避免启动后挂死
# ============================================================
DB_HOST=$(grep -E "^DB_HOST=" .env 2>/dev/null | head -1 | cut -d= -f2 | tr -d '\r')
DB_HOST=${DB_HOST:-127.0.0.1}
DB_PORT=$(grep -E "^DB_PORT=" .env 2>/dev/null | head -1 | cut -d= -f2 | tr -d '\r')
DB_PORT=${DB_PORT:-5432}

if [ "${DB_SKIP_WAIT}" != "1" ]; then
    echo "[2/4] 等待数据库 ${DB_HOST}:${DB_PORT} 就绪..."
    DB_READY=0
    for i in $(seq 1 ${DB_WAIT_TRIES:-60}); do
        if timeout 3 bash -c "echo > /dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null; then
            DB_READY=1
            break
        fi
        printf "  数据库端口暂不可达，重试 %s/%s...\n" "$i" "${DB_WAIT_TRIES:-60}"
        sleep 5
    done
    if [ ${DB_READY} = 1 ]; then
        echo "✅ 数据库端口可达"
    else
        echo "⚠️ 数据库端口不可达（远端服务可能未启动或正在抖动）"
        echo "→ 将继续构建与启动；若启动失败（连接超时/挂起），请远端恢复后重新运行 ./start.sh"
    fi
    echo ""
fi

# 构建阶段：仅构建 easy-lowcode-startup 及其依赖链（-am），跳过独立部署的 gateway 模块
case "${MODE}" in
    incremental|back)
        echo "[3/4] 增量构建（无 clean，只重编有变化的模块）..."
        if ! mvn install -DskipTests -Djacoco.skip=true -pl ${BACKEND_DIR} -am; then
            echo "❌ 错误: 项目编译失败"
            exit 1
        fi
        echo "✅ 增量构建完成"
        ;;
    clean)
        echo "[3/4] 全量重建（clean install）..."
        if ! mvn clean install -DskipTests -pl ${BACKEND_DIR} -am; then
            echo "❌ 错误: 项目编译失败"
            exit 1
        fi
        echo "✅ 全量重建完成"
        ;;
    code)
        echo "[3/4] 跳过构建，直接启动（使用 target/ 下已有产物）"
        ;;
esac
echo ""

# 前端依赖检查（首次运行自动安装）
if [ ${START_FRONTEND} = 1 ]; then
    if [ ! -d "${FRONTEND_DIR}/node_modules" ]; then
        echo "[4/4] 前端依赖不存在，自动安装中（仅首次，需要几分钟）..."
        (cd "${FRONTEND_DIR}" && npm install) || exit 1
    fi
fi

# ============================================================
# 启动阶段：后端与前端并行；Ctrl+C 一起停止
# ============================================================

# 停止服务：Windows 下 kill 后台 job 不一定级联杀掉 mvn fork 出的 java JVM，
# 因此按端口杀进程树（taskkill /T），确保端口释放、可再次启动
cleanup() {
    echo ""
    echo "正在停止服务..."
    local pid
    for port in ${BACK_PORT} ${FRONT_PORT}; do
        pid=$(netstat -ano | grep ":${port}" | grep -i "LISTEN" | awk '{print $NF}' | head -1)
        if [ -n "${pid}" ]; then
            taskkill //F //T //PID "${pid}" >/dev/null 2>&1
            echo "  已停止端口 ${port} (PID ${pid})"
        fi
    done
    if [ -n "${BACK_JOB}" ]; then kill "${BACK_JOB}" 2>/dev/null; fi
    if [ -n "${FRONT_JOB}" ]; then kill "${FRONT_JOB}" 2>/dev/null; fi
    echo "已全部停止。"
}
trap cleanup EXIT INT TERM

echo "[4/4] 启动服务..."
echo ""
echo "========================================"
echo "  应用正在启动..."
if [ ${START_FRONTEND} = 1 ]; then
echo "  前端: http://localhost:${FRONT_PORT}  ← 测试从这个地址进"
fi
echo "  后端: http://localhost:${BACK_PORT}"
echo "  Swagger: http://localhost:${BACK_PORT}/swagger-ui.html"
echo "  提示: 请确保 PostgreSQL/Redis/RocketMQ 已启动"
echo "  停止: 按 Ctrl+C（前后端一起停）"
echo "========================================"
echo ""

(cd "${BACKEND_DIR}" && exec mvn spring-boot:run "-Dspring-boot.run.jvmArguments=${RUN_JVM_ARGS}") &
BACK_JOB=$!

if [ ${START_FRONTEND} = 1 ]; then
    # 前端端口预清理：残留的 vite(上次未杀干净)会导致端口漂移到 6174+ 或启动失败，这里检测并强制停止
    fp=$(netstat -ano | grep ":${FRONT_PORT}" | grep -i "LISTEN" | awk '{print $NF}' | head -1)
    if [ -n "${fp}" ]; then
        echo "⚠️ 端口 ${FRONT_PORT} 被残留进程(PID ${fp})占用，正在强制停止..."
        taskkill //F //T //PID "${fp}" >/dev/null 2>&1
        sleep 1
    fi
    (cd "${FRONTEND_DIR}" && exec npm run dev) &
    FRONT_JOB=$!
fi

# 后端就绪提示：vite 秒级就绪而 Spring Boot 需 1-3 分钟，就绪前点登录/调接口会
# 得到 502/ECONNREFUSED——这里轮询 Actuator，就绪后打印醒目提示（不阻塞主流程）
(
    for i in $(seq 1 120); do
        sleep 5
        if curl -s -m 3 http://localhost:8081/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
            echo ""
            echo "=============================================="
            echo "  ✅ 后端已就绪: http://localhost:8081"
            echo "  ✅ 现在可以登录/测试了"
            echo "=============================================="
            exit 0
        fi
    done
) &

# 等待核心服务任一退出：一方挂掉立即全停（避免孤儿进程/半截服务）；Ctrl+C 走 trap cleanup
# （只盯后端/前端主任务，上面的就绪 watcher 不参与退出判定）
if [ -n "${FRONT_JOB}" ]; then
    wait -n "${BACK_JOB}" "${FRONT_JOB}"
else
    wait -n "${BACK_JOB}"
fi
cleanup
