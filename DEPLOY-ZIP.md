# 🚀 DEPLOY CON PAQUETE COMPRIMIDO

Esta guía te permite crear un paquete comprimido con todos los cambios del backend y subirlo a producción sin depender de Git.

---

## 📋 PREREQUISITOS

- Acceso SSH al servidor: `root@72.60.245.66`
- Docker y docker-compose funcionando en el servidor
- Archivo `.env` configurado en el servidor en `/opt/sigma/SigmaBack/.env`

---

## 🚀 OPCIÓN 1: DEPLOY AUTOMÁTICO (RECOMENDADO)

Este método hace todo automáticamente: genera el paquete, lo sube y lo instala.

```bash
# 1. Generar el paquete
./deploy.sh

# 2. Subir e instalar automáticamente
./deploy-auto.sh
```

**¡Listo!** El script se encargará de todo.

---

## 🔧 OPCIÓN 2: DEPLOY MANUAL (PASO A PASO)

Si preferís tener más control sobre cada paso:

### En tu máquina local:

```bash
# 1. Generar el paquete de deploy
./deploy.sh

# Esto creará un archivo: deploy_YYYYMMDD_HHMMSS.tar.gz
```

### Subir al servidor:

```bash
# 2. Copiar el archivo al servidor
scp deploy_20251020_214917.tar.gz root@72.60.245.66:/tmp/
```

### En el servidor:

```bash
# 3. Conectar al servidor
ssh root@72.60.245.66

# 4. Ir al directorio temporal
cd /tmp

# 5. Descomprimir el paquete
tar -xzf deploy_20251020_214917.tar.gz

# 6. Entrar al directorio
cd deploy_20251020_214917

# 7. Ejecutar el instalador
./install.sh
```

El script `install.sh` hará:
- ✅ Verificar que existe el archivo `.env`
- ✅ Crear backup de la base de datos
- ✅ Aplicar migraciones SQL (si las hay)
- ✅ Copiar archivos al directorio de trabajo
- ✅ Reconstruir la imagen Docker
- ✅ Reiniciar el contenedor backend
- ✅ Mostrar logs para verificar

---

## 📦 ¿QUÉ INCLUYE EL PAQUETE?

El archivo `.tar.gz` contiene:

- `app.jar` - El JAR compilado del backend con todos los cambios
- `Dockerfile` - Para construir la imagen Docker
- `docker-compose.yml` - Configuración de servicios
- `migrations/` - Scripts SQL de migraciones
- `install.sh` - Script de instalación automática
- `app.jar.sha256` - Checksum para verificar integridad
- Documentación de deploy

---

## 🔍 VERIFICACIÓN POST-DEPLOY

Después del deploy, verificá que todo funciona:

```bash
# Ver logs en tiempo real
docker logs gym-backend -f

# Verificar que el contenedor está corriendo
docker ps | grep gym-backend

# Probar el endpoint de login
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123"}'

# Probar el endpoint de presentismo (con token válido)
curl -X PUT http://127.0.0.1:8080/api/turnos/reservation/952/attendance \
  -H "Authorization: Bearer TU_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"attended": true}'
```

---

## ❌ SI ALGO SALE MAL - ROLLBACK

El script crea un backup automático antes de cada deploy. Para restaurar:

```bash
# 1. Detener el backend
docker-compose -f /opt/sigma/SigmaBack/docker-compose.yml stop backend

# 2. Restaurar el último backup
cd /opt/sigma/SigmaBack
LATEST_BACKUP=$(ls -t backups/*.sql.gz | head -1)
gunzip < $LATEST_BACKUP | docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb

# 3. Volver a levantar con la versión anterior
# (puedes descomprimir un paquete anterior o usar git)
docker-compose up -d backend
```

---

## 🐛 TROUBLESHOOTING

### Error: "No se encuentra app.jar"
**Causa:** El script se ejecutó desde el directorio incorrecto.  
**Solución:** Asegúrate de estar en el directorio descomprimido antes de ejecutar `./install.sh`

### Error: "No existe archivo .env"
**Causa:** Falta configurar las variables de entorno SMTP.  
**Solución:**
```bash
cd /opt/sigma/SigmaBack
nano .env
# Agregar las variables MAIL_HOST, MAIL_PORT, etc.
```

### Error 404 en /api/turnos/reservation/{id}/attendance
**Causa:** El código nuevo no se aplicó correctamente.  
**Solución:**
1. Verificar que el JAR incluye los cambios: `jar tf app.jar | grep TurnosController`
2. Verificar logs del backend: `docker logs gym-backend | grep "attendance"`
3. Reconstruir sin caché: `docker-compose build --no-cache backend`

### El backend no levanta después del deploy
**Causa:** Puede haber un error en la configuración o en el código.  
**Solución:**
```bash
# Ver logs completos
docker logs gym-backend --tail=100

# Verificar que el .env existe y es válido
cat /opt/sigma/SigmaBack/.env

# Verificar conectividad con MySQL
docker exec -it gym-mysql mysql -u gymuser -pgympass -e "SELECT 1"
```

---

## 💡 CONSEJOS

1. **Siempre hace backup antes de deploy** - El script lo hace automáticamente, pero podés hacerlo manualmente también.

2. **Probá localmente primero** - Ejecutá tests antes de generar el paquete:
   ```bash
   ./mvnw test
   ```

3. **Guardá los paquetes de deploy** - Por si necesitas hacer rollback a una versión anterior.

4. **Verificá los logs después del deploy** - Asegúrate de que no hay errores en el inicio.

5. **Probá todos los endpoints críticos** - Login, reservas, presentismo, etc.

---

## 📚 ARCHIVOS RELACIONADOS

- `deploy.sh` - Genera el paquete de deploy
- `deploy-auto.sh` - Deploy automático (genera + sube + instala)
- `ANTES-DE-SUBIR.md` - Guía completa de deploy manual
- `DEPLOY-RAPIDO.md` - Comandos rápidos de deploy
- `.env.example` - Ejemplo de variables de entorno

---

## 🆘 CONTACTO DE EMERGENCIA

Si algo sale mal y no podés resolverlo:

1. Revisar logs: `docker logs gym-backend --tail=100`
2. Revisar estado de contenedores: `docker ps -a`
3. Verificar red: `docker network inspect sigmaback_gym-network`
4. Restaurar backup más reciente
5. Revisar esta documentación nuevamente

---

**Última actualización:** 20 de octubre de 2025
