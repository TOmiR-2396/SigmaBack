# Guía de Pruebas - Módulo de Ejercicios

## Prerequisitos
- Servidor corriendo en `http://localhost:8080`
- Base de datos con datos de prueba (usuarios, planes de entrenamiento)
- Postman instalado (o similar para probar APIs)

## Usuarios de Prueba

```
MEMBER:
  email: member@gym.com
  password: member123

TRAINER:
  email: trainer@gym.com
  password: trainer123

OWNER:
  email: owner@gym.com
  password: owner123
```

## Flujo de Pruebas Recomendado

### 1. Autenticación
Primero, obtén los tokens JWT para cada rol:

**Endpoint:** `POST /api/auth/login`

```json
{
  "email": "member@gym.com",
  "password": "member123"
}
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "member@gym.com",
    "role": "MEMBER"
  }
}
```

Guarda el token en la variable `{{memberToken}}` de Postman.

Repite para TRAINER y OWNER.

---

### 2. Consultar Ejercicios Existentes

**GET `/api/exercises`** - Obtener todos los ejercicios

Respuesta esperada:
```json
[
  {
    "id": 1,
    "name": "Flexiones",
    "description": "Ejercicio de pecho",
    "sets": 3,
    "reps": 10,
    "weight": 0,
    "videoUrl": null,
    "memberComment": "",
    "trainerComment": "",
    "trainingPlanId": 1,
    "previousWeight": 0,
    "progressPercentage": 0,
    "progressHistory": []
  }
]
```

**GET `/api/exercises/by-plan/1`** - Ejercicios de un plan específico

**GET `/api/exercises/1`** - Detalle de un ejercicio con progresión

---

### 3. Crear Ejercicio (TRAINER/OWNER only)

**POST `/api/exercises/create?planId=1`** - Crear nuevo ejercicio

```json
{
  "name": "Sentadillas",
  "description": "Ejercicio de piernas",
  "sets": 4,
  "reps": 8,
  "weight": 20,
  "memberComment": "",
  "trainerComment": ""
}
```

Respuesta esperada: `201 Created` con objeto ejercicio + primer registro de progreso guardado

---

### 4. Subir Videos

#### 4.1 MEMBER sube video a su propio ejercicio

**POST `/api/exercises/upload-video/1`** (con token de MEMBER)

- **Success (200):** Si el ejercicio pertenece a su plan
- **Forbidden (403):** Si intenta subir a un ejercicio de otro usuario

#### 4.2 TRAINER sube video a cualquier ejercicio

**POST `/api/exercises/upload-video/1`** (con token de TRAINER)

- **Success (200):** Siempre permitido para TRAINER/OWNER

#### 4.3 Prueba de Validación de Archivo

- **Archivo válido:** `video.mp4` (video/mp4)
- **Archivo inválido:** `document.pdf` → Error: "Solo se permiten archivos de video"
- **Archivo muy grande:** > 150MB → Error: "El video excede 150 MB"

---

### 5. Actualizar Ejercicio y Registrar Progresión

**PUT `/api/exercises/1`** - Actualizar ejercicio

```json
{
  "sets": 5,
  "reps": 10,
  "weight": 25
}
```

**Comportamiento esperado:**
- Si `weight`, `sets` o `reps` cambian → Se crea automáticamente un registro de progresión con los valores **anteriores**
- El ejercicio se actualiza a los nuevos valores
- El histórico se puede consultar en `/api/exercises/1/progress`

**Ejemplo de flujo:**

1. Ejercicio creado con weight: 20
2. PATCH a weight: 25 → Se crea progreso con weight: 20
3. PATCH a weight: 30 → Se crea progreso con weight: 25
4. GET `/api/exercises/1/progress` → Muestra [30, 25, 20]

---

### 6. Consultar Progresión

**GET `/api/exercises/1/progress`** - Histórico completo

