# SENNIAF Match — prototipo académico con datos sintéticos

Aplicación Spring Boot 3.3 / Java 17 (compatible como versión de compilación; recomendado JDK 21) / PostgreSQL. Implementa registro de familias, registro y gestión de casos de niños, scoring explicable y decisiones del Comité.

## Ejecutar localmente

1. Instala **JDK 21 (recomendado)** y Maven. Abre una terminal en esta carpeta; comprueba con `java -version` y `mvn -version` que ambos usen el mismo JDK.
2. Inicia Docker Desktop y ejecuta `docker compose up -d`.
3. Ejecuta `mvn clean test` y `mvn spring-boot:run`.
4. Abre `http://localhost:8080/index.html`.

**Cuentas de prueba** (solo habilitadas cuando `SENNIAF_DEMO=true`, valor predeterminado en este prototipo):

| Identificador | Rol | Contraseña de ejemplo |
|---|---|---|
| TS-001 | Trabajador social | Cambio123! |
| PS-014 | Psicólogo | Cambio123! |
| CM-001 | Comité | Cambio123! |

Se puede cambiar la contraseña mediante la variable `SENNIAF_DEMO_PASSWORD` antes del primer arranque. La creación de familias muestra un código de verificación **en pantalla**: **no se envían correos reales**.

## Cómo probar el flujo

1. Inicia sesión como TS-001 y elige **Ingresar nuevo perfil**. Completa la entrevista usando únicamente datos ficticios.
2. En **Ver perfiles registrados**, prueba **Editar**, **Ver auditoría** y **Eliminar**. Solo puedes gestionar perfiles creados por tu cuenta. Eliminar realiza una **baja lógica** auditada; los casos dados de baja no se muestran ni se puntúan.
3. Registra una o varias familias sintéticas en el inicio; verifica el código mostrado y envía la entrevista familiar. Cada cédula solo puede tener una entrevista.
4. Inicia sesión como CM-001 y abre el panel del Comité. Selecciona un caso, configura pesos (suman 100) y revisa el ranking. Los perfiles incompletos no participan. El sistema no toma decisiones automáticamente.
5. En un candidato, selecciona **Aceptar**, **Descartar** o **Solicitar reevaluación** y escribe un motivo (10 caracteres mínimo). La decisión se guarda en PostgreSQL junto con la versión de los pesos y el puntaje.
6. Las familias solo ven información sobre su propio proceso; no acceden al ranking ni a los perfiles infantiles.

## Reglas de seguridad implementadas

- La API usa tokens aleatorios de sesión con hash SHA-256 en base de datos y caducidad de 8 horas. No se confía en cabeceras `X-User-Role` ni `X-User-Id`; solo cuenta la identidad autenticada.
- Roles diferenciados: `FAMILIA`, `TRABAJADOR_SOCIAL`, `PSICOLOGO`, `COMITE`.
- Propiedad del caso verificada en el servidor. Operaciones de crear, editar, dar de baja, cambiar pesos y decidir generan auditoría.
- Los tokens residen en `sessionStorage` para este prototipo, se revocan al cerrar sesión y solo deben utilizarse bajo HTTPS en un despliegue real.

## Límites del prototipo

**Este trabajo NO está habilitado para información real de niños o familias**, ni para adopciones productivas. Faltan requisitos institucionales como emisión de correos real, autorización formal de perfiles idóneos, gobierno de pesos, evaluación multidisciplinaria, protección de infraestructura y auditorías verificables. Los pesos son una hipótesis académica configurable, **no representan criterios aprobados por SENNIAF**. Aspectos como cultura, personalidad, talentos, historia familiar, contexto y vínculos se revisan cualitativamente y no se convierten en una puntuación simplista.

Se conservó `spring.jpa.hibernate.ddl-auto=update` para desarrollo. Para producción se requieren migraciones formales, controles adicionales y revisión institucional. No publiques el servicio con las claves de ejemplo; usa PostgreSQL con credenciales seguras y HTTPS.

## Pruebas unitarias y error Mockito con Java 26

La edición original fallaba con `Mockito cannot mock this class` al simular `AuditoriaService` y `AutenticacionService` bajo Java 26. En esta revisión los tests usan dobles explícitos para dichos servicios y Mockito solo para los repositorios (interfaces), seleccionando `mock-maker-proxy` en `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`. Así se evita la instrumentación inline de clases que causaba los 17 errores reportados, sin cambiar la lógica productiva.

**Entorno recomendado:** JDK 21 LTS, Maven 3.9+, Docker Desktop y PostgreSQL para iniciar la aplicación. El `pom.xml` conserva `<java.version>17</java.version>` como nivel de compilación; GitHub Actions ejecuta las pruebas con JDK 21.

En PowerShell, dentro de la carpeta que contiene `pom.xml`:

```powershell
java -version
mvn -version
mvn clean test
```

Si `mvn -version` sigue mostrando Java 26, el `JAVA_HOME` de la terminal aún apunta al JDK equivocado. Configúralo al directorio instalado del JDK 21, abre otra terminal y vuelve a comprobar.

**Nota:** el cambio elimina el mocking inline del conjunto actual de tests, pero no implica que todas las dependencias de Spring Boot 3.3 estén verificadas bajo Java 26. Para una ejecución reproducible usa JDK 21.
