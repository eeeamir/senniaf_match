"""Pruebas sin acceso a Azure, PostgreSQL ni systemd."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

RAIZ = Path(__file__).resolve().parents[3]
especificacion = importlib.util.spec_from_file_location("compatibilidad", RAIZ / "deploy/azure/verificar-compatibilidad.py")
modulo = importlib.util.module_from_spec(especificacion)
especificacion.loader.exec_module(modulo)


class CompatibilidadDespliegueTest(unittest.TestCase):
    def setUp(self):
        (RAIZ / "target").mkdir(exist_ok=True)
        self.directorio = tempfile.TemporaryDirectory(dir=RAIZ / "target")
        self.addCleanup(self.directorio.cleanup)
        self.anterior = Path(self.directorio.name) / "anterior.jar"
        self.actual = Path(self.directorio.name) / "hu19.jar"
        with zipfile.ZipFile(self.anterior, "w") as jar:
            jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
        with zipfile.ZipFile(self.actual, "w") as jar:
            jar.writestr("BOOT-INF/classes/com/senniaf/match/model/Vinculacion.class", b"fixture")

    def test_version_anterior_con_esquema_anterior(self):
        self.assertTrue(modulo.compatible(self.anterior, "f")[0])

    def test_hu19_requiere_su_migracion(self):
        self.assertFalse(modulo.compatible(self.actual, "f")[0])

    def test_impide_regreso_anterior_tras_migrar(self):
        self.assertFalse(modulo.compatible(self.anterior, "t")[0])

    def test_hu19_con_esquema_migrado(self):
        self.assertTrue(modulo.compatible(self.actual, "t")[0])

    def test_esquema_no_confirmado_no_publica(self):
        self.assertFalse(modulo.compatible(self.actual, "")[0])

    def test_artefacto_ilegible_no_publica(self):
        self.actual.write_bytes(b"no es un jar")
        self.assertFalse(modulo.compatible(self.actual, "t")[0])

    def test_hu11_requiere_migracion_013(self):
        with zipfile.ZipFile(self.actual,"a") as jar:
            jar.writestr("BOOT-INF/classes/com/senniaf/match/model/InterrupcionConvivencia.class",b"fixture")
        self.assertFalse(modulo.compatible(self.actual,"t","f")[0])
        self.assertTrue(modulo.compatible(self.actual,"t","t")[0])

    def test_hu19_no_recupera_sin_revision_de_interrupciones(self):
        self.assertFalse(modulo.compatible(self.actual,"t","t")[0])

    def test_cierre_requiere_014_y_no_recupera_jar_sin_cierre(self):
        with zipfile.ZipFile(self.actual,"a") as jar:
            jar.writestr("BOOT-INF/classes/com/senniaf/match/model/InterrupcionConvivencia.class",b"fixture")
            jar.writestr("BOOT-INF/classes/com/senniaf/match/dto/CierreInstitucionalRequest.class",b"fixture")
        self.assertFalse(modulo.compatible(self.actual,"t","t","f")[0])
        self.assertTrue(modulo.compatible(self.actual,"t","t","t")[0])
        self.assertFalse(modulo.compatible(self.anterior,"t","t","t")[0])

    def test_refuerzos_exigen_015_y_rechazan_recuperacion_anterior(self):
        with zipfile.ZipFile(self.actual,"a") as jar:
            jar.writestr("BOOT-INF/classes/com/senniaf/match/model/InterrupcionConvivencia.class",b"fixture")
            jar.writestr("BOOT-INF/classes/com/senniaf/match/dto/CierreInstitucionalRequest.class",b"fixture")
        self.assertFalse(modulo.compatible(self.actual,"t","t","t","t")[0])
        with zipfile.ZipFile(self.actual,"a") as jar:
            jar.writestr("BOOT-INF/classes/com/senniaf/match/service/PesosProgramadosService.class",b"fixture")
        self.assertFalse(modulo.compatible(self.actual,"t","t","t","f")[0])
        self.assertTrue(modulo.compatible(self.actual,"t","t","t","t")[0])
        self.assertFalse(modulo.compatible(self.actual,"t","t","t","incompleto")[0])

if __name__ == "__main__":
    unittest.main()
