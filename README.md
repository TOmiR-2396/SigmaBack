# Gym Backend (Spring Boot + MySQL)

Estructura mínima para que **la base de datos funcione** con JPA/Hibernate y MySQL.

## Requisitos
- Java 17+
- Maven 3.9+
- Docker (opcional, para levantar MySQL rápido)

## Arranque rápido (con Docker para MySQL)
```bash

INICIAR DATABASE CON --> docker compose up -d
INGRESAR A DATABASE CON --> mysql -h 127.0.0.1 -P 3306 -u gymuser -p

export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=gymdb
export DB_USER=gymuser
export DB_PASSWORD=gympass

INICIAR EL BACKEND CON --> mvn spring-boot:run
```

La app se inicia en `http://localhost:8080`. Hibernate creará/actualizará el esquema automáticamente (`spring.jpa.hibernate.ddl-auto=update`).

## Módulos actuales
- **Entidades**: `Member`, `MembershipPlan`, `Subscription`, `Attendance`
- **Repositorios**: interfaces `JpaRepository` para CRUD básico
- **Configuración**: `application.yml` usa variables de entorno para credenciales

## Siguientes pasos sugeridos
- Agregar controladores REST
- DTOs y validaciones
- Seguridad (Spring Security / JWT)
- Migraciones con Flyway o Liquibase si prefieres control de esquema
```

