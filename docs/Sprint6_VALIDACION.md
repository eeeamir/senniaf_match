# Sprint 6 - Validación integral y evidencias

Guía para el prototipo académico. Los códigos HTTP son **resultados esperados**; deben confirmarse sobre una instalación real y acompañarse de capturas anonimizadas.

## Ejecutar las pruebas

```bash
docker compose up -d
mvn clean verify -Pe2e
mvn spring-boot:run
```

La suite `Sprint6E2EIT` requiere PostgreSQL en `localhost:5432` y usa expedientes sintéticos. Al finalizar Maven, consultar `target/surefire-reports` y `target/failsafe-reports`.

| Escenario | Qué comprueba |
| --- | --- |
| E2E01 | Registro, verificación, entrevista y persistencia de familia |
| E2E02 | Alta, edición y auditoría de un caso infantil propio |
| E2E03 | Ranking reproducible y desglose explicable |
| E2E04 | Exclusión de perfil infantil incompleto |
| E2E05 | Tres reevaluaciones, correcciones, alerta y ausencia de sanción automática |
| E2E06 | Cinco controles HTTP de seguridad |
| E2E07 | Rechazo de pesos que no suman 100 |
| E2E08 | p50 y p95 de 15 consultas al ranking |

La prueba E2E08 emite una línea `SPRINT6_LATENCIA_RANKING_MS` con tiempos del entorno de prueba. No mide la duración de un proceso real de adopción.

## Cinco pruebas de seguridad con Burp Repeater

1. Ejecutar la aplicación con PostgreSQL y acceder desde el navegador configurado con el proxy de Burp Suite Community.
2. Capturar una solicitud a `/api/scoring/casos` o `/api/ninos/{codigo}` y escoger **Send to Repeater**.
3. Cambiar solo la cabecera indicada y pulsar **Send**.
4. Comparar resultado esperado y obtenido. Guardar evidencia sin tokens ni información personal.

| ID | Petición modificada | HTTP esperado |
| --- | --- | --- |
| B1 | GET `/api/scoring/casos` sin `Authorization` | 401 |
| B2 | GET `/api/scoring/casos` con sesión `FAMILIA` | 403 |
| B3 | GET `/api/ninos/{codigo}` con la sesión de otro profesional | 403 |
| B4 | GET `/api/scoring/casos` con token `TS-001` más `X-User-Role: COMITE` y `X-User-Id: CM-001` | 403 |
| B5 | GET `/api/scoring/casos` con token Bearer falso | 401 |

Documentación oficial: https://portswigger.net/burp/documentation/desktop/tools/repeater

## Matriz de evidencias

| Evidencia | Qué se debe adjuntar | Estado |
| --- | --- | --- |
| JUnit | Resumen y reportes XML de Maven | Pendiente de ejecución |
| E2E PostgreSQL | Reporte Failsafe y capturas de tablas | Pendiente de ejecución |
| Burp B1–B5 | Cinco pares de peticiones/respuestas anonimizadas | Pendiente de ejecución |
| CI | URL a una ejecución de GitHub Actions y artefacto de reportes | Pendiente de ejecución |
| Rendimiento | Salida con p50 y p95 | Pendiente de ejecución |
| Sesgos | Revisión humana de siete parejas de perfiles sintéticos | Pendiente de revisión |

## Reevaluación y legislación

La Ley 46 de 17 de julio de 2013, publicada por el Órgano Judicial de Panamá, regula la declaración de idoneidad en el artículo 82 y las asignaciones y revocaciones en los artículos 86 a 88. En esas disposiciones no se encontró sanción automática por tres reevaluaciones del formulario familiar. Por ello, al tercer evento se muestra una alerta de seguimiento al equipo técnico, sin bloqueo permanente ni reducción del puntaje. Toda sanción o procedimiento adicional requiere verificar una norma o protocolo oficial.

Fuente: https://www.organojudicial.gob.pa/uploads/wp_repo/blogs.dir/cendoj/b3wq53i9ad-ley-46-de-17-de-julio-de-2013---general-de-adopciones-de-la-republica-de-panam%C3%81.pdf

**Limitación:** el código de verificación se entrega solo en modo demostración. No utilizar datos reales de menores o familias ni interpretar el scoring como una asignación legal.
