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

# 4. Copiar Dockerfile
cp Dockerfile "$DEPLOY_DIR/"

# 5. Copiar docker-compose.yml
cp docker-compose.yml "$DEPLOY_DIR/"

# 6. Copiar scripts de migración
mkdir -p "$DEPLOY_DIR/migrations"
if [ -d "migrations" ]; then
  cp migrations/*.sql "$DEPLOY_DIR/migrations/" 2>/dev/null || true
  cp migrations/README.md "$DEPLOY_DIR/migrations/" 2>/dev/null || true
fi

# 7. Copiar documentación
cp ANTES-DE-SUBIR.md "$DEPLOY_DIR/" 2>/dev/null || true
cp DEPLOY-RAPIDO.md "$DEPLOY_DIR/" 2>/dev/null || true
cp .env.example "$DEPLOY_DIR/" 2>/dev/null || true

# 8. Crear script de instalación en el servidor
cat > "$DEPLOY_DIR/install.sh" << 'EOF'
#!/bin/bash
# Script de instalación en el servidor

set -e

echo "🚀 INSTALANDO BACKEND EN PRODUCCIÓN..."
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "app.jar" ]; then
    echo "❌ Error: No se encuentra app.jar. Asegúrate de estar en el directorio correcto."
    exit 1
fi

# 1. Verificar .env
if [ ! -f "/opt/sigma/SigmaBack/.env" ]; then
    echo "⚠️  ATENCIÓN: No existe archivo .env"
    echo "📝 Copia .env.example a /opt/sigma/SigmaBack/.env y configúralo:"
    echo ""
    echo "   cp .env.example /opt/sigma/SigmaBack/.env"
    echo "   nano /opt/sigma/SigmaBack/.env"
    echo ""
    read -p "¿Ya configuraste el .env? (s/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        echo "❌ Instalación cancelada. Configura .env primero."
        exit 1
    fi
fi

# 2. Backup de la base de datos
echo "💾 Creando backup de la base de datos..."
mkdir -p /opt/sigma/SigmaBack/backups
docker exec gym-mysql mysqldump -u gymuser -pgympass \
  --routines --triggers --single-transaction --no-tablespaces \
  gymdb | gzip > /opt/sigma/SigmaBack/backups/gymdb_backup_$(date +%Y%m%d_%H%M%S).sql.gz

echo "✅ Backup creado"

# 3. Aplicar migraciones SQL (verificar primero si ya existen)
echo "🔄 Verificando migraciones de base de datos..."

# Verificar si la columna 'attended' ya existe
COLUMN_EXISTS=$(docker exec gym-mysql mysql -u gymuser -pgympass gymdb -N -s -e \
  "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='gymdb' AND TABLE_NAME='reservations' AND COLUMN_NAME='attended';" 2>/dev/null || echo "0")

if [ "$COLUMN_EXISTS" = "0" ]; then
    echo "� Aplicando migración: agregando columnas de asistencia..."
    docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb << 'SQL'
ALTER TABLE reservations 
  ADD COLUMN attended TINYINT(1) NOT NULL DEFAULT 0 
  COMMENT 'Indica si el usuario asistió';

ALTER TABLE reservations 
  ADD COLUMN attended_at DATETIME NULL 
  COMMENT 'Fecha y hora cuando se marcó la asistencia';

CREATE INDEX idx_reservations_attended_date 
  ON reservations(attended, date);
SQL
    echo "✅ Migraciones aplicadas correctamente"
else
    echo "ℹ️  Las columnas de asistencia ya existen, omitiendo migración"
fi

# 4. Copiar archivos al directorio de trabajo
echo "📦 Copiando archivos..."
cp -f Dockerfile /opt/sigma/SigmaBack/
cp -f docker-compose.yml /opt/sigma/SigmaBack/
cp -f app.jar /opt/sigma/SigmaBack/target/gym-0.0.1-SNAPSHOT.jar

# 5. Detener contenedor actual
echo "🛑 Deteniendo contenedor actual..."
docker-compose -f /opt/sigma/SigmaBack/docker-compose.yml stop backend || true
docker rm -f gym-backend || true

# 6. Reconstruir imagen
echo "🔨 Reconstruyendo imagen..."
cd /opt/sigma/SigmaBack
docker-compose build --no-cache backend

# 7. Iniciar backend
echo "🚀 Iniciando backend..."
docker-compose up -d backend

# 8. Esperar a que inicie
echo "⏳ Esperando a que el backend inicie..."
sleep 10

# 9. Verificar logs
echo "📋 Logs del backend:"
docker logs gym-backend --tail=30

echo ""
echo "✅ ¡DEPLOY COMPLETADO!"
echo ""
echo "🔍 Verificaciones recomendadas:"
echo "   1. docker ps | grep gym-backend"
echo "   2. docker logs gym-backend -f"
echo "   3. curl http://127.0.0.1:8080/api/auth/login"
echo ""
EOF

chmod +x "$DEPLOY_DIR/install.sh"

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
echo "2. En el servidor, descomprimir e instalar:"
echo "   ssh root@72.60.245.66"
echo "   cd /tmp"
echo "   tar -xzf $DEPLOY_FILE"
echo "   cd ${DEPLOY_DIR}"
echo "   ./install.sh"
echo ""
echo "3. Verificar que funciona:"
echo "   docker logs gym-backend --tail=50 -f"
echo ""
