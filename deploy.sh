#!/bin/bash
# Script para crear paquete de deployment del backend

set -e  # Exit on error

echo "🚀 GENERANDO PAQUETE DE DEPLOY..."
echo ""

# 1. Compilar el proyecto
echo "📦 Compilando proyecto..."
./mvnw clean package -DskipTests

# 2. Crear directorio temporal
DEPLOY_DIR="deploy_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$DEPLOY_DIR"

echo "📋 Copiando archivos necesarios..."

# 3. Copiar el JAR compilado
cp target/gym-*.jar "$DEPLOY_DIR/app.jar"

# 4. Copiar Dockerfile (build local) y Dockerfile.deploy (usa app.jar generado)
cp Dockerfile "$DEPLOY_DIR/"
cp Dockerfile.deploy "$DEPLOY_DIR/"

# 5. (omitido) No incluimos docker-compose ni migraciones ni docs para backend-only

# 7.1 Incluir script de instalación backend-only (no toca MySQL)
if [ -f "install-fix.sh" ]; then
  cp install-fix.sh "$DEPLOY_DIR/"
  chmod +x "$DEPLOY_DIR/install-fix.sh"
fi

# 8. (omitido) No generamos install.sh; usaremos install-fix.sh (backend-only)

# 9. Crear archivo de verificación con hash del JAR
echo "📝 Generando checksums..."
cd "$DEPLOY_DIR"
sha256sum app.jar > app.jar.sha256
cd ..

# 10. Comprimir todo
echo "🗜️  Comprimiendo archivos..."
DEPLOY_FILE="${DEPLOY_DIR}.tar.gz"
tar -czf "$DEPLOY_FILE" "$DEPLOY_DIR"

# 11. Limpiar directorio temporal
rm -rf "$DEPLOY_DIR"

echo ""
echo "✅ ¡PAQUETE DE DEPLOY CREADO!"
echo ""
echo "📦 Archivo generado: $DEPLOY_FILE"
echo "📊 Tamaño: $(du -h "$DEPLOY_FILE" | cut -f1)"
echo ""
echo "📤 PASOS PARA SUBIR A PRODUCCIÓN:"
echo ""
echo "1. Copiar el archivo al servidor:"
echo "   scp $DEPLOY_FILE root@72.60.245.66:/tmp/"
echo ""
echo "2. En el servidor, descomprimir e instalar (backend-only):"
echo "   ssh root@72.60.245.66"
echo "   cd /tmp"
echo "   tar -xzf $DEPLOY_FILE"
echo "   cd ${DEPLOY_DIR}"
echo "   ./install-fix.sh"
echo ""
echo "3. Verificar que funciona:"
echo "   docker logs gym-backend --tail=50 -f"
echo ""
