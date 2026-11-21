# Sprint 9 — despliegue consolidado del 21 de noviembre de 2026

**Estado: desplegado y verificado en Azure.** Aplicación: https://senniafmatch.westus2.cloudapp.azure.com/ . Inicio público: https://senniafmatch.westus2.cloudapp.azure.com/inicio.html .

## Estado recuperado y trabajo realizado

Al retomar, los cambios de aplicación ya estaban confirmados en `mejora/sprint9`: correcciones lingüísticas `07c7132`, Inicio público `c40b08c`, mediciones `9461de5`, backend de citas `ffc2759` e interfaz/integración `8628cea`, conservando sus antecesores de vinculaciones. El único archivo pendiente era la referencia preexistente del usuario `docs/image.png`; no se modificó ni incorporó.

Se comprobó directamente la VM antes de intervenir: ejecutaba `78a2ee0e5d8d03ae14726b7560c5c4e3b9e4dc12`, SHA-256 `b9e4d1021ca70c2413352e444f4db34b64c86d68e4668936785a89bf21cdf562`, motor `EVIDENCIA_TEXTO_6`; 012–019 completas y 020 ausente. Servicio y PostgreSQL activos, Java 17 y 55 GiB disponibles. No se asumió que una interrupción de conversación equivaliera a una actualización fallida ni se volvió a ejecutar una migración ya aplicada.

Quedaban pendientes la validación ampliada, el artefacto consolidado, respaldos actuales, 020, configuración de horario y publicación. Se ejecutaron esos pasos y la comprobación posterior. No hubo cambios adicionales de código, funcionalidades, diseño, motor, fórmulas o pruebas. No se hizo push ni merge ni se modificaron otras ramas.

## Artefacto publicado

| Dato | Resultado |
| --- | --- |
| Commit de aplicación | `8628cea8b6101345529422329f8f3746cb099439` |
| Rama | `mejora/sprint9` |
| Artefacto | `senniaf-match-sprint9-consolidado-20261121-8628cea.jar` |
| SHA-256 local y remoto | `d19726ebcb44b5895ab350b596fc53006bc05b4b3774a19c3382cf1154fba067` |
| JAR activo | `/srv/senniaf/releases/d19726ebcb44b5895ab350b596fc53006bc05b4b3774a19c3382cf1154fba067.jar` |
| Recuperación conservada | `/srv/senniaf/releases/b9e4d1021ca70c2413352e444f4db34b64c86d68e4668936785a89bf21cdf562.jar` |
| Final de actualización | 21/11/2026, 14:21:06, America/Panama (19:21:06 UTC) |

Compilación con Java 17 después de las pruebas: `mvn -B package -DskipTests` y propiedades de nombre, commit, rama y árbol. Los metadatos declaran honestamente `modificado:docs/image.png-preexistente-excluido`: el código versionado estaba limpio, pero existía esa referencia no versionada y excluida del JAR. El paquete contiene Inicio, Citas y la clase del motor con `EVIDENCIA_TEXTO_7`; no contiene archivos `.env` ni la imagen excluida.

La sustitución se realizó **exclusivamente mediante `/usr/local/lib/senniaf/actualizar.sh`**, con su verificador revisado. Se verificaron las huellas de los cuatro archivos transferidos: JAR, actualizador, auxiliar y SQL. Se conservaron copias privadas de scripts y configuración anteriores; no se sustituyó manualmente el JAR activo. No fue necesario recuperar la versión anterior.

[Evidencia del artefacto](despliegue-sprint9-20261121/artefacto.json), [resultado de actualización](despliegue-sprint9-20261121/despliegue.json), [estado previo](despliegue-sprint9-20261121/estado-inicial.json).

## Pruebas previas

