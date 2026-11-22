#!/usr/bin/env python3
"""Carga aditiva y explícita de la demostración autorizada, sin desplegar la aplicación.

Vista previa (solo lectura): python3 deploy/azure/preparar-demo.py
Aplicar: python3 deploy/azure/preparar-demo.py --aplicar --confirmar SENNIAF_DEMO_AUTORIZADA --familias-nuevas
Las claves se solicitan con getpass. Para un consumidor privado en memoria, añadir
--generar-credenciales: devuelve las claves nuevas por stdout SOLO si es una tubería.
Nunca redirigir esa salida a archivos, registros ni evidencias del repositorio.
Requiere Python bcrypt; usa BCrypt $2a$, coste 10, igual a SecurityConfig.
"""
import argparse
import getpass
import json
import re
import secrets
import subprocess
import sys
from pathlib import Path

BASE = 'senniaf_demo'
SSH = ['ssh', '-i', '/home/anyel/.local/state/senniaf-azure-pbi27/ssh_ed25519',
       '-o', 'UserKnownHostsFile=/home/anyel/.local/state/senniaf-azure-pbi27/known_hosts',
       '-o', 'StrictHostKeyChecking=yes', '-o', 'BatchMode=yes', '-o', 'ConnectTimeout=10',
       'senniafops@20.94.242.158']
TABLAS = ['usuarios', 'perfiles_nino', 'perfiles_familia', 'decisiones_comite',
          'solicitudes_estado_caso', 'evaluaciones_psicologicas', 'auditoria_casos', 'pesos_scoring',
          'nino_habilidades', 'nino_gustos', 'nino_personalidad', 'nino_alergias',
          'nino_condiciones_salud', 'familia_red_apoyo', 'familia_salud_hoy', 'familia_salud_apoyo']


def sql(valor):
    if valor is None:
        return 'NULL'
    if isinstance(valor, bool):
        return 'TRUE' if valor else 'FALSE'
    if isinstance(valor, int):
        return str(valor)
    return "'" + str(valor).replace("'", "''") + "'"


def psql(sentencia):
    # SQL via stdin: no hay claves en argumentos de proceso ni en archivos remotos.
    r = subprocess.run(SSH + [f'sudo -n -u postgres psql -X -qAt -v ON_ERROR_STOP=1 -d {BASE}'],
                       input=sentencia, text=True, capture_output=True, timeout=60)
    if r.returncode:
        # No imprimir SQL ni material de autenticación, incluso ante errores de PostgreSQL.
        raise RuntimeError('La operación PostgreSQL no se completó. No se confirma ninguna cuenta nueva. '
                           'Revisar acceso SSH, esquema, permisos y posibles conflictos del destino autorizado.')
    return r.stdout.strip()


def resumen():
    return """SELECT json_build_object(
      'base',current_database(),
      'usuarios',(SELECT coalesce(json_agg(json_build_object('identificador',identificador,'rol',rol,'verificado',verificado) ORDER BY rol,identificador),'[]') FROM usuarios),
      'ninos',(SELECT coalesce(json_agg(json_build_object('codigo',codigo_caso,'edad',edad,'nivelEscolar',nivel_escolar,'completo',perfil_completo,'ts',trabajador_social_asignado,'ps',psicologo_asignado,'referencia',numero_expediente_institucional) ORDER BY id),'[]') FROM perfiles_nino),
      'familias',(SELECT coalesce(json_agg(json_build_object('codigo',codigo_caso,'usuario',registrado_por,'tipo',tipo_familia,'provincia',provincia,'completa',perfil_completo) ORDER BY id),'[]') FROM perfiles_familia)
    )::text;"""


