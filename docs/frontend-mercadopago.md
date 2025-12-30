# Integración Frontend - Mercado Pago Wallet Brick

Esta guía documenta cómo integrar Wallet Brick en tu frontend React/Vue/JS para procesar pagos.

---

## Endpoints Backend

### 1. Crear Preference (Wallet Brick)
**POST** `/api/mp/preference`

Crea una preferencia de pago que luego se usa en el Wallet Brick del frontend.

**Autenticación**: Requiere JWT en header `Authorization: Bearer <token>`

**Request Body**:
```json
{
  "title": "SigmaGym - Membresía Mensual",
  "quantity": 1,
  "unitPrice": 15000.0,
  "currency": "ARS",
  "payerEmail": "usuario@example.com",
  "externalReference": "membresia-123"
}
```

**Response 200 OK**:
```json
{
  "preferenceId": "1234567890-abcd-1234-abcd-1234567890ab",
  "initPoint": "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=...",
  "sandboxInitPoint": "https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=...",
  "publicKey": "TEST-3f15c0a6-7779-46c4-b157-072cecf35042"
}
```

**Errores**:
- `400`: Monto o cantidad inválidos
- `502`: Error comunicándose con API de Mercado Pago
- `500`: Error interno del servidor

---

### 2. Webhook (Notificaciones de MP)
**POST** `/webhooks/mercadopago`

Este endpoint lo llama Mercado Pago automáticamente cuando hay cambios en el pago (aprobado, rechazado, etc.).

**Autenticación**: Público (no requiere JWT)

**Nota**: Configura este endpoint en tu panel de MP con la URL pública de tu backend.

---

## Integración Frontend

### Paso 1: Cargar SDK de Mercado Pago

En tu `index.html` o componente principal:

```html
<script src="https://sdk.mercadopago.com/js/v2"></script>
```

### Paso 2: Crear Preference desde tu Backend

```javascript
// Ejemplo con fetch (adaptalo a tu servicio HTTP)
async function createPaymentPreference(membershipData) {
  const token = localStorage.getItem('authToken'); // tu JWT
  
  const response = await fetch('http://localhost:8081/api/mp/preference', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      title: membershipData.planName,
      quantity: 1,
      unitPrice: membershipData.price,
      currency: 'ARS',
      payerEmail: membershipData.userEmail,
      externalReference: `plan-${membershipData.planId}-user-${membershipData.userId}`
    })
  });
  
  if (!response.ok) {
    throw new Error('Error creando preferencia de pago');
  }
  
  return await response.json();
}
```

### Paso 3: Montar Wallet Brick

```javascript
async function initWalletBrick(containerDivId, membershipData) {
  try {
    // 1. Crear preference en tu backend
    const { preferenceId, publicKey } = await createPaymentPreference(membershipData);
    
    // 2. Inicializar SDK de MP
    const mp = new MercadoPago(publicKey, {
      locale: 'es-AR'
    });
    
    const bricksBuilder = mp.bricks();
    
    // 3. Crear y montar Wallet Brick
    await bricksBuilder.create('wallet', containerDivId, {
      initialization: {
        preferenceId: preferenceId,
        redirectMode: 'self' // o 'blank' para abrir en nueva pestaña
      },
      customization: {
        texts: {
          valueProp: 'smart_option' // mensaje mostrado en el brick
        }
      },
      callbacks: {
        onReady: () => {
          console.log('Wallet Brick listo');
        },
        onSubmit: ({ formData }) => {
          // Este callback se ejecuta cuando el usuario hace submit del brick
          // formData contiene info del pago
          console.log('Pago enviado', formData);
        },
        onError: (error) => {
          console.error('Error en Wallet Brick:', error);
        }
      }
    });
    
  } catch (error) {
    console.error('Error inicializando pago:', error);
  }
}
```

### Paso 4: HTML y uso en componente

```html
<!-- En tu template -->
<div id="walletBrick"></div>

<script>
  // Datos de ejemplo (obtenlos de tu estado/store)
  const membershipData = {
    planName: 'Membresía Mensual',
    price: 15000,
    userEmail: 'usuario@example.com',
    planId: 1,
    userId: 42
  };
  
  // Inicializar cuando cargue el componente
  initWalletBrick('walletBrick', membershipData);
</script>
```

---

## Ejemplo React

