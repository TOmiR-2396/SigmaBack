-- Agrega campos de asistencia/presentismo a la tabla reservations (MySQL 5.7/8.0)
-- NOTA: Realiza backup antes de ejecutar.

ALTER TABLE reservations 
  ADD COLUMN attended TINYINT(1) NOT NULL DEFAULT 0 
  COMMENT 'Indica si el usuario asistió (0=No, 1=Sí)';

ALTER TABLE reservations 
  ADD COLUMN attended_at DATETIME NULL 
  COMMENT 'Fecha y hora cuando se marcó la asistencia';

CREATE INDEX idx_reservations_attended_date 
  ON reservations(attended, date);