def cuenta(identificador, rol, hash_clave, solo_si_falta_rol=False):
    condicion = f"AND (SELECT count(*) FROM usuarios WHERE rol={sql(rol)} AND verificado)<2" if solo_si_falta_rol else ''
    return f"""WITH nuevo AS (
      INSERT INTO usuarios(identificador,nombre,apellido,correo,password_hash,rol,verificado,intentos_verificacion,fecha_creacion)
      SELECT {sql(identificador)},'Demostración',{sql(identificador)},{sql(identificador.lower()+'@demo.senniaf.invalid')},
             {sql(hash_clave)},{sql(rol)},TRUE,0,localtimestamp
      WHERE NOT EXISTS(SELECT 1 FROM usuarios WHERE identificador={sql(identificador)}) {condicion}
      RETURNING identificador)
    INSERT INTO _usuarios_nuevos SELECT identificador FROM nuevo;
    DO $cuenta$ BEGIN
      IF NOT EXISTS(SELECT 1 FROM usuarios WHERE identificador={sql(identificador)} AND rol={sql(rol)} AND verificado)
      THEN RAISE EXCEPTION 'Cuenta reservada en conflicto o no habilitada'; END IF;
    END $cuenta$;
    """


def infantil(escenario, letra):
    d = dict(escenario)
    # Compatibilidad con el JAR vigente: las categorías nuevas quedan descritas con OTRA/OTRO.
    # No se despliega código ni se introducen enums desconocidos para ese JAR.
    if d['rendimientoAcademico'] == 'NO_APLICA':
        d['rendimientoAcademico'] = None
    permitidas = {'habilidades': {'NINGUNA_IDENTIFICADA', 'DEPORTES', 'ARTE', 'MUSICA', 'MATEMATICAS',
                                 'LECTURA_ESCRITURA', 'TECNOLOGIA', 'CIENCIAS', 'MANUALIDADES', 'OTRA'},
                 'gustos': {'NINGUNO_IDENTIFICADO', 'DEPORTES', 'VIDEOJUEGOS', 'MUSICA', 'ARTE_DIBUJO',
                            'LECTURA', 'NATURALEZA_ANIMALES', 'BAILE', 'JUEGOS_DE_MESA', 'OTRO'},
                 'personalidad': {'RESERVADO', 'SOCIABLE', 'EXTROVERTIDO', 'TIMIDO', 'CURIOSO',
                                  'SENSIBLE', 'INDEPENDIENTE', 'CARINOSO', 'OTRO'}}
    for grupo, opciones in permitidas.items():
        valores = d[grupo]
        otro = 'OTRA' if grupo == 'habilidades' else 'OTRO'
        d[grupo] = list(dict.fromkeys(v if v in opciones else otro for v in valores))
        if otro in d[grupo] and not d[grupo + 'Detalle'].strip():
            raise ValueError('El escenario debe describir la observación de desarrollo')
    campos = {'edad': 'edad', 'nivelEscolar': 'nivel_escolar', 'gradoEscolar': 'grado_escolar',
              'rendimientoAcademico': 'rendimiento_academico', 'habilidadesDetalle': 'habilidades_detalle',
              'gustosDetalle': 'gustos_detalle', 'personalidadDetalle': 'personalidad_detalle',
              'alergiasDetalle': 'alergias_detalle', 'intoleranciaLactosa': 'intolerancia_lactosa',
              'condicionesSaludDetalle': 'condiciones_salud_detalle', 'necesidadesApoyo': 'necesidades_apoyo',
              'tieneHermanos': 'tiene_hermanos', 'cantidadHermanos': 'cantidad_hermanos',
              'hermanosDetalle': 'hermanos_detalle', 'contextoCultural': 'contexto_cultural'}
    referencia = 'DEMO-FUNCIONAL-' + letra
    nombres = ','.join(campos.values())
    valores = ','.join(sql(d[k]) for k in campos)
    colecciones = [('habilidades', 'nino_habilidades', 'habilidad'), ('gustos', 'nino_gustos', 'gusto'),
                   ('personalidad', 'nino_personalidad', 'rasgo'), ('alergias', 'nino_alergias', 'alergia'),
                   ('condicionesSalud', 'nino_condiciones_salud', 'condicion')]
    listas = '\n'.join(f'INSERT INTO {tabla}(perfil_id,{col}) VALUES (perfil,{sql(v)});'
                       for clave, tabla, col in colecciones for v in d[clave])
    alerta = any(v != 'NINGUNA_CONOCIDA' for v in d['alergias'] + d['condicionesSalud']) or d['intoleranciaLactosa'] == 'SI'
    etapa = ('Juego, comunicación y respuesta a las figuras de cuidado.' if d['edad'] < 6 else
             'Participación escolar, expresión de intereses y relación con pares.' if d['edad'] < 12 else
             'Participación del adolescente, autonomía progresiva y orientación de su formación.')
    evaluacion = f"Escenario simulado de {d['edad']} años. {etapa} {d['personalidadDetalle']}"
    observaciones = f"Información descriptiva del expediente: {d['habilidadesDetalle']} Intereses: {d['gustosDetalle']} {d['hermanosDetalle']}"
    valoracion = 'La valoración se limita al escenario académico y a los apoyos documentados. No se establece diagnóstico clínico.'
    contexto = json.dumps(dict(formato=1, metodos=['REVISION_ANTECEDENTES'], apoyos=['EMOCIONAL', 'RUTINAS'],
                              detalleAcompanamiento=d['necesidadesApoyo']), ensure_ascii=False)
    return f"""DO $infantil$ DECLARE perfil bigint; ts text; ps text; codigo text;
    BEGIN
      IF NOT EXISTS(SELECT 1 FROM perfiles_nino WHERE numero_expediente_institucional={sql(referencia)}) THEN
        SELECT u.identificador INTO ts FROM usuarios u WHERE u.rol='TRABAJADOR_SOCIAL' AND u.verificado
          ORDER BY (SELECT count(*) FROM perfiles_nino n WHERE n.trabajador_social_asignado=u.identificador),u.identificador LIMIT 1;
        SELECT u.identificador INTO ps FROM usuarios u WHERE u.rol='PSICOLOGO' AND u.verificado
          ORDER BY (SELECT count(*) FROM perfiles_nino n WHERE n.psicologo_asignado=u.identificador),u.identificador LIMIT 1;
        IF ts IS NULL OR ps IS NULL THEN RAISE EXCEPTION 'Faltan profesionales habilitados'; END IF;
        SELECT 'NNA-'||lpad(numero::text,greatest(3,length(numero::text)),'0') INTO codigo
          FROM (SELECT coalesce(max(substring(codigo_caso from 5)::bigint),0)+1 AS numero FROM perfiles_nino WHERE codigo_caso ~ '^NNA-[0-9]+$') siguiente;
        INSERT INTO perfiles_nino(codigo_caso,numero_expediente_institucional,{nombres},registrado_por,
          trabajador_social_asignado,psicologo_asignado,fecha_registro,perfil_completo,alerta_medica,baja_logica,cierre_por_adopcion)
        VALUES(codigo,{sql(referencia)},{valores},ts,ts,ps,localtimestamp,TRUE,{sql(alerta)},FALSE,FALSE) RETURNING id INTO perfil;
        {listas}
        INSERT INTO evaluaciones_psicologicas(perfil_id,profesional,fecha,numero_version,evaluacion,observaciones,recomendaciones,
          sin_diagnostico,areas_observadas,hallazgos_profesionales,contexto_evaluacion)
        VALUES(perfil,ps,localtimestamp,1,{sql(evaluacion)},{sql(observaciones)},{sql(d['necesidadesApoyo'])},TRUE,
          '["ADAPTACION_EMOCIONAL","ACOMPANAMIENTO"]',{sql(valoracion)},{sql(contexto)});
        INSERT INTO auditoria_casos(codigo_caso,actor,accion,descripcion,fecha)
        VALUES(codigo,'CONFIGURACION_FICTICIA','VINCULACION_INSTITUCIONAL_FICTICIA',
          json_build_object('rol','OPERACION_TECNICA','resumen','Expediente simulado preparado para demostración; asignación equilibrada exclusiva de este contexto.','trabajadorSocial',ts,'psicologo',ps)::text,localtimestamp);
      END IF;
    END $infantil$;"""


