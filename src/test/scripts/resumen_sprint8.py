import glob
import os
import sys
import xml.etree.ElementTree as ET

DESTINO = os.path.join("target", "pruebas-sprint8-resumen.md")

def grupo(nombre):
    for hu in ("HU19", "HU11", "HU12", "HU14", "HU13"):
        if hu in nombre:
            return hu
    return "Unitarias de soporte"

def leer_reportes():
    datos = {}
    for patron in ("target/surefire-reports/TEST-*.xml", "target/failsafe-reports/TEST-*.xml"):
        for archivo in glob.glob(patron):
            raiz = ET.parse(archivo).getroot()
            nombre = raiz.attrib.get("name", os.path.basename(archivo))
            clase = nombre.rsplit(".", 1)[-1]
            if patron.startswith("target/surefire") and clase not in ("VinculacionDecisionTest", "VinculacionServiceTest"):
                continue
            if patron.startswith("target/failsafe") and not clase.startswith("HU"):
                continue
            clave = grupo(nombre)
            fila = datos.setdefault(clave, {"ejecutadas": 0, "fallos": 0, "errores": 0, "omitidas": 0, "segundos": 0.0, "clases": []})
            ejecutadas = int(raiz.attrib.get("tests", 0))
            fallos = int(raiz.attrib.get("failures", 0))
            errores = int(raiz.attrib.get("errors", 0))
            omitidas = int(raiz.attrib.get("skipped", 0))
            fila["ejecutadas"] += ejecutadas
            fila["fallos"] += fallos
            fila["errores"] += errores
            fila["omitidas"] += omitidas
            fila["segundos"] += float(raiz.attrib.get("time", 0))
            fila["clases"].append(clase)
    return datos

def main():
    datos = leer_reportes()
    orden = ["HU19", "HU11", "HU12", "HU14", "HU13", "Unitarias de soporte"]
    total = {k: 0 for k in ("ejecutadas", "fallos", "errores", "omitidas", "segundos")}
    lineas = ["# Resumen Sprint 8", "", "| Sección | Ejecutadas | Aprobadas | Fallos | Errores | Omitidas | Tiempo (s) |", "| --- | ---: | ---: | ---: | ---: | ---: | ---: |"]
    for clave in orden:
        fila = datos.get(clave)
        if not fila:
            continue
        aprobadas = fila["ejecutadas"] - fila["fallos"] - fila["errores"] - fila["omitidas"]
        lineas.append(f"| {clave} | {fila['ejecutadas']} | {aprobadas} | {fila['fallos']} | {fila['errores']} | {fila['omitidas']} | {fila['segundos']:.2f} |")
        for campo in total:
            total[campo] += fila[campo]
        lineas.append(f"| Pruebas: {', '.join(sorted(set(fila['clases'])))} |  |  |  |  |  |  |")
    aprobadas = total["ejecutadas"] - total["fallos"] - total["errores"] - total["omitidas"]
    lineas += ["", f"**Total:** {total['ejecutadas']} ejecutadas, {aprobadas} aprobadas, {total['fallos']} fallos, {total['errores']} errores y {total['omitidas']} omitidas.", f"**Tiempo acumulado de reportes:** {total['segundos']:.2f} segundos.", f"**Resultado:** {'BUILD SUCCESS' if total['fallos'] + total['errores'] == 0 else 'REVISAR FALLOS'}"]
    os.makedirs(os.path.dirname(DESTINO), exist_ok=True)
    contenido = "\n".join(lineas) + "\n"
    with open(DESTINO, "w", encoding="utf-8") as archivo:
        archivo.write(contenido)
    print(contenido)
    return 0 if total["fallos"] + total["errores"] == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
