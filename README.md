# SENNIAF Match — Sprint 8

Prototipo académico para el análisis de compatibilidad en procesos de adopción. **No es un sistema oficial de SENNIAF ni decide adopciones.** Utilizar exclusivamente información sintética.

Demostración: https://senniafmatch.westus2.cloudapp.azure.com/

## Funciones

- **Trabajo social:** registrar, consultar y editar expedientes propios; solicitar retiro o reactivación sin alterar automáticamente su participación en Match; registrar interrupciones de convivencia de vinculaciones asignadas.
- **Psicología:** consultar casos asignados, registrar y actualizar evaluaciones mediante versiones, métodos, instrumentos, valoración, diagnóstico opcional y acompañamiento; registrar interrupciones de convivencia de vinculaciones asignadas.
- **Comité:** consultar expedientes, analizar compatibilidad, registrar decisiones justificadas, consultar vinculaciones y respuestas familiares, revisar interrupciones y alertas por tres reevaluaciones completadas, resolver solicitudes y ajustar los cinco pesos con auditoría y control de versión.
- **Familia adoptante:** registrar y actualizar su entrevista y sus apoyos culturales y comunitarios, completar reevaluaciones y consultar etapas, historial propio y Mi vinculación, con respuesta familiar única y actualización periódica.

La identidad y los permisos se validan en el servidor. Los informes y actuaciones anteriores se conservan; la exclusión del Match no elimina el seguimiento autorizado. Los hallazgos clínicos no se convierten automáticamente en diagnósticos ni modifican el algoritmo.

## Ejecutar y mantener

Requiere Java 17 o 21, Maven y PostgreSQL 16. La entrega local del Sprint 8 se validó con Java 17 y PostgreSQL 16.15.

1. Consultar [configuración local de PostgreSQL](README-BASE-DATOS.md).
2. Configurar las variables privadas a partir de `.env.example`; no versionar `.env`.
3. Compilar sin pruebas cuando corresponda:

   ```bash
   mvn -B -Dmaven.test.skip=true package
   ```

4. Con el esquema preparado y las variables exportadas: `mvn spring-boot:run`.

No utilizar `clean` sobre el directorio de un servicio local en ejecución. No reemplazar volúmenes existentes ni copiar la base local sobre Azure.

- [Resultados locales del Sprint 8](docs/SPRINT8_RESULTADOS.md) y [matriz de aceptación](docs/SPRINT8_MATRIZ_ACEPTACION.md)
- [Guía de integración, acceso técnico y matriz de pruebas](docs/GUIA_INTEGRACION_SPRINT7_SENNIAF_MATCH.md)
- [Estado y alcance del Sprint 7](docs/ACTA_CIERRE_SPRINT7.md)
- [Reglas y correcciones de seguridad](docs/SPRINT7_CORRECCIONES_F01_F13.md)
- [Operación, respaldo, actualización y HTTPS](deploy/azure/README.md)
- [Guía histórica de validación del Sprint 6](docs/Sprint6_VALIDACION.md)
- [Diseño anterior conservado como antecedente](docs/DISENO_INTERFAZ.md)

Las pruebas históricas y el aporte del equipo se conservan en `src/test`. La revisión de HU19 corrige contratos y mocks antiguos, adapta las pruebas HTTP y valida el proyecto con Java 17 y PostgreSQL 16.15. Para ejecutar la integración se requiere una base aislada preparada expresamente; no usar una base habitual ni la demostración. GitHub Actions sigue siendo un flujo de pruebas, sin despliegue automático.

Las cinco historias están integradas localmente: HU19, HU11, HU12, HU14 y HU13. La regresión completa aprobó 273 pruebas Java (234 unitarias y 39 de integración) y ocho del actualizador. Las fórmulas y los cinco factores se conservaron.

La nueva versión todavía no está publicada en Azure: Vinculaciones devuelve 404 allí y las migraciones 012 y 013 requieren una intervención autorizada. La [verificación integral de HU19](docs/VERIFICACION_INTEGRAL_HU19.md) se conserva como antecedente; la petición posterior permitió desarrollar las otras historias localmente sin esperar al despliegue. No se realizaron push ni cambios remotos.

No hay contraseñas públicas de demostración. Los datos y respaldos de Azure, sesiones, secretos SMTP y claves SSH permanecen fuera del repositorio.
