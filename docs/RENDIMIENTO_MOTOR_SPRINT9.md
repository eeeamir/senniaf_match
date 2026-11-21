# Validación puntual de rendimiento — Sprint 9

Fecha: 2026-11-21. Rama `mejora/sprint9`. Versiones comparadas: `78a2ee0e5d8d03ae14726b7560c5c4e3b9e4dc12` y `07c7132a366198565e9bff345a74ad29d191b4a9`.

**Resultado local del ranking, sin PostgreSQL:** 14–335 ms de mediana para 10–500 familias. El texto concentra el coste; el cálculo de puntuaciones es pequeño. **No es una medición integral de producción**: SQL, autenticación, HTTPS, red de Azure y carga simultánea real del servidor quedan sin comprobar. No se ha hecho ninguna optimización.

## Mediciones

| Familias evaluadas | Tiempo mediano | P95 | Consultas SQL | Memoria máxima |
|---:|---:|---:|---|---:|
| 10 | 14.26 ms | 19.47 ms | N/D (30 llamadas a repositorios) | 254.7 MiB RSS |
| 50 | 38.22 ms | 49.03 ms | N/D (110 llamadas a repositorios) | 261.2 MiB RSS |
| 100 | 73.23 ms | 108.44 ms | N/D (210 llamadas a repositorios) | 283.6 MiB RSS |
| 500 | 334.88 ms | 434.21 ms | N/D (1010 llamadas a repositorios) | 539.6 MiB RSS |

Tiempo del servicio `ScoringService.casos()`, que utiliza la vista del Comité. N/D significa **no medido**, no cero consultas: los contadores son de repositorios sintéticos, no de SQL. RSS es el máximo residente observado de la JVM completa en ambos procesos de cada tamaño; incluye framework, arnés, datos, análisis precalculado para la fase de puntuaciones, JIT y concurrencia. No es memoria incremental por petición ni consumo de Azure.

Mediana / P95, en milisegundos:

| Familias | Texto | Puntuaciones con análisis precalculado | Endpoint MVC | Endpoint con 4 solicitudes simultáneas | Vista local |
|---:|---:|---:|---:|---:|---:|
| 10 | 9.66 / 12.29 | 0.13 / 0.35 | 21.32 / 23.66 | 22.05 / 27.10 | 33.30 / 33.50 |
| 50 | 32.80 / 47.09 | 0.24 / 0.37 | 63.13 / 73.82 | 77.39 / 92.86 | 33.30 / 33.70 |
| 100 | 75.44 / 84.28 | 0.42 / 0.62 | 134.42 / 153.51 | 170.25 / 225.52 | 33.40 / 47.90 |
| 500 | 330.28 / 396.03 | 1.29 / 1.57 | 533.42 / 623.07 | 929.01 / 1329.25 | 123.10 / 143.70 |

Las fases de texto y puntuaciones se cronometran **por separado**, no instrumentando cada llamada del ranking. No deben sumarse ni interpretarse como un reparto exacto de su tiempo. La fase de puntuaciones conserva el método productivo completo y los metadatos, con análisis ya disponibles; no cambia fórmulas.

El endpoint usa el controlador real, despacho MVC y serialización mediante MockMvc; excluye socket HTTP, filtros de sesión y base de datos. Todas las solicitudes medidas finalizaron con estado 200. Con cuatro solicitudes simultáneas y 500 familias, cada una tuvo 929 ms de mediana y 1329 ms de P95; no hubo errores ni agotamiento de memoria en esta carga.

La vista usa Chromium headless y los archivos reales de estilos, componentes y `Comite.iniciar`, con sesión sintética y JSON previamente generado servido por HTTP en loopback. Se mide recepción/parseo del JSON, construcción del DOM y dos cuadros de renderizado, después de cargar los recursos. **No incluye generación del ranking**, arranque del navegador ni autenticación; no se suma al endpoint para inventar un tiempo extremo a extremo. En todas las 320 cargas medidas se observaron exactamente tres solicitudes distintas: casos, programación de pesos y reevaluaciones pendientes. No hubo duplicación del ranking dentro de esa carga inicial; no se extrapola esta comprobación a todas las navegaciones de la aplicación.

