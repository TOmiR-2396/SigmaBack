#!/bin/bash
# Script para configurar variables de entorno de Mercado Pago (TEST/Sandbox)
# Uso: source mp-env-test.sh

echo "🧪 Configurando credenciales de Mercado Pago (TEST/Sandbox)..."

# Credenciales de TEST
export MP_ACCESS_TOKEN="TEST-2436687625853807-122916-333d8ee89b70c597963e87a02acbefe0-113499327"
export MP_PUBLIC_KEY="TEST-3f15c0a6-7779-46c4-b157-072cecf35042"

# Webhook secret de prueba
export MP_WEBHOOK_SECRET="c6545fcb9a46c29a7c8904c22527b57cdd7ceb811f7a14ef6b65a01069d8c8db"
export MP_WEBHOOK_STRICT="false"  # En test: solo advertir, no rechazar

echo "✅ Variables configuradas (TEST):"
echo "   MP_PUBLIC_KEY: ${MP_PUBLIC_KEY:0:20}..."
echo "   MP_ACCESS_TOKEN: ${MP_ACCESS_TOKEN:0:20}..."
echo "   MP_WEBHOOK_STRICT: $MP_WEBHOOK_STRICT"
