# 🚀 GUÍA RÁPIDA DE DEPLOY - RESUMEN EJECUTIVO

## ⚡ COMANDOS RÁPIDOS (copiar y pegar)

### En el VPS:
```bash
# 1. Conectar y actualizar código
ssh root@72.60.245.66
cd /opt/sigma/SigmaBack
git pull origin main

# 2. CREAR .env (SOLO LA PRIMERA VEZ O SI CAMBIÓ)
cat > .env << 'EOF'
MAIL_HOST=smtp.envialosimple.email
MAIL_PORT=587
MAIL_USERNAME=qZqyMqW4mhtpqYePb39c1af5@sigmagym.com.ar
MAIL_PASSWORD=K6ReDDE3vyLcyz9ds1bGPGg5F5WLMC28
MAIL_FROM=no-reply@sigmagym.com.ar
APP_FRONTEND_RESET_URL=https://sigmagym.com.ar/reset-password?token=
EOF

# 3. BACKUP (OBLIGATORIO)
mkdir -p backups
docker exec gym-mysql mysqldump -u gymuser -pgympass \
  --routines --triggers --single-transaction --no-tablespaces \
  gymdb | gzip > backups/gymdb_backup_$(date +%Y%m%d_%H%M%S).sql.gz

# 4. MIGRACIÓN SQL (SOLO LA PRIMERA VEZ)
docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb << 'SQL'
ALTER TABLE reservations 
  ADD COLUMN IF NOT EXISTS attended TINYINT(1) NOT NULL DEFAULT 0 
  COMMENT 'Indica si el usuario asistió';
ALTER TABLE reservations 
  ADD COLUMN IF NOT EXISTS attended_at DATETIME NULL 
  COMMENT 'Fecha y hora cuando se marcó la asistencia';
CREATE INDEX IF NOT EXISTS idx_reservations_attended_date 
  ON reservations(attended, date);
SQL

# 5. REBUILD Y DEPLOY
docker-compose stop backend
docker rm -f gym-backend
docker-compose build --no-cache backend
docker-compose up -d backend

# 6. VERIFICAR
docker logs gym-backend --tail=30 -f
```

---

## 🔴 ERRORES COMUNES Y SOLUCIONES

### Error: "The MAIL_HOST variable is not set"
**Causa:** Falta el archivo `.env`  
**Solución:**
```bash
cd /opt/sigma/SigmaBack
nano .env
# Agregar las variables MAIL_* y APP_FRONTEND_RESET_URL
# Guardar: Ctrl+O, Enter, Ctrl+X
cat .env  # Verificar
```

### Error: "Couldn't find env file"
**Causa:** El archivo `.env` no existe en `/opt/sigma/SigmaBack/`  
**Solución:** Crear el archivo (ver punto 2 arriba)

### Error: "Column 'attended' already exists"
**Causa:** La migración ya se aplicó  
**Solución:** Ignorar, es normal. No volver a ejecutar el SQL.

### Error: "UnknownHostException: mysql"
**Causa:** El backend no encuentra el contenedor MySQL  
**Solución:**
```bash
docker network inspect sigmaback_gym-network | grep gym-mysql
# Si no aparece, revisar que gym-mysql esté corriendo
docker ps | grep gym-mysql
```

---

## ✅ CHECKLIST PRE-DEPLOY

- [ ] Código commiteado y pusheado a GitHub
- [ ] Archivo `.env` creado con credenciales SMTP válidas
- [ ] URL del frontend configurada en `APP_FRONTEND_RESET_URL`
- [ ] Backup de base de datos realizado (< 5 minutos de antigüedad)
- [ ] Migración SQL aplicada (verificar con `DESCRIBE reservations`)

---

## 🧪 TESTING POST-DEPLOY

```bash
# 1. Verificar que el backend levantó
docker ps | grep gym-backend

# 2. Ver logs en tiempo real
docker logs gym-backend -f

# 3. Probar login (desde otra terminal)
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# 4. Probar "olvidé contraseña" desde el frontend
# - Verificar que llegue el email
# - Verificar que el link funcione

# 5. Probar reservas (hacer una nueva reserva)
# - Verificar que no permite overbooking
# - Verificar que se puede cancelar y volver a reservar

# 6. Probar asistencia (como OWNER/TRAINER)
# - Marcar asistencia en una reserva CONFIRMED
# - Verificar que aparece en el listado
```

---

## 🆘 ROLLBACK DE EMERGENCIA

```bash
# Si algo sale muy mal, restaurar backup:
cd /opt/sigma/SigmaBack

# 1. Detener el backend
docker-compose stop backend

# 2. Restaurar el último backup
LATEST_BACKUP=$(ls -t backups/*.sql.gz | head -1)
gunzip < $LATEST_BACKUP | docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb

# 3. Volver a la versión anterior del código
git log --oneline -5  # Ver commits recientes
git reset --hard COMMIT_HASH_ANTERIOR
docker-compose build --no-cache backend
docker-compose up -d backend

# 4. Verificar logs
docker logs gym-backend --tail=50
```

---

## 📧 CONTACTO

- **Documentación completa:** Ver `ANTES-DE-SUBIR.md`
- **Migraciones SQL:** Ver `migrations/README.md`
- **Variables de entorno:** Ver `.env.example`