En 500 familias, el CPU del proceso durante el ranking promedió **367,83 ms por petición** (incluye JIT y GC), aproximadamente 1,07 núcleos equivalentes sobre los intervalos medidos. Se asignaron **222,56 MiB temporales por ranking**, La fase independiente de texto asignó 219,65 MiB. Estas cifras miden asignación, no retención; por sí solas no diagnostican una fuga. Los tamaños pequeños conservan actividad JIT significativa; no se interpreta su CPU como régimen estable. El CSV conserva CPU, asignaciones y picos de pools de heap por fase.

## Comparación controlada

Mismo host, JVM, dependencias, datos sintéticos y arnés; ejecuciones secuenciales, invirtiendo el orden de versiones en la segunda ronda. Los códigos, orden, puntuaciones y desglose del conjunto sintético coinciden entre versiones; se comprobó la comparabilidad de la carga, no se abrió otra auditoría funcional.

Mediana / P95 del ranking, en ms:

| Familias | Ranking 78a2ee0 | Ranking 07c7132 | Cambio mediano observado | Endpoint 78a2ee0 → 07c7132 (mediana) |
|---:|---:|---:|---:|---:|
| 10 | 11.32 / 16.75 | 14.26 / 19.47 | +26.0% | 14.77 → 21.32 ms |
| 50 | 37.69 / 51.16 | 38.22 / 49.03 | +1.4% | 60.39 → 63.13 ms |
| 100 | 61.46 / 72.44 | 73.23 / 108.44 | +19.2% | 94.81 → 134.42 ms |
| 500 | 302.35 / 374.74 | 334.88 / 434.21 | +10.8% | 517.60 → 533.42 ms |

Las correcciones no son gratuitas: en esta carga de 500 familias el ranking aumentó 10,8% de mediana; el endpoint aumentó 3,1%. No se atribuye toda diferencia exclusivamente al algoritmo: dos JVM por caso no eliminan variación de JIT, GC, planificación de WSL y carga del host. No se compararon estos tiempos locales con latencias de Azure.

## Ineficiencias comprobadas y propuestas, sin cambios aplicados

1. **Necesidades y recomendaciones: sin repetición por familia.** La instrumentación separada verificó una llamada a `necesidades`, N a `capacidades` y una consulta de repositorio de recomendaciones por ranking. No se recomienda añadir otra caché de necesidades ni una caché global con riesgo de datos desactualizados.

2. **Accesos de repositorio repetidos:** N comprobaciones de reserva familiar y N consultas de antecedentes de interrupción; con 500 familias son 1000 llamadas, más 10 accesos en el arnés. También se comprueba dos veces la reserva del mismo niño. El patrón existe en ambas versiones. Es un patrón N+1 de accesos en el servicio; **su número real de sentencias SQL y duración no están medidos**, ni las consultas adicionales de las colecciones EAGER. Su impacto potencial es espera del Comité por acceso a datos, no cambio de puntos. Propuesta: medir primero con PostgreSQL aislado; si el coste lo justifica, obtener reservas/antecedentes en lote y reutilizar el estado infantil dentro de la petición. Riesgo: conservar exactamente exclusiones, frescura y reservas concurrentes; no introducir caché global.

3. **Compilación de regex durante el análisis:** el vocabulario y los patrones declarados estáticos se reutilizan. Persisten compilaciones por `String.matches/replaceAll` y normalizaciones repetidas. Un perfil JFR separado, después de calentamiento, encontró `Pattern.compile` en 225 de 2298 muestras y `String.replaceAll` en 610; son muestras inclusivas, pueden solaparse y no equivalen a tiempo exclusivo ni a número de compilaciones. La fase completa de texto consume 330 ms de mediana para 500 familias; **el ahorro de precompilar no se ha medido**. Propuesta secundaria: experimentar después con patrones fijos precompilados y reutilización de normalización dentro del análisis. Riesgo bajo si se conservan patrones/flags y se crean Matchers independientes, pero requiere verificar las pruebas existentes. No se promete una aceleración determinada.

4. **Volumen de respuesta:** 500 familias producen 4.674.413 bytes de JSON sin comprimir (4,46 MiB); las necesidades compartidas aparecen en las evidencias de cada candidato. Está medido el volumen, no el coste de transporte en producción ni la compresión del proxy. No se propone cambiar ahora el contrato del endpoint: el riesgo de alterar el desglose es mayor y falta medir el beneficio en red real. `findAll` de expedientes y colecciones EAGER requieren medir volumen SQL; no se declara que PostgreSQL esté leyendo datos innecesarios sin esa medición.

## Respuestas y conclusión