| Grupo | Ejecutadas | Satisfactorias | Fallidas | Errores | Omitidas |
| --- | ---: | ---: | ---: | ---: | ---: |
| Java unitarias `*Test` | 1051 | 1051 | 0 | 0 | 0 |
| Java integración HTTP/PostgreSQL `*IT` | 77 | 77 | 0 | 0 | 0 |
| Python, compatibilidad del actualizador | 13 | 13 | 0 | 0 | 0 |
| **Total final** | **1141** | **1141** | **0** | **0** | **0** |

La línea base anterior de 1082 se amplió con las restantes suites PostgreSQL existentes. Las nueve clases de integración incluyen Citas (18), HU19, HU11–HU14, cierres, combinaciones rechazadas y Sprint6 E2E. Se conservan todas las pruebas anteriores.

Entorno: Java 17, PostgreSQL 16.4 aislado en loopback, base `hu19_citas_sprint_nueve`, usuario real de aplicación `senniaf_app`, exclusivamente datos sintéticos. La primera invocación ampliada detectó dos omisiones de preparación: puerto de PostgreSQL local y contraseña explícita exigida por el inicializador de cuentas ficticias. Se corrigieron únicamente el arranque y las variables del entorno de pruebas; no se cambiaron código ni expectativas para ocultar errores. Las suites afectadas se volvieron a ejecutar y finalizaron satisfactoriamente.

Secuencia: `mvn -B verify -Pe2e` completó las unitarias; `mvn -B failsafe:integration-test failsafe:verify` completó ocho clases de integración con el puerto correcto; `mvn -B -Dit.test=Sprint6E2EIT failsafe:integration-test failsafe:verify`, con la contraseña sintética explícita, completó los diez escenarios E2E restantes. Además: las 13 pruebas Python, `bash -n` y `git diff --check`. La evidencia final combina los XML de las suites, sin duplicar reintentos ni sumar el informe antiguo de Citas en Surefire.

Cubren motor y resultados históricos, autenticación/registro y roles, citas completas, mensajes editables, duraciones, fechas, horarios, solapamientos, permisos, duplicados, versiones y concurrencia. Las capturas y el recorrido real de navegador sobre datos sintéticos se conservaron en [CITAS_SPRINT9.md](CITAS_SPRINT9.md); el JAR publicado lleva esos mismos recursos y código.

[Conteos por clase](despliegue-sprint9-20261121/pruebas.json).

## Respaldo y recuperación

Antes de cualquier modificación del esquema se creó un respaldo reciente con el procedimiento existente; se comprobó la lectura completa y se descargó desde el contenedor privado para contrastar SHA-256. Con el backend detenido se generó un respaldo adicional consistente inmediatamente antes de 020. El actualizador generó además su propio respaldo después de 020 y antes de activar el JAR.

| Respaldo | SHA-256 | Verificación |
| --- | --- | --- |
| **Premigración:** `/var/backups/senniaf/20261121T192050Z.LAcuLv.dump` | `c7d19a44504c259c097a4424b1b1f5dc1327b5975a9f23915d978ea62c58572b` | Lectura completa local y de descarga Blob; huellas iguales |
| Actualizador: `/var/backups/senniaf/20261121T192051Z.8SH58q.dump` | `6dc5657ffd3904f4d74b63364714f99d41afc3fb2618fc34c827aa322eb39967` | Carga privada confirmada y lectura completa del archivo local |

Blob premigración: `backups/postgres/20261121/20261121T192050Z.LAcuLv.dump`, cuenta `stsenniafpbi279aa1635a`; acceso privado por identidad administrada, sin claves publicadas. No se sobrescribieron respaldos anteriores ni se restauró o reinicializó ninguna base.

Se comprobó previamente con el auxiliar que el JAR anterior admite el esquema aditivo 020, que el candidato nuevo se rechaza sin 020 y se acepta con 020. El procedimiento conserva el JAR anterior intacto por SHA. Recuperar código no elimina las tablas de citas ni restaura automáticamente datos; el código anterior no presenta las pantallas nuevas.

[Evidencia de descarga y lectura completa](despliegue-sprint9-20261121/respaldo-premigracion-externo-verificado.json), [compatibilidad de recuperación](despliegue-sprint9-20261121/compatibilidad-previa.json).

