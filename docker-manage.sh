#!/bin/bash

# =================================================================
# Script de Gestión Docker SigmaBack VPS
# =================================================================
# Comandos específicos para gestionar containers Docker
# =================================================================

VPS_HOST="72.60.245.66"
VPS_USER="root"
VPS_PROJECT_PATH="/opt/sigma/SigmaBack"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

show_help() {
    echo -e "${BLUE}"
    echo "==============================================="
    echo "🐳 GESTIÓN DOCKER SIGMABACK VPS"
    echo "==============================================="
    echo -e "${NC}"
    echo "Uso: $0 [COMANDO]"
    echo
    echo "Comandos disponibles:"
    echo -e "${GREEN}  status${NC}      - Ver estado de containers"
    echo -e "${GREEN}  logs${NC}        - Ver logs del container gym-backend"
    echo -e "${GREEN}  logs-db${NC}     - Ver logs del container mysql"
    echo -e "${GREEN}  stop${NC}        - Detener container gym-backend"
    echo -e "${GREEN}  start${NC}       - Iniciar container gym-backend"
    echo -e "${GREEN}  restart${NC}     - Reiniciar container gym-backend"
    echo -e "${GREEN}  rebuild${NC}     - Reconstruir y reiniciar container"
    echo -e "${GREEN}  ps${NC}          - Ver todos los containers"
    echo -e "${GREEN}  exec${NC}        - Entrar al container gym-backend"
    echo -e "${GREEN}  connect${NC}     - Conectar por SSH al VPS"
    echo
}

case "$1" in
    "status")
        echo -e "${BLUE}📊 Estado de containers Docker:${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            echo '🐳 Containers activos:'
            docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
            echo
            echo '📊 Estado del container gym-backend:'
            if docker ps | grep -q 'gym-backend'; then
                echo -e '${GREEN}✅ gym-backend: CORRIENDO${NC}'
                echo 'Uptime:' \$(docker ps --format '{{.Status}}' --filter 'name=gym-backend')
            else
                echo -e '${RED}❌ gym-backend: DETENIDO${NC}'
            fi
            echo
            echo '📊 Estado del container gym-mysql:'
            if docker ps | grep -q 'gym-mysql'; then
                echo -e '${GREEN}✅ gym-mysql: CORRIENDO${NC}'
                echo 'Uptime:' \$(docker ps --format '{{.Status}}' --filter 'name=gym-mysql')
            else
                echo -e '${RED}❌ gym-mysql: DETENIDO${NC}'
            fi
        "
        ;;
    "logs")
        echo -e "${BLUE}📋 Logs del container gym-backend (Ctrl+C para salir):${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "cd ${VPS_PROJECT_PATH} && docker logs gym-backend -f"
        ;;
    "logs-db")
        echo -e "${BLUE}📋 Logs del container gym-mysql (Ctrl+C para salir):${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "cd ${VPS_PROJECT_PATH} && docker logs gym-mysql -f"
        ;;
    "stop")
        echo -e "${YELLOW}🛑 Deteniendo container gym-backend...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            if docker ps | grep -q 'gym-backend'; then
                docker-compose stop gym-backend
                echo -e '${GREEN}✅ Container detenido${NC}'
            else
                echo -e '${YELLOW}⚠️  El container ya estaba detenido${NC}'
            fi
        "
        ;;
    "start")
        echo -e "${GREEN}🚀 Iniciando container gym-backend...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            if docker ps | grep -q 'gym-backend'; then
                echo -e '${YELLOW}⚠️  El container ya está corriendo${NC}'
            else
                docker-compose up -d gym-backend
                sleep 3
                if docker ps | grep -q 'gym-backend'; then
                    echo -e '${GREEN}✅ Container iniciado exitosamente${NC}'
                else
                    echo -e '${RED}❌ Error al iniciar el container${NC}'
                fi
            fi
        "
        ;;
    "restart")
        echo -e "${YELLOW}🔄 Reiniciando container gym-backend...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            docker-compose restart gym-backend
            sleep 3
            if docker ps | grep -q 'gym-backend'; then
                echo -e '${GREEN}✅ Container reiniciado exitosamente${NC}'
            else
                echo -e '${RED}❌ Error al reiniciar el container${NC}'
                docker logs gym-backend --tail=10
            fi
        "
        ;;
    "rebuild")
        echo -e "${BLUE}🔨 Reconstruyendo container gym-backend...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            echo 'Deteniendo container...'
            docker-compose stop gym-backend
            echo 'Reconstruyendo imagen...'
            docker-compose build gym-backend --no-cache
            echo 'Iniciando container...'
            docker-compose up -d gym-backend
            sleep 5
            if docker ps | grep -q 'gym-backend'; then
                echo -e '${GREEN}✅ Container reconstruido y iniciado exitosamente${NC}'
            else
                echo -e '${RED}❌ Error al reconstruir el container${NC}'
                docker logs gym-backend --tail=10
            fi
        "
        ;;
    "ps")
        echo -e "${BLUE}🔍 Todos los containers:${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "docker ps -a"
        ;;
    "exec")
        echo -e "${BLUE}🔧 Entrando al container gym-backend...${NC}"
        ssh -t "${VPS_USER}@${VPS_HOST}" "docker exec -it gym-backend /bin/bash"
        ;;
    "connect")
        echo -e "${BLUE}🔌 Conectando al VPS...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}"
        ;;
    *)
        show_help
        ;;
esac