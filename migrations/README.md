# Migraciones de Base de Datos

## 📋 add_attendance_fields.sql

**Fecha:** 2025-10-20  
**Descripción:** Agrega campos de presentismo/asistencia a la tabla `reservations`

### Columnas agregadas:
- `attended` (TINYINT): Indica si el usuario asistió (0=No, 1=Sí)
- `attended_at` (DATETIME): Timestamp cuando se marcó la asistencia

### Índice agregado:
- `idx_reservations_attended_date`: Para consultas rápidas por asistencia y fecha

---

## 🚀 CÓMO APLICAR LA MIGRACIÓN

### Opción 1: Automática (Hibernate DDL-auto)
Si usas `ddl-auto: update` en producción, Hibernate creará las columnas automáticamente al desplegar.

**NO RECOMENDADO** para producción. Mejor hacerlo manualmente.

---

### Opción 2: Manual en el VPS (RECOMENDADO)

#### A. Conectar al VPS y ejecutar el script:

```bash
# 1. Conectar al VPS
ssh root@72.60.245.66
cd /opt/sigma/SigmaBack

# 2. Copiar el script al servidor (desde tu máquina local)
# En tu máquina local:
scp migrations/add_attendance_simple.sql root@72.60.245.66:/opt/sigma/SigmaBack/migrations/

# 3. Ejecutar el script
docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb < migrations/add_attendance_simple.sql
```

#### B. O ejecutar comandos directamente:

```bash
# Conectar a MySQL
docker exec -it gym-mysql mysql -u gymuser -pgympass gymdb

# Ejecutar los comandos:
ALTER TABLE reservations 
ADD COLUMN attended TINYINT(1) NOT NULL DEFAULT 0 
COMMENT 'Indica si el usuario asistió (0=No, 1=Sí)';

ALTER TABLE reservations 
ADD COLUMN attended_at DATETIME NULL 
COMMENT 'Fecha y hora cuando se marcó la asistencia';

CREATE INDEX idx_reservations_attended_date 
ON reservations(attended, date);

-- Verificar
DESCRIBE reservations;
exit
```

---

## ✅ VERIFICAR LA MIGRACIÓN

```bash
# Verificar que las columnas se crearon
docker exec -it gym-mysql mysql -u gymuser -pgympass gymdb -e "DESCRIBE reservations;"

# Ver estadísticas
docker exec -it gym-mysql mysql -u gymuser -pgympass gymdb -e "
SELECT 
    COUNT(*) as total_reservations,
    SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmed,
    SUM(CASE WHEN attended = 1 THEN 1 ELSE 0 END) as attended,
    SUM(CASE WHEN attended = 0 AND status = 'CONFIRMED' THEN 1 ELSE 0 END) as pending_attendance
FROM reservations;
"
```

---

## 🔄 ROLLBACK (si algo sale mal)

```sql
-- Revertir cambios
ALTER TABLE reservations DROP COLUMN attended;
ALTER TABLE reservations DROP COLUMN attended_at;
DROP INDEX idx_reservations_attended_date ON reservations;
```

---

## 📝 NOTAS IMPORTANTES

1. **Backup obligatorio:** Siempre haz backup antes de aplicar migraciones
   ```bash
   docker exec gym-mysql mysqldump -u gymuser -pgympass \
     --routines --triggers --single-transaction --no-tablespaces \
     gymdb > backups/gymdb_backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Valores por defecto:** Todas las reservas existentes tendrán `attended = 0` (no asistieron)

3. **Sin downtime:** Esta migración es segura, no afecta reservas existentes

4. **Compatible con:** MySQL 5.7+ y MySQL 8.0+

---

## 🧪 TESTING

Después de aplicar la migración, prueba el nuevo endpoint:

```bash
# Marcar asistencia (como OWNER/TRAINER)
curl -X PUT http://72.60.245.66:8080/api/turnos/reservation/123/attendance \
  -H "Authorization: Bearer TOKEN_OWNER" \
  -H "Content-Type: application/json" \
  -d '{"attended": true}'

# Verificar reservas (como MEMBER)
curl -X GET http://72.60.245.66:8080/api/turnos/my-reservations \
  -H "Authorization: Bearer TOKEN_MEMBER"
```
