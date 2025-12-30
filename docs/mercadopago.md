# Integración Mercado Pago (Argentina) – Wallet Brick

Esta guía documenta la integración de Mercado Pago Bricks (Wallet Brick) en el backend y el frontend.

## Credenciales y configuración
- Variables de entorno (recomendado):
  - `MP_ACCESS_TOKEN`: Access Token (backend).
  - `MP_PUBLIC_KEY`: Public Key (frontend).
  - `MP_WEBHOOK_SECRET` (opcional): secreto para validar firma del webhook.
- En `src/main/resources/application.yml` se leen bajo el bloque `mercadopago`.

## Dependencia (backend)
- En `pom.xml` se agregó:
  - `com.mercadopago:sdk-java:2.1.9`
- Configuración inicial del SDK: `com.example.gym.config.MercadoPagoConfigBean` setea el Access Token.

## Endpoints (backend)
- `POST /api/mp/preference`: crea una `Preference` para Wallet Brick.
  - Request (ejemplo):
    ```json
    {
      "title": "SigmaGym - Membresía",
      "quantity": 1,
      "unitPrice": 1000.0,
      "currency": "ARS",
      "payerEmail": "test_user@example.com",
      "externalReference": "order-123"
    }
    ```
  - Response:
    ```json
    {
      "preferenceId": "1234567890",
      "initPoint": "https://...",
      "sandboxInitPoint": "https://...",
      "publicKey": "TEST-..."
    }
    ```
- `POST /webhooks/mercadopago`: recibe notificaciones de MP.
  - Responde `200 OK` rápido y, si `type=payment`, consulta el pago (`PaymentClient.get`) para actualizar estado interno.
  - Seguridad: este endpoint está permitido públicamente en `SecurityConfig`.

## Frontend (Wallet Brick)
1. Cargar el SDK:
   ```html
   <script src="https://sdk.mercadopago.com/js/v2"></script>
   ```
2. Inicializar:
   ```js
   const mp = new MercadoPago(PUBLIC_KEY, { locale: 'es-AR' });
   const bricksBuilder = mp.bricks();
   ```
3. Crear preference desde el backend y montar el Brick:
   ```js
   const { preferenceId } = await fetch('/api/mp/preference', {
     method: 'POST',
     body: JSON.stringify({ quantity: 1, unitPrice: 1000, currency: 'ARS', title: 'Membresía' }),
     headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
   }).then(r => r.json());

   bricksBuilder.create('wallet', 'wallet_container', {
     initialization: { preferenceId },
     callbacks: {
       onReady: () => {},
       onError: (error) => console.error(error)
     }
   });
   ```

## Webhook en local con ngrok
Para que Mercado Pago pueda llamar a tu webhook en local:
1. Instala ngrok y ejecuta:
   ```bash
   ngrok http 8080
   ```
2. Obtén la URL pública (ej: `https://xxxx.ngrok.io`) y configura el webhook en el panel de MP apuntando a `https://xxxx.ngrok.io/webhooks/mercadopago`.
3. Deja ngrok corriendo mientras probás.

## Notas de seguridad/CORS
- `SecurityConfig` permite `POST /webhooks/mercadopago` público; el resto requiere JWT.
- `WebConfig` define orígenes permitidos (localhost + dominios del sitio).
- Considerá `Idempotency-Key` para pagos al integrar Payment Brick.

## Próximos pasos
- Agregar Status Screen Brick para mostrar resultados.
- Persistir órdenes/pagos en DB y actualizar planes/membresías según `payment.status`.
- Validar firma del webhook si configurás `MP_WEBHOOK_SECRET`.
