# Pruebas Sprint 8

Esta carpeta contiene únicamente las pruebas nuevas de las funcionalidades implementadas en el Sprint 8:

- HU19: vinculaciones.
- HU11: interrupción de convivencia.
- HU12: continuidad cultural y lingüística.
- HU14: alertas por reevaluaciones.
- HU13: seguridad y auditoría de pesos.

Las pruebas conservan los paquetes Java originales y se agregan al ciclo Maven solo mediante el perfil `sprint8`.

## Ejecutar todas las pruebas

Para ejecutar las pruebas unitarias y de integración de Sprint 7 y Sprint 8:

```bash
mvn -Psprint7,sprint8,e2e verify
```

Las pruebas que requieren PostgreSQL deben usar una base aislada de pruebas y las variables de entorno documentadas por cada integración.