1. **Generación real medida:** 14,26 / 38,22 / 73,23 / 334,88 ms de mediana para 10 / 50 / 100 / 500 familias, en el servicio aislado. El tiempo completo con PostgreSQL está pendiente.
2. **Texto:** sí, es el principal coste de CPU en esta carga; la fórmula con análisis precalculado tarda 1,29 ms de mediana para 500 familias frente a 330,28 ms del texto. No se ha demostrado bloqueo del servicio en esta escala.
3. **Crecimiento:** aproximadamente proporcional desde 50 familias; 10 veces más familias (50 → 500) requieren 8,8 veces el tiempo mediano de ranking.
4. **Trabajo innecesario:** no hay reprocesamiento del niño por candidato; sí hay accesos repetidos y compilaciones de regex acreditados arriba. El coste SQL continúa desconocido.
5. **Interfaz:** render local 33–123 ms de mediana; no se detectó una demora grande del renderizado ni duplicación en la carga inicial medida. La demora percibida por el Comité en Azure no está certificada.
6. **Antes de cerrar Sprint 9:** estas cifras no justifican cambiar fórmulas ni permiten declarar imprescindible una optimización. La prioridad es medir el recorrido con PostgreSQL aislado y un umbral de respuesta acordado antes de certificar el rendimiento integral. Las posibles optimizaciones quedan propuestas, sin aplicar.

## Reproducción y límites

WSL2 Ubuntu, Intel Core i5-13500HX, 20 CPU lógicas visibles, 7799 MiB asignados a WSL; Java 17.0.21+9, G1, `-Xms128m -Xmx512m`. No es hardware dedicado ni la VM publicada. Un niño, una recomendación vigente y familias con distintas identificaciones y los mismos textos para controlar la carga: asma, diabetes, TDAH, alergia, terapia psicológica, adaptación, rutinas, apoyo emocional, supervisión, tareas y educación individualizada; opciones actuales y con apoyo. No representa todos los tamaños de historial ni longitudes de texto posibles.

Dos JVM independientes por versión/tamaño; 12 calentamientos por fase y 30 repeticiones medidas en cada JVM (60 muestras). Cuatro clientes simultáneos: 12 lotes por JVM, descartando los primeros dos (80 solicitudes medidas por versión/tamaño). Interfaz: dos rondas, cinco calentamientos y 20 repeticiones por ronda (40 muestras). P95 por rango más próximo: ordenación y posición `ceil(0,95 × n)`. Sin eliminación posterior de valores atípicos. JFR y espía de llamadas se ejecutaron separados de los tiempos publicados.

PostgreSQL no está instalado localmente y Docker no tiene integración con esta distribución WSL. No se provisionó otra base ni se accedió a Azure. Los repositorios en memoria devuelven perfiles sintéticos e historiales vacíos, con pesos vigentes leídos mediante la alternativa `pesosRepo`; se excluye el servicio de programación de pesos real. Falta medir JDBC/ORM, historial grande, toda la configuración de Spring, seguridad, HTTPS/red y recorrido integrado navegador–servidor–PostgreSQL. No se extrapolan los contadores del arnés al total real de SQL.

Fuentes verificables: [resumen.csv](rendimiento-sprint9/resumen.csv), [2560 muestras](rendimiento-sprint9/muestras.jsonl.gz), [320 cargas de interfaz](rendimiento-sprint9/interfaz.json), [contadores y perfil](rendimiento-sprint9/diagnostico.json), [JFR](rendimiento-sprint9/perfil-regex.jfr), [entorno](rendimiento-sprint9/entorno.json) y [SHA-256](rendimiento-sprint9/SHA256SUMS). El arnés manual está en `tools/rendimiento`; no se añade ninguna dependencia al producto ni suite JUnit.

```bash
export JAVA_HOME=/home/anyel/.local/share/senniaf-tools/jdk-17.0.21+9
export PATH="$JAVA_HOME/bin:$PATH"
python3 tools/rendimiento/ejecutar.py /tmp/nueva-medicion   --chromium /home/anyel/.cache/ms-playwright/chromium_headless_shell-1255/chrome-headless-shell-linux64/chrome-headless-shell
python3 tools/rendimiento/resumir.py /tmp/nueva-medicion /tmp/nuevo-resumen
```

No se modifican código productivo, ponderaciones, fórmulas, pantallas, expedientes, decisiones, historial ni otras ramas. No se hizo push, merge, despliegue ni prueba de carga de producción.
