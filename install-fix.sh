#!/bin/bash
# Script de corrección rápida - ejecutar en el servidor después de descomprimir

set -e

echo "🔧 INSTALACIÓN CORREGIDA - SOLO BACKEND"
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -f "app.jar" ]; then
    echo "❌ Error: No se encuentra app.jar"
    exit 1
fi

# 1. Copiar archivos
echo "📦 Copiando archivos..."
mkdir -p /opt/sigma/SigmaBack/target
cp -f app.jar /opt/sigma/SigmaBack/target/gym-0.0.1-SNAPSHOT.jar
cp -f Dockerfile /opt/sigma/SigmaBack/

# 2. Detener y eliminar contenedor backend
echo "🛑 Deteniendo contenedor backend..."
docker stop gym-backend 2>/dev/null || true
docker rm gym-backend 2>/dev/null || true

# 3. Reconstruir imagen (sin compose)
echo "🔨 Reconstruyendo imagen backend..."
cd /opt/sigma/SigmaBack
docker build -t sigmaback-backend .

# 4. Levantar SOLO el backend (sin compose para evitar recrear MySQL)
echo "🚀 Iniciando backend..."
docker run -d \
  --name gym-backend \
  --network sigmaback_gym-network \
  -p 127.0.0.1:8080:8080 \
  --env-file /opt/sigma/SigmaBack/.env \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://gym-mysql:3306/gymdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME="gymuser" \
  -e SPRING_DATASOURCE_PASSWORD="gympass" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  -e PORT="8080" \
  -e JWT_SECRET="Z3xO1raNs7SJxocFht4wpds4AYoo7Q9QbRMaOST6H9dtq7vzKrh/+eoB6c036Du6Mr2tlv0w2joGUetlHu6Hxg==" \
  -e JWT_EXPIRATION="86400000" \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  --restart unless-stopped \
  sigmaback-backend

# 5. Esperar a que inicie
echo "⏳ Esperando 10 segundos..."
sleep 10

# 6. Verificar logs
echo ""
echo "📋 Logs del backend:"
docker logs gym-backend --tail=30

echo ""
echo "✅ ¡DEPLOY COMPLETADO!"
echo ""
echo "🔍 Verificar estado:"
echo "   docker ps | grep gym"
echo "   docker logs gym-backend -f"
echo ""
