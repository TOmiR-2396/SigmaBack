#!/bin/bash

# =================================================================
# Script de Despliegue Manual para SigmaBack VPS
# =================================================================
# Version que requiere autenticación manual por contraseña
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

echo -e "${BLUE}🚀 Despliegue Manual SigmaBack VPS${NC}"
echo -e "${YELLOW}⚠️  Este script requiere que ingreses la contraseña del VPS manualmente${NC}"
echo

# 1. Commit y push local si hay cambios
if [[ -n $(git status --porcelain) ]]; then
    echo -e "${YELLOW}📝 Hay cambios sin commit${NC}"
    git status --short
    echo
    read -p "$(echo -e "${YELLOW}❓ ¿Mensaje del commit? (Enter para mensaje automático): ${NC}")" commit_msg
    
    if [[ -z "$commit_msg" ]]; then
        commit_msg="Deploy manual - $(date '+%Y-%m-%d %H:%M')"
    fi
    
    git add .
    git commit -m "$commit_msg"
    git push origin main
    echo -e "${GREEN}✅ Cambios subidos a GitHub${NC}"
fi

# 2. Mostrar comandos que se ejecutarán en el VPS
echo -e "${BLUE}🔧 Comandos que se ejecutarán en el VPS:${NC}"
echo -e "${YELLOW}"
cat << 'EOF'
cd /opt/sigma/SigmaBack
git pull origin main
pkill -f "gym-0.0.1-SNAPSHOT.jar" || true
mvn clean package -DskipTests
nohup java -jar -Dspring.profiles.active=prod target/gym-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
sleep 3
pgrep -f "gym-0.0.1-SNAPSHOT.jar" && echo "✅ Aplicación iniciada" || echo "❌ Error al iniciar"
EOF
echo -e "${NC}"

echo -e "${YELLOW}⚠️  Necesitarás ingresar la contraseña del VPS${NC}"
echo

# 3. Crear script temporal para ejecutar en el VPS
cat > /tmp/deploy_commands.sh << 'EOF'
#!/bin/bash
set -e

echo "🔧 Desplegando SigmaBack..."

# Verificar directorio
if [[ ! -d "/opt/sigma/SigmaBack" ]]; then
    echo "📁 Creando directorio y clonando repositorio..."
    mkdir -p /opt/sigma
    cd /opt/sigma
    git clone https://github.com/TOmiR-2396/SigmaBack.git
    cd SigmaBack
else
    echo "📁 Directorio encontrado"
    cd /opt/sigma/SigmaBack
fi

echo "📥 Descargando últimos cambios..."
git pull origin main

echo "🛑 Deteniendo aplicación anterior..."
pkill -f "gym-0.0.1-SNAPSHOT.jar" 2>/dev/null || echo "No hay aplicación corriendo"

sleep 2

echo "🔨 Compilando aplicación..."
mvn clean package -DskipTests -q

echo "🚀 Iniciando aplicación..."
nohup java -jar -Dspring.profiles.active=prod target/gym-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

sleep 5

echo "🔍 Verificando estado..."
if pgrep -f "gym-0.0.1-SNAPSHOT.jar" > /dev/null; then
    echo "✅ Aplicación desplegada exitosamente"
    echo "PID: $(pgrep -f gym-0.0.1-SNAPSHOT.jar)"
    echo "Para ver logs: tail -f /opt/sigma/SigmaBack/app.log"
else
    echo "❌ Error al iniciar la aplicación"
    echo "Últimos logs:"
    tail -20 app.log 2>/dev/null || echo "No hay logs disponibles"
    exit 1
fi
EOF

# 4. Copiar y ejecutar script en el VPS
echo -e "${BLUE}📤 Copiando script al VPS...${NC}"
scp /tmp/deploy_commands.sh "${VPS_USER}@${VPS_HOST}:/tmp/"

echo -e "${BLUE}🚀 Ejecutando despliegue en el VPS...${NC}"
ssh "${VPS_USER}@${VPS_HOST}" "chmod +x /tmp/deploy_commands.sh && /tmp/deploy_commands.sh"

# 5. Limpiar archivo temporal
rm /tmp/deploy_commands.sh

if [[ $? -eq 0 ]]; then
    echo
    echo -e "${GREEN}🎉 Despliegue completado exitosamente${NC}"
    echo -e "${BLUE}📋 Información:${NC}"
    echo "• Servidor: ${VPS_HOST}"
    echo "• Aplicación: /opt/sigma/SigmaBack"
    echo "• Logs: ssh ${VPS_USER}@${VPS_HOST} 'tail -f /opt/sigma/SigmaBack/app.log'"
else
    echo -e "${RED}❌ Error durante el despliegue${NC}"
    exit 1
fi