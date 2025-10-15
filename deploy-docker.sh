#!/bin/bash

# =================================================================
# Script de Despliegue Docker para SigmaBack VPS
# =================================================================
# Version específica para despliegue con Docker Compose
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

echo -e "${BLUE}🚀 Despliegue Docker SigmaBack VPS${NC}"
echo -e "${YELLOW}⚠️  Este script requiere que ingreses la contraseña del VPS manualmente${NC}"
echo

# 1. Commit y push local si hay cambios
if [[ -n $(git status --porcelain) ]]; then
    echo -e "${YELLOW}📝 Hay cambios sin commit${NC}"
    git status --short
    echo
    read -p "$(echo -e "${YELLOW}❓ ¿Mensaje del commit? (Enter para mensaje automático): ${NC}")" commit_msg
    
    if [[ -z "$commit_msg" ]]; then
        commit_msg="Deploy Docker - Fix reservas + debug logs - $(date '+%Y-%m-%d %H:%M')"
    fi
    
    git add .
    git commit -m "$commit_msg"
    git push origin main
    echo -e "${GREEN}✅ Cambios subidos a GitHub${NC}"
fi

# 2. Mostrar comandos que se ejecutarán en el VPS
echo -e "${BLUE}🔧 Comandos Docker que se ejecutarán en el VPS:${NC}"
echo -e "${YELLOW}"
cat << 'EOF'
cd /opt/sigma/SigmaBack
git pull origin main
docker-compose stop backend
docker-compose build backend --no-cache
docker-compose up -d backend
docker ps
docker logs gym-backend --tail=20
EOF
echo -e "${NC}"

echo -e "${YELLOW}⚠️  Necesitarás ingresar la contraseña del VPS${NC}"
echo

# 3. Crear script temporal para ejecutar en el VPS con Docker
cat > /tmp/deploy_docker_commands.sh << 'EOF'
#!/bin/bash
set -e

echo "🔧 Desplegando SigmaBack con Docker..."

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

echo "🛑 Deteniendo container anterior..."
docker-compose stop backend || echo "Container no estaba corriendo"

echo "🔨 Construyendo nueva imagen Docker..."
docker-compose build backend --no-cache

echo "🚀 Iniciando container actualizado..."
docker-compose up -d backend

sleep 5

echo "🔍 Verificando estado del container..."
if docker ps | grep -q "gym-backend"; then
    echo "✅ Container desplegado exitosamente"
    echo "Estado de containers:"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo
    echo "📋 Últimos logs del container:"
    docker logs gym-backend --tail=10
    echo
    echo "Para ver logs en tiempo real: docker logs gym-backend -f"
else
    echo "❌ Error al iniciar el container"
    echo "Estado de containers:"
    docker ps -a | grep gym-backend
    echo "Logs del container:"
    docker logs gym-backend --tail=20
    exit 1
fi
EOF

# 4. Copiar y ejecutar script en el VPS
echo -e "${BLUE}📤 Copiando script al VPS...${NC}"
scp /tmp/deploy_docker_commands.sh "${VPS_USER}@${VPS_HOST}:/tmp/"

echo -e "${BLUE}🚀 Ejecutando despliegue Docker en el VPS...${NC}"
ssh "${VPS_USER}@${VPS_HOST}" "chmod +x /tmp/deploy_docker_commands.sh && /tmp/deploy_docker_commands.sh"

# 5. Limpiar archivo temporal
rm /tmp/deploy_docker_commands.sh

if [[ $? -eq 0 ]]; then
    echo
    echo -e "${GREEN}🎉 Despliegue Docker completado exitosamente${NC}"
    echo -e "${BLUE}📋 Información:${NC}"
    echo "• Servidor: ${VPS_HOST}"
    echo "• Aplicación: Docker container gym-backend"
    echo "• Logs: ssh ${VPS_USER}@${VPS_HOST} 'docker logs gym-backend -f'"
    echo "• Estado: ssh ${VPS_USER}@${VPS_HOST} 'docker ps'"
else
    echo -e "${RED}❌ Error durante el despliegue Docker${NC}"
    exit 1
fi