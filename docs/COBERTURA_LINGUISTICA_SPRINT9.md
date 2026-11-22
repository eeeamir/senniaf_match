# Cobertura lingüística — verificación del motor EVIDENCIA_TEXTO_6

Este informe conserva la referencia anterior a la corrección. El resultado posterior se documenta en [CORRECCION_LINGUISTICA_SPRINT9.md](CORRECCION_LINGUISTICA_SPRINT9.md).

Código examinado: `78a2ee0e5d8d03ae14726b7560c5c4e3b9e4dc12`. Producción sin cambios locales: **True**.

## A. Inventario y técnicas

Hay **33 conceptos léxicos** y un marcador técnico `NECESIDAD_SIN_PRECISAR`: 34 entradas del mapa. Edad es una comparación numérica, no un concepto del diccionario. Las recomendaciones profesionales son una fuente, no otro concepto.

El diccionario contiene **65 ramas alternativas principales** y admite **286 formas literales finitas**, expandiendo opcionales, artículos, singular/plural y variantes expresas. No se cuentan mayúsculas, tildes ni cambios exclusivos de espacios. **No son 286 sinónimos semánticos ni 286 oraciones válidas**: las combinaciones mecánicas incluyen discordancias gramaticales. El código no almacena un catálogo de oraciones ni un número de equivalencias semánticas. Se ofrecen ambas cantidades auditables por concepto, junto al patrón completo en [inventario.tsv](cobertura-linguistica/inventario.tsv).

Implementado: NFD y eliminación de marcas diacríticas; minúsculas con Locale.ROOT; compactación de espacios; límites de palabra; segmentación por puntuación y conectores; patrones de verbos, negación, condición e incertidumbre; ámbito de acciones coordinadas; referencias limitadas a terceros y anáforas; deduplicación por concepto y resolución conservadora de contradicciones. No hay lematizador, corrección ortográfica general, análisis sintáctico completo, raíces productivas ni similitud aproximada. Las conjugaciones y abreviaturas son listas explícitas. [Patrones exactos y flags](cobertura-linguistica/patrones.json).

## B. Matriz completa

Fuentes: **N** = opciones clínicas pertinentes, detalles de salud/alergias y necesidadesApoyo. **P** = recomendaciones vigentes explícitas, descartando hipótesis, preguntas y antecedentes según sus filtros. **F** = capacidades estructuradas aplicables y notaCondicionesSalud; las terapias/apoyos sin enum dependen de la nota. **S** = notaCondicionesSalud, notaRedApoyo y notaEstructuraHogar, solo para los acompañamientos habilitados. **H** = existencia/cantidad/detalle infantil y disposición/nota familiar. **R** = necesidadesApoyo frente a redApoyo/notaRedApoyo. **L** = disponibilidadViaje/notaDistancia.

Las formas nominales necesitan además una interpretación válida de sujeto y acción. En Salud, cada necesidad única comparte el peso P: actual=1, con apoyo=0,5, sin cobertura=0. Red, Hermanos y Logística conservan sus fórmulas; un FN de extracción no implica por sí solo una reducción de puntos cuando existe una opción estructurada suficiente.

