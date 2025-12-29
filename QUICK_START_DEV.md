# 🚀 GUÍA RÁPIDA - SigmaGym Backend (DEV)

**Fecha:** 28 de Diciembre, 2025  
**Estado:** ✅ Backend levantado en perfil DEV (H2 en memoria)

---

## 🎯 Arrancar Backend en Desarrollo

```bash
cd /Users/santiago/Proyectos/SigmaBack

# Opción 1: Limpio (recomendado primera vez)
./mvnw clean
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# Opción 2: Sin limpiar (más rápido después)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

**Espera a ver:** `Started GymApplication in X.XXX seconds`

**Puerto:** `http://localhost:8081`

---

## 👥 Usuarios de Prueba (Precargados automáticamente)

| Email | Rol | Password | Estado |
|-------|-----|----------|--------|
| `sabrinaadmin@sigmagym.com.ar` | **OWNER** | `/t@D5ncA` | ✅ Activo |
| `carlos.trainer@sigmagym.com.ar` | **TRAINER** | `/t@D5ncA` | ✅ Activo |
| `maria.trainer@sigmagym.com.ar` | **TRAINER** | `/t@D5ncA` | ✅ Activo |
| `juan.member@gmail.com` | **MEMBER** | `/t@D5ncA` | ✅ Activo |
| `ana.member@gmail.com` | **MEMBER** | `/t@D5ncA` | ❌ Inactivo |
| `pedro.member@gmail.com` | **MEMBER** | `/t@D5ncA` | ✅ Activo |

---

## 🔑 Login (Obtener JWT Token)

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sabrinaadmin@sigmagym.com.ar",
    "password": "/t@D5ncA"
  }'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "sabrinaadmin@sigmagym.com.ar",
    "firstName": "Sabrina",
    "role": "OWNER",
    "status": "ACTIVE"
  }
}
```

**Usa el token para requests autenticados:**
```bash
curl -X GET http://localhost:8081/api/exercises \
  -H "Authorization: Bearer <tu-token-aqui>"
```

---

## 📚 Endpoints Principales

### Autenticación
- **POST** `/api/auth/login` - Login
- **POST** `/api/auth/register` - Registro
- **POST** `/api/auth/refresh` - Refresh token

### Ejercicios
- **GET** `/api/exercises` - Listar todos
- **GET** `/api/exercises/{id}` - Obtener uno
- **POST** `/api/exercises` - Crear (TRAINER/OWNER)
- **PUT** `/api/exercises/{id}` - Actualizar (TRAINER/OWNER)
- **POST** `/api/exercises/{id}/upload-video` - Subir video
  - MEMBER: solo a ejercicios de su plan
  - TRAINER/OWNER: a cualquiera

### Planes de Entrenamiento
- **GET** `/api/training-plans` - Listar
- **GET** `/api/training-plans/{id}` - Obtener
- **POST** `/api/training-plans` - Crear
- **PUT** `/api/training-plans/{id}` - Actualizar
- **DELETE** `/api/training-plans/{id}` - Eliminar

### Turnos (Horarios y Reservas)
- **GET** `/api/turnos/schedules` - Listar horarios
- **POST** `/api/turnos/schedules` - Crear horario
- **POST** `/api/turnos/reservations/make` - Hacer reserva
- **POST** `/api/turnos/{id}/pause-day` - Pausar día

---

## 🗄️ BD H2 (En memoria)

**Consola H2:**
```
http://localhost:8081/h2-console
```

**Credenciales:**
- JDBC URL: `jdbc:h2:mem:gymdb`
- Usuario: `sa`
- Contraseña: (vacío)

**Nota:** Los datos se pierden al reiniciar. Son recreados automáticamente por `DataInitializer.java`.

---

## 🛑 Detener Backend

En la terminal donde corre el backend:
```bash
Ctrl + C
```

---

## 📋 Configuración Actual (application.yml)

| Setting | Valor |
|---------|-------|
| **Perfil** | `dev` |
| **Puerto** | `8081` |
| **BD** | H2 en memoria (`gymdb`) |
| **Hibernation** | `ddl-auto: create-drop` (crea/destruye cada vez) |
| **JWT Secret** | `YjFhMmMzZDRl...` (en application.yml) |
| **Logs** | DEBUG para Hibernate SQL |

---

## ⚙️ Variables de Entorno (Opcionales)

```bash
# Cambiar perfil (dev, prod, docker)
export SPRING_PROFILES_ACTIVE=dev

# Cambiar puerto (default: 8081)
export PORT=9000

# JWT Secret (sobrescribe el de application.yml)
export JWT_SECRET="tu-secreto-aqui"

# Credenciales MySQL (solo para perfil prod)
export DB_HOST=localhost
export DB_PORT=3307
export DB_NAME=gymdb
export DB_USER=gymuser
export DB_PASSWORD=gympass
```

---

## 🐛 Troubleshooting

### Puerto 8081 ya ocupado
```bash
# Buscar qué está usando el puerto
lsof -i :8081

# Cambiar a otro puerto
PORT=8082 SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

### Login falla con "User not found"
- Verifica que el backend esté en perfil `dev`: `echo $SPRING_PROFILES_ACTIVE`
- Confirma que `DataInitializer.java` cargó los usuarios (busca "✓ OWNER creado" en logs)
- Si falta, relanza con `./mvnw clean` antes

### Error 500 en requests
- Abre la consola del navegador (F12 → Network) y ve la respuesta JSON
- Copia el error exacto y comparte
- Ve los logs del backend: `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run 2>&1 | grep ERROR`

### H2 Console no carga
- Verifica que el backend esté corriendo: `http://localhost:8081` debe responder
- Asegúrate de que perfil sea `dev` (H2 console solo en dev, no en prod)

---

## 🎬 Próximos Pasos

1. **Testing de endpoints** → Usa Postman o `postman_exercise_collection.json`
2. **Conectar frontend** → Apunta a `http://localhost:8081` en tu client
3. **Escribir tests unitarios** → `src/test/java/...`
4. **Deploy a producción** → Cambiar a perfil `prod` con MySQL

---

**¿Dudas?** Consulta:
- `CODE_REVIEW_2025.md` - Crítica del código y mejoras pendientes
- `README_REFACTORIZATION.md` - Detalles de la refactorización de servicios
- `TESTING_GUIDE_EXERCISES.md` - Guía de pruebas del módulo Ejercicios
