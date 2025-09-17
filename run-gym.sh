#!/bin/bash

# Script para ejecutar la aplicación Gym con las credenciales correctas
echo "🚀 Iniciando aplicación Gym..."
echo "📊 Verificando MySQL..."

# Verificar que MySQL esté corriendo
if ! docker ps | grep -q "sigmaback-mysql-1.*healthy"; then
    echo "⚠️  MySQL no está corriendo. Iniciando..."
    docker start sigmaback-mysql-1
    echo "⏳ Esperando que MySQL esté listo..."
    sleep 10
fi

echo "✅ MySQL está corriendo"
echo "🏃‍♂️ Ejecutando aplicación Spring Boot..."

# Cambiar al directorio del proyecto
cd /Users/santiago/Proyectos/SigmaBack

# Ejecutar con las credenciales correctas
DB_HOST=localhost \
DB_PORT=3307 \
DB_USER=gymuser \
DB_PASSWORD=gympass \
DB_NAME=gymdb \
./mvnw spring-boot:run
