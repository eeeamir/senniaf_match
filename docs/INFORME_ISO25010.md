# Informe de evidencias ISO/IEC 25010 — SENNIAF Match Sprint 7

## Resumen ejecutivo

Los XML actuales de Surefire registran **182 pruebas ISO**, de las cuales **165 aprobaron y 17 fallaron**, sin omitidas. La aprobación ISO es:

```text
165 / 182 × 100 = 90.7 %
```

El conjunto total de reportes Surefire disponibles —incluyendo pruebas originales— registra **209 pruebas, 190 aprobadas, 19 fallidas y 0 omitidas**. Esta cifra global no debe confundirse con la métrica exclusiva ISO.

## Métricas por característica

| Característica | Ejecutadas | Aprobadas | Fallidas/errores | Omitidas | Aprobación |
|---|---:|---:|---:|---:|---:|
| Adecuación funcional | 38 | 38 | 0 | 0 | 100.0 % |
| Fiabilidad | 15 | 15 | 0 | 0 | 100.0 % |
| Seguridad | 119 | 102 | 17 | 0 | 85.7 % |
| Eficiencia de desempeño | 10 | 10 | 0 | 0 | 100.0 % |
| **Total ISO** | **182** | **165** | **17** | **0** | **90.7 %** |

Fórmula usada: `aprobación = aprobadas ÷ ejecutadas × 100`.

## Adecuación funcional

Se aprobaron **38 de 38 casos (100.0 %)**. Se verificaron los cinco factores del motor de compatibilidad, rangos de edad, hermanos, salud, red de apoyo, logística, reevaluaciones y reglas de decisión.

## Fiabilidad

Se aprobaron **15 de 15 casos (100.0 %)**. Se comprobó determinismo: mismas entradas producen las mismas salidas.

## Seguridad

Se aprobaron **102 de 119 casos (85.7 %)**. La suite incluye control de roles, límite de intentos, scoring, familia y la matriz `/api/ninos`.

### Matriz `/api/ninos`

`NinosAccesoIsoTest` contiene **52 casos**: 13 endpoints × 4 roles (`TRABAJADOR_SOCIAL`, `PSICOLOGO`, `COMITE`, `FAMILIA`).

| Endpoint | Roles permitidos según código | Resultado HTTP esperado para rol no autorizado |
|---|---|---:|
| `GET /api/ninos` | Trabajo Social, Psicología, Comité | 403 |
| `POST /api/ninos` | Trabajo Social | 403 |
| `GET /api/ninos/{codigo}` | Trabajo Social, Psicología, Comité | 403 |
| `GET /api/ninos/{codigo}/auditoria` | Trabajo Social, Psicología, Comité | 403 |
| `PUT /api/ninos/{codigo}` | Trabajo Social | 403 |
| `DELETE /api/ninos/{codigo}` | Ninguno; baja directa bloqueada | 403 |
| `GET /api/ninos/{codigo}/evaluaciones-psicologicas` | Profesionales con expediente asignado; Comité | 403 |
| `POST /api/ninos/{codigo}/evaluaciones-psicologicas` | Psicología | 403 |
| `PUT /api/ninos/{codigo}/evaluaciones-psicologicas/{id}` | Psicología | 403 |
| `GET /api/ninos/solicitudes-estado/pendientes` | Comité | 403 |
| `GET /api/ninos/{codigo}/solicitudes-estado` | Profesionales con expediente asignado; Comité | 403 |
| `POST /api/ninos/{codigo}/solicitudes-estado` | Trabajo Social | 403 |
| `POST /api/ninos/solicitudes-estado/{id}/resolucion` | Comité | 403 |

### Clasificación de los 17 fallos de la matriz

Los 17 fallos son **opción (a): diseño de la prueba**, no opción (b). La prueba usa un `PerfilNinoService` mock; por ello no ejecuta las reglas que el servicio implementa realmente:

- evaluaciones solo para `PSICOLOGO`: `PerfilNinoService.java:162` y `176`;
- pendientes y resoluciones solo para `COMITE`: líneas `230` y `264`;
- solicitud de cambio solo para `TRABAJADOR_SOCIAL`: líneas `244-247`;
- asignación del expediente: líneas `114-125`.

Los casos afectados son: `SEG-NIN-28`, `29`, `31`, `32`, `33`, `35`, `36`, `37`, `38`, `40`, `44`, `46`, `47`, `48`, `49`, `50` y `52`. El mock permite que el controlador llegue al servicio simulado, por lo que la ausencia de excepción no demuestra un acceso indebido en producción.

### Expediente sintético de familia

`FamiliaExpedienteIsoTest` usa MockMvc y un expediente sintético con rol literal `FAMILIA`. Verifica `200` en:

- `GET /api/familias/mi-perfil`;
- `GET /api/familias/mi-perfil/detalle`.

También verifica que no se expongan `puntaje` ni datos de niños. La clase aparece en los XML actuales con **2 casos aprobados**.

## Eficiencia de desempeño

El código de `ScoringEficienciaIsoTest` usa `assertTimeoutPreemptively(Duration.ofMillis(1000), ...)`. El XML actual de eficiencia fue escrito a las **04:25:36 (-05:00)** y contiene estos tiempos `testcase/@time`:

| Caso | Lote | Tiempo XML |
|---|---:|---:|
| EFI-01 | 1 | 1463 ms |
| EFI-02 | 10 | 4 ms |
| EFI-03 | 25 | 4 ms |
| EFI-04 | 50 | 4 ms |
| EFI-05 | 100 | 5 ms |
| EFI-06 | 200 | 4 ms |
| EFI-07 | 300 | 6 ms |
| EFI-08 | 400 | 4 ms |
| EFI-09 | 500 | 4 ms |
| EFI-10 | 1000 | 5 ms |
| **Promedio** | — | **150.3 ms** |

Los diez casos están marcados como aprobados. EFI-01 registra 1463 ms en Surefire y aun así aprueba porque la aserción mide únicamente el cuerpo del cálculo (`s.candidato(...)`), mientras `testcase/@time` mide la invocación parametrizada completa, incluyendo inicialización, mocks y coste de la primera ejecución/JIT. Son métricas distintas; la prueba no afirma que el tiempo total de Surefire sea menor que 1000 ms.

## Fallos existentes fuera de ISO

El conjunto total de XML contiene 19 fallos: 17 de la matriz descritos arriba y 2 de pruebas originales.

1. `PerfilNinoControllerTest.marcaAlertaMedicaCuandoElPerfilDeclaraCondicionesDeSalud`: espera `201` usando `PSICOLOGO`, pero el controlador permite registrar únicamente a `TRABAJADOR_SOCIAL`. Clasificación: supuesto incorrecto de la prueba.
2. `ScoringReevaluacionServiceTest.solicitudDelComiteMarcaPendienteSinSumarUnaReevaluacionCompletada`: el mock configura `findByCodigoCaso`, pero producción usa `ninos.bloquear`. Clasificación: mock incompleto de la prueba.

## Entorno y limitaciones

- Java: OpenJDK 21.0.9.
- Maven: 3.9.9.
- PostgreSQL local: contenedor activo, 17 tablas públicas.
- `mvn -B test-compile`: `BUILD SUCCESS`.
- Las pruebas unitarias usan mocks y datos sintéticos.
- No se validó Azure ni infraestructura remota.
- Las credenciales se usaron solo como variables temporales y no se guardaron en archivos.

## Verificación de cambios

No se modificaron `src/main`, `pom.xml`, recursos ni la referencia `original_zip`. Los cambios se limitaron a las pruebas ISO y documentación bajo `docs/evidencias-iso25010/`.
