# 🔧 Configuración del Proyecto

## 📋 Archivos de Configuración Requeridos

### 1. application.yml

Copia el archivo `application.yml.template` a `application.yml` y configura las siguientes variables:

```bash
cp src/main/resources/application.yml.template src/main/resources/application.yml
```

### 2. Variables a Configurar

#### JWT Secret
- Genera una clave segura para JWT
- Reemplaza `TU_JWT_SECRET_AQUI` con tu clave

#### Resend API Key
- Obtén tu API key de [Resend](https://resend.com)
- Reemplaza `TU_RESEND_API_KEY_AQUI` con tu clave

## 🚀 Para Desarrollo Local

1. Copia el template:
   ```bash
   cp src/main/resources/application.yml.template src/main/resources/application.yml
   ```

2. Configura tus claves en `application.yml`

3. Ejecuta la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```

## 🐳 Para Producción con Docker

1. Configura las variables de entorno en tu servidor
2. Usa el perfil `prod`:
   ```bash
   SPRING_PROFILES_ACTIVE=prod docker-compose up -d
   ```

## ⚠️ Notas Importantes

- **NUNCA** subas `application.yml` al repositorio
- Usa variables de entorno en producción
- Mantén las claves seguras y privadas

## 🔐 Variables de Entorno para VPS

```bash
# Base de datos
DB_HOST=localhost
DB_PORT=3306
DB_NAME=gymdb_prod
DB_USER=gym_user_prod
DB_PASSWORD=tu_password_seguro

# JWT
JWT_SECRET=tu_jwt_secret_super_largo_y_seguro

# Email
RESEND_API_KEY=tu_resend_api_key

# Servidor
PORT=8080
SPRING_PROFILES_ACTIVE=prod
```