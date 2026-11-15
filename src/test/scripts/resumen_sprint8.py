import glob
import sys
import xml.etree.ElementTree as ET

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
            nombre = raiz.attrib.get("name", archivo.rsplit("/", 1)[-1].rsplit("\\", 1)[-1])
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
    lineas = ["", "================ RESUMEN SPRINT 8 ================", "", "SECCIÓN                         SUITE                         CASOS  OK  FALL  ERR  OMIT  TIEMPO", "------------------------------------------------------------------------------------------------"]
    for clave in orden:
        fila = datos.get(clave)
        if not fila:
            continue
        aprobadas = fila["ejecutadas"] - fila["fallos"] - fila["errores"] - fila["omitidas"]
        suites = ", ".join(sorted(set(fila["clases"])))
        lineas.append(f"{clave:<32} {suites:<30} {fila['ejecutadas']:>5} {aprobadas:>3} {fila['fallos']:>5} {fila['errores']:>4} {fila['omitidas']:>5} {fila['segundos']:>7.2f}s")
        for campo in total:
            total[campo] += fila[campo]
    aprobadas = total["ejecutadas"] - total["fallos"] - total["errores"] - total["omitidas"]
    lineas += ["------------------------------------------------------------------------------------------------", f"TOTAL SPRINT 8                    {total['ejecutadas']:>5} {aprobadas:>3} {total['fallos']:>5} {total['errores']:>4} {total['omitidas']:>5} {total['segundos']:>7.2f}s", "", f"Pruebas ejecutadas: {total['ejecutadas']}", f"Pruebas aprobadas:  {aprobadas}", f"Fallos:              {total['fallos']}", f"Errores:             {total['errores']}", f"Omitidas:            {total['omitidas']}", f"Tiempo de reportes:  {total['segundos']:.2f} segundos", f"Resultado:           {'BUILD SUCCESS' if total['fallos'] + total['errores'] == 0 else 'REVISAR FALLOS'}", ""]
    contenido = "\n".join(lineas) + "\n"
    print(contenido)
    return 0 if total["fallos"] + total["errores"] == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