def familiar(escenario, usuario):
    campos = {'tipoFamilia': 'tipo_familia', 'notaEstructuraHogar': 'nota_estructura_hogar',
              'notaRedApoyo': 'nota_red_apoyo', 'provincia': 'provincia', 'disponibilidadViaje': 'disponibilidad_viaje',
              'notaDistancia': 'nota_distancia', 'edadMinimaAceptada': 'edad_minima_aceptada',
              'edadMaximaAceptada': 'edad_maxima_aceptada', 'notaRangoEdad': 'nota_rango_edad',
              'disposicionHermanos': 'disposicion_hermanos', 'notaHermanos': 'nota_hermanos',
              'notaCondicionesSalud': 'nota_condiciones_salud'}
    nombres = ','.join(campos.values())
    valores = ','.join(sql(escenario[k]) for k in campos)
    listas = '\n'.join(f'INSERT INTO {tabla}(perfil_id,{col}) VALUES (perfil,{sql(v)});'
                       for clave, tabla, col in [('redApoyo', 'familia_red_apoyo', 'fuente'),
                                                ('condicionesPuedeAcompanarHoy', 'familia_salud_hoy', 'condicion'),
                                                ('condicionesConApoyo', 'familia_salud_apoyo', 'condicion')]
                       for v in escenario[clave])
    return f"""DO $familiar$ DECLARE perfil bigint; codigo text;
    BEGIN
      IF NOT EXISTS(SELECT 1 FROM perfiles_familia WHERE registrado_por={sql(usuario)}) THEN
        IF NOT EXISTS(SELECT 1 FROM usuarios WHERE identificador={sql(usuario)} AND rol='FAMILIA' AND verificado)
        THEN RAISE EXCEPTION 'Falta la cuenta familiar habilitada'; END IF;
        SELECT 'FAM-'||lpad(numero::text,greatest(3,length(numero::text)),'0') INTO codigo
          FROM (SELECT coalesce(max(substring(codigo_caso from 5)::bigint),0)+1 AS numero FROM perfiles_familia WHERE codigo_caso ~ '^FAM-[0-9]+$') siguiente;
        INSERT INTO perfiles_familia(codigo_caso,registrado_por,{nombres},fecha_registro,perfil_completo,reevaluacion_pendiente,numero_reevaluaciones)
          VALUES(codigo,{sql(usuario)},{valores},localtimestamp,TRUE,FALSE,0) RETURNING id INTO perfil;
        {listas}
        INSERT INTO auditoria_casos(codigo_caso,actor,accion,descripcion,fecha)
        VALUES(codigo,'CONFIGURACION_FICTICIA','CONTENIDO_ACADEMICO_ACTUALIZADO',
          '{{"rol":"OPERACION_TECNICA","resumen":"Entrevista simulada nueva preparada para demostración, sin modificar familias anteriores."}}',localtimestamp);
      END IF;
    END $familiar$;"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--aplicar', action='store_true')
    parser.add_argument('--confirmar')
    parser.add_argument('--familias-nuevas', action='store_true', help='Autorización expresa de las dos cuentas familiares adicionales')
    parser.add_argument('--generar-credenciales', action='store_true', help='Entrega privada por tubería; nunca guardar la salida')
    args = parser.parse_args()
    estado = json.loads(psql(resumen()))
    if estado['base'] != BASE:
        raise RuntimeError('El destino no es la base de demostración autorizada')
    if not args.aplicar:
        print(json.dumps(estado, ensure_ascii=False))
        return
    if args.confirmar != 'SENNIAF_DEMO_AUTORIZADA':
        raise ValueError('Se requiere --confirmar SENNIAF_DEMO_AUTORIZADA; no se modificó la base')
    if args.generar_credenciales and sys.stdout.isatty():
        raise ValueError('La salida de credenciales exige un consumidor privado por tubería')
    import bcrypt
    nuevas = []
    existentes = {u['identificador']: u for u in estado['usuarios']}
    requeridas = []
    for rol, identificador in [('TRABAJADOR_SOCIAL', 'TS-002'), ('PSICOLOGO', 'PS-015'), ('COMITE', 'CM-002')]:
        if sum(u['rol'] == rol and u['verificado'] for u in estado['usuarios']) < 2:
            if identificador in existentes:
                raise ValueError('Una cuenta profesional reservada ya existe; no se modifican sus credenciales')
            requeridas.append((identificador, rol, True))
    if args.familias_nuevas:
        for identificador in ['DEMO-FAM-002', 'DEMO-FAM-003']:
            if identificador in existentes:
                if existentes[identificador]['rol'] != 'FAMILIA' or not existentes[identificador]['verificado']:
                    raise ValueError('Una cuenta familiar reservada está en conflicto; no se modifica')
            else:
                requeridas.append((identificador, 'FAMILIA', False))
    sentencias = ["BEGIN; SET LOCAL lock_timeout='5s'; SET LOCAL statement_timeout='30s';",
                  "DO $destino$ BEGIN IF current_database()<>'senniaf_demo' THEN RAISE EXCEPTION 'Destino no autorizado'; END IF; END $destino$;",
                  "SELECT pg_advisory_xact_lock(7362112201); SELECT pg_advisory_xact_lock(7362112202);",
                  'LOCK TABLE '+','.join('public.'+t for t in TABLAS)+' IN SHARE ROW EXCLUSIVE MODE;',
                  'CREATE TEMP TABLE _antes(tabla text,huella text,cantidad bigint) ON COMMIT DROP;',
                  'CREATE TEMP TABLE _usuarios_nuevos(identificador text);']
    for tabla in TABLAS:
        sentencias.append(f"INSERT INTO _antes SELECT {sql(tabla)},md5(to_jsonb(t)::text),count(*) FROM {tabla} t GROUP BY md5(to_jsonb(t)::text);")
    for identificador, rol, solo_si_falta in requeridas:
        clave = secrets.token_urlsafe(24) if args.generar_credenciales else getpass.getpass(f'Clave nueva de {identificador} (16–72 bytes): ')
        if not 16 <= len(clave.encode('utf-8')) <= 72:
            raise ValueError('La clave debe tener entre 16 y 72 bytes')
        if any(c['password'] == clave for c in nuevas):
            raise ValueError('Las claves nuevas deben ser únicas')
        hash_clave = bcrypt.hashpw(clave.encode('utf-8'), bcrypt.gensalt(rounds=10, prefix=b'2a')).decode('ascii')
        nuevas.append(dict(identificador=identificador, rol=rol, password=clave))
        sentencias.append(cuenta(identificador, rol, hash_clave, solo_si_falta))
    catalogo = json.loads((Path(__file__).resolve().parents[2]/'src/main/resources/static/escenarios-demo.json').read_text())
    for i, letra in enumerate('ABC'):
        sentencias.append(infantil(catalogo['nino'][i], letra))
    if args.familias_nuevas:
        sentencias += [familiar(catalogo['familia'][1], 'DEMO-FAM-002'), familiar(catalogo['familia'][2], 'DEMO-FAM-003')]
    # Invariante de la operación: todas las filas previas siguen existiendo con los mismos valores.
    for tabla in TABLAS:
        sentencias.append(f"""DO $preservar$ BEGIN IF EXISTS(
          SELECT 1 FROM _antes a LEFT JOIN (SELECT md5(to_jsonb(t)::text) huella,count(*) cantidad FROM {tabla} t GROUP BY md5(to_jsonb(t)::text)) b USING(huella)
          WHERE a.tabla={sql(tabla)} AND coalesce(b.cantidad,0)<a.cantidad)
          THEN RAISE EXCEPTION 'Una fila anterior cambió; se cancela toda la carga'; END IF; END $preservar$;""")
    sentencias += ['COMMIT;', "SELECT json_build_object('creadas',coalesce(json_agg(identificador),'[]'))::text FROM _usuarios_nuevos;", resumen()]
    salida = psql('\n'.join(sentencias)).splitlines()
    objetos = [json.loads(linea) for linea in salida if linea.startswith('{')]
    creadas = set(objetos[-2]['creadas'])
    resultado = objetos[-1]
    resultado['credencialesNuevas'] = [c if args.generar_credenciales else {k:v for k,v in c.items() if k!='password'}
                                     for c in nuevas if c['identificador'] in creadas]
    resultado['filasAnterioresPreservadas'] = True
    print(json.dumps(resultado, ensure_ascii=False))


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, ValueError, OSError, subprocess.SubprocessError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
