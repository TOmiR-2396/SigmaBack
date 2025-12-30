# Feature Flags & Multi-Tenancy

## Resumen

Se implementó un sistema de **feature flags por tenant** que permite controlar qué funcionalidades están habilitadas en cada gimnasio sin modificar código.

### Componentes Clave

1. **`TenantContext`**: ThreadLocal que mantiene el tenant_id por request.
2. **`TenantRequestFilter`**: Resuelve el tenant desde el header `X-Tenant-ID`, habilita el filtro de Hibernate y limpia después.
3. **`TenantEntity`**: Clase base que agrega `tenant_id` a todas las entidades y aplica Hibernate filter.
4. **`TenantSwitch`**: Entidad para guardar feature flags (clave, enabled, payload JSON).
5. **`TenantSwitchService`**: Consulta y cachea switches por tenant.
6. **`FeatureFlagService`**: Wrapper conveniente con `isEnabled()`, `getPayload()` y `requireEnabled()`.

---

## Uso en Código

### 1. Consultar un Flag (Sin Requerimiento)

```java
@Autowired
private FeatureFlagService featureFlagService;

if (featureFlagService.isEnabled("MERCADO_PAGO", false)) {
    // Crear preference de MP
} else {
    // Alternativa
}
```

### 2. Guard-Clause (Con Requerimiento)

```java
// En ScheduleService.pauseDay():
featureFlagService.requireEnabled(
    "PAUSE_DAYS",
    "La pausa de horarios no está habilitada en este gimnasio"
);
// Si no está enabled, lanza FeatureNotEnabledException (HTTP 403)
```

### 3. Acceder a Metadata (Payload JSON)

```java
Optional<String> payload = featureFlagService.getPayload("DESCUENTOS_GRADUALES");
// {"descuento":0.15}
if (payload.isPresent()) {
    // Parse y usa
}
```

---

## Endpoints

### GET /api/tenants/me/features
Lista todos los feature flags del tenant actual.

**Request:**
```
GET /api/tenants/me/features
Header: X-Tenant-ID: sigma-gym
Authorization: Bearer <token>
```

**Response:**
```json
[
  {"key": "MERCADO_PAGO", "enabled": true, "payload": null},
  {"key": "PAUSE_DAYS", "enabled": true, "payload": null},
  {"key": "DESCUENTOS_GRADUALES", "enabled": true, "payload": "{\"descuento\":0.15}"}
]
```

### GET /api/tenants/me
Información del tenant actual.

### POST /api/schedules/{id}/pause-day
Pausa un día en un horario (requiere `PAUSE_DAYS` enabled).

**Request:**
```json
{
  "date": "2025-12-25"
}
```

**Si el flag no está enabled → HTTP 403:**
```json
{"error": "La pausa de horarios no está habilitada en este gimnasio"}
```

---

## Setup Inicial para Sigma Gym

1. **Crear tenants_switches** (en BD):
```sql
INSERT INTO tenant_switches (tenant_id, flag_key, enabled, payload) 
VALUES 
  ('sigma-gym', 'MERCADO_PAGO', 1, NULL),
  ('sigma-gym', 'PAUSE_DAYS', 1, NULL),
  ('sigma-gym', 'DESCUENTOS_GRADUALES', 1, '{"descuento":0.15}');
```

2. **Enviar requests con header:**
```bash
curl -X GET http://localhost:8081/api/tenants/me/features \
  -H "X-Tenant-ID: sigma-gym" \
  -H "Authorization: Bearer <token>"
```

---

## Próximas Etapas

- **CRUD de Switches** (POST, PUT, DELETE) con permisos OWNER.
- **Auditoría** de cambios de flags.
- **Cache distribuido** (Redis) para switches.
- **Health check** por tenant.

---

## Patrón: Cómo Agregar un Nuevo Flag

1. Injecta `FeatureFlagService` en tu servicio.
2. Llama `featureFlagService.requireEnabled("TU_FLAG", "mensaje")` para guard-clause.
3. Crea filas en `tenant_switches` con tu flag activado/desactivado por tenant.
4. Listo, sin redeploy.

---

## Notas

- El filtro de Hibernate se aplica automáticamente a todas las entidades `TenantEntity`.
- Los datos sin `tenant_id` pueden backfillears primero (ej. actualizar a 'sigma-gym').
- Cachea switches por 60s (configurable con `multitenancy.switches.cache-seconds`).
- Si no hay tenant en contexto, `FeatureFlagService` lanza excepción.