```jsx
import { useEffect, useState } from 'react';

function PaymentPage({ membershipPlan, user }) {
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const loadPaymentBrick = async () => {
      try {
        const token = localStorage.getItem('authToken');
        
        // 1. Crear preference
        const response = await fetch('http://localhost:8081/api/mp/preference', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            title: membershipPlan.name,
            quantity: 1,
            unitPrice: membershipPlan.price,
            currency: 'ARS',
            payerEmail: user.email,
            externalReference: `plan-${membershipPlan.id}-user-${user.id}`
          })
        });
        
        const { preferenceId, publicKey } = await response.json();
        
        // 2. Inicializar MP
        const mp = new window.MercadoPago(publicKey, { locale: 'es-AR' });
        const bricksBuilder = mp.bricks();
        
        // 3. Montar Wallet Brick
        await bricksBuilder.create('wallet', 'walletBrick', {
          initialization: { preferenceId },
          callbacks: {
            onReady: () => setLoading(false),
            onError: (error) => console.error(error)
          }
        });
      } catch (error) {
        console.error('Error cargando pago:', error);
        setLoading(false);
      }
    };
    
    loadPaymentBrick();
  }, [membershipPlan, user]);
  
  return (
    <div>
      <h2>Pagar {membershipPlan.name}</h2>
      <p>Precio: ${membershipPlan.price}</p>
      {loading && <p>Cargando...</p>}
      <div id="walletBrick"></div>
    </div>
  );
}

export default PaymentPage;
```

---

## Flujo completo

1. **Usuario selecciona plan** → Frontend llama `POST /api/mp/preference`
2. **Backend crea preference** → Devuelve `preferenceId` y `publicKey`
3. **Frontend monta Wallet Brick** → Usuario ve opciones de pago (tarjeta, débito, etc.)
4. **Usuario paga** → Mercado Pago procesa
5. **MP notifica tu backend** → `POST /webhooks/mercadopago` con estado del pago
6. **Backend actualiza membresía** → Activa/renueva el plan del usuario
7. **Usuario ve confirmación** → MP redirige a `success` o muestra estado

---

## Testing con credenciales de prueba

### Usuarios de prueba
- **Comprador**: `test_user_123456@testuser.com`
- Contraseña: usa la que te da MP al crear usuarios de prueba

### Tarjetas de prueba (Argentina)
- **Visa aprobada**: `4509 9535 6623 3704` / CVV: 123 / Venc: cualquier fecha futura
- **Mastercard rechazada**: `5031 7557 3453 0604` / CVV: 123 / Venc: cualquier fecha futura
- **Amex pendiente**: `3711 803032 57522` / CVV: 1234 / Venc: cualquier fecha futura

Más info: https://www.mercadopago.com.ar/developers/es/docs/checkout-bricks/additional-content/test-cards

---

## Configuración del webhook en Mercado Pago

### Local (ngrok)
1. Ejecuta:
   ```bash
   ngrok http 8081
   ```
2. Copia la URL pública (ej: `https://abc123.ngrok-free.app`)
3. Ve a tu panel de MP → Developers → Webhooks
4. Agrega: `https://abc123.ngrok-free.app/webhooks/mercadopago`
5. Eventos: selecciona "Pagos" (payments)

### Producción
- Usa tu dominio real: `https://api.sigmagym.com.ar/webhooks/mercadopago`

---

## Verificar estado del pago

Después de que el usuario pague, puedes:
- Mostrar un Status Screen Brick (otra integración)
- Consultar el estado en tu backend mediante un endpoint que liste las transacciones del usuario
- Esperar la notificación del webhook para confirmar

---

## Variables de entorno recomendadas

Para producción, mové las credenciales a variables de entorno:

```bash
export MP_ACCESS_TOKEN="APP-xxxxxxxxxxxxxxxx"
export MP_PUBLIC_KEY="APP-xxxxxxxxxxxxxxxx"
export MP_WEBHOOK_SECRET="tu_secreto_webhook"
```

Y en `application.yml`:
```yaml
mercadopago:
  access-token: ${MP_ACCESS_TOKEN:}
  public-key: ${MP_PUBLIC_KEY:}
  webhook-secret: ${MP_WEBHOOK_SECRET:}
  webhook-strict: true  # en prod, rechazar firmas inválidas
```

---

## Errores comunes

### "Cannot find symbol MercadoPago"
- Asegúrate de cargar el SDK: `<script src="https://sdk.mercadopago.com/js/v2"></script>`

### "preferenceId is required"
- Verifica que el backend devuelva correctamente el `preferenceId` en la respuesta

### Webhook no recibe notificaciones
- Verifica que ngrok esté corriendo
- Confirma que la URL en MP panel coincida con tu ngrok
- Chequea logs del backend para ver si llegan requests

### Error 401 en preference
- Confirma que envías el JWT en header `Authorization: Bearer <token>`
- Verifica que el token no esté expirado

---

## Próximos pasos

- Implementar Status Screen Brick para mostrar resultado del pago
- Persistir transacciones en tu DB
- Actualizar estado de membresías automáticamente desde el webhook
- Agregar Payment Brick si querés tarjeta en tu sitio (más complejo)

¿Dudas? Revisá la doc oficial: https://www.mercadopago.com.ar/developers/es/docs/checkout-bricks/wallet-brick/introduction
