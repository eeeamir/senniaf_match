# Inicio público — Sprint 9

> Limpieza de cierre: las capturas y salidas temporales citadas se retiraron del árbol de trabajo; se conservan en el historial del commit `298c292`. Este documento conserva las decisiones y los resultados de aquella verificación.

Página nueva en `/inicio.html`, en la rama `mejora/sprint9`. `/` e `index.html` mantienen el acceso existente. No se añade un controlador ni se cambia el enrutamiento del servidor.

## Implementación

- `src/main/resources/static/inicio.html`: composición de las referencias, titular original, logo original, portada en dos columnas, tarjeta de iconos conectados y cuatro roles con los textos aprobados. El mensaje sobre criterio profesional aparece una sola vez, en la tarjeta principal.
- `src/main/resources/static/inicio.css`: estilos propios de la página, contenedor centrado común de hasta 1200 px, tarjetas con altura equilibrada por fila, separación de 24 px y padding de 24 px; en móvil, una columna, separación de 16 px y padding de 20 px. La portada se apila por debajo de 900 px para conservar el espacio del texto y la ilustración.
- `src/main/resources/static/senniaf.js`: se añade únicamente un adaptador para `index.html#registro`. Al terminar de cargar esa ruta, activa el botón existente que abre el registro y coloca el foco. No se cambia el código previo, los formularios, estilos, validaciones, contraseña, guía digital ni llamadas de autenticación.

Respecto a la segunda referencia, los roles se distribuyen en **dos columnas en escritorio**, según el requisito expreso, y se elimina el aviso inferior que duplicaba el mensaje de criterio profesional. Los títulos de sección y tarjetas usan colores explícitos y legibles. Se conservan la identidad visual y las fuentes del proyecto; no se añaden fuentes externas ni imágenes generadas.

## Comprobación real en Chromium

Página servida por HTTP local, sin Azure ni datos reales. Se revisaron capturas completas en 1920×1080, 1366×768, 768×1024, 390×844 y 320×844. Sin desbordamiento horizontal, texto recortado ni superposición; márgenes de los cuatro contenedores alineados. Se inspeccionaron visualmente las capturas de escritorio, tableta y móviles.

- Títulos blancos del Comité: **10,77:1**; títulos azul oscuro de tarjetas blancas: **12,62:1**; descripciones de tarjetas blancas: **5,89:1**. El titular azul grande supera 3:1 y los textos normales superan 4,5:1. Se usó el fondo más oscuro de la portada para las comprobaciones sobre gradiente. Los contrastes de los enlaces hover y del foco mantienen el azul oscuro sobre fondo claro.
- Colores de títulos conservados en estado normal y emulación de hover/focus/focus-visible; también con preferencia de tema oscuro. La página usa superficies explícitas claras; no se introduce un tema nuevo.
- Navegación por Tab verificada, foco visible y enlaces de al menos 44 px de alto. Enlace para saltar al contenido. Los artículos informativos no se convierten en controles ni añaden tabulaciones innecesarias.
- «Conocer el sistema» usa un ancla nativa a la sección informativa. Con `prefers-reduced-motion: reduce`, el desplazamiento es inmediato.
- Inicio e inicio de sesión enlazan las rutas existentes; «Registrarse» abre directamente la vista de registro de `index.html`, con foco en Nombre. Comprobadas la validación de registro vacío, alternancia al login y mostrar contraseña, sin enviar solicitudes POST.
- Comparación con el acceso original de Git HEAD: mismos formularios y mismos estilos calculados en cuerpo, guía, panel, controles y títulos. `index.html`, CSS de acceso, tema compartido, logo, motor y `vinculaciones.js` conservan sus bytes originales.
- Pruebas existentes con Java 17: `mvn -q -Dtest=SeguridadIsoTest,CompatibilidadIntegracionTest test`: **73 satisfactorias, 0 fallos, 0 errores, 0 omitidas**. No se creó una suite nueva ni se repitió la auditoría del motor. No se probó un registro real ni autenticación con PostgreSQL.

## Evidencias históricas

Las capturas y mediciones de escritorio y móvil se conservan en el historial indicado al inicio.

Solo se implementó esta página pública y su acceso directo al registro. Sin cambios en backend, base de datos, puntuación, paneles, expedientes ni funcionalidades de citas. Sin push, merge ni despliegue. Se conserva el trabajo previo de rendimiento y la imagen de referencia sin modificar.
