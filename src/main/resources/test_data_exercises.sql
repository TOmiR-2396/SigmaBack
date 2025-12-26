-- Script para insertar datos de prueba para ejercicios

-- 1. Insertar usuarios de prueba si no existen
INSERT INTO users (email, password, first_name, last_name, phone, role, status, created_at, updated_at)
VALUES 
  ('member@gym.com', '$2y$10$NqRWm3E0P4EzF5P6hVkZ2eoGPn3j8I0P0L1M2N3O4P5Q6R7S8T9U', 'Member', 'Test', '1234567890', 'MEMBER', 'ACTIVE', NOW(), NOW()),
  ('trainer@gym.com', '$2y$10$NqRWm3E0P4EzF5P6hVkZ2eoGPn3j8I0P0L1M2N3O4P5Q6R7S8T9U', 'Trainer', 'Test', '0987654321', 'TRAINER', 'ACTIVE', NOW(), NOW()),
  ('owner@gym.com', '$2y$10$NqRWm3E0P4EzF5P6hVkZ2eoGPn3j8I0P0L1M2N3O4P5Q6R7S8T9U', 'Owner', 'Test', '1111111111', 'OWNER', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 2. Obtener IDs de usuarios
SET @member_id = (SELECT id FROM users WHERE email = 'member@gym.com' LIMIT 1);
SET @trainer_id = (SELECT id FROM users WHERE email = 'trainer@gym.com' LIMIT 1);
SET @owner_id = (SELECT id FROM users WHERE email = 'owner@gym.com' LIMIT 1);

-- 3. Insertar plan de entrenamiento para member
INSERT INTO planes (name, description, is_template, user_id, start_date, end_date, status, created_at, updated_at)
VALUES 
  ('Plan de Pecho y Brazos', 'Plan personalizado para fortalecer pecho y brazos', FALSE, @member_id, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4. Obtener ID del plan
SET @plan_id = (SELECT id FROM planes WHERE name = 'Plan de Pecho y Brazos' AND user_id = @member_id LIMIT 1);

-- 5. Insertar ejercicios de prueba
INSERT INTO exercises (name, description, training_plan_id, sets, reps, weight, video_url, member_comment, trainer_comment, created_at, updated_at)
VALUES 
  ('Flexiones', 'Ejercicio básico de pecho y brazos', @plan_id, 3, 10, 0, NULL, '', '', NOW(), NOW()),
  ('Sentadillas', 'Ejercicio de piernas y glúteos', @plan_id, 4, 8, 20, NULL, '', '', NOW(), NOW()),
  ('Dominadas', 'Ejercicio de espalda y brazos', @plan_id, 3, 5, 0, NULL, '', '', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 6. Obtener IDs de ejercicios
SET @exercise_1_id = (SELECT id FROM exercises WHERE name = 'Flexiones' AND training_plan_id = @plan_id LIMIT 1);
SET @exercise_2_id = (SELECT id FROM exercises WHERE name = 'Sentadillas' AND training_plan_id = @plan_id LIMIT 1);
SET @exercise_3_id = (SELECT id FROM exercises WHERE name = 'Dominadas' AND training_plan_id = @plan_id LIMIT 1);

-- 7. Insertar progresión inicial para cada ejercicio
INSERT INTO exercise_progress (exercise_id, weight, sets, reps, recorded_at, created_at)
VALUES 
  (@exercise_1_id, 0, 3, 10, NOW(), NOW()),
  (@exercise_2_id, 20, 4, 8, NOW(), NOW()),
  (@exercise_3_id, 0, 3, 5, NOW(), NOW())
ON DUPLICATE KEY UPDATE recorded_at = NOW();

-- Salida de confirmación
SELECT CONCAT('Datos de prueba insertados exitosamente:
- Member ID: ', @member_id, '
- Trainer ID: ', @trainer_id, '
- Owner ID: ', @owner_id, '
- Plan ID: ', @plan_id, '
- Exercise IDs: ', @exercise_1_id, ', ', @exercise_2_id, ', ', @exercise_3_id) as resultado;
