# ANTES DE SUBIR - GUÍA RÁPIDA

## 🔒 BACKUP (SIEMPRE PRIMERO)
```bash
ssh root@72.60.245.66
cd /opt/sigma/SigmaBack
mkdir -p backups

# Backup automático con timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
docker exec -i gym-mysql mysqldump -u gymuser -pgympass \
  --routines --triggers --single-transaction --no-tablespaces \
  gymdb > backups/gymdb_backup_${TIMESTAMP}.sql && gzip -9 backups/gymdb_backup_${TIMESTAMP}.sql

# Limpiar backups viejos (mantener últimos 5)
cd backups && ls -t gymdb_backup_*.sql.gz | tail -n +6 | xargs rm -f && cd ..

# Ver backups
ls -lh backups/gymdb_backup_*.sql.gz
```

---

## 📊 MIGRACIÓN SQL (Solo si es la primera vez)

### **4.a - Asistencia**
```bash
docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb << 'EOF'
ALTER TABLE reservations ADD COLUMN attended TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE reservations ADD COLUMN attended_at DATETIME NULL;
CREATE INDEX idx_reservations_attended_date ON reservations(attended, date);
EOF
```

### **4.b - Pausas por fecha**
```bash
docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb << 'EOF'
ALTER TABLE schedules ADD COLUMN paused_dates VARCHAR(1000) NULL COMMENT 'CSV de fechas pausadas YYYY-MM-DD';
EOF
```

### **4.c - Progresión de ejercicios (NUEVA)**
```bash
docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb << 'EOF'
CREATE TABLE IF NOT EXISTS exercise_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exercise_id BIGINT NOT NULL,
  weight DOUBLE NOT NULL,
  sets INT,
  reps INT,
  recorded_at DATETIME NOT NULL,
  notes VARCHAR(500) NULL,
  CONSTRAINT fk_exercise_id FOREIGN KEY (exercise_id) REFERENCES ejercicios(id) ON DELETE CASCADE,
  INDEX idx_exercise_recorded (exercise_id, recorded_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
EOF
```

### **4.d - Comentarios en ejercicios (miembro / entrenador)**
```bash
docker exec -i gym-mysql mysql -u gymuser -pgympass gymdb << 'EOF'
ALTER TABLE ejercicios 
  ADD COLUMN member_comment VARCHAR(2000) NULL,
  ADD COLUMN trainer_comment VARCHAR(2000) NULL;
EOF
```

---

## 🚀 DEPLOY BACKEND

En local:
```bash
./deploy.sh
scp deploy_*.tar.gz root@72.60.245.66:/tmp/
```

En servidor:
```bash
cd /tmp && tar -xzf deploy_*.tar.gz && cd deploy_* && ./install-fix.sh
```

Ver logs:
```bash
docker logs gym-backend -f
```

---

## ✅ ENDPOINTS NUEVOS

**Progresión de ejercicios:**
- `GET /api/exercises/{id}` → devuelve `weight`, `previousWeight`, `progressPercentage`, `progressHistory`
- `GET /api/exercises/{id}/progress` → histórico completo
- `PUT /api/exercises/{id}` → actualiza y guarda progreso automáticamente

**Pausar día completo:**
- `PUT /api/turnos/pause-day?date=YYYY-MM-DD` → pausa todos los horarios
- `DELETE /api/turnos/pause-day?date=YYYY-MM-DD` → despausa todos