# Sistema de Pagos con Mercado Pago - Gym Management

## Configuración Inicial

### 1. Variables de Entorno

Configura las siguientes variables de entorno en tu sistema o en el archivo `application.yml`:

```yaml
mercadopago:
  access:
    token: "TU_ACCESS_TOKEN_DE_MERCADOPAGO"
  webhook:
    url: "https://tu-dominio.com/api/payments/webhook"
  success:
    url: "https://tu-frontend.com/payment/success"
  failure:
    url: "https://tu-frontend.com/payment/failure"
  pending:
    url: "https://tu-frontend.com/payment/pending"
```

### 2. Configuración de Mercado Pago

1. Regístrate en [Mercado Pago Developers](https://www.mercadopago.com/developers)
2. Crea una aplicación
3. Obtén tu Access Token de prueba y producción
4. Configura las URLs de webhook y redirección

## Endpoints Implementados

### 1. Crear Preferencia de Pago

**POST** `/api/payments/create-preference`

```json
{
  "membershipPlanId": 1,
  "description": "Membresía Premium - 3 meses",
  "payerEmail": "cliente@example.com",
  "externalReference": "opcional-ref-123",
  "amount": 50000.00
}
```

**Respuesta exitosa:**
```json
{
  "preferenceId": "123456789-abcd-1234-efgh-123456789012",
  "initPoint": "https://www.mercadopago.com/checkout/v1/redirect?pref_id=123456789-abcd-1234-efgh-123456789012",
  "sandboxInitPoint": "https://sandbox.mercadopago.com/checkout/v1/redirect?pref_id=123456789-abcd-1234-efgh-123456789012",
  "externalReference": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Preferencia de pago creada exitosamente",
  "success": true
}
```

### 2. Webhook para Notificaciones

**POST** `/api/payments/webhook`

Este endpoint recibe automáticamente las notificaciones de Mercado Pago cuando cambia el estado de un pago.

### 3. Consultar Mis Pagos

**GET** `/api/payments/my-payments`

Retorna la lista de pagos del usuario autenticado.

### 4. Consultar Todos los Pagos (Solo OWNER)

**GET** `/api/payments/all`

Retorna todos los pagos del sistema.

### 5. Información de Membresía Mejorada

**GET** `/api/membership-info-enhanced`

```json
{
  "active": true,
  "membershipPlan": "Premium",
  "startDate": "2024-01-15",
  "endDate": "2024-04-15",
  "daysRemaining": 45,
  "totalPayments": 3,
  "userStatus": "ACTIVE",
  "membershipStatus": "ACTIVE"
}
```

## Flujo de Pago Completo

### 1. Frontend - Crear Preferencia de Pago

```javascript
// Ejemplo en JavaScript
const createPaymentPreference = async (membershipPlanId) => {
  const response = await fetch('/api/payments/create-preference', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    },
    body: JSON.stringify({
      membershipPlanId: membershipPlanId,
      description: 'Membresía del gimnasio',
      payerEmail: userEmail
    })
  });
  
  const data = await response.json();
  
  if (data.success) {
    // Redirigir al usuario a Mercado Pago
    window.location.href = data.initPoint;
  }
};
```

### 2. Procesamiento Automático

1. El usuario completa el pago en Mercado Pago
2. Mercado Pago envía una notificación al webhook `/api/payments/webhook`
3. El sistema procesa automáticamente el pago y:
   - Actualiza el estado del pago en la base de datos
   - Crea o extiende la suscripción del usuario
   - Activa al usuario si estaba inactivo

### 3. URLs de Retorno

Los usuarios son redirigidos automáticamente según el resultado:

- **Éxito**: `/api/payments/success?payment_id={id}&status={status}&external_reference={ref}`
- **Fallo**: `/api/payments/failure?payment_id={id}&status={status}&external_reference={ref}`
- **Pendiente**: `/api/payments/pending?payment_id={id}&status={status}&external_reference={ref}`

## Base de Datos

### Tabla Payments

```sql
CREATE TABLE payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  preference_id VARCHAR(255) NOT NULL UNIQUE,
  payment_id VARCHAR(255),
  payment_method_id VARCHAR(255),
  payment_type_id VARCHAR(255),
  user_id BIGINT NOT NULL,
  membership_plan_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(50) NOT NULL,
  external_reference VARCHAR(255),
  transaction_amount DECIMAL(10,2),
  currency_id VARCHAR(10),
  description TEXT,
  collector_id VARCHAR(255),
  payer_email VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  approved_at TIMESTAMP,
  notification_url TEXT,
  back_url TEXT,
  failure_message TEXT,
  INDEX idx_preference_id (preference_id),
  INDEX idx_payment_id (payment_id),
  INDEX idx_user_id (user_id),
  INDEX idx_external_reference (external_reference),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (membership_plan_id) REFERENCES membership_plans(id)
);
```

### Actualización en MembershipPlan

Se agregó el campo `external_id` para futuras integraciones:

```sql
ALTER TABLE membership_plans ADD COLUMN external_id VARCHAR(255) UNIQUE;
```

## Estados de Pago

- **PENDING**: Pago pendiente de aprobación
- **APPROVED**: Pago aprobado y procesado
- **AUTHORIZED**: Pago autorizado pero no capturado
- **IN_PROCESS**: Pago en proceso
- **IN_MEDIATION**: Pago en mediación
- **REJECTED**: Pago rechazado
- **CANCELLED**: Pago cancelado
- **REFUNDED**: Pago reembolsado
- **CHARGED_BACK**: Contracargo

## Seguridad

- Todos los endpoints de pagos requieren autenticación JWT
- Solo usuarios con rol OWNER pueden ver todos los pagos
- Los webhooks son públicos para recibir notificaciones de Mercado Pago
- Se valida que el usuario autenticado sea el propietario del pago

## Testing

### Ambiente de Pruebas

1. Usa el Access Token de prueba de Mercado Pago
2. Utiliza las tarjetas de prueba proporcionadas por Mercado Pago
3. Las URLs de webhook deben ser accesibles públicamente (usa ngrok para desarrollo local)

### Tarjetas de Prueba

- **Visa aprobada**: 4170068810108020
- **Mastercard rechazada**: 5031755734530604
- **American Express pendiente**: 376414000000009

## Monitoreo y Logs

El sistema registra automáticamente:

- Creación de preferencias de pago
- Notificaciones recibidas de Mercado Pago
- Cambios de estado de pagos
- Errores en el procesamiento

Revisa los logs para debugging:

```bash
tail -f logs/spring.log | grep -i "payment\|mercadopago"
```

## Troubleshooting

### Problemas Comunes

1. **Webhook no recibe notificaciones**
   - Verifica que la URL del webhook sea accesible públicamente
   - Revisa que no haya firewall bloqueando las peticiones
   - Asegúrate de que el endpoint retorne HTTP 200

2. **Pagos no se procesan automáticamente**
   - Verifica los logs del webhook
   - Confirma que el external_reference coincida
   - Revisa la configuración del Access Token

3. **URLs de retorno no funcionan**
   - Verifica que las URLs estén configuradas correctamente
   - Asegúrate de que el frontend pueda manejar los parámetros de retorno

## Próximas Mejoras

- [ ] Reembolsos automáticos
- [ ] Pagos recurrentes
- [ ] Integración con otros métodos de pago
- [ ] Dashboard de analytics de pagos
- [ ] Notificaciones por email al usuario
- [ ] Sistema de descuentos y cupones