| Concepto | Ramas / formas | Reconocidas en el corpus | No reconocidas o subacreditadas | Fuentes / criterio / impacto |
|---|---:|---|---|---|
| Asma (`ASMA`) | 1 / 1 | Podemos acompañar asma si contamos con apoyo; Podemos acompañar asma sin apoyo adicional | No tenemos ningún impedimento para acompañar asma; Nos podemos hacer cargo de un niño con asma | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Diabetes (`DIABETES`) | 1 / 1 | Podemos ayudarlo a seguir su tratamiento para la diabetes; Podemos acompañar diabetes | Estamos en condiciones de cuidar a un niño con diabetes | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Epilepsia (`EPILEPSIA`) | 1 / 1 | Podemos acompañar epilepsia | Tenemos disposición para atender a un niño con epilepsia | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| TDAH (`TDAH`) | 2 / 2 | Podemos acompañar TDAH | Podemos apoyar al niño con déficit de atención e hiperactividad | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| TEA (`TEA`) | 2 / 2 | Podemos acompañar TEA | Podemos acompañar a un niño dentro del espectro autista | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Discapacidad física (`DISCAPACIDAD_FISICA`) | 1 / 4 | Podemos acompañar discapacidad física | Nos sentimos capaces de cuidar a un niño con discapacidad física | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Discapacidad intelectual (`DISCAPACIDAD_INTELECTUAL`) | 1 / 4 | Tenemos capacidad para cuidar de un niño con discapacidad intelectual; Podemos acompañar discapacidad intelectual | Sin FN en este conjunto; no demuestra cobertura universal | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Discapacidad visual (`DISCAPACIDAD_VISUAL`) | 1 / 4 | Podemos asistir a un niño con discapacidad visual en su día a día; Podemos acompañar discapacidad visual | Sin FN en este conjunto; no demuestra cobertura universal | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Discapacidad auditiva (`DISCAPACIDAD_AUDITIVA`) | 1 / 4 | Podemos acompañar discapacidad auditiva | Estamos disponibles para acompañar a un niño con discapacidad auditiva | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Intolerancia a la lactosa (`INTOLERANCIA_LACTOSA`) | 1 / 3 | Podemos acompañar intolerancia a la lactosa | Podemos atender a un niño intolerante a la lactosa | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Alergia alimentaria (`ALERGIA_ALIMENTARIA`) | 1 / 4 | Podemos acompañar alergia alimentaria | Podemos cuidar a un niño con alergias a los alimentos | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Alergia a medicamentos (`ALERGIA_MEDICAMENTOS`) | 1 / 4 | Podemos acompañar alergia a medicamentos | Podemos acompañar al niño si tiene alergia a los medicamentos | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Alergia ambiental o al polen (`ALERGIA_AMBIENTAL_POLEN`) | 2 / 6 | Podemos acompañar alergia al polen | Podemos atender a un niño alérgico al polen | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Alergia a picaduras de insectos (`ALERGIA_PICADURA_INSECTOS`) | 1 / 8 | Podemos acompañar alergia a picaduras de insectos | Podemos cuidar a un niño alérgico a las picaduras de insectos | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Terapia de lenguaje (`TERAPIA_LENGUAJE`) | 1 / 4 | Tenemos disponibilidad para llevarlo a sus terapias del lenguaje; Podemos acompañar terapia del lenguaje | Sin FN en este conjunto; no demuestra cobertura universal | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Fisioterapia (`FISIOTERAPIA`) | 2 / 6 | Podemos llevarlo donde el fisioterapeuta para sus sesiones de fisioterapia; Podemos acompañar fisioterapia | Sin FN en este conjunto; no demuestra cobertura universal | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Terapia ocupacional (`TERAPIA_OCUPACIONAL`) | 1 / 4 | Podemos acompañar terapia ocupacional | Estamos disponibles para acompañarlo a terapia ocupacional | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Acompañamiento psicológico (`ACOMPANAMIENTO_PSICOLOGICO`) | 5 / 12 | Podemos acompañar sesiones psicológicas con apoyo; Podemos acompañarlo a terapia psicológica | Podemos facilitar que continúe su atención psicológica si recibimos apoyo; Podemos llevarlo a sus citas con el psicólogo | N/P → F; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Rutinas estables (`RUTINA_ESTABLE`) | 2 / 72 | Podemos mantener horarios constantes y no ofrecer apoyo emocional; Mantenemos horarios constantes y podemos acompañarlo | Podemos mantener una rutina fija todos los días; Podemos organizar horarios regulares para sus actividades | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Acompañamiento emocional (`ACOMPANAMIENTO_EMOCIONAL`) | 1 / 6 | Podemos acompañar apoyo emocional | Podemos darle apoyo emocional cuando lo necesite; Podemos apoyarlo emocionalmente durante su adaptación | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Apoyo durante la adaptación (`APOYO_ADAPTACION`) | 3 / 4 | Podemos acompañarlo en su proceso de adaptación; Podemos acompañar apoyo durante su adaptación | Podemos apoyarlo para que se acostumbre al nuevo hogar; Podemos ayudarlo a adaptarse al hogar | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Supervisión cotidiana (`SUPERVISION_COTIDIANA`) | 2 / 6 | Podemos acompañar supervisión cotidiana | Podemos vigilarlo todos los días; Podemos supervisarlo diariamente | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Supervisión permanente (`SUPERVISION_PERMANENTE`) | 1 / 5 | Podemos acompañar supervisión permanente | Podemos supervisarlo las veinticuatro horas; Podemos garantizar supervisión las 24 horas | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Apoyo educativo individualizado (`APOYO_EDUCATIVO_INDIVIDUALIZADO`) | 1 / 4 | Podemos ofrecer acompañamiento escolar individualizado; Podemos acompañar apoyo educativo individualizado | Podemos brindarle apoyo educativo individualizado | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Apoyo educativo especializado (`APOYO_EDUCATIVO_ESPECIALIZADO`) | 1 / 4 | Tenemos disponibilidad para facilitar apoyo escolar especializado; Podemos acompañar apoyo educativo especializado | Sin FN en este conjunto; no demuestra cobertura universal | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Ayuda con tareas (`AYUDA_TAREAS`) | 2 / 69 | Podemos ayudarlo con sus tareas; Podemos acompañar ayuda con las tareas | Podemos echarle una mano con sus tareas escolares; Podemos ayudarlo a hacer las tareas de la escuela | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Seguimiento escolar (`SEGUIMIENTO_ESCOLAR`) | 2 / 8 | Podemos realizar seguimiento de sus actividades escolares; Podemos acompañar seguimiento educativo | Podemos estar pendientes de sus actividades escolares | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Refuerzo escolar (`REFUERZO_ESCOLAR`) | 1 / 1 | Podemos llevarlo a sus clases de refuerzo escolar; Podemos acompañar refuerzo escolar | Sin FN en este conjunto; no demuestra cobertura universal | N/P → S; Salud y acompañamiento. Cobertura dentro de P; puede pasar a 0 ante FN |
| Acompañamiento de hermanos (`HERMANOS`) | 4 / 7 | Podemos acompañar hermanos | Estamos dispuestos a recibir a los hermanitos juntos | H → H; Hermanos. Extracción contextual; depende también de opciones estructuradas |
| Red de apoyo (`RED_APOYO`) | 8 / 8 | Contamos con una red de apoyo | Tenemos personas que nos pueden ayudar en el día a día | R → R; Red de apoyo. Extracción contextual; depende también de opciones estructuradas |
| Apoyo en cuidados diarios (`CUIDADOS_DIARIOS`) | 3 / 10 | Contamos con una persona de apoyo que puede acompañarlo en actividades cotidianas | Contamos con una persona que puede ayudar con los quehaceres diarios del niño | R → R; Red de apoyo. Extracción contextual; depende también de opciones estructuradas |
| Traslados (`VIAJE`) | 7 / 7 | Tenemos disponibilidad para trasladarnos; Podemos viajar | Sin FN en este conjunto; no demuestra cobertura universal | Sin contraparte infantil → L; Logística. Extracción contextual; depende también de opciones estructuradas |
| Recepción de visitas (`VISITAS`) | 1 / 1 | Podemos recibir visitas | Estamos disponibles para recibir las visitas | Sin contraparte infantil → L; Logística. Extracción contextual; depende también de opciones estructuradas |

