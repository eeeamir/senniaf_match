# Sprint 7 — estado de entrega

Fecha: 9 de noviembre de 2026, Panamá. Este documento describe el código entregado; no sustituye las evidencias privadas ni acredita pruebas que no se ejecutaron en esta publicación.

| PBI | Implementación conservada |
| --- | --- |
| 22 | Evaluaciones psicológicas independientes, versiones sin sobrescritura, autoría y fecha del servidor. Contexto estructurado de métodos, instrumentos y acompañamiento; diagnóstico opcional. |
| 23 | Mis expedientes y control por vinculación/especialidad. Sin reasignación desde los cuatro roles. Registro social con creador autenticado y referencia institucional opcional; referencias históricas preservadas. |
| 24 | Consulta infantil y familiar del Comité; sin permisos para modificar evaluaciones clínicas. |
| 25 | Solicitud justificada de retiro; solo la decisión del Comité cambia la participación. Cierre por adopción distinto de suspensión temporal. |
| 26 | Reactivación sujeta a elegibilidad y autorización del Comité. Seguimiento profesional conservado durante exclusión, sin retorno automático al Match. |
| 27 | Spring Boot, PostgreSQL y Nginx en la VM existente, HTTPS, secretos externos, respaldos privados y publicación por huella con reversión del JAR. |

## Evidencia y límites

La versión remota consultada corresponde al JAR `senniaf-match-cierre-sprint7-20261109`, SHA256 `33921e268adaafd12c646e45eaef8eea85a0213bcea66c5fd42421b31a77e5c3`. La publicación anterior comprobó arranque, conexión JDBC, servicios y coincidencia de recursos HTTPS. Los respaldos y evidencias detalladas permanecen privados; no se incluyen bases ni narraciones clínicas en Git.

La lectura del 9 de noviembre confirmó campos revisados completos en los perfiles académicos existentes y ausencia de las etiquetas genéricas señaladas en el informe psicológico vigente. Los antecedentes se preservaron. Persisten un expediente con responsables sin cuentas correspondientes y sin evaluación, y contexto profesional estructurado aún no rellenado en el informe vigente. No se inventaron registros para completar estas situaciones.

La entrega incluye las mejoras de propuestas aceptadas del commit remoto `b684f775`. Se conservan las pruebas históricas y las añadidas por los compañeros; las ampliaciones propias del Sprint 7 se archivan localmente fuera del repositorio, conforme a la distribución acordada del trabajo de pruebas.

En esta publicación se compila sin ejecutar pruebas. La revisión visual y los recorridos funcionales actuales quedan a cargo del equipo; no se presentan como validados. Los resultados anteriores son históricos y se conservan en el archivo académico local, sin convertirlos en evidencia de la versión actual.

## Documentación

La [guía única de integración y continuidad](GUIA_INTEGRACION_SPRINT7_SENNIAF_MATCH.md) incorpora los criterios del DOCX, acceso desde otro equipo, matriz manual y operación. El cierre final añade el ejemplo psicológico sin guardado automático; los estados de aceptación manual permanecen pendientes.

- [Operación y actualización](../deploy/azure/README.md)
- [Reglas de seguridad e integridad](SPRINT7_CORRECCIONES_F01_F13.md)
- [Preparación local](../README-BASE-DATOS.md)

Los informes provisionales fueron archivados fuera del repositorio. Se conservan sus originales y la copia del trabajo anterior a la limpieza. El cierre final publicó el botón psicológico mediante el procedimiento existente. Se comprobaron servicios, JDBC, recursos HTTPS y 17 tablas/37 filas idénticas antes y después; sin migraciones ni cargas de datos. Las pruebas manuales siguen pendientes.
