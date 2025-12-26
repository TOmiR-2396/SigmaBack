# SigmaGym - Backend API (Refactorización de Arquitectura Completada)

## Estado del Proyecto

### ✅ Completado (Refactorización de Arquitectura)

#### Servicios Implementados
- ✅ **TrainingPlanService** (300+ líneas) - Gestión de planes de entrenamiento
- ✅ **ExerciseService** (340+ líneas) - Gestión de ejercicios con carga de videos
- ✅ **TurnosService** (450+ líneas) - Gestión de horarios y reservas con bloqueos pessimistas

#### Controladores Refactorizados
- ✅ **TrainingPlanController** - Reducido de 311 a ~100 líneas
- ✅ **ExerciseController** - Reducido de 347 a ~100 líneas
- ✅ **TurnosController** - Reducido de 689 a ~80 líneas (¡89% reducción!)

#### Patrones de Arquitectura
- ✅ Separación de responsabilidades (HTTP handling vs lógica de negocio)
- ✅ Exception handling unificado (service lanza IllegalArgumentException)
- ✅ @Service con @Transactional en todos los servicios
- ✅ DTOs para mapeo de datos

---

## Construcción y Ejecución

### Requisitos
- Java 17+
- Maven 3.6+
- MySQL 5.7 o 8.0
- Docker (opcional, para ambiente de pruebas)

### Compilación
```bash
cd /Users/santiago/Proyectos/SigmaBack
./mvnw clean package -DskipTests
```

### Ejecución Local
```bash
./mvnw spring-boot:run
```

Servidor disponible en: `http://localhost:8080`

---

## Bases de Datos

### MySQL (Producción)
```bash
# Conectar a MySQL
mysql -h localhost -u root -p

# Crear base de datos
CREATE DATABASE gym_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Ejecutar migraciones (Flyway)
# Las migraciones se ejecutan automáticamente al iniciar la aplicación
```

### H2 (Desarrollo)
- Configuración automática en `application.yml`
- Acceso a consola: `http://localhost:8080/h2-console`

---

## Módulo de Ejercicios - Guía de Pruebas

### Inicio Rápido

1. **Iniciar servidor:**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Cargar datos de prueba:**
   ```bash
   # Ejecutar el script SQL en MySQL
   mysql -u root -p gym_db < src/main/resources/test_data_exercises.sql
   ```

3. **Importar colección Postman:**
   - Abrir Postman
   - Click en "Import"
   - Seleccionar `postman_exercise_collection.json`
   - Las variables `{{token}}`, `{{memberToken}}`, `{{trainerToken}}` se rellenan automáticamente

### Flujo de Prueba

#### Paso 1: Autenticación
```bash
# Login como Member
POST http://localhost:8080/api/auth/login
Body: {"email": "member@gym.com", "password": "member123"}

# Guardar el token en {{memberToken}}
# Repetir para trainer@gym.com y owner@gym.com
```

#### Paso 2: Consultar Ejercicios
```bash
# Ver todos los ejercicios
GET http://localhost:8080/api/exercises
Header: Authorization: Bearer {{memberToken}}

# Ver ejercicios de un plan
GET http://localhost:8080/api/exercises/by-plan/1
Header: Authorization: Bearer {{memberToken}}

# Ver detalle de un ejercicio
GET http://localhost:8080/api/exercises/1
Header: Authorization: Bearer {{memberToken}}
```

#### Paso 3: Cargar Video
```bash
# Member sube video a su ejercicio
POST http://localhost:8080/api/exercises/upload-video/1
Header: Authorization: Bearer {{memberToken}}
Body: form-data → file: (seleccionar archivo .mp4)

# Esperado: 200 OK con URL del video guardada
```

#### Paso 4: Actualizar Ejercicio
```bash
# Cambiar weight/sets/reps para registrar progresión
PUT http://localhost:8080/api/exercises/1
Header: Authorization: Bearer {{memberToken}}
Body: {"weight": 25, "sets": 4, "reps": 12}

# Se crea automáticamente un registro de progreso
```

#### Paso 5: Consultar Progresión
```bash
# Ver histórico de progreso
GET http://localhost:8080/api/exercises/1/progress
Header: Authorization: Bearer {{memberToken}}

# Respuesta: Array de registros históricos con weight, sets, reps, recordedAt
```

#### Paso 6: Agregar Comentarios
```bash
# Member comenta
PUT http://localhost:8080/api/exercises/1/comment/member
Header: Authorization: Bearer {{memberToken}}
Body: {"comment": "Ejercicio muy efectivo"}

# Trainer comenta
PUT http://localhost:8080/api/exercises/1/comment/trainer
Header: Authorization: Bearer {{trainerToken}}
Body: {"comment": "Buen trabajo, aumenta peso"}
```

---

## Casos de Uso Principales

