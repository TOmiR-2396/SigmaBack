# 🚀 Scripts de Despliegue SigmaBack VPS

Este directorio contiene scripts automatizados para gestionar el despliegue y administración de SigmaBack en el VPS.

## 📋 Scripts Disponibles

### 1. `deploy-to-vps.sh` - Despliegue Completo
Script principal con confirmaciones y validaciones completas.

```bash
./deploy-to-vps.sh
```

**Características:**
- ✅ Verificación de cambios sin commit
- ✅ Confirmación interactiva
- ✅ Compilación local previa
- ✅ Backup automático del JAR anterior
- ✅ Logs detallados del proceso
- ✅ Verificación post-despliegue

### 2. `deploy-quick.sh` - Despliegue Rápido
Versión simplificada para despliegues rápidos sin confirmaciones.

```bash
./deploy-quick.sh
```

**Características:**
- 🚀 Commit automático con timestamp
- 🚀 Push inmediato a GitHub
- 🚀 Despliegue directo sin confirmaciones
- 🚀 Ideal para desarrollo iterativo

### 3. `vps-manage.sh` - Gestión del VPS
Script de administración para gestionar la aplicación en el servidor.

```bash
./vps-manage.sh [COMANDO]
```

**Comandos disponibles:**
- `status` - Ver estado de la aplicación
- `logs` - Ver logs en tiempo real
- `stop` - Detener aplicación
- `start` - Iniciar aplicación  
- `restart` - Reiniciar aplicación
- `ps` - Ver procesos Java
- `disk` - Ver uso de disco
- `backup` - Hacer backup del JAR
- `connect` - Conectar por SSH al VPS

## 🔧 Configuración del VPS

**Servidor:** srv1042314  
**Usuario:** root  
**Ruta del proyecto:** `/opt/sigma/SigmaBack`  
**Perfil de ejecución:** prod (MySQL)

## 📝 Flujo de Trabajo Recomendado

### Desarrollo Activo (cambios frecuentes):
```bash
# Hacer cambios en el código
# ...

# Despliegue rápido
./deploy-quick.sh
```

### Despliegue de Producción:
```bash
# Hacer cambios en el código
# ...

# Despliegue completo con validaciones
./deploy-to-vps.sh
```

### Monitoreo Post-Despliegue:
```bash
# Ver estado
./vps-manage.sh status

# Ver logs en tiempo real
./vps-manage.sh logs

# Si hay problemas, reiniciar
./vps-manage.sh restart
```

## 🛠️ Troubleshooting

### La aplicación no inicia:
```bash
# Ver logs detallados
./vps-manage.sh logs

# Verificar procesos
./vps-manage.sh ps

# Reiniciar
./vps-manage.sh restart
```

### Problemas de conectividad SSH:
```bash
# Conectar manualmente
./vps-manage.sh connect

# O directamente:
ssh root@srv1042314
```

### Backup antes de desplegar:
```bash
# Crear backup manual
./vps-manage.sh backup

# El deploy-to-vps.sh hace backup automático
```

## 📊 Monitoreo

### Verificar que la aplicación está funcionando:
```bash
./vps-manage.sh status
```

### Ver logs en tiempo real:
```bash
./vps-manage.sh logs
```

### Verificar uso de recursos:
```bash
./vps-manage.sh disk
./vps-manage.sh ps
```

## 🔒 Seguridad

- Los scripts usan SSH con autenticación por clave
- El perfil 'prod' usa MySQL con configuración segura
- Los backups se mantienen automáticamente
- Los logs se rotan automáticamente por Spring Boot

## ⚠️ Notas Importantes

1. **Perfiles de Spring:** La aplicación se ejecuta con `spring.profiles.active=prod`
2. **Base de datos:** En producción usa MySQL, no H2
3. **Puerto:** La aplicación corre en el puerto configurado en `application-prod.yml`
4. **Logs:** Se guardan en `/opt/sigma/SigmaBack/app.log`
5. **Backups:** Se crean automáticamente con timestamp antes de cada despliegue

## 🆘 Soporte

Si tienes problemas:

1. Revisa los logs: `./vps-manage.sh logs`
2. Verifica el estado: `./vps-manage.sh status` 
3. Intenta reiniciar: `./vps-manage.sh restart`
4. Si nada funciona, conecta al VPS: `./vps-manage.sh connect`