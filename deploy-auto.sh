#!/bin/bash
# Script para deploy automático: genera paquete, lo sube y lo instala en el servidor

set -e

SERVER="root@72.60.245.66"
DEPLOY_FILE=$(ls -t deploy_*.tar.gz 2>/dev/null | head -1)

if [ -z "$DEPLOY_FILE" ]; then
    echo "❌ No se encontró ningún paquete de deploy."
    echo "Ejecuta primero: ./deploy.sh"
    exit 1
fi

echo "🚀 DEPLOY AUTOMÁTICO A PRODUCCIÓN"
echo ""
echo "📦 Paquete: $DEPLOY_FILE"
echo "🖥️  Servidor: $SERVER"
echo ""

read -p "¿Continuar con el deploy? (s/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Ss]$ ]]; then
    echo "❌ Deploy cancelado"
    exit 1
fi

# 1. Copiar al servidor
echo ""
echo "📤 Copiando archivo al servidor..."
scp "$DEPLOY_FILE" "$SERVER:/tmp/"

# 2. Extraer nombre del directorio
DEPLOY_DIR="${DEPLOY_FILE%.tar.gz}"

# 3. Ejecutar instalación remota
echo ""
echo "🔧 Ejecutando instalación en el servidor..."
ssh "$SERVER" << EOF
set -e
cd /tmp
echo "📦 Descomprimiendo..."
tar -xzf $DEPLOY_FILE
cd $DEPLOY_DIR
echo ""
echo "🚀 Iniciando instalación..."
./install.sh
EOF

echo ""
echo "✅ ¡DEPLOY COMPLETADO!"
echo ""
echo "🔍 Para ver los logs en tiempo real:"
echo "   ssh $SERVER 'docker logs gym-backend -f'"
echo ""