### Los cinco ejemplos solicitados

Se consideran equivalentes dentro de la definición operativa vigente de acompañar o facilitar sesiones, no de aplicar terapia profesional. El motor ya admite llevar/asistir/facilitar sesiones psicológicas: el contraste con citas/atención/tratamiento psicológico examina ese mismo alcance. Pagar un profesional o tener un familiar psicólogo, por sí solos, no se etiquetan como cobertura. Estar pendiente todos los días se conserva como ambiguo respecto de supervisión específica; no se cuenta como FN. El corpus usa vigilar/supervisar para los contrastes afirmativos inequívocos.

| Declaración | Resultado real | Salud con necesidad psicológica y P=35 |
|---|---|---:|
| Podemos acompañarlo a terapia psicológica | ACTUAL / VP | 35 |
| Podemos llevarlo a sus citas con el psicólogo | AUSENTE / FN | 0 |
| Tenemos disponibilidad para asistir con él a sesiones de psicología | ACTUAL / VP | 35 |
| Podemos facilitar que continúe su atención psicológica | AUSENTE / FN | 0 |
| Podemos acompañarlo durante su tratamiento psicológico | AUSENTE / FN | 0 |

## C. Resultados medidos

Corpus manual sintético: **129 expresiones distintas**, 145 evaluaciones de cobertura/fuente. Las 16 expresiones infantiles se evalúan tanto en perfil como en recomendaciones. Hay además 12 escenarios de cálculo que reutilizan expresiones o añaden casos de impacto; no se suman a la estadística de cobertura del corpus.

