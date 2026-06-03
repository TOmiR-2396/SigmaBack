# Gym Backend (Spring Boot + MySQL)

## Requisitos

- Java 17+
- Docker Desktop corriendo

---

## Levantar para desarrollo (Maven + MySQL en Docker)

### 1. Iniciar MySQL

```powershell
docker-compose up mysql -d
```

Esperar hasta que el contenedor esté `healthy` (~20 segundos):

```powershell
docker inspect gym-mysql --format "{{.State.Health.Status}}"
# debe mostrar: healthy
```

### 2. Iniciar el backend

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"; .\mvnw.cmd spring-boot:run
```

El servidor levanta en **`http://localhost:8081`**

---

## Usuarios de prueba

Todos usan la misma contraseña: `/t@D5ncA`

| Email | Nombre | Rol | Estado |
|-------|--------|-----|--------|
| `sabrinaadmin@sigmagym.com.ar` | Sabrina Admin | OWNER | ACTIVE | Sigma2024! 
| `carlos.trainer@sigmagym.com.ar` | Carlos Rodríguez | TRAINER | ACTIVE |
| `maria.trainer@sigmagym.com.ar` | María González | TRAINER | ACTIVE |
| `juan.member@gmail.com` | Juan Pérez | MEMBER | ACTIVE |
| `ana.member@gmail.com` | Ana López | MEMBER | INACTIVE |
| `pedro.member@gmail.com` | Pedro Martín | MEMBER | ACTIVE |

> Los usuarios se cargan automáticamente desde `docker-entrypoint-initdb.d/01-init-data.sql` la primera vez que se crea el volumen de MySQL.

---

## Detener

```powershell
# Solo MySQL
docker-compose stop mysql

# Todo
docker-compose down
```

## Resetear la base de datos (borrar datos y volver a cargar el SQL inicial)

```powershell
docker-compose down -v
docker-compose up mysql -d
```