Respuesta esperada:
```json
[
  {
    "weight": 30,
    "sets": 5,
    "reps": 10,
    "recordedAt": "2025-12-26T13:46:30"
  },
  {
    "weight": 25,
    "sets": 4,
    "reps": 8,
    "recordedAt": "2025-12-26T13:40:00"
  },
  {
    "weight": 20,
    "sets": 4,
    "reps": 8,
    "recordedAt": "2025-12-26T13:35:00"
  }
]
```

---

### 7. Comentarios

#### 7.1 MEMBER agrega comentario

**PUT `/api/exercises/1/comment/member`**

```json
{
  "comment": "Este ejercicio me está ayudando mucho"
}
```

**Restricciones:**
- MEMBER solo puede comentar en ejercicios de su propio plan
- Max 2000 caracteres
- Si intenta comentar en ejercicio de otro plan → `403 Forbidden`

#### 7.2 TRAINER agrega comentario

**PUT `/api/exercises/1/comment/trainer`**

```json
{
  "comment": "Muy bien, aumenta peso en próxima sesión"
}
```

**Restricciones:**
- Solo TRAINER u OWNER pueden comentar como entrenador
- Max 2000 caracteres

---

## Casos de Prueba Específicos

### Caso 1: MEMBER intenta subir video a ejercicio de otro usuario
```
1. Crear Plan A asignado a MEMBER 1
2. Crear Ejercicio 1 en Plan A
3. Crear Plan B asignado a MEMBER 2
4. Login como MEMBER 2
5. POST /api/exercises/upload-video/1

Esperado: 403 Forbidden - "Solo puedes subir videos a ejercicios de tu propio plan"
```

### Caso 2: MEMBER intenta comentar en ejercicio de otro usuario
```
1. Login como MEMBER 1 (propietario de Plan A)
2. PUT /api/exercises/comment/member para Ejercicio 1 (en Plan A) → 200 OK

3. Login como MEMBER 2 (propietario de Plan B)
4. PUT /api/exercises/comment/member para Ejercicio 1 (en Plan A)

Esperado: 403 Forbidden - "Solo puedes comentar ejercicios de tu propio plan"
```

### Caso 3: Progresión se calcula correctamente
```
1. Crear ejercicio con weight: 20, sets: 3, reps: 10
2. GET /api/exercises/1 → progressPercentage: 0

3. PUT /api/exercises/1 con weight: 25
4. GET /api/exercises/1 → progressPercentage: 25%

5. PUT /api/exercises/1 con weight: 20 (regresión)
6. GET /api/exercises/1 → progressPercentage: -20%
```

### Caso 4: Limpieza automática de videos antiguos
```
1. Subir video 1 (day 0)
2. Subir video 2 (day 5)
3. Subir video 3 (day 14)
4. Esperar 15 días
5. Subir video 4

Esperado: Archivos video 1 y 2 se eliminan automáticamente
```

---

## Respuestas de Error Esperadas

| Escenario | Código | Mensaje |
|-----------|--------|---------|
| MEMBER intenta crear ejercicio | 403 | "Only TRAINER or OWNER can create exercises" |
| MEMBER intenta actualizar ejercicio | 403 | "Only TRAINER or OWNER can update exercises" |
| MEMBER intenta subir video a plan ajeno | 403 | "Solo puedes subir videos a ejercicios de tu propio plan" |
| Archivo inválido en upload | 400 | "Solo se permiten archivos de video (mp4)" |
| Video > 150MB | 400 | "El video excede 150 MB. Súbelo comprimido..." |
| Ejercicio no existe | 404 | Not Found |
| Comentario > 2000 chars | 400 | "El comentario supera el máximo de 2000 caracteres" |

---

## Notas Importantes

1. **Progresión automática:** Se registra **antes** de actualizar, con los valores **anteriores**
2. **Bloqueo pessimista:** Los endpoints de reservas (turnos) usan bloqueo para evitar race conditions
3. **Limpieza de videos:** Se ejecuta cada vez que se sube un video
4. **Múltiples roles:** Un usuario con TRAINER puede hacer todo lo que hace MEMBER + crear/actualizar ejercicios