## Migración 020 y configuración

Se inspeccionó el SQL exacto, huella `b5303e5a9a1276c1792fafad52fcf13ca202a724a19d7a9d04fcafc3816477f8`. Solo crea `citas_vinculacion`, `citas_historial`, índices, restricciones, función de historial inmutable y permisos de esos objetos. No actualiza ni elimina filas, columnas o tablas existentes.

Se aplicó **una sola vez**, con backend detenido y transacción explícita, mediante el procedimiento manual PostgreSQL documentado: `psql -X -v ON_ERROR_STOP=1`, base autorizada `senniaf_demo` y confirmación `CREAR_CITAS_SIN_ALTERAR_HISTORICOS`. Terminó en **COMMIT**. Las comprobaciones 012–019 siguen dando `t` y la comprobación completa de 020 pasó de `f` a `t`.

El proyecto **no dispone de Flyway/Liquibase ni tabla automática de historial de migraciones**. No se creó un mecanismo nuevo. La ejecución queda registrada en `/var/backups/senniaf/operacion-sprint9-20261121-8628cea/migracion020.log`, junto con la huella del SQL y las verificaciones de catálogo. No se presenta ese log como si fuera un registro automático en la base.

Se añadieron únicamente `SENNIAF_CITAS_FRANJAS` y `SENNIAF_CITAS_INTERVALO_MINUTOS` a la configuración privada existente: lunes–viernes 08:00–16:00, intervalos de 30 minutos. America/Panama y duración predeterminada de 120 minutos confirmados en las opciones de API. Es el horario provisional del prototipo, **no un horario oficial de SENNIAF**. Se conservaron todos los demás valores de configuración, credenciales y medidas de seguridad, con copia privada recuperable del archivo previo.

## Comprobaciones en Azure

- HTTPS válido; recursos y nueve enlaces públicos comprobados con respuesta correcta. `/inicio.html`, logotipo, cuatro roles y ancla «Conocer el sistema» funcionan. [Captura pública real](despliegue-sprint9-20261121/inicio-azure.png).
- Login real de las cuatro cuentas autorizadas, paneles renderizados y sesiones de comprobación cerradas. Formularios de login y registro accesibles; **no se envió un registro ni un correo en producción**. El registro completo se probó en la base aislada.
- Comité consultó el análisis y recibió `EVIDENCIA_TEXTO_7` en el ranking. Pesos, programaciones e históricos mantienen sus huellas anteriores.
- Familia dispone de **Mis citas**, y Trabajo Social de **Gestión de citas**. Listados y opciones reales responden 200, con configuración habilitada; comprobación de escritorio y móvil sin errores JavaScript observados ni desbordamiento horizontal.
- La familia disponible para la comprobación no tenía una vinculación habilitada para abrir una nueva solicitud. El botón permanece deshabilitado conforme a permisos/estado; **no se forzó el formulario ni se modificó una vinculación para probarlo**. Los selectores, plantillas y todos los cambios de estado se comprobaron previamente en navegador y PostgreSQL aislados, no se declaran ensayados sobre expedientes de producción.
- Matriz autenticada de permisos: Citas admite Familia/Trabajo Social y devuelve 403 a Comité/Psicología; criterios del motor solo admiten Comité; expedientes infantiles rechazan Familia y admiten los roles profesionales autorizados. Citas sin sesión devuelve 401. Aislamiento entre distintas familias/profesionales y concurrencia cubiertos por integración local, sin usar cuentas ajenas en producción.
- `senniaf.service` y PostgreSQL activos; PostgreSQL 16.15 conserva `/var/lib/postgresql/16/main`. Sin errores críticos detectados en el journal desde la intervención. El reinicio tuvo las respuestas transitorias esperables hasta que el actualizador obtuvo tres comprobaciones de salud consecutivas.
- El usuario `senniaf_app` no puede actualizar decisiones, evaluaciones ni historial de citas. No se ampliaron permisos sobre tablas históricas.

