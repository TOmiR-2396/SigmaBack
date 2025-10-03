-- Script de inicialización de la base de datos
-- Este archivo se ejecuta automáticamente cuando se crea el contenedor de MySQL

-- La base de datos 'gymdb' ya existe por la configuración de docker-compose
USE gymdb;

-- Insertar roles por defecto
INSERT IGNORE INTO roles (id, name, description, created_at) VALUES 
(1, 'OWNER', 'Propietario del gimnasio con acceso total', NOW()),
(2, 'TRAINER', 'Entrenador con acceso a clientes y rutinas', NOW()),
(3, 'MEMBER', 'Miembro del gimnasio con acceso básico', NOW());

-- Insertar usuario administrador (OWNER)
-- Contraseña: /t@D5ncA (hasheada con BCrypt)
INSERT IGNORE INTO users (
    id, 
    email, 
    password, 
    first_name, 
    last_name, 
    phone, 
    birth_date, 
    join_date, 
    role, 
    status, 
    created_at, 
    updated_at
) VALUES (
    1,
    'sabrinaadmin@sigmagym.com.ar',
    '$2a$10$O47X1/AyUo2kcBPPUKpsEOaDshFqtyW7oKdkYomQXG.shVprb7rc6', -- BCrypt hash de '/t@D5ncA'
    'Sabrina',
    'Admin',
    '+54 9 3743 52-0718',
    '2000-08-23',
    NOW(),
    'OWNER',
    'ACTIVE',
    NOW(),
    NOW()
);

-- Insertar algunos entrenadores de ejemplo
INSERT IGNORE INTO users (
    id, 
    email, 
    password, 
    first_name, 
    last_name, 
    phone, 
    role, 
    status, 
    created_at, 
    updated_at
) VALUES 
(2, 'carlos.trainer@sigmagym.com.ar', '$2a$10$O47X1/AyUo2kcBPPUKpsEOaDshFqtyW7oKdkYomQXG.shVprb7rc6', 'Carlos', 'Rodríguez', '+54 11 2345-6789', 'TRAINER', 'ACTIVE', NOW(), NOW()),
(3, 'maria.trainer@sigmagym.com.ar', '$2a$10$O47X1/AyUo2kcBPPUKpsEOaDshFqtyW7oKdkYomQXG.shVprb7rc6', 'María', 'González', '+54 11 3456-7890', 'TRAINER', 'ACTIVE', NOW(), NOW());

-- Insertar algunos miembros de ejemplo
INSERT IGNORE INTO users (
    id, 
    email, 
    password, 
    first_name, 
    last_name, 
    phone, 
    role, 
    status, 
    created_at, 
    updated_at
) VALUES 
(4, 'juan.member@gmail.com', '$2a$10$O47X1/AyUo2kcBPPUKpsEOaDshFqtyW7oKdkYomQXG.shVprb7rc6', 'Juan', 'Pérez', '+54 11 4567-8901', 'MEMBER', 'ACTIVE', NOW(), NOW()),
(5, 'ana.member@gmail.com', '$2a$10$O47X1/AyUo2kcBPPUKpsEOaDshFqtyW7oKdkYomQXG.shVprb7rc6', 'Ana', 'López', '+54 11 5678-9012', 'MEMBER', 'INACTIVE', NOW(), NOW()),
(6, 'pedro.member@gmail.com', '$2a$10$O47X1/AyUo2kcBPPUKpsEOaDshFqtyW7oKdkYomQXG.shVprb7rc6', 'Pedro', 'Martín', '+54 11 6789-0123', 'MEMBER', 'ACTIVE', NOW(), NOW());

-- Insertar planes de membresía
INSERT IGNORE INTO membership_plans (
    id,
    name,
    description,
    price,
    duration_days,
    is_active,
    created_at,
    updated_at
) VALUES 
(1, 'Plan Básico', 'Acceso al gimnasio en horarios regulares', 2500.00, 30, true, NOW(), NOW()),
(2, 'Plan Premium', 'Acceso completo + clases grupales', 3500.00, 30, true, NOW(), NOW()),
(3, 'Plan Full', 'Acceso total + entrenador personal', 5000.00, 30, true, NOW(), NOW()),
(4, 'Plan Anual Básico', 'Plan básico con descuento anual', 25000.00, 365, true, NOW(), NOW()),
(5, 'Plan Anual Premium', 'Plan premium con descuento anual', 35000.00, 365, true, NOW(), NOW());

