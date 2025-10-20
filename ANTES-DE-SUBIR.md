# ANTES DE SUBIR

## 🚨 PASOS MANUALES PARA APLICAR CAMBIOS CON DOCKER COMPOSE

### PREREQUISITOS
- Asegúrate de que todos los cambios estén commiteados y pusheados a GitHub
- Tener acceso SSH al VPS: `ssh root@72.60.245.66`

---

## � PASO 0 - BACKUP DE BASE DE DATOS (OBLIGATORIO)

### **🔒 CREAR BACKUP COMPLETO DE LA BASE DE DATOS**
```bash
# Conectar al VPS
ssh root@72.60.245.66
cd /opt/sigma/SigmaBack

# Crear directorio de backups si no existe
mkdir -p backups

# Crear backup con timestamp (sin tablespaces para evitar privilegio PROCESS)
docker exec gym-mysql mysqldump -u gymuser -pgympass \
  --routines --triggers --single-transaction --no-tablespaces \
  gymdb > backups/gymdb_backup_$(date +%Y%m%d_%H%M%S).sql

# Opcional: comprimir el backup para ahorrar espacio
gzip -9 backups/gymdb_backup_*.sql

# Verificar que el backup se creó correctamente
ls -la backups/
tail -10 backups/gymdb_backup_*.sql
```

### **📊 INFORMACIÓN DEL BACKUP**
```bash
# Ver tamaño del backup
du -sh backups/gymdb_backup_*.sql

# Ver contenido del backup (primeras líneas)
head -20 backups/gymdb_backup_*.sql

# Mantener solo los últimos 5 backups (limpiar antiguos)
cd backups && ls -t gymdb_backup_*.sql | tail -n +6 | xargs rm -f && cd ..
```

### **🔄 CÓMO RESTAURAR EL BACKUP (si algo sale mal)**
```bash
# En caso de necesitar restaurar:
docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb < backups/gymdb_backup_YYYYMMDD_HHMMSS.sql
```

---

## �📋 PROCESO COMPLETO PASO A PASO

### 1. **CONECTAR AL VPS**
```bash
ssh root@72.60.245.66
cd /opt/sigma/SigmaBack
```

### 2. **DESCARGAR ÚLTIMOS CAMBIOS**
```bash
git pull origin main
```

### 3. **DETENER SOLO EL BACKEND (PRESERVAR MYSQL)**
```bash
docker-compose stop backend
```

### 4. **ELIMINAR CONTAINER BACKEND ANTERIOR**
```bash
docker rm gym-backend

# Si ves: "container is running" entonces primero detenelo o fuerza la eliminación
docker stop gym-backend || true
docker rm -f gym-backend
```

### 5. **RECONSTRUIR LA IMAGEN BACKEND**
```bash
docker-compose build --no-cache backend
```

### 6. **INICIAR EL BACKEND ACTUALIZADO**
```bash
# Opción A: Usar compose sin recrear MySQL (RECOMENDADO)
docker-compose up -d --no-recreate backend

# Opción B: Si gym-backend no existe, créalo directamente con docker run
docker run -d \
  --name gym-backend \
  --network sigmaback_gym-network \
  -p 127.0.0.1:8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://gym-mysql:3306/gymdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME="gymuser" \
  -e SPRING_DATASOURCE_PASSWORD="gympass" \
  -e SPRING_PROFILES_ACTIVE="docker" \
  -e JWT_SECRET="Z3xO1raNs7SJxocFht4wpds4AYoo7Q9QbRMaOST6H9dtq7vzKrh/+eoB6c036Du6Mr2tlv0w2joGUetlHu6Hxg==" \
  -e JWT_EXPIRATION="86400000" \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  --restart unless-stopped \
  sigmaback-backend

# Opción C: Si gym-backend ya existe y está detenido
docker start gym-backend
```

### 7. **VERIFICAR QUE TODO FUNCIONE**
```bash
# Ver estado de containers
docker ps

# Ver logs del backend (últimas 20 líneas)
docker logs gym-backend --tail=20

# Ver logs en tiempo real (Ctrl+C para salir)
docker logs gym-backend -f
```

---

## ⚠️ SI HAY PROBLEMAS DE CONEXIÓN MYSQL

### **Problema común: "UnknownHostException: mysql"**

**Opción A - Recrear backend con URL correcta:**
```bash
docker stop gym-backend
docker rm gym-backend

docker run -d \
  --name gym-backend \
  --network sigmaback_gym-network \
  -p 127.0.0.1:8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://gym-mysql:3306/gymdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME="gymuser" \
  -e SPRING_DATASOURCE_PASSWORD="gympass" \
  -e SPRING_PROFILES_ACTIVE="docker" \
  -e JWT_SECRET="Z3xO1raNs7SJxocFht4wpds4AYoo7Q9QbRMaOST6H9dtq7vzKrh/+eoB6c036Du6Mr2tlv0w2joGUetlHu6Hxg==" \
  -e JWT_EXPIRATION="86400000" \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  --restart unless-stopped \
  sigmaback-backend
```

---

## 🔍 COMANDOS DE DIAGNÓSTICO

### **Ver estado general:**
```bash
docker ps
docker-compose ps
```

### **Ver logs detallados:**
```bash
# Backend
docker logs gym-backend --tail=50

# MySQL
docker logs gym-mysql --tail=20
```

### **Conectar a MySQL directamente:**
```bash
docker exec -it gym-mysql mysql -u gymuser -p
# Contraseña: gympass
```

### **Ver redes de Docker:**
```bash
docker network ls
docker network inspect sigmaback_gym-network
```

---

## 🚨 EN CASO DE EMERGENCIA - RESTART COMPLETO

**SOLO si todo está roto y necesitas empezar de cero:**
```bash
# CUIDADO: Esto preserva los datos de MySQL
docker-compose down
docker-compose up -d
```

---

## ✅ VERIFICACIÓN FINAL

### **Comprobar que la aplicación funciona:**
1. `docker ps` - Ambos containers corriendo
2. `docker logs gym-backend --tail=10` - Sin errores
3. Probar endpoint: `curl http://127.0.0.1:8080/api/auth/test` o similar

### **Ver debug logs en acción:**
```bash
docker logs gym-backend -f
# Hacer una reserva desde la app para ver los logs de debug
```

---

## 📝 NOTAS IMPORTANTES

- 🔥 **SIEMPRE hacer backup ANTES de cualquier cambio** - Es obligatorio, no opcional
- ⚠️ **NUNCA elimines el container gym-mysql** - Perderás todos los datos
- ✅ **Siempre usa `docker-compose stop backend`** - No `docker-compose down`
- 🔍 **Los debug logs aparecen cuando haces reservas** - Busca líneas con `[DEBUG]`
- 🚀 **Si cambias código, siempre usa `--no-cache`** - Para asegurar build limpio
- 💾 **Los backups se guardan en `/opt/sigma/SigmaBack/backups/`** - Con timestamp automático
- 🗂️ **Se mantienen los últimos 5 backups** - Los antiguos se eliminan automáticamente

---

## 🆘 CONTACTOS DE EMERGENCIA

Si algo sale mal:
1. Revisar logs: `docker logs gym-backend --tail=50`
2. Verificar red: `docker network inspect sigmaback_gym-network`
3. Verificar MySQL: `docker exec -it gym-mysql mysql -u gymuser -p`