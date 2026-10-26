# SENNIAF Match - Sprint 6

Prototipo académico de compatibilidad de perfiles en procesos de adopción. **No es un sistema oficial de SENNIAF y no decide adopciones.** Usar solo datos sintéticos.

## Diseño visual adaptado

Se aplicó la referencia `DESIGN.md` entregada por el equipo, inspirada en el lenguaje visual de Calendly, sin utilizar marcas, componentes ni servicios de terceros:

- Azul marino `#0b3558` para texto, azul de acción `#006bff`, fondo `#f8f9fb` y tarjetas blancas.
- Bordes finos `#d4e0ed`, tarjetas de 24 px, campos y botones de 8 px, sombras de tinte azul.
- Pantalla de acceso editorial con recorrido informativo, paneles institucionales y formularios responsivos.
- Estilos coherentes en `index.html`, `funcionario.html`, `perfiles.html`, `perfil.html` y `match.html`.
- CSS compartido `calendly-theme.css` y estilos específicos de inicio `calendly-login.css`.

Se utiliza la pila de fuentes Manrope / Inter / Segoe UI, con alternativas del sistema para que la interfaz funcione también sin conexión. Los cambios visuales no alteran las rutas HTTP, los permisos ni los procedimientos del Sprint 6.

## Requisitos

- JDK 21 recomendado (código compatible con Java 17).
- Maven 3.8 o superior.
- PostgreSQL 16 con Docker Compose.

## Puesta en marcha

```bash
docker compose up -d
mvn clean test
mvn clean verify -Pe2e
mvn spring-boot:run
```

Abrir `http://localhost:8080`. El proyecto usa PostgreSQL en `localhost:5432` por defecto.

| Usuario de demostración | ID | Contraseña inicial |
| --- | --- | --- |
| Trabajador social | `TS-001` | `Cambio123!` |
| Psicólogo | `PS-014` | `Cambio123!` |
| Comité | `CM-001` | `Cambio123!` |

La variable `SENNIAF_DEMO_PASSWORD` cambia la contraseña de estos usuarios antes de su creación. El registro de familias muestra un código de verificación en pantalla porque **no está conectado a un servicio de correo real**.

## Qué incorpora el Sprint 6

- Profesionales: registro, consulta de casos propios, edición, baja lógica y auditoría.
- Familias: una entrevista por cédula; posibilidad de corregirla sin crear otro expediente.
- Comité: ranking de cinco factores, pesos configurables que suman 100, decisiones motivadas y trazabilidad PostgreSQL.
- Reevaluación: pausa temporal en el ranking y actualización familiar. Desde la tercera solicitud se presenta un **aviso de seguimiento humano, sin penalización automática**.
- Seguridad: sesiones aleatorias de ocho horas, hash SHA-256 del token en la base de datos y autorización validada por el servidor, sin confiar en cabeceras de identidad enviadas por el navegador.

Pesos académicos iniciales: salud 35, vínculos entre hermanos 25, edad 20, apoyo 15 y logística 5. Nunca sustituyen el criterio humano o la idoneidad legal.

## Pruebas y evidencias

- `src/test/java/com/senniaf/match/Sprint6E2EIT.java` contiene ocho escenarios HTTP + PostgreSQL.
- `.github/workflows/maven.yml` configura JDK 21, PostgreSQL 16 y reportes de Maven.
- `docs/Sprint6_VALIDACION.md` explica las pruebas, las cinco comprobaciones de Burp Repeater y las evidencias que hay que anexar.

Los resultados de Burp, GitHub Actions y los tiempos de respuesta se deben obtener mediante ejecuciones reales. La aplicación no está lista para datos institucionales.
