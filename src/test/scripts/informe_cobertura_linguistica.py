#!/usr/bin/env python3
"""Publica resultados sintéticos de la auditoría; no consulta la aplicación ni modifica el motor."""
import csv
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
INPUT = ROOT / "target/cobertura-linguistica"
OUTPUT = ROOT / "docs/cobertura-linguistica"


class ExpresionFinita:
    """Expande solo los literales, grupos, alternativas y opcionales usados por este vocabulario."""

    def __init__(self, texto):
        self.texto = texto
        self.posicion = 0

    def alternativas(self):
        resultado, secuencia = set(), {""}
        while self.posicion < len(self.texto) and self.texto[self.posicion] != ")":
            if self.texto[self.posicion] == "|":
                resultado |= secuencia
                secuencia = {""}
                self.posicion += 1
                continue
            opciones = self.atomo()
            if self.posicion < len(self.texto) and self.texto[self.posicion] == "?":
                opciones |= {""}
                self.posicion += 1
            secuencia = {a + b for a in secuencia for b in opciones}
        return resultado | secuencia

    def atomo(self):
        caracter = self.texto[self.posicion]
        if self.texto.startswith("(?:", self.posicion):
            self.posicion += 3
            opciones = self.alternativas()
            assert self.texto[self.posicion] == ")"
            self.posicion += 1
            return opciones
        if caracter == "[":
            fin = self.texto.index("]", self.posicion)
            opciones = set(self.texto[self.posicion + 1:fin])
            assert "-" not in opciones, "Rango no contemplado por este contador"
            self.posicion = fin + 1
            return opciones
        if caracter == "\\":
            self.posicion += 1
            caracter = self.texto[self.posicion]
        assert caracter not in "*+{}", "El contador no admite repeticiones abiertas"
        self.posicion += 1
        return {caracter}


def leer(nombre):
    return json.loads((INPUT / nombre).read_text())


def exportar(nombre, filas):
    columnas = list(dict.fromkeys(k for fila in filas for k in fila))
    with (OUTPUT / nombre).open("w", newline="", encoding="utf-8") as archivo:
        escritor = csv.DictWriter(archivo, fieldnames=columnas, delimiter="\t", lineterminator="\n")
        escritor.writeheader()
        escritor.writerows(filas)


def texto(valor):
    return str(valor).replace("|", "\\|").replace("\n", " ")


