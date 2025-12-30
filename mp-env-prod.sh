#!/bin/bash
# Script para configurar variables de entorno de Mercado Pago
# Uso: source mp-env-prod.sh

echo "🔐 Configurando credenciales de Mercado Pago (PRODUCCIÓN)..."

# Credenciales de PRODUCCIÓN
export MP_ACCESS_TOKEN="APP_USR-2436687625853807-122916-6e0383d29556b692a52f4efbd6bb6a2f-113499327"
export MP_PUBLIC_KEY="APP_USR-f6f14fda-3a34-4b22-ba72-1d0a6c8248db"

# Client ID y Secret (por si los necesitas después)
export MP_CLIENT_ID="2436687625853807"
export MP_CLIENT_SECRET="5ybKAtc2Da6tHYFbwMy3dGWfoGPia7EY"

# Webhook (configura el secret desde el panel de MP)
export MP_WEBHOOK_SECRET=""
export MP_WEBHOOK_STRICT="true"  # En producción: rechazar firmas inválidas

echo "✅ Variables configuradas:"
echo "   MP_PUBLIC_KEY: ${MP_PUBLIC_KEY:0:20}..."
echo "   MP_ACCESS_TOKEN: ${MP_ACCESS_TOKEN:0:20}..."
echo "   MP_WEBHOOK_STRICT: $MP_WEBHOOK_STRICT"
echo ""
echo "⚠️  IMPORTANTE: Este archivo NO debe commitearse a git"
echo "    Agregalo a .gitignore si no está ya"
