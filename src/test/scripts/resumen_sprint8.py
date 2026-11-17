import glob, re, sys, xml.etree.ElementTree as ET
from pathlib import Path

METRICAS = ("Adecuacion funcional", "Fiabilidad", "Eficiencia de rendimiento", "Seguridad")
GRUPOS = ("HU19", "HU11", "HU12", "HU13", "HU14", "Pruebas adicionales")
CAMPOS = ("ejecutadas", "aprobadas", "fallos", "errores", "omitidas")

def clasificar(nombre):
    """Devuelve todas las métricas que cubre el método.

    La cobertura es transversal: una prueba puede contar en varias métricas,
    pero nunca se suma entre métricas para obtener el total de ejecuciones.
    La eficiencia se registra para cada caso porque todos forman parte de la
    ejecución y tiempo de la suite de la HU.
    """
    n = nombre.lower()
    metricas = {"Adecuacion funcional", "Eficiencia de rendimiento"}
    if any(x in n for x in ("rol", "permiso", "acceso", "suplant", "cabecera", "privacidad", "ajena", "expone", "autoriz")):
        metricas.add("Seguridad")
    if any(x in n for x in ("concurr", "simultan", "duplic", "persist", "auditor", "historial", "version", "reserva", "transaccion", "conserva", "libera", "serializ")):
        metricas.add("Fiabilidad")
    return metricas

def grupo(clase):
    m = re.search(r"HU(?:19|11|12|13|14)", clase)
    return m.group(0) if m else "Pruebas adicionales"

def metodos():
    indice = {}
    patron = re.compile(r"(?:@Test\s*)+(?:public\s+|private\s+|protected\s+)?(?:\w+[<>, ?\[\]]*\s+)?(\w+)\s*\(")
    for archivo in Path("src/test/pruebas-sprint8").rglob("*.java"):
        for match in patron.finditer(archivo.read_text(encoding="utf-8")):
            indice[(archivo.stem, match.group(1))] = clasificar(match.group(1))
    return indice

def vacia():
    return {m: {**{c: 0 for c in CAMPOS}, "casos": []} for m in METRICAS}

def leer():
    indice = metodos()
    datos = {g: vacia() for g in GRUPOS}
    for patron in ("target/surefire-reports/TEST-*.xml", "target/failsafe-reports/TEST-*.xml"):
        for archivo in glob.glob(patron):
            raiz = ET.parse(archivo).getroot()
            clase = raiz.attrib.get("name", Path(archivo).stem).rsplit(".", 1)[-1]
            if patron.startswith("target/failsafe") and not clase.startswith("HU"): continue
            if patron.startswith("target/surefire") and clase not in ("VinculacionDecisionTest", "VinculacionServiceTest"): continue
            for caso in raiz.findall("testcase"):
                nombre = caso.attrib.get("name", "")
                estado = "fallos" if caso.find("failure") is not None else "errores" if caso.find("error") is not None else "omitidas" if caso.find("skipped") is not None else "aprobadas"
                metricas = indice.get((clase, nombre), clasificar(nombre))
                for metrica in metricas:
                    f = datos[grupo(clase)][metrica]
                    f["ejecutadas"] += 1
                    f[estado] += 1
                    f["casos"].append(f"{clase}.{nombre}")
    return datos

def main():
    datos = leer()
    resumen = vacia()
    out = ["", "================ RESUMEN SPRINT 8 ================", ""]
    for g in GRUPOS:
        out.append(g)
        for m in METRICAS:
            f = datos[g][m]
            out.append(f"- {m}: {f['ejecutadas']} pruebas (aprobadas: {f['aprobadas']}, fallos: {f['fallos']}, errores: {f['errores']}, omitidas: {f['omitidas']})")
            for caso in f["casos"]:
                out.append(f"    - {caso}")
            for c in CAMPOS: resumen[m][c] += f[c]
        out.append("")
    out += ["RESUMEN FINAL", "(Las métricas son cobertura transversal; por eso sus subtotales no se suman entre sí.)"]
    for m in METRICAS: out.append(f"- {m}: {resumen[m]['ejecutadas']}")
    total = {c: sum(resumen[m][c] for m in METRICAS) for c in CAMPOS}
    out += [f"- Total de pruebas realizadas: {total['ejecutadas']}", f"- Aprobadas: {total['aprobadas']}", f"- Fallos: {total['fallos']}", f"- Errores: {total['errores']}", f"- Omitidas: {total['omitidas']}", ""]
    print("\n".join(out))
    return 0 if total["fallos"] + total["errores"] == 0 else 1

if __name__ == "__main__": sys.exit(main())