def fuentes(concepto, dominio):
    if dominio == "SALUD":
        sociales = concepto in {"RUTINA_ESTABLE", "ACOMPANAMIENTO_EMOCIONAL", "APOYO_ADAPTACION",
                                "SUPERVISION_COTIDIANA", "SUPERVISION_PERMANENTE", "AYUDA_TAREAS",
                                "SEGUIMIENTO_ESCOLAR", "REFUERZO_ESCOLAR"} or concepto.startswith("APOYO_EDUCATIVO_")
        return "N/P", "S" if sociales else "F", "Salud y acompañamiento"
    return {"HERMANOS": ("H", "H", "Hermanos"), "RED": ("R", "R", "Red de apoyo"),
            "LOGISTICA": ("Sin contraparte infantil", "L", "Logística")}[dominio]


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    resultados, inventario, impacto = leer("resultados.json"), leer("inventario.json"), leer("impacto.json")
    resumen, patrones = leer("resumen.json"), leer("patrones.json")
    lexical = []
    for c in inventario:
        if c["id"] == "NECESIDAD_SIN_PRECISAR":
            continue
        prefijo, sufijo = "(?<![a-z0-9])(?:", ")(?![a-z0-9])"
        assert c["expresion"].startswith(prefijo) and c["expresion"].endswith(sufijo)
        cuerpo = c["expresion"][len(prefijo):-len(sufijo)]
        contador = ExpresionFinita(cuerpo)
        formas = contador.alternativas()
        assert contador.posicion == len(cuerpo)
        profundidad, en_clase, ramas = 0, False, 1
        for caracter in cuerpo:
            if caracter == "[": en_clase = True
            elif caracter == "]": en_clase = False
            elif not en_clase:
                if caracter == "(": profundidad += 1
                elif caracter == ")": profundidad -= 1
                elif caracter == "|" and profundidad == 0: ramas += 1
        n, f, criterio = fuentes(c["id"], c["dominio"])
        lexical.append(dict(c, ramas=ramas, formas_finitas=len({"".join(s.split()) for s in formas}),
                            fuente_infantil=n, fuente_familiar=f, criterio=criterio))
    exportar("inventario.tsv", lexical)
    exportar("resultados.tsv", resultados)
    exportar("impacto.tsv", impacto)
    (OUTPUT / "patrones.json").write_text(json.dumps(patrones, ensure_ascii=False, indent=2) + "\n")
    anteriores, nuevas = [0] * 4, [0] * 4
    for archivo in (ROOT / "target/surefire-reports").glob("TEST-*.xml"):
        r = ET.parse(archivo).getroot()
        destino = nuevas if any(n in archivo.name for n in ("CoberturaLinguisticaVerificacionTest", "ImpactoLinguisticoVerificacionTest")) else anteriores
        for i, campo in enumerate(("tests", "failures", "errors", "skipped")):
            destino[i] += int(r.get(campo, 0))
    total = [a + b for a, b in zip(anteriores, nuevas)]
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    intacto = subprocess.run(["git", "diff", "--quiet", "HEAD", "--", "src/main"], cwd=ROOT).returncode == 0
    unicas = len({r["texto"] for r in resultados})
    lineas = ["# Cobertura lingüística — verificación del motor EVIDENCIA_TEXTO_6", "",
              f"Código examinado: `{commit}`. Producción sin cambios locales: **{intacto}**.", "",
              "## A. Inventario y técnicas", "",
              f"Hay **{len(lexical)} conceptos léxicos** y un marcador técnico `NECESIDAD_SIN_PRECISAR`: {len(inventario)} entradas del mapa. Edad es una comparación numérica, no un concepto del diccionario. Las recomendaciones profesionales son una fuente, no otro concepto.", "",
              f"El diccionario contiene **{sum(c['ramas'] for c in lexical)} ramas alternativas principales** y admite **{sum(c['formas_finitas'] for c in lexical)} formas literales finitas**, expandiendo opcionales, artículos, singular/plural y variantes expresas. No se cuentan mayúsculas, tildes ni cambios exclusivos de espacios. **No son 286 sinónimos semánticos ni 286 oraciones válidas**: las combinaciones mecánicas incluyen discordancias gramaticales. El código no almacena un catálogo de oraciones ni un número de equivalencias semánticas. Se ofrecen ambas cantidades auditables por concepto, junto al patrón completo en [inventario.tsv](cobertura-linguistica/inventario.tsv).", "",
              "Implementado: NFD y eliminación de marcas diacríticas; minúsculas con Locale.ROOT; compactación de espacios; límites de palabra; segmentación por puntuación y conectores; patrones de verbos, negación, condición e incertidumbre; ámbito de acciones coordinadas; referencias limitadas a terceros y anáforas; deduplicación por concepto y resolución conservadora de contradicciones. No hay lematizador, corrección ortográfica general, análisis sintáctico completo, raíces productivas ni similitud aproximada. Las conjugaciones y abreviaturas son listas explícitas. [Patrones exactos y flags](cobertura-linguistica/patrones.json).", "",
              "## B. Matriz completa", "",
              "Fuentes: **N** = opciones clínicas pertinentes, detalles de salud/alergias y necesidadesApoyo. **P** = recomendaciones vigentes explícitas, descartando hipótesis, preguntas y antecedentes según sus filtros. **F** = capacidades estructuradas aplicables y notaCondicionesSalud; las terapias/apoyos sin enum dependen de la nota. **S** = notaCondicionesSalud, notaRedApoyo y notaEstructuraHogar, solo para los acompañamientos habilitados. **H** = existencia/cantidad/detalle infantil y disposición/nota familiar. **R** = necesidadesApoyo frente a redApoyo/notaRedApoyo. **L** = disponibilidadViaje/notaDistancia.", "",
              "Las formas nominales necesitan además una interpretación válida de sujeto y acción. En Salud, cada necesidad única comparte el peso P: actual=1, con apoyo=0,5, sin cobertura=0. Red, Hermanos y Logística conservan sus fórmulas; un FN de extracción no implica por sí solo una reducción de puntos cuando existe una opción estructurada suficiente.", "",
              "| Concepto | Ramas / formas | Reconocidas en el corpus | No reconocidas o subacreditadas | Fuentes / criterio / impacto |",
              "|---|---:|---|---|---|"]
    for c in lexical:
        casos = [r for r in resultados if r["concepto"] == c["id"] and r["fuente"] == "familia"]
        bien = sorted((r for r in casos if r["resultado"] == "VP"), key=lambda r: r["grupo"] == "control")
        mal = [r for r in casos if r["resultado"] == "FN"]
        positivo = "; ".join(r["texto"] for r in bien[:2]) or "Sin caso favorable"
        negativo = "; ".join(r["texto"] for r in mal[:2]) or "Sin FN en este conjunto; no demuestra cobertura universal"
        efecto = "Cobertura dentro de P; puede pasar a 0 ante FN" if c["dominio"] == "SALUD" else "Extracción contextual; depende también de opciones estructuradas"
        lineas.append(f"| {c['nombre']} (`{c['id']}`) | {c['ramas']} / {c['formas_finitas']} | {texto(positivo)} | {texto(negativo)} | {c['fuente_infantil']} → {c['fuente_familiar']}; {c['criterio']}. {efecto} |")
    lineas += ["", "### Los cinco ejemplos solicitados", "",
               "Se consideran equivalentes dentro de la definición operativa vigente de acompañar o facilitar sesiones, no de aplicar terapia profesional. El motor ya admite llevar/asistir/facilitar sesiones psicológicas: el contraste con citas/atención/tratamiento psicológico examina ese mismo alcance. Pagar un profesional o tener un familiar psicólogo, por sí solos, no se etiquetan como cobertura. Estar pendiente todos los días se conserva como ambiguo respecto de supervisión específica; no se cuenta como FN. El corpus usa vigilar/supervisar para los contrastes afirmativos inequívocos.", "",
               "| Declaración | Resultado real | Salud con necesidad psicológica y P=35 |", "|---|---|---:|"]
    for r in resultados:
        if r["grupo"] == "ejemplo_usuario":
            lineas.append(f"| {texto(r['texto'])} | {r['declaracion']} / {r['resultado']} | {r.get('puntosSalud', '—')} |")
    lineas += ["", "## C. Resultados medidos", "",
               f"Corpus manual sintético: **{unicas} expresiones distintas**, {len(resultados)} evaluaciones de cobertura/fuente. Las 16 expresiones infantiles se evalúan tanto en perfil como en recomendaciones. Hay además {len(impacto)} escenarios de cálculo que reutilizan expresiones o añaden casos de impacto; no se suman a la estadística de cobertura del corpus.", "",
               "Las etiquetas se fijaron antes de ejecutar. VP/VN indican acuerdo de cobertura; FN indica menor cobertura o necesidad omitida; FP indica cobertura excesiva o inclusión indebida de una necesidad. Una declaración ambigua no se etiqueta como capacidad afirmativa. Para las necesidades se mide inclusión en el conjunto puntuable, incluido el marcador desconocido, no autenticidad clínica. Las etiquetas expresan el significado operativo del criterio, no una validación profesional documental.", "",
               "| Fuente | Evaluaciones | VP | VN | FN | FP | Correctas |", "|---|---:|---:|---:|---:|---:|---:|"]
    for fuente, valores in resumen.items():
        lineas.append(f"| {fuente} | {sum(valores.values())} | {valores.get('VP',0)} | {valores.get('VN',0)} | {valores.get('FN',0)} | {valores.get('FP',0)} | {valores.get('VP',0)+valores.get('VN',0)} |")
    lineas += ["", "No es una muestra aleatoria ni representativa de hablantes de Panamá. Incluye español cotidiano compartido —echar una mano, estar pendiente, llevar a citas— sin afirmar que sea exclusivo de Panamá. Los ejemplos comparten reglas y no son estadísticamente independientes. No se extrapola una tasa de aciertos a todo el idioma.", "",
               f"Pruebas anteriores: {anteriores[0]}, fallos {anteriores[1]}, errores {anteriores[2]}, omitidas {anteriores[3]}. Nuevas: {nuevas[0]}, fallos {nuevas[1]}, errores {nuevas[2]}, omitidas {nuevas[3]}. Suite completa: **{total[0]} ejecutadas, {total[0]-sum(total[1:])} satisfactorias, {total[1]} fallidas, {total[2]} errores, {total[3]} omitidas**. Los fallos nuevos permanecen visibles: no se modifica el motor ni se rebajan expectativas para ocultarlos. No representan 62 causas independientes; varios reproducen el mismo defecto.", "",
               "## D. Impacto matemático y riesgos", "",
               "P=35 en estos escenarios, conservando los demás pesos válidos. Los valores esperados aplican la fórmula actual al significado declarado, no una fórmula nueva.", "",
               "| Caso | Necesidad adicional | Capacidad textual | Salud esperada → real | Estado real |", "|---|---|---|---:|---|"]
    for r in impacto:
        lineas.append(f"| {r['id']} {'(asma estructurada)' if r['asmaEstructurada'] else ''} | {texto(r['necesidad'])} | {texto(r['capacidad'])} | {r['esperado']} → {r['actual']} | {r['estado']} |")
    lineas += ["", "Hallazgos principales:", "",
               "1. **Capacidad no reconocida:** citas con el psicólogo, atención/tratamiento psicológico y construcciones como estamos disponibles/brindar/echar una mano pueden perder cobertura. En los ejemplos I02/I03 la necesidad sí existe, pero Salud cae de 35 esperados a 0 y queda DESCONOCIDO. No se trata de una incapacidad expresamente declarada.",
               "2. **Necesidad no reconocida:** con asma cubierta, la necesidad de acudir al psicólogo redactada sin los términos del diccionario desaparece. Salud queda en 35 en lugar de 18. Hemodiálisis también puede desaparecer; tratamiento especializado sí conserva un marcador desconocido y produce 18. La protección contra omisiones depende de una lista limitada de indicadores y no es universal.",
               "3. **Falsos positivos de contexto:** no siempre podemos y antes/en el pasado acompañamos acreditan cobertura actual completa en este corpus. Son defectos de ámbito/temporalidad, no solo una limitación normal del vocabulario.",
               "4. **Fuente inconsistente:** una pregunta o un descarte puede entrar en el perfil infantil como necesidad; el filtro profesional sí evita esos casos concretos. Una pregunta con capacidad familiar cubierta produce 35 improcedentes en I07. Una misma cadena no recibe siempre el mismo tratamiento por fuente.",
               "5. **Ausencia frente a negativa:** la negativa explícita I11 da 0/EVALUADO y razón de incapacidad declarada; la expresión no interpretada I02 da 0/DESCONOCIDO. Otras omisiones pueden quedar EVALUADO si no activan las señales de texto sin correspondencia. El motor no llama negativa a toda ausencia, pero la identificación de lo no interpretado es incompleta.",
               "6. **Duplicación y mezcla:** las pruebas anteriores de deduplicación, fuente profesional vigente, pesos, redondeo y mezcla de necesidades siguen pasando. Eso no compensa las necesidades que nunca se detectan. Una misma condición se unifica; el problema observado está antes, al extraer su significado.", "",
               "## E. Mejoras propuestas, sin implementar", "",
               "| Prioridad | Problema y causa | Técnica propuesta | Impacto y validación necesaria |", "|---|---|---|---|",
               "| Indispensable | Cobertura completa indebida por no siempre / pasado; la capacidad busca un verbo sin resolver esos modificadores | Reglas acotadas de negación cuantificada y temporalidad por acción | Evitar FP sin alterar pesos; pares contrastivos presente/pasado, nunca/no siempre/no solo y condiciones por acción |",
               "| Indispensable | Preguntas, descartes y condiciones se filtran de forma distinta en perfil y recomendación | Compartir reglas de necesidad activa entre fuentes autorizadas, preservando sus propósitos | Evitar necesidades ficticias; no elevar observaciones ni hipótesis al denominador |",
               "| Indispensable | Necesidades explícitas fuera del vocabulario pueden desaparecer y elevar puntos | Detección acotada de necesidad pendiente en campos autorizados, separada de su equivalencia | Conservar incertidumbre y fórmula; no convertir gustos/cariño/personalidad en necesidades médicas. Validar agrupación de desconocidos |",
               "| Recomendable | Atención/citas/tratamiento psicológico y construcciones cotidianas explícitas no reconocidas | Equivalencias compuestas controladas y variantes verbales explícitas | Igualar coberturas cuando el alcance sea realmente equivalente; contrastar transporte general, pago, parentesco y terapia profesional |",
               "| Recomendable | Adjetivos o conjugaciones transparentes no equivalen al sustantivo/formato esperado | Normalización morfológica pequeña y comprobable por dominio | Admitir intolerante/alérgico cuando corresponda; no usar raíces amplias que mezclen condiciones diferentes |",
               "| Opcional | Coordinación/anáforas complejas requieren más contexto | Tokenización o análisis gramatical determinista limitado, solo si nuevos casos justifican el coste | No hace falta introducir un parser completo para resolver primero los fallos reproducidos |", "",
               "No se proponen IA, modelos, APIs, distancias léxicas indiscriminadas, nuevos criterios, pesos ni pantallas. Un diccionario más largo por sí solo no corrige los FP de contexto.", "",
               "## F. Conclusión y reproducción", "",
               "La cobertura es finita y útil para expresiones previstas, pero los resultados justifican una corrección puntual: hay tanto subacreditación por redacción como sobreacreditación y omisiones del denominador. Se conserva la arquitectura y la fórmula; no es necesario rehacer el motor. No se puede considerar lingüísticamente cerrada la versión actual. Esta auditoría no la modifica ni publica otra versión.", "",
               "Ejecutar con Java 17:", "", "```bash", "mvn test", "python3 src/test/scripts/informe_cobertura_linguistica.py", "```", "",
               "`mvn test` devuelve error mientras los contraejemplos semánticos sigan fallando. Para aislar el corpus: `mvn -Dtest=CoberturaLinguisticaVerificacionTest,ImpactoLinguisticoVerificacionTest test`. Las pruebas existentes se ejecutaron primero sobre el árbol limpio y pasaron. No se borran pruebas ni se alteran sus expectativas.", "",
               "[Resultados completos por expresión](cobertura-linguistica/resultados.tsv), [cálculos y razones](cobertura-linguistica/impacto.tsv), [corpus etiquetado](../src/test/resources/linguistica/capacidades.tsv). Todos los datos son sintéticos locales. No se accede a Azure, PostgreSQL ni servicios externos; no se modifica src/main, interfaces, fórmula o ponderaciones. Se conservan las ramas y el commit desplegado."]
    (ROOT / "docs/COBERTURA_LINGUISTICA_SPRINT9.md").write_text("\n".join(lineas) + "\n")
    print(json.dumps({"conceptos": len(lexical), "entradas": len(inventario), "ramas": sum(c["ramas"] for c in lexical),
                      "formas": sum(c["formas_finitas"] for c in lexical), "expresiones_unicas": unicas,
                      "evaluaciones": len(resultados), "pruebas": total, "resumen": resumen}, ensure_ascii=False))


if __name__ == "__main__":
    main()
