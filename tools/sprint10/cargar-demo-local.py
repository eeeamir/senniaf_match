#!/usr/bin/env python3
"""Registra exclusivamente el par ficticio del Sprint 10 en PostgreSQL por socket local."""
import argparse
import getpass
import json
import re
import runpy
import subprocess
from pathlib import Path

import bcrypt

RAIZ = Path(__file__).resolve().parents[2]
GENERADOR = runpy.run_path(str(RAIZ / 'deploy/azure/preparar-demo.py'))
USUARIO = 'DEMO-FAM-004'
CORREO = 'demo-fam-004@demo.senniaf.invalid'
REFERENCIA = 'DEMO-FUNCIONAL-SPRINT10'


def ejecutar(args, sentencia):
    proceso = subprocess.run(
        ['psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1', '-h', str(args.socket),
         '-p', str(args.puerto), '-d', args.base, '-U', args.administrador],
        input=sentencia, text=True, capture_output=True, timeout=60,
    )
    if proceso.returncode:
        raise SystemExit('PostgreSQL no completó la carga. Comprueba el esquema 023 y el destino local.')
    return proceso.stdout.strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--socket', required=True, type=Path)
    parser.add_argument('--puerto', type=int, default=5432)
    parser.add_argument('--base', required=True)
    parser.add_argument('--administrador', required=True)
    args = parser.parse_args()
    if (not args.socket.is_absolute() or not (args.socket / f'.s.PGSQL.{args.puerto}').is_socket()
            or not re.fullmatch(r'[a-z0-9_]+', args.base)):
        parser.error('Selecciona el socket Unix y el nombre de una base local autorizada.')

    estado = ejecutar(args, f"""
        SELECT CASE WHEN inet_server_addr() IS NOT NULL OR to_regclass('public.visitas_domiciliarias') IS NULL THEN 'INVALIDO'
            WHEN EXISTS (SELECT 1 FROM usuarios WHERE identificador='{USUARIO}') THEN 'EXISTE'
            ELSE 'NUEVO' END;
    """)
    if estado == 'INVALIDO':
        raise SystemExit('El destino debe ser local y tener las migraciones hasta la 023.')

    clave_hash = None
    if estado == 'NUEVO':
        clave = getpass.getpass('Clave nueva de DEMO-FAM-004 (16–72 bytes): ').encode()
        if not 16 <= len(clave) <= 72:
            raise SystemExit('La clave debe tener entre 16 y 72 bytes.')
        clave_hash = bcrypt.hashpw(clave, bcrypt.gensalt(rounds=10, prefix=b'2a')).decode()

    datos = json.loads(Path(__file__).with_name('perfiles-ficticios.json').read_text())
    sql = GENERADOR['sql']
    cuenta = f"""INSERT INTO usuarios(identificador,nombre,apellido,correo,password_hash,rol,verificado,intentos_verificacion,fecha_creacion)
        SELECT '{USUARIO}','Ada','Varela (Demo)','{CORREO}',{sql(clave_hash)},'FAMILIA',TRUE,0,localtimestamp
        WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE identificador='{USUARIO}');""" if clave_hash else ''
    resultado = ejecutar(args, f"""
        BEGIN;
        SET LOCAL lock_timeout = '5s';
        LOCK TABLE usuarios, perfiles_nino, perfiles_familia IN SHARE ROW EXCLUSIVE MODE;
        {cuenta}
        DO $cuenta$ BEGIN
            IF NOT EXISTS (SELECT 1 FROM usuarios WHERE identificador='{USUARIO}' AND correo='{CORREO}'
                AND rol='FAMILIA' AND verificado)
            THEN RAISE EXCEPTION 'Cuenta en conflicto'; END IF;
        END $cuenta$;
        {GENERADOR['infantil'](datos['nino'], 'SPRINT10', evaluacion_personalizada=datos['evaluacion'])}
        {GENERADOR['familiar'](datos['familia'], USUARIO, incluir_apoyos_culturales=True)}
        COMMIT;
        SELECT json_build_object('usuario','{USUARIO}',
            'familia',(SELECT codigo_caso FROM perfiles_familia WHERE registrado_por='{USUARIO}'),
            'infantil',(SELECT codigo_caso FROM perfiles_nino WHERE numero_expediente_institucional='{REFERENCIA}'));
    """)
    print(resultado)


if __name__ == '__main__':
    main()
