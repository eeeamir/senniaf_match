# SENNIAF Match — Sprint 7

Prototipo académico para el análisis de compatibilidad en procesos de adopción. **No es un sistema oficial de SENNIAF ni decide adopciones.** Utilizar exclusivamente información sintética.

Demostración: https://senniafmatch.westus2.cloudapp.azure.com/

## Funciones

- **Trabajo social:** registrar, consultar y editar expedientes propios; solicitar retiro o reactivación sin alterar automáticamente su participación en Match.
- **Psicología:** consultar casos asignados, registrar y actualizar evaluaciones mediante versiones, métodos, instrumentos, valoración, diagnóstico opcional y acompañamiento.
- **Comité:** consultar expedientes, analizar compatibilidad, registrar decisiones justificadas, resolver solicitudes y ajustar los cinco pesos con auditoría y control de versión.
- **Familia adoptante:** registrar y actualizar su entrevista, completar reevaluaciones y consultar etapas, historial propio y propuestas aceptadas, con actualización periódica.

La identidad y los permisos se validan en el servidor. Los informes y actuaciones anteriores se conservan; la exclusión del Match no elimina el seguimiento autorizado. Los hallazgos clínicos no se convierten automáticamente en diagnósticos ni modifican el algoritmo.

## Ejecutar y mantener

Requiere Java 21 (compilación compatible con Java 17), Maven y PostgreSQL 16.

1. Consultar [configuración local de PostgreSQL](README-BASE-DATOS.md).
2. Configurar las variables privadas a partir de `.env.example`; no versionar `.env`.
3. Compilar sin pruebas cuando corresponda:

   ```bash
   mvn -B -Dmaven.test.skip=true package
   ```

4. Con el esquema preparado y las variables exportadas: `mvn spring-boot:run`.

No utilizar `clean` sobre el directorio de un servicio local en ejecución. No reemplazar volúmenes existentes ni copiar la base local sobre Azure.

- [Guía de integración, acceso técnico y matriz de pruebas](docs/GUIA_INTEGRACION_SPRINT7_SENNIAF_MATCH.md)
- [Estado y alcance del Sprint 7](docs/ACTA_CIERRE_SPRINT7.md)
- [Reglas y correcciones de seguridad](docs/SPRINT7_CORRECCIONES_F01_F13.md)
- [Operación, respaldo, actualización y HTTPS](deploy/azure/README.md)
- [Guía histórica de validación del Sprint 6](docs/Sprint6_VALIDACION.md)
- [Diseño anterior conservado como antecedente](docs/DISENO_INTERFAZ.md)

Las pruebas históricas y el aporte remoto del equipo permanecen en `src/test`. Las ampliaciones de pruebas y evidencias privadas de trabajo del Sprint 7 no se publican. La suite histórica requiere revisión por el equipo frente a los contratos actuales; no se presenta esta entrega como una ejecución de pruebas aprobada. GitHub Actions conserva su configuración; esta publicación utiliza `[skip ci]` por instrucción de no ejecutar pruebas.

No hay contraseñas públicas de demostración. Los datos y respaldos de Azure, sesiones, secretos SMTP y claves SSH permanecen fuera del repositorio.
