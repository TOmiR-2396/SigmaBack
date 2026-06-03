#!/usr/bin/env bash
# deploy.sh — Build local + deploy al VPS con docker-compose
# Uso: ./deploy.sh [usuario@ip]
# Ejemplo: ./deploy.sh root@72.60.245.66
#
# Lo que hace:
#   1. Buildea el frontend (npm run build)
#   2. Buildea el backend JAR (mvnw package)
#   3. Sube los archivos al servidor (rsync)
#   4. Levanta los contenedores en el servidor (docker compose up --build)

set -e

SERVER="${1:-root@72.60.245.66}"
DEPLOY_DIR="/srv/gestigym"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONT_DIR="$(cd "$SCRIPT_DIR/../../frontSigma" 2>/dev/null && pwd || echo "")"

echo ""
echo "======================================================"
echo "  GestiGym -- Deploy --> $SERVER"
echo "======================================================"

# 1. Build frontend ─────────────────────────────────────────────────────────
if [ -n "$FRONT_DIR" ] && [ -d "$FRONT_DIR" ]; then
    echo ""
    echo "[1/4] Buildando frontend..."
    cd "$FRONT_DIR"
    npm ci --silent
    npm run build
    echo "      Listo: $FRONT_DIR/dist/"
else
    echo ""
    echo "[1/4] No se encontro frontSigma/ -- saltando frontend"
fi

# 2. Build backend JAR ──────────────────────────────────────────────────────
echo ""
echo "[2/4] Buildando backend JAR..."
cd "$SCRIPT_DIR"
./mvnw package -DskipTests -q
echo "      Listo: $SCRIPT_DIR/target/"

# 3. Preparar servidor ──────────────────────────────────────────────────────
echo ""
echo "[3/4] Subiendo archivos al servidor..."
ssh "$SERVER" "mkdir -p $DEPLOY_DIR/frontend/dist $DEPLOY_DIR/nginx $DEPLOY_DIR/target"

# Backend
rsync -az --progress \
    "$SCRIPT_DIR/target/"*.jar \
    "$SERVER:$DEPLOY_DIR/target/"

rsync -az \
    "$SCRIPT_DIR/Dockerfile" \
    "$SCRIPT_DIR/docker-compose.yml" \
    "$SERVER:$DEPLOY_DIR/"

rsync -az \
    "$SCRIPT_DIR/nginx/" \
    "$SERVER:$DEPLOY_DIR/nginx/"

# Frontend
if [ -n "$FRONT_DIR" ] && [ -d "$FRONT_DIR/dist" ]; then
    rsync -az --delete --progress \
        "$FRONT_DIR/dist/" \
        "$SERVER:$DEPLOY_DIR/frontend/dist/"
fi

echo "      Sincronizado."

# 4. Deploy ─────────────────────────────────────────────────────────────────
echo ""
echo "[4/4] Levantando contenedores en el servidor..."
ssh "$SERVER" "cd $DEPLOY_DIR && docker compose up -d --build"

echo ""
echo "======================================================"
echo "  Deploy completo!"
echo ""
echo "  Ver logs:  ssh $SERVER 'docker logs -f gym-backend'"
echo "  Salud:     curl http://$SERVER/api/actuator/health"
echo "======================================================"
