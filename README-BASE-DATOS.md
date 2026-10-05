# SENNIAF Match - Base de datos PostgreSQL

## 1. Iniciar PostgreSQL

Requiere Docker Desktop.

```bash
docker compose up -d
```

Esto crea:
- Base de datos: `senniaf_match`
- Usuario: `postgres`
- Contraseña de desarrollo: `postgres`
- Puerto: `5432`

## 2. Ejecutar Spring Boot

```bash
mvn spring-boot:run
```

Spring Boot crea/actualiza las tablas mediante JPA/Hibernate (`ddl-auto=update`).

## 3. Datos de funcionarios para la demo

Al iniciar por primera vez se crean si no existen:

- `TS-001` -> `TRABAJADOR_SOCIAL`
- `PS-014` -> `PSICOLOGO`
- `CM-001` -> `COMITE`

Contraseña de desarrollo: `Cambio123!`

Se puede cambiar con la variable de entorno `SENNIAF_DEMO_PASSWORD`.

## 4. Familias

El registro de familia ahora guarda en PostgreSQL:

- nombre
- apellido
- cédula
- correo
- hash BCrypt de contraseña
- estado de verificación
- código de verificación temporal (también almacenado como hash)
- rol

La contraseña original nunca se guarda.

## 5. Perfiles

Los perfiles de niños y familias también se guardan en PostgreSQL. Las listas de habilidades, gustos, personalidad, alergias, condiciones de salud y redes de apoyo usan tablas auxiliares generadas por JPA.

## 6. Importante

Esta configuración es para desarrollo/demostración. Para producción se deben usar credenciales fuera del repositorio, HTTPS, control de acceso real, gestión de sesiones/tokens, backups y una política de protección de datos adecuada.
