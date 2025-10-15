#!/bin/bash

# =================================================================
# Script de Verificación Estado SigmaBack VPS
# =================================================================

VPS_HOST="72.60.245.66"
VPS_USER="root"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "=================================="
echo "📊 ESTADO SIGMABACK VPS"
echo "=================================="
echo -e "${NC}"

echo -e "${YELLOW}⚠️  Ingresa la contraseña del VPS cuando se solicite${NC}"
echo

# Crear script de verificación temporal
cat > /tmp/check_status.sh << 'EOF'
#!/bin/bash

echo -e "\033[0;34m🔍 Verificando estado de SigmaBack...\033[0m"
echo

# Verificar directorio
if [[ -d "/opt/sigma/SigmaBack" ]]; then
    echo -e "\033[0;32m✅ Directorio del proyecto: OK\033[0m"
    cd /opt/sigma/SigmaBack
    
    # Verificar Git
    if [[ -d ".git" ]]; then
        echo -e "\033[0;32m✅ Repositorio Git: OK\033[0m"
        echo "   Rama actual: $(git branch --show-current)"
        echo "   Último commit: $(git log -1 --oneline)"
    else
        echo -e "\033[0;31m❌ No es un repositorio Git\033[0m"
    fi
    
    # Verificar JAR
    if [[ -f "target/gym-0.0.1-SNAPSHOT.jar" ]]; then
        echo -e "\033[0;32m✅ JAR compilado: OK\033[0m"
        echo "   Tamaño: $(du -h target/gym-0.0.1-SNAPSHOT.jar | cut -f1)"
        echo "   Fecha: $(date -r target/gym-0.0.1-SNAPSHOT.jar)"
    else
        echo -e "\033[0;31m❌ JAR no encontrado\033[0m"
    fi
    
    # Verificar proceso
    if pgrep -f "gym-0.0.1-SNAPSHOT.jar" > /dev/null; then
        echo -e "\033[0;32m✅ Aplicación: CORRIENDO\033[0m"
        echo "   PID: $(pgrep -f gym-0.0.1-SNAPSHOT.jar)"
        echo "   Memoria: $(ps -p $(pgrep -f gym-0.0.1-SNAPSHOT.jar) -o %mem --no-headers)%"
        echo "   CPU: $(ps -p $(pgrep -f gym-0.0.1-SNAPSHOT.jar) -o %cpu --no-headers)%"
    else
        echo -e "\033[0;31m❌ Aplicación: DETENIDA\033[0m"
    fi
    
    # Verificar logs
    if [[ -f "app.log" ]]; then
        echo -e "\033[0;32m✅ Logs disponibles\033[0m"
        echo "   Tamaño: $(du -h app.log | cut -f1)"
        echo "   Últimas 3 líneas:"
        tail -3 app.log | sed 's/^/   /'
    else
        echo -e "\033[0;33m⚠️  No hay logs disponibles\033[0m"
    fi
    
else
    echo -e "\033[0;31m❌ Directorio del proyecto no encontrado: /opt/sigma/SigmaBack\033[0m"
fi

echo
echo -e "\033[0;34m💾 Uso de disco:\033[0m"
df -h / | grep -v Filesystem

echo
echo -e "\033[0;34m🖥️  Memoria:\033[0m"
free -h

echo
echo -e "\033[0;34m⚡ Procesos Java:\033[0m"
ps aux | grep java | grep -v grep || echo "   No hay procesos Java corriendo"
EOF

# Ejecutar verificación en el VPS
ssh "${VPS_USER}@${VPS_HOST}" "bash -s" < /tmp/check_status.sh

# Limpiar
rm /tmp/check_status.sh

echo
echo -e "${BLUE}=================================${NC}"
echo -e "${GREEN}📋 Verificación completada${NC}"
echo -e "${BLUE}=================================${NC}"