-- Insertar ejercicios de ejemplo
INSERT IGNORE INTO exercises (
    id,
    name,
    description,
    muscle_group,
    equipment,
    difficulty_level,
    instructions,
    video_url,
    created_at,
    updated_at
) VALUES 
(1, 'Press de Banca', 'Ejercicio básico para pecho', 'CHEST', 'Barra y banco', 'BEGINNER', 'Acostarse en el banco, bajar la barra al pecho y empujar hacia arriba', NULL, NOW(), NOW()),
(2, 'Sentadillas', 'Ejercicio fundamental para piernas', 'LEGS', 'Barra o peso corporal', 'BEGINNER', 'Bajar manteniendo la espalda recta hasta 90 grados y subir', NULL, NOW(), NOW()),
(3, 'Peso Muerto', 'Ejercicio compuesto para espalda y piernas', 'BACK', 'Barra con discos', 'INTERMEDIATE', 'Levantar la barra desde el suelo manteniendo la espalda recta', NULL, NOW(), NOW()),
(4, 'Dominadas', 'Ejercicio para espalda y bíceps', 'BACK', 'Barra de dominadas', 'INTERMEDIATE', 'Colgarse de la barra y subir hasta que el mentón supere la barra', NULL, NOW(), NOW()),
(5, 'Flexiones de Brazos', 'Ejercicio de peso corporal para pecho', 'CHEST', 'Peso corporal', 'BEGINNER', 'En posición de plancha, bajar y subir el cuerpo', NULL, NOW(), NOW());

-- Insertar planes de entrenamiento de ejemplo
INSERT IGNORE INTO training_plans (
    id,
    name,
    description,
    difficulty_level,
    duration_weeks,
    created_by_trainer_id,
    created_at,
    updated_at
) VALUES 
(1, 'Rutina Principiante', 'Plan básico para comenzar en el gimnasio', 'BEGINNER', 4, 2, NOW(), NOW()),
(2, 'Rutina Fuerza', 'Plan enfocado en ganar fuerza muscular', 'INTERMEDIATE', 8, 2, NOW(), NOW()),
(3, 'Rutina Definición', 'Plan para definir y tonificar', 'INTERMEDIATE', 6, 3, NOW(), NOW());

-- Insertar suscripciones de ejemplo
INSERT IGNORE INTO subscriptions (
    id,
    user_id,
    membership_plan_id,
    start_date,
    end_date,
    status,
    payment_amount,
    created_at,
    updated_at
) VALUES 
(1, 4, 2, '2024-10-01', '2024-10-31', 'ACTIVE', 3500.00, NOW(), NOW()),
(2, 6, 1, '2024-09-15', '2024-10-15', 'ACTIVE', 2500.00, NOW(), NOW()),
(3, 5, 3, '2024-08-01', '2024-08-31', 'EXPIRED', 5000.00, NOW(), NOW());

-- Insertar asistencias de ejemplo
INSERT IGNORE INTO attendances (
    id,
    user_id,
    check_in_time,
    check_out_time,
    date,
    created_at,
    updated_at
) VALUES 
(1, 4, '08:00:00', '10:00:00', '2024-10-01', NOW(), NOW()),
(2, 6, '18:00:00', '20:30:00', '2024-10-01', NOW(), NOW()),
(3, 4, '07:30:00', '09:15:00', '2024-10-02', NOW(), NOW()),
(4, 6, '17:45:00', NULL, '2024-10-02', NOW(), NOW());

-- Mostrar mensaje de confirmación
SELECT 'Base de datos inicializada correctamente!' as message;
SELECT CONCAT('Usuario administrador creado: ', email) as admin_user FROM users WHERE role = 'OWNER' LIMIT 1;