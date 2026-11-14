#!/usr/bin/env python3
"""Impide que una versión anterior escriba decisiones sin su vinculación correspondiente."""
import sys
import zipfile


def compatible(archivo, esquema_vinculaciones, esquema_interrupciones="f", esquema_cierre="f"):
    if esquema_vinculaciones not in {"t", "f"} or esquema_interrupciones not in {"t", "f"} or esquema_cierre not in {"t", "f"}:
        return False, "No se pudo confirmar el esquema. Se conserva el servicio sin sustituir el JAR."
    try:
        with zipfile.ZipFile(archivo) as jar:
            tiene_hu19 = "BOOT-INF/classes/com/senniaf/match/model/Vinculacion.class" in jar.namelist()
            tiene_cierre = "BOOT-INF/classes/com/senniaf/match/dto/CierreInstitucionalRequest.class" in jar.namelist()
            tiene_hu11 = "BOOT-INF/classes/com/senniaf/match/model/InterrupcionConvivencia.class" in jar.namelist()
    except (OSError, zipfile.BadZipFile):
        return False, "El archivo no es un JAR legible. No se sustituirá la versión actual."
    if tiene_hu19 and esquema_vinculaciones == "f":
        return False, "Falta la migración 012. Revisa respaldo y procedimiento antes de actualizar."
    if not tiene_hu19 and esquema_vinculaciones == "t":
        return False, "La base utiliza vinculaciones. Esta versión anterior no puede publicarse ni recuperarse automáticamente."
    if tiene_hu11 and esquema_interrupciones == "f":
        return False, "Falta la migración 013 de interrupciones. No se sustituirá la versión actual."
    if not tiene_hu11 and esquema_interrupciones == "t":
        return False, "La base registra interrupciones. Esta versión anterior omitiría su revisión."
    if tiene_cierre != (esquema_cierre == "t"):
        return False, "El JAR requiere un esquema de cierres compatible (migración 014). No se sustituirá ni recuperará esta versión."
    return True, "El JAR corresponde a la etapa actual del esquema de vinculaciones."


if __name__ == "__main__":
    if len(sys.argv) not in {3,4,5}:
        sys.exit("Uso: verificar-compatibilidad.py archivo.jar vinculaciones:t|f [interrupciones:t|f] [cierres:t|f]")
    valido, mensaje = compatible(sys.argv[1], sys.argv[2],sys.argv[3] if len(sys.argv)>=4 else "f",sys.argv[4] if len(sys.argv)==5 else "f")
    print(mensaje)
    sys.exit(0 if valido else 1)