### 1. Member sube video a su ejercicio
- ✅ Acceso permitido al ejercicio de su plan
- ✅ Validación: solo archivos video/*
- ✅ Validación: máximo 150MB
- ✅ Limpieza automática de videos > 14 días

### 2. Seguimiento de progresión
- ✅ Se registra automáticamente cada vez que cambia weight/sets/reps
- ✅ Cálculo automático de % de mejora: `((weight_new - weight_old) / weight_old) * 100`
- ✅ Histórico completo accesible en `/api/exercises/{id}/progress`

### 3. Comentarios con control de acceso
- ✅ MEMBER: solo comenta en ejercicios de su plan (memberComment)
- ✅ TRAINER/OWNER: comenta en cualquier ejercicio (trainerComment)
- ✅ Máximo 2000 caracteres

### 4. Gestión de planes de entrenamiento
- ✅ TRAINER/OWNER: crear, actualizar, eliminar planes
- ✅ Asignar templates a usuarios
- ✅ Archivar planes con snapshots JSON de ejercicios
- ✅ Pausar días específicos o días completos

### 5. Sistema de reservas (turnos)
- ✅ Horarios semanales repetitivos con capacidad máxima
- ✅ Bloqueo pessimista para evitar sobreventa
- ✅ Pausar fechas específicas o días completos
- ✅ Marcar asistencia (presentismo)

---

## Seguridad

### Autenticación
- JWT (JSON Web Token) con BCrypt ($2y$10$ - bcrypt)
- Tokens con expiración configurable
- Refresh tokens para renovación de sesiones

### Autorización
- @PreAuthorize en endpoints sensibles
- Control de acceso por rol: MEMBER, TRAINER, OWNER
- Validaciones adicionales en servicios para contexto

### Validación de Datos
- DTOs con validaciones
- Límites de tamaño en comentarios (2000 chars)
- Límites de tamaño en archivos (150MB)
- Validación de rangos (sets, reps, weight)

---

## Detalles Técnicos

### Stack Tecnológico
- **Framework:** Spring Boot 3.3.2
- **Java:** 17
- **ORM:** JPA/Hibernate
- **Base de Datos:** MySQL 5.7+ o H2
- **Seguridad:** Spring Security 6.0 + JWT
- **Build:** Maven 3.11.0
- **Logging:** SLF4J + Logback

### Modelos Principales
```
User
├── Subscription (membresías)
├── TrainingPlan (planes)
│   ├── Exercise (ejercicios)
│   │   └── ExerciseProgress (progresión)
│   └── TrainingPlanHistory (snapshots archivados)
├── Schedule (horarios)
│   └── Reservation (reservas/turnos)
├── Attendance (asistencia)
└── PasswordResetToken
```

### Endpoints Principales

#### Autenticación
```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout
```

#### Ejercicios
```
GET    /api/exercises                          # Todos
GET    /api/exercises/by-plan/{planId}         # Por plan
GET    /api/exercises/{id}                     # Detalle
POST   /api/exercises/create?planId={id}       # Crear
PUT    /api/exercises/{id}                     # Actualizar
GET    /api/exercises/{id}/progress            # Progresión
POST   /api/exercises/upload-video/{id}        # Cargar video
PUT    /api/exercises/{id}/comment/member      # Comentario miembro
PUT    /api/exercises/{id}/comment/trainer     # Comentario entrenador
```

#### Planes de Entrenamiento
```
POST   /api/training-plans/template            # Crear template
GET    /api/training-plans/templates           # Listar templates
PUT    /api/training-plans/template/{id}       # Actualizar template
DELETE /api/training-plans/template/{id}       # Eliminar template
POST   /api/training-plans/assign              # Asignar a usuario
GET    /api/training-plans/my-plan             # Mi plan actual
PUT    /api/training-plans/{id}                # Actualizar mi plan
GET    /api/training-plans/{id}/history        # Histórico archivado
PUT    /api/training-plans/{id}/archive        # Archivar con snapshot
```

#### Turnos y Reservas
```
POST   /api/turnos/schedule                    # Crear horario
GET    /api/turnos/schedules                   # Listar horarios
PUT    /api/turnos/schedule/{id}               # Actualizar horario
DELETE /api/turnos/schedule/{id}               # Eliminar horario
GET    /api/turnos/available?date={date}       # Turnos disponibles
POST   /api/turnos/reservation                 # Hacer reserva
DELETE /api/turnos/reservation/{id}            # Cancelar reserva
PUT    /api/turnos/schedule/{id}/pause         # Pausar día
DELETE /api/turnos/schedule/{id}/pause         # Despausar día
PUT    /api/turnos/pause-day                   # Pausar día completo
DELETE /api/turnos/pause-day                   # Despausar día completo
PUT    /api/turnos/reservation/{id}/attendance # Marcar asistencia
GET    /api/turnos/my-reservations             # Mis reservas
GET    /api/turnos/reservations                # Todas las reservas
```

---

## Próximas Mejoras

### En Progreso
- [ ] Módulo de membresías (MembershipPlanController)
- [ ] Controladores restantes con servicios

### Backlog
- [ ] Tests unitarios (0% cobertura actual)
- [ ] Notificaciones (email, SMS) para pausas de turnos
- [ ] Reportes de progresión
- [ ] Chat en vivo entre miembros y trainers
- [ ] Galería de fotos (adicional a videos)
- [ ] Integración con pagos (Stripe, MercadoPago)

---

## Troubleshooting

### Error: "No such table: users"
```bash
# Ejecutar migraciones manualmente
mvn flyway:migrate -Dflyway.configFiles=src/main/resources/flyway.properties
```

### Error: 404 en endpoints de ejercicios
```bash
# Verificar que los datos de prueba existan
SELECT * FROM exercises LIMIT 1;
```

### Error: JWT token expirado
```bash
# Hacer login nuevamente para obtener token fresco
POST /api/auth/login
```

### Error: Video no sube
```bash
# Verificar permisos en directorio /videos/
mkdir -p /Users/santiago/Proyectos/SigmaBack/videos
chmod 755 videos/
```

---

## Contribuciones

Este proyecto está en refactorización continua. Los cambios principales implementados:

1. **Separación de capas:** Lógica de negocio en servicios
2. **Exception handling:** Excepciones específicas en servicios
3. **Transacciones:** @Transactional en métodos de modificación
4. **DTOs:** Mapeo consistente de datos
5. **Bloqueos:** Pessimistic locking en operaciones críticas

---

## Licencia

Proyecto privado - SigmaGym
