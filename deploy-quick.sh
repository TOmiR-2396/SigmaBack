#!/bin/bash

# =================================================================
# Script de Despliegue Rápido para SigmaBack VPS
# =================================================================
# Versión simplificada para despliegues rápidos sin confirmaciones
# =================================================================

set -e

# Configuración
VPS_HOST="srv1042314"
VPS_USER="root"
VPS_PROJECT_PATH="/opt/sigma/SigmaBack"

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Despliegue Rápido SigmaBack${NC}"

# 1. Commit y push automático
if [[ -n $(git status --porcelain) ]]; then
    echo "📝 Haciendo commit automático..."
    git add .
    git commit -m "Quick deploy - $(date '+%Y-%m-%d %H:%M')"
    git push origin main
    echo -e "${GREEN}✅ Cambios subidos${NC}"
fi

# 2. Desplegar en VPS
echo "🔧 Desplegando en VPS..."
ssh "${VPS_USER}@${VPS_HOST}" << 'ENDSSH'
cd /opt/sigma/SigmaBack
git pull origin main
pkill -f "gym-0.0.1-SNAPSHOT.jar" 2>/dev/null || true
mvn clean package -DskipTests -q
nohup java -jar -Dspring.profiles.active=prod target/gym-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
sleep 3
if pgrep -f "gym-0.0.1-SNAPSHOT.jar" > /dev/null; then
    echo "✅ Aplicación desplegada exitosamente"
else
    echo "❌ Error en el despliegue"
    tail -10 app.log
    exit 1
fi
ENDSSH

echo -e "${GREEN}🎉 Despliegue completado${NC}"
