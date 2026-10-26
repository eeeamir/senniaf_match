# Adaptación visual de SENNIAF Match

Referencia: `DESIGN.md` proporcionado por el equipo, con una estética inspirada en Calendly. Se implementa un lenguaje de diseño propio para el prototipo SENNIAF Match, sin integrar Calendly ni servicios de terceros.

## Componentes y colores

| Componente | Tratamiento |
| --- | --- |
| Lienzo | Gris frío `#f8f9fb` |
| Títulos y texto | Azul marino `#0b3558` |
| Texto auxiliar | Gris azulado `#476788` |
| Acción principal | Azul `#006bff`, radio 8 px |
| Tarjetas | Blanco, borde `#d4e0ed`, radio 24 px |
| Entradas | Relleno `#f0f3f8`, radio 8 px |
| Etiquetas de estado | Píldoras con azul informativo y tonos semánticos |
| Elevación | Sombras exclusivamente azuladas |

Los estilos de cabecera, tarjetas, botones, formularios, validaciones y tablas se centralizaron en `src/main/resources/static/calendly-theme.css`. El acceso dispone de `calendly-login.css`, con una presentación editorial, una tarjeta descriptiva del proceso y elementos decorativos en cian y magenta que no interfieren con las acciones.

## Pantallas actualizadas

1. **Acceso:** inicio de sesión, registro familiar y verificación en una tarjeta sobre fondo claro.
2. **Menú funcionario:** módulos de consulta y nuevo expediente, con íconos lineales y acciones visibles.
3. **Perfiles registrados:** tarjetas de casos con estado, edición, auditoría y baja lógica.
4. **Entrevistas:** controles, apartados y selección de rol unificados.
5. **Motor de compatibilidad:** lista de casos, clasificación, factores, decisiones y seguimiento familiar.

## Compatibilidad

- Diseño adaptable a escritorio, tablet y móvil, con puntos de ajuste a 1000, 860, 760 y 700 px.
- Se preservaron los IDs, los atributos para JavaScript y las rutas de navegación existentes.
- Se mantienen los estados de error, aviso y éxito; no se reemplazaron señales legales o técnicas por estados estéticos.
- Se respeta `prefers-reduced-motion` y se mantiene indicador de foco visible.
- La tipografía utiliza sustituciones locales si Manrope o Inter no están instaladas.

La tarjeta de la portada describe el flujo; **no muestra expedientes reales ni afirma que SENNIAF haya aprobado casos**. La app continúa siendo un prototipo académico.