[Navegador real](despliegue-sprint9-20261121/navegador-azure.json), [permisos por rol](despliegue-sprint9-20261121/permisos-azure.json), [estado y esquema de la VM](despliegue-sprint9-20261121/servidor-verificado.json).

## Conservación de datos

Se compararon conteos y huellas deterministas de **las 20 tablas anteriores**, dentro de la ventana con backend detenido y nuevamente después de las comprobaciones. Todas idénticas: 11 usuarios, seis expedientes infantiles, tres perfiles familiares, tres vinculaciones, seis decisiones, nueve evaluaciones, 40 eventos de auditoría y el resto de relaciones, pesos y programaciones intactos. La huella incluye todas las columnas, también credenciales y campos históricos, sin publicar sus valores.

Las dos tablas nuevas permanecen vacías. Se excluyó únicamente `sesiones_acceso`, porque los inicios/cierres de sesión de verificación alteran esa tabla operativa; no se excluyeron usuarios ni auditorías. No se crearon o modificaron citas, expedientes, decisiones o autorizaciones reales.

[Comparación en la ventana de publicación](despliegue-sprint9-20261121/integridad-ventana-despliegue.json), [verificación final de las 22 tablas](despliegue-sprint9-20261121/integridad-final.json).

## Rendimiento puntual, sin carga

Mismo servidor y mismo caso existente, con una familia candidata. Una ejecución de calentamiento y tres mediciones secuenciales HTTPS por versión; sin solicitudes concurrentes ni pruebas de carga.

| Medida | Antes | Después |
| --- | ---: | ---: |
| Mediana del endpoint de ranking, incluyendo red/TLS | 475,22 ms | 504,91 ms |
| Muestras posteriores | — | 504,91 / 521,08 / 499,69 ms |
| Consulta inicial del listado de tres casos | 574,51 ms | 926,84 ms |

En navegador, la primera carga visible de Compatibilidad del Comité tardó **4,81 s**, incluidos navegación, recursos, solicitudes secuenciales y renderizado; su petición de casos tardó 1,11 s. No se confunde ese recorrido con el tiempo de cálculo del ranking. Los GET de Citas observados en navegador tardaron 156–195 ms. No se afirma que toda la experiencia sea instantánea ni se atribuyen estas diferencias exclusivamente al algoritmo.

Lecturas PostgreSQL mediante `EXPLAIN ANALYZE`, sin devolver expedientes: perfiles familiares 0,026 ms, perfiles infantiles 0,038 ms y agenda confirmada 0,032 ms, excluida planificación de 0,275–0,473 ms. Son consultas puntuales de esas tablas, **no una medición desglosada de todo el SQL del motor**. La agenda estaba vacía y el conjunto actual es pequeño. Tres muestras no permiten establecer P95 fiable, escalabilidad o una regresión estadística; no justifican una optimización/refactorización en este cierre.

[Antes](despliegue-sprint9-20261121/http-antes.json), [después](despliegue-sprint9-20261121/http-despues.json), [tiempos SQL](despliegue-sprint9-20261121/sql-tiempos.json).

## Git y evidencias privadas

La aplicación desplegada corresponde al commit `8628cea`; el commit posterior de este informe solo añade documentación y evidencias sanitizadas. Rama final `mejora/sprint9`, sin cambios de aplicación pendientes. `main` y `origin/sprint8-pruebas` conservan respectivamente `2981b24705a81ab1e815c2930e45f6570622020b` y `b6192be75bf857eb64750dfce2eaf168bfcf8328`. Único archivo no versionado conservado: `docs/image.png`.

Registros operativos completos y configuración previa permanecen privados en `/var/backups/senniaf/operacion-sprint9-20261121-8628cea` y en el directorio privado del operador fuera de Git. Los archivos adjuntos a este informe contienen únicamente metadatos, estados, conteos, huellas agregadas y tiempos; no contienen tokens, contraseñas, mensajes familiares ni expedientes.
