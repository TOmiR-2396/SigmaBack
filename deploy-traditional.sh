#!/bin/bash

# =================================================================
# Script de Despliegue Tradicional (tar.gz) para SigmaBack VPS
# =================================================================
# Basado en tu método anterior: tar -xvzf sigma-backend.tar.gz
# =================================================================

# Configuración
VPS_HOST="72.60.245.66"
VPS_USER="root"
VPS_PROJECT_PATH="/opt/sigma/SigmaBack"

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🚀 Despliegue Tradicional SigmaBack (tar.gz)${NC}"
echo -e "${YELLOW}⚠️  Método tradicional con compresión y descompresión${NC}"
echo

# 1. Compilar y empaquetar localmente
echo -e "${BLUE}🔨 Compilando localmente...${NC}"
./mvnw clean package -DskipTests

if [[ ! -f "target/gym-0.0.1-SNAPSHOT.jar" ]]; then
    echo -e "${RED}❌ Error: No se pudo compilar el JAR${NC}"
    exit 1
fi

# 2. Crear el tar.gz con todo lo necesario
echo -e "${BLUE}📦 Creando sigma-backend.tar.gz...${NC}"
mkdir -p temp-deploy
cp target/gym-0.0.1-SNAPSHOT.jar temp-deploy/
cp src/main/resources/application*.yml temp-deploy/ 2>/dev/null || true

# Crear script de inicio
cat > temp-deploy/start.sh << 'EOF'
#!/bin/bash
echo "🚀 Iniciando SigmaBack..."

# Detener proceso anterior
pkill -f "gym-0.0.1-SNAPSHOT.jar" 2>/dev/null || echo "No hay proceso anterior"

sleep 2

# Iniciar aplicación
nohup java -jar -Dspring.profiles.active=prod gym-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

sleep 5

# Verificar que inició
if pgrep -f "gym-0.0.1-SNAPSHOT.jar" > /dev/null; then
    echo "✅ Aplicación iniciada exitosamente"
    echo "PID: $(pgrep -f gym-0.0.1-SNAPSHOT.jar)"
    echo "Para ver logs: tail -f app.log"
else
    echo "❌ Error al iniciar la aplicación"
    echo "Últimos logs:"
    tail -20 app.log 2>/dev/null || echo "No hay logs"
    exit 1
fi
EOF

chmod +x temp-deploy/start.sh

tar -czf sigma-backend.tar.gz -C temp-deploy .
rm -rf temp-deploy

echo -e "${GREEN}✅ sigma-backend.tar.gz creado ($(du -h sigma-backend.tar.gz | cut -f1))${NC}"

# 3. Commit si hay cambios
if [[ -n $(git status --porcelain) ]]; then
    echo -e "${YELLOW}📝 Hay cambios sin commit${NC}"
    read -p "$(echo -e "${YELLOW}❓ ¿Mensaje del commit? (Enter para automático): ${NC}")" commit_msg
    
    if [[ -z "$commit_msg" ]]; then
        commit_msg="Deploy tradicional tar.gz - $(date '+%Y-%m-%d %H:%M')"
    fi
    
    git add .
    git commit -m "$commit_msg"
    git push origin main
    echo -e "${GREEN}✅ Cambios subidos a GitHub${NC}"
fi

# 4. Subir y desplegar en VPS
echo -e "${BLUE}📤 Subiendo sigma-backend.tar.gz al VPS...${NC}"
scp sigma-backend.tar.gz "${VPS_USER}@${VPS_HOST}:/opt/sigma/"

echo -e "${BLUE}🚀 Desplegando en VPS...${NC}"
ssh "${VPS_USER}@${VPS_HOST}" << 'ENDSSH'
set -e

echo "🔧 Desplegando SigmaBack tradicional..."

cd /opt/sigma

# Hacer backup si existe
if [[ -d "SigmaBack-current" ]]; then
    echo "📋 Haciendo backup..."
    mv SigmaBack-current SigmaBack-backup-$(date +%Y%m%d_%H%M%S)
fi

# Crear directorio y extraer
mkdir -p SigmaBack-current
cd SigmaBack-current

echo "📦 Extrayendo sigma-backend.tar.gz..."
tar -xzf ../sigma-backend.tar.gz

echo "🚀 Iniciando aplicación..."
./start.sh

echo "✅ Despliegue tradicional completado"
ENDSSH

# 5. Limpiar archivos temporales
rm sigma-backend.tar.gz

if [[ $? -eq 0 ]]; then
    echo
    echo -e "${GREEN}🎉 Despliegue tradicional completado exitosamente${NC}"
    echo -e "${BLUE}📋 Información:${NC}"
    echo "• Servidor: ${VPS_HOST}"
    echo "• Aplicación: /opt/sigma/SigmaBack-current"
    echo "• Logs: ssh ${VPS_USER}@${VPS_HOST} 'tail -f /opt/sigma/SigmaBack-current/app.log'"
    echo "• Método: Tradicional tar.gz (como antes)"
else
    echo -e "${RED}❌ Error durante el despliegue tradicional${NC}"
    exit 1
fi