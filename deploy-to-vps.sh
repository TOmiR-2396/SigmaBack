#!/bin/bash

# =================================================================
# Script de Despliegue Automático para SigmaBack VPS
# =================================================================
# Este script hace commit de los cambios locales y los despliega 
# automáticamente en el servidor VPS
# =================================================================

set -e  # Terminar si cualquier comando falla

# Configuración del VPS
VPS_HOST="srv1042314"
VPS_USER="root"
VPS_PROJECT_PATH="/opt/sigma/SigmaBack"
LOCAL_PROJECT_PATH="/Users/santiago/Proyectos/SigmaBack"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para log con colores
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Función para confirmar acción
confirm() {
    read -p "$(echo -e "${YELLOW}❓ $1 (y/n): ${NC}")" -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]]
}

echo -e "${BLUE}"
echo "======================================"
echo "🚀 DESPLIEGUE AUTOMÁTICO SIGMABACK VPS"
echo "======================================"
echo -e "${NC}"

# 1. Verificar estado local del repositorio
log_info "Verificando estado del repositorio local..."
cd "$LOCAL_PROJECT_PATH"

# Verificar si hay cambios sin commit
if [[ -n $(git status --porcelain) ]]; then
    log_warning "Hay cambios sin commit en el repositorio local"
    git status --short
    
    if confirm "¿Quieres hacer commit de estos cambios?"; then
        echo
        read -p "$(echo -e "${YELLOW}📝 Mensaje del commit: ${NC}")" commit_message
        
        if [[ -z "$commit_message" ]]; then
            commit_message="Fix: Reservas después de cancelación - $(date '+%Y-%m-%d %H:%M')"
        fi
        
        log_info "Haciendo commit de los cambios..."
        git add .
        git commit -m "$commit_message"
        log_success "Commit realizado: $commit_message"
    else
        log_error "Abortando despliegue. Hay cambios sin commit."
        exit 1
    fi
else
    log_success "El repositorio local está limpio"
fi

# 2. Push a origin main
log_info "Subiendo cambios a GitHub..."
git push origin main
log_success "Cambios subidos a GitHub"

# 3. Compilar localmente para verificar
log_info "Compilando proyecto localmente para verificar..."
./mvnw clean compile -q
log_success "Compilación local exitosa"

# 4. Conectar al VPS y desplegar
log_info "Conectando al VPS y desplegando..."

ssh "${VPS_USER}@${VPS_HOST}" << 'ENDSSH'
set -e

# Colores para SSH
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[VPS] ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}[VPS] ✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}[VPS] ⚠️  $1${NC}"; }
log_error() { echo -e "${RED}[VPS] ❌ $1${NC}"; }

echo -e "${BLUE}==============================${NC}"
echo -e "${BLUE}🔧 DESPLIEGUE EN VPS INICIADO${NC}"
echo -e "${BLUE}==============================${NC}"

# Ir al directorio del proyecto
cd /opt/sigma/SigmaBack

# Verificar si el directorio existe
if [[ ! -d "/opt/sigma/SigmaBack" ]]; then
    log_error "El directorio /opt/sigma/SigmaBack no existe"
    exit 1
fi

log_info "Directorio actual: $(pwd)"

# Hacer backup del JAR actual si existe
if [[ -f "target/gym-0.0.1-SNAPSHOT.jar" ]]; then
    log_info "Haciendo backup del JAR actual..."
    cp target/gym-0.0.1-SNAPSHOT.jar target/gym-0.0.1-SNAPSHOT.jar.backup.$(date +%Y%m%d_%H%M%S)
    log_success "Backup creado"
fi

# Pull de los últimos cambios
log_info "Descargando últimos cambios desde GitHub..."
git fetch origin
git reset --hard origin/main
log_success "Código actualizado desde GitHub"

# Verificar que Java está disponible
if ! command -v java &> /dev/null; then
    log_error "Java no está disponible en el PATH"
    exit 1
fi

log_info "Versión de Java: $(java -version 2>&1 | head -n 1)"

# Verificar que Maven está disponible
if ! command -v mvn &> /dev/null; then
    log_error "Maven no está disponible en el PATH"
    exit 1
fi

# Compilar el proyecto
log_info "Compilando proyecto en VPS..."
mvn clean compile -q
log_success "Compilación exitosa"

# Ejecutar tests (si existen)
log_info "Ejecutando tests..."
mvn test -q || log_warning "No hay tests o fallaron algunos tests"

# Generar JAR
log_info "Generando JAR ejecutable..."
mvn package -DskipTests -q
log_success "JAR generado exitosamente"

# Verificar que el JAR se creó
if [[ ! -f "target/gym-0.0.1-SNAPSHOT.jar" ]]; then
    log_error "No se pudo generar el JAR"
    exit 1
fi

log_success "JAR disponible en: target/gym-0.0.1-SNAPSHOT.jar"
log_info "Tamaño del JAR: $(du -h target/gym-0.0.1-SNAPSHOT.jar | cut -f1)"

# Verificar si la aplicación está corriendo y detenerla
log_info "Verificando procesos Java en ejecución..."
if pgrep -f "gym-0.0.1-SNAPSHOT.jar" > /dev/null; then
    log_warning "Aplicación corriendo, deteniéndola..."
    pkill -f "gym-0.0.1-SNAPSHOT.jar" || true
    sleep 3
    log_success "Aplicación detenida"
else
    log_info "No hay instancias previas corriendo"
fi

# Iniciar la aplicación en background
log_info "Iniciando aplicación con perfil de producción..."
nohup java -jar -Dspring.profiles.active=prod target/gym-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Esperar un momento para verificar que inició correctamente
sleep 5

# Verificar que la aplicación está corriendo
if pgrep -f "gym-0.0.1-SNAPSHOT.jar" > /dev/null; then
    log_success "✨ Aplicación desplegada y corriendo exitosamente"
    log_info "PID del proceso: $(pgrep -f gym-0.0.1-SNAPSHOT.jar)"
    log_info "Para ver logs: tail -f /opt/sigma/SigmaBack/app.log"
    log_info "Para detener: pkill -f gym-0.0.1-SNAPSHOT.jar"
else
    log_error "La aplicación no pudo iniciar correctamente"
    log_info "Últimas líneas del log:"
    tail -20 app.log 2>/dev/null || echo "No hay log disponible"
    exit 1
fi

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}🎉 DESPLIEGUE COMPLETADO EXITOSAMENTE${NC}"
echo -e "${GREEN}================================${NC}"

ENDSSH

# Verificar que el SSH fue exitoso
if [[ $? -eq 0 ]]; then
    log_success "🎉 Despliegue completado exitosamente en el VPS"
    echo
    log_info "📋 INFORMACIÓN DEL DESPLIEGUE:"
    log_info "• Servidor: ${VPS_HOST}"
    log_info "• Ruta: ${VPS_PROJECT_PATH}"
    log_info "• Perfil: prod (MySQL)"
    log_info "• Para logs: ssh ${VPS_USER}@${VPS_HOST} 'tail -f ${VPS_PROJECT_PATH}/app.log'"
    log_info "• Para detener: ssh ${VPS_USER}@${VPS_HOST} 'pkill -f gym-0.0.1-SNAPSHOT.jar'"
    echo
    log_success "🚀 Tu aplicación debería estar disponible en el servidor"
else
    log_error "Error durante el despliegue en el VPS"
    exit 1
fi