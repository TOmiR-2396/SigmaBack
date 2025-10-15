#!/bin/bash

# =================================================================
# Configurador de SSH para SigmaBack VPS  
# =================================================================
# Este script ayuda a configurar la conexión SSH al VPS
# =================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
log_success() { echo -e "${GREEN}✅ $1${NC}"; }
log_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
log_error() { echo -e "${RED}❌ $1${NC}"; }

echo -e "${BLUE}"
echo "=================================="
echo "🔐 CONFIGURACIÓN SSH SIGMABACK VPS"
echo "=================================="
echo -e "${NC}"

# Información del VPS por defecto
VPS_HOST="srv1042314"
VPS_USER="root"

echo "Configuración actual:"
echo "• Hostname: ${VPS_HOST}"
echo "• Usuario: ${VPS_USER}"
echo

# Verificar si tenemos la IP del servidor
log_info "Verificando resolución de DNS..."
if nslookup "${VPS_HOST}" >/dev/null 2>&1; then
    VPS_IP=$(nslookup "${VPS_HOST}" | grep -A1 "Name:" | tail -n1 | awk '{print $2}' | head -n1)
    log_success "DNS resuelve a: ${VPS_IP}"
else
    log_warning "No se puede resolver DNS para ${VPS_HOST}"
    echo
    read -p "$(echo -e "${YELLOW}💡 Ingresa la IP del servidor manualmente: ${NC}")" manual_ip
    if [[ -n "$manual_ip" ]]; then
        VPS_IP="$manual_ip"
        log_info "Usando IP manual: ${VPS_IP}"
    else
        log_error "IP requerida para continuar"
        exit 1
    fi
fi

# Probar conectividad básica
log_info "Probando conectividad básica..."
if ping -c 1 "${VPS_IP}" >/dev/null 2>&1; then
    log_success "El servidor responde a ping"
else
    log_warning "El servidor no responde a ping (podría estar bloqueado)"
fi

# Probar conexión SSH
log_info "Probando conexión SSH..."
if ssh -o ConnectTimeout=10 -o BatchMode=yes "${VPS_USER}@${VPS_IP}" exit 2>/dev/null; then
    log_success "✨ Conexión SSH exitosa!"
    
    # Verificar si el directorio del proyecto existe
    log_info "Verificando directorio del proyecto..."
    if ssh "${VPS_USER}@${VPS_IP}" "test -d /opt/sigma/SigmaBack" 2>/dev/null; then
        log_success "Directorio del proyecto encontrado: /opt/sigma/SigmaBack"
    else
        log_warning "Directorio del proyecto no encontrado"
        if read -p "$(echo -e "${YELLOW}❓ ¿Quieres crearlo? (y/n): ${NC}")" -n 1 -r && [[ $REPLY =~ ^[Yy]$ ]]; then
            echo
            ssh "${VPS_USER}@${VPS_IP}" "mkdir -p /opt/sigma && cd /opt/sigma && git clone https://github.com/TOmiR-2396/SigmaBack.git" 
            log_success "Directorio creado y repositorio clonado"
        fi
    fi
    
    echo
    log_success "🎉 SSH configurado correctamente"
    log_info "Puedes usar los scripts de deploy normalmente"
    
else
    log_error "No se puede conectar por SSH"
    echo
    log_warning "Posibles soluciones:"
    log_warning "1. Verifica que tengas una clave SSH configurada:"
    log_warning "   ssh-keygen -t rsa -b 4096"
    log_warning "   ssh-copy-id ${VPS_USER}@${VPS_IP}"
    echo
    log_warning "2. O conecta manualmente primero:"
    log_warning "   ssh ${VPS_USER}@${VPS_IP}"
    echo
    log_warning "3. Si tienes problemas de DNS, actualiza los scripts con la IP:"
    echo "   Cambiar VPS_HOST=\"${VPS_HOST}\" por VPS_HOST=\"${VPS_IP}\""
fi

# Configurar archivo SSH config si es necesario
if [[ -n "$VPS_IP" && "$VPS_IP" != "$VPS_HOST" ]]; then
    log_info "¿Quieres configurar un alias SSH para facilitar la conexión?"
    if read -p "$(echo -e "${YELLOW}❓ Crear alias '${VPS_HOST}' para ${VPS_IP}? (y/n): ${NC}")" -n 1 -r && [[ $REPLY =~ ^[Yy]$ ]]; then
        echo
        mkdir -p ~/.ssh
        if ! grep -q "Host ${VPS_HOST}" ~/.ssh/config 2>/dev/null; then
            cat >> ~/.ssh/config << EOF

# SigmaBack VPS
Host ${VPS_HOST}
    HostName ${VPS_IP}
    User ${VPS_USER}
    IdentityFile ~/.ssh/id_rsa

EOF
            log_success "Alias SSH creado en ~/.ssh/config"
        else
            log_warning "El alias ya existe en ~/.ssh/config"
        fi
    fi
fi

echo
echo -e "${BLUE}=================================${NC}"
echo -e "${GREEN}🔧 Configuración completada${NC}"
echo -e "${BLUE}=================================${NC}"