Las etiquetas se fijaron antes de ejecutar. VP/VN indican acuerdo de cobertura; FN indica menor cobertura o necesidad omitida; FP indica cobertura excesiva o inclusión indebida de una necesidad. Una declaración ambigua no se etiqueta como capacidad afirmativa. Para las necesidades se mide inclusión en el conjunto puntuable, incluido el marcador desconocido, no autenticidad clínica. Las etiquetas expresan el significado operativo del criterio, no una validación profesional documental.

| Fuente | Evaluaciones | VP | VN | FN | FP | Correctas |
|---|---:|---:|---:|---:|---:|---:|
| familia | 113 | 54 | 17 | 38 | 4 | 71 |
| perfil | 16 | 3 | 5 | 5 | 3 | 8 |
| recomendacion | 16 | 3 | 8 | 5 | 0 | 11 |

No es una muestra aleatoria ni representativa de hablantes de Panamá. Incluye español cotidiano compartido —echar una mano, estar pendiente, llevar a citas— sin afirmar que sea exclusivo de Panamá. Los ejemplos comparten reglas y no son estadísticamente independientes. No se extrapola una tasa de aciertos a todo el idioma.

Pruebas anteriores: 810, fallos 0, errores 0, omitidas 0. Nuevas: 157, fallos 62, errores 0, omitidas 0. Suite completa: **967 ejecutadas, 905 satisfactorias, 62 fallidas, 0 errores, 0 omitidas**. Los fallos nuevos permanecen visibles: no se modifica el motor ni se rebajan expectativas para ocultarlos. No representan 62 causas independientes; varios reproducen el mismo defecto.

## D. Impacto matemático y riesgos

P=35 en estos escenarios, conservando los demás pesos válidos. Los valores esperados aplican la fórmula actual al significado declarado, no una fórmula nueva.

| Caso | Necesidad adicional | Capacidad textual | Salud esperada → real | Estado real |
|---|---|---|---:|---|
| I01  | Requiere acompañamiento psicológico | Podemos acompañarlo a terapia psicológica | 35 → 35 | EVALUADO |
| I02  | Requiere acompañamiento psicológico | Podemos llevarlo a sus citas con el psicólogo | 35 → 0 | DESCONOCIDO |
| I03  | Requiere acompañamiento psicológico | Podemos facilitar que continúe su atención psicológica | 35 → 0 | DESCONOCIDO |
| I04 (asma estructurada) | Necesita que lo lleven a sus citas con el psicólogo | sin capacidad adicional | 18 → 35 | EVALUADO |
| I05 (asma estructurada) | Requiere tratamiento especializado | sin capacidad adicional | 18 → 18 | DESCONOCIDO |
| I06 (asma estructurada) | Requiere hemodiálisis | sin capacidad adicional | 18 → 35 | EVALUADO |
| I07  | ¿Requiere terapia psicológica? | Podemos acompañar terapia psicológica | 0 → 35 | EVALUADO |
| I08 (asma estructurada) | Sin otra necesidad | No siempre podemos acompañar asma | 0 → 35 | EVALUADO |
| I09 (asma estructurada) | Sin otra necesidad | Antes acompañamos a un niño con asma | 0 → 35 | EVALUADO |
| I10 (asma estructurada) | Es introvertido | Podemos acompañar asma | 35 → 35 | EVALUADO |
| I11  | Requiere acompañamiento psicológico | No podemos acompañar terapia psicológica | 0 → 0 | EVALUADO |
| I12  | Requiere acompañamiento psicológico | Quizás podríamos acompañar terapia psicológica | 0 → 0 | DESCONOCIDO |

Hallazgos principales:

1. **Capacidad no reconocida:** citas con el psicólogo, atención/tratamiento psicológico y construcciones como estamos disponibles/brindar/echar una mano pueden perder cobertura. En los ejemplos I02/I03 la necesidad sí existe, pero Salud cae de 35 esperados a 0 y queda DESCONOCIDO. No se trata de una incapacidad expresamente declarada.
2. **Necesidad no reconocida:** con asma cubierta, la necesidad de acudir al psicólogo redactada sin los términos del diccionario desaparece. Salud queda en 35 en lugar de 18. Hemodiálisis también puede desaparecer; tratamiento especializado sí conserva un marcador desconocido y produce 18. La protección contra omisiones depende de una lista limitada de indicadores y no es universal.
3. **Falsos positivos de contexto:** no siempre podemos y antes/en el pasado acompañamos acreditan cobertura actual completa en este corpus. Son defectos de ámbito/temporalidad, no solo una limitación normal del vocabulario.
4. **Fuente inconsistente:** una pregunta o un descarte puede entrar en el perfil infantil como necesidad; el filtro profesional sí evita esos casos concretos. Una pregunta con capacidad familiar cubierta produce 35 improcedentes en I07. Una misma cadena no recibe siempre el mismo tratamiento por fuente.
5. **Ausencia frente a negativa:** la negativa explícita I11 da 0/EVALUADO y razón de incapacidad declarada; la expresión no interpretada I02 da 0/DESCONOCIDO. Otras omisiones pueden quedar EVALUADO si no activan las señales de texto sin correspondencia. El motor no llama negativa a toda ausencia, pero la identificación de lo no interpretado es incompleta.
6. **Duplicación y mezcla:** las pruebas anteriores de deduplicación, fuente profesional vigente, pesos, redondeo y mezcla de necesidades siguen pasando. Eso no compensa las necesidades que nunca se detectan. Una misma condición se unifica; el problema observado está antes, al extraer su significado.

## E. Mejoras propuestas, sin implementar

| Prioridad | Problema y causa | Técnica propuesta | Impacto y validación necesaria |
|---|---|---|---|
| Indispensable | Cobertura completa indebida por no siempre / pasado; la capacidad busca un verbo sin resolver esos modificadores | Reglas acotadas de negación cuantificada y temporalidad por acción | Evitar FP sin alterar pesos; pares contrastivos presente/pasado, nunca/no siempre/no solo y condiciones por acción |
| Indispensable | Preguntas, descartes y condiciones se filtran de forma distinta en perfil y recomendación | Compartir reglas de necesidad activa entre fuentes autorizadas, preservando sus propósitos | Evitar necesidades ficticias; no elevar observaciones ni hipótesis al denominador |
| Indispensable | Necesidades explícitas fuera del vocabulario pueden desaparecer y elevar puntos | Detección acotada de necesidad pendiente en campos autorizados, separada de su equivalencia | Conservar incertidumbre y fórmula; no convertir gustos/cariño/personalidad en necesidades médicas. Validar agrupación de desconocidos |
| Recomendable | Atención/citas/tratamiento psicológico y construcciones cotidianas explícitas no reconocidas | Equivalencias compuestas controladas y variantes verbales explícitas | Igualar coberturas cuando el alcance sea realmente equivalente; contrastar transporte general, pago, parentesco y terapia profesional |
| Recomendable | Adjetivos o conjugaciones transparentes no equivalen al sustantivo/formato esperado | Normalización morfológica pequeña y comprobable por dominio | Admitir intolerante/alérgico cuando corresponda; no usar raíces amplias que mezclen condiciones diferentes |
| Opcional | Coordinación/anáforas complejas requieren más contexto | Tokenización o análisis gramatical determinista limitado, solo si nuevos casos justifican el coste | No hace falta introducir un parser completo para resolver primero los fallos reproducidos |

No se proponen IA, modelos, APIs, distancias léxicas indiscriminadas, nuevos criterios, pesos ni pantallas. Un diccionario más largo por sí solo no corrige los FP de contexto.

## F. Conclusión y reproducción

La cobertura es finita y útil para expresiones previstas, pero los resultados justifican una corrección puntual: hay tanto subacreditación por redacción como sobreacreditación y omisiones del denominador. Se conserva la arquitectura y la fórmula; no es necesario rehacer el motor. No se puede considerar lingüísticamente cerrada la versión actual. Esta auditoría no la modifica ni publica otra versión.

Ejecutar con Java 17:

```bash
mvn test
python3 src/test/scripts/informe_cobertura_linguistica.py
```

`mvn test` devuelve error mientras los contraejemplos semánticos sigan fallando. Para aislar el corpus: `mvn -Dtest=CoberturaLinguisticaVerificacionTest,ImpactoLinguisticoVerificacionTest test`. Las pruebas existentes se ejecutaron primero sobre el árbol limpio y pasaron. No se borran pruebas ni se alteran sus expectativas.

[Resultados completos por expresión](cobertura-linguistica/resultados.tsv), [cálculos y razones](cobertura-linguistica/impacto.tsv), [corpus etiquetado](../src/test/resources/linguistica/capacidades.tsv). Todos los datos son sintéticos locales. No se accede a Azure, PostgreSQL ni servicios externos; no se modifica src/main, interfaces, fórmula o ponderaciones. Se conservan las ramas y el commit desplegado.
