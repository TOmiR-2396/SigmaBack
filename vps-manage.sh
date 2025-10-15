#!/bin/bash

# =================================================================
# Script de Gestión SigmaBack VPS
# =================================================================
# Comandos útiles para gestionar la aplicación en el VPS
# =================================================================

VPS_HOST="srv1042314"
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
    echo "🛠️  GESTIÓN SIGMABACK VPS"
    echo "==============================================="
    echo -e "${NC}"
    echo "Uso: $0 [COMANDO]"
    echo
    echo "Comandos disponibles:"
    echo -e "${GREEN}  status${NC}     - Ver estado de la aplicación"
    echo -e "${GREEN}  logs${NC}       - Ver logs en tiempo real"
    echo -e "${GREEN}  stop${NC}       - Detener aplicación"
    echo -e "${GREEN}  start${NC}      - Iniciar aplicación"
    echo -e "${GREEN}  restart${NC}    - Reiniciar aplicación"
    echo -e "${GREEN}  ps${NC}         - Ver procesos Java"
    echo -e "${GREEN}  disk${NC}       - Ver uso de disco"
    echo -e "${GREEN}  backup${NC}     - Hacer backup del JAR"
    echo -e "${GREEN}  connect${NC}    - Conectar por SSH al VPS"
    echo
}

case "$1" in
    "status")
        echo -e "${BLUE}📊 Estado de la aplicación:${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            if pgrep -f 'gym-0.0.1-SNAPSHOT.jar' > /dev/null; then
                echo -e '${GREEN}✅ Aplicación CORRIENDO${NC}'
                echo 'PID:' \$(pgrep -f gym-0.0.1-SNAPSHOT.jar)
                echo 'Memoria:' \$(ps -p \$(pgrep -f gym-0.0.1-SNAPSHOT.jar) -o %mem --no-headers) '%'
                echo 'CPU:' \$(ps -p \$(pgrep -f gym-0.0.1-SNAPSHOT.jar) -o %cpu --no-headers) '%'
            else
                echo -e '${RED}❌ Aplicación DETENIDA${NC}'
            fi
        "
        ;;
    "logs")
        echo -e "${BLUE}📋 Logs en tiempo real (Ctrl+C para salir):${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "cd ${VPS_PROJECT_PATH} && tail -f app.log"
        ;;
    "stop")
        echo -e "${YELLOW}🛑 Deteniendo aplicación...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            if pgrep -f 'gym-0.0.1-SNAPSHOT.jar' > /dev/null; then
                pkill -f 'gym-0.0.1-SNAPSHOT.jar'
                echo -e '${GREEN}✅ Aplicación detenida${NC}'
            else
                echo -e '${YELLOW}⚠️  La aplicación ya estaba detenida${NC}'
            fi
        "
        ;;
    "start")
        echo -e "${GREEN}🚀 Iniciando aplicación...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            if pgrep -f 'gym-0.0.1-SNAPSHOT.jar' > /dev/null; then
                echo -e '${YELLOW}⚠️  La aplicación ya está corriendo${NC}'
            else
                nohup java -jar -Dspring.profiles.active=prod target/gym-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
                sleep 3
                if pgrep -f 'gym-0.0.1-SNAPSHOT.jar' > /dev/null; then
                    echo -e '${GREEN}✅ Aplicación iniciada exitosamente${NC}'
                else
                    echo -e '${RED}❌ Error al iniciar la aplicación${NC}'
                fi
            fi
        "
        ;;
    "restart")
        echo -e "${YELLOW}🔄 Reiniciando aplicación...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            pkill -f 'gym-0.0.1-SNAPSHOT.jar' 2>/dev/null || true
            sleep 2
            nohup java -jar -Dspring.profiles.active=prod target/gym-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
            sleep 3
            if pgrep -f 'gym-0.0.1-SNAPSHOT.jar' > /dev/null; then
                echo -e '${GREEN}✅ Aplicación reiniciada exitosamente${NC}'
            else
                echo -e '${RED}❌ Error al reiniciar la aplicación${NC}'
            fi
        "
        ;;
    "ps")
        echo -e "${BLUE}🔍 Procesos Java en el VPS:${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "ps aux | grep java | grep -v grep"
        ;;
    "disk")
        echo -e "${BLUE}💾 Uso de disco en ${VPS_PROJECT_PATH}:${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            echo 'Directorio del proyecto:'
            du -sh .
            echo
            echo 'Archivos más grandes:'
            find . -type f -exec du -h {} + | sort -hr | head -10
        "
        ;;
    "backup")
        echo -e "${BLUE}📦 Creando backup del JAR...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}" "
            cd ${VPS_PROJECT_PATH}
            if [[ -f 'target/gym-0.0.1-SNAPSHOT.jar' ]]; then
                cp target/gym-0.0.1-SNAPSHOT.jar target/gym-0.0.1-SNAPSHOT.jar.backup.\$(date +%Y%m%d_%H%M%S)
                echo -e '${GREEN}✅ Backup creado exitosamente${NC}'
                echo 'Backups disponibles:'
                ls -la target/*.backup.* 2>/dev/null || echo 'No hay backups previos'
            else
                echo -e '${RED}❌ No se encontró el JAR para hacer backup${NC}'
            fi
        "
        ;;
    "connect")
        echo -e "${BLUE}🔌 Conectando al VPS...${NC}"
        ssh "${VPS_USER}@${VPS_HOST}"
        ;;
    *)
        show_help
        ;;
esac