#!/usr/bin/env bash
# Publicación manual del JAR; nunca crea, migra ni restaura automáticamente la base.
set -euo pipefail
[[ $EUID -eq 0 && $# -eq 2 ]] || { echo 'Uso: sudo actualizar.sh nuevo.jar SHA256_ESPERADO' >&2; exit 1; }
artefacto=$(realpath -- "$1")
esperado=$2
[[ $esperado =~ ^[0-9a-f]{64}$ && -f $artefacto ]] || exit 1
[[ $(sha256sum "$artefacto" | cut -d' ' -f1) == "$esperado" ]] || { echo 'SHA-256 incorrecto.' >&2; exit 1; }
[[ -f /etc/senniaf/remoto.env && -d /srv/senniaf/releases ]] || { echo 'Falta el aprovisionamiento revisado.' >&2; exit 1; }
exec 9>/run/lock/senniaf-actualizar.lock
flock -n 9 || { echo 'Hay otra actualización en curso.' >&2; exit 1; }
anterior=''
if [[ -e /srv/senniaf/current.jar && ! -L /srv/senniaf/current.jar ]]; then
    echo 'El JAR actual no está versionado mediante enlace: revisar antes de sustituirlo.' >&2; exit 1
fi
if [[ -L /srv/senniaf/current.jar && -f /srv/senniaf/current.jar ]]; then
    anterior=$(readlink -f /srv/senniaf/current.jar)
    [[ $anterior =~ ^/srv/senniaf/releases/[0-9a-f]{64}\.jar$ ]] || { echo 'Ruta anterior inesperada: revisar sin sustituirla.' >&2; exit 1; }
fi
# El esquema es persistente: un rollback de código no debe omitir las vinculaciones.
esquema=$(runuser -u postgres -- psql -X -d senniaf_demo -Atqc "SELECT to_regclass('public.vinculaciones') IS NOT NULL, to_regclass('public.interrupciones_convivencia') IS NOT NULL, EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='vinculaciones' AND column_name='fecha_cierre'), CASE WHEN to_regclass('public.programaciones_pesos') IS NOT NULL AND to_regclass('public.combinaciones_excluidas') IS NOT NULL THEN 't' WHEN to_regclass('public.programaciones_pesos') IS NULL AND to_regclass('public.combinaciones_excluidas') IS NULL THEN 'f' ELSE 'incompleto' END, EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass('public.vinculaciones') AND conname='vinculaciones_cierre_registro_check'), (SELECT CASE count(*) WHEN 5 THEN 't' WHEN 0 THEN 'f' ELSE 'incompleto' END FROM information_schema.columns WHERE table_schema='public' AND table_name='perfiles_nino' AND data_type='text' AND column_name IN ('idioma_comunicacion','costumbres_tradiciones','vinculos_culturales','opinion_cultural','recomendaciones_culturales')), CASE WHEN (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='perfiles_familia' AND data_type='text' AND column_name IN ('apoyo_idiomas','respeto_tradiciones','recursos_comunitarios','respeto_preferencias','compromiso_cultural'))=5 AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='interrupciones_convivencia' AND column_name='motivo' AND data_type='character varying') AND (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='interrupciones_convivencia' AND column_name IN ('fecha_interrupcion','referencia_actuacion') AND is_nullable='YES')=2 AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass('public.interrupciones_convivencia') AND conname='interrupciones_motivo_check') THEN 't' WHEN NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND ((table_name='perfiles_familia' AND column_name IN ('apoyo_idiomas','respeto_tradiciones','recursos_comunitarios','respeto_preferencias','compromiso_cultural')) OR (table_name='interrupciones_convivencia' AND column_name='motivo'))) THEN 'f' ELSE 'incompleto' END, CASE WHEN (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='vinculaciones' AND column_name IN ('referencia_autorizacion','fecha_emision_autorizacion','autorizacion_registrada_por','autorizacion_rol','fecha_registro_autorizacion','fecha_inicio_convivencia','inicio_registrado_por','inicio_rol','fecha_registro_inicio'))=9 AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='decisiones_comite' AND column_name='apartados_reevaluacion' AND data_type='jsonb') AND EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='decisiones_comite' AND column_name='categoria_reevaluacion') AND (SELECT count(*) FROM pg_trigger WHERE (tgrelid=to_regclass('public.interrupciones_convivencia') AND tgname='interrupcion_autorizada_insert' OR tgrelid=to_regclass('public.decisiones_comite') AND tgname='combinacion_interrumpida_insert') AND tgenabled='O' AND NOT tgisinternal)=2 AND (SELECT count(*) FROM pg_constraint WHERE conrelid=to_regclass('public.vinculaciones') AND conname IN ('vinculaciones_autorizacion_check','vinculaciones_inicio_check'))=2 AND (SELECT count(*) FROM pg_constraint WHERE conrelid=to_regclass('public.decisiones_comite') AND conname IN ('decisiones_apartados_check','decisiones_categoria_reevaluacion_check'))=2 THEN 't' WHEN NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND ((table_name='vinculaciones' AND column_name IN ('referencia_autorizacion','fecha_emision_autorizacion','autorizacion_registrada_por','autorizacion_rol','fecha_registro_autorizacion','fecha_inicio_convivencia','inicio_registrado_por','inicio_rol','fecha_registro_inicio')) OR (table_name='decisiones_comite' AND column_name IN ('apartados_reevaluacion','categoria_reevaluacion')))) THEN 'f' ELSE 'incompleto' END")
# 020 es aditiva; se comprueba sin aplicar migraciones ni modificar registros.
esquema_citas=$(runuser -u postgres -- psql -X -d senniaf_demo -Atq <<'SQL_CITAS'
SELECT CASE WHEN
 (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='citas_vinculacion' AND column_name IN ('id','vinculacion_id','profesional','creado_por','motivo','estado','inicio','fin','duracion','mensaje_inicial','mensaje','respuesta','ultimo_rol','creada','actualizada','version'))=16
 AND (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='citas_historial' AND column_name IN ('id','cita_id','actor','rol','profesional','accion','anterior','nuevo','motivo_respuesta','mensaje','inicio','fin','duracion','fecha'))=14
 AND EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid=to_regclass('public.citas_vinculacion') AND conname='citas_intervalo_check' AND convalidated)
 AND EXISTS(SELECT 1 FROM pg_index WHERE indexrelid=to_regclass('public.citas_una_abierta_por_vinculacion') AND indisunique AND indisvalid AND indpred IS NOT NULL)
 AND EXISTS(SELECT 1 FROM pg_trigger WHERE tgrelid=to_regclass('public.citas_historial') AND tgname='citas_historial_inmutable' AND tgenabled='O' AND NOT tgisinternal)
 THEN 't' WHEN to_regclass('public.citas_vinculacion') IS NULL AND to_regclass('public.citas_historial') IS NULL THEN 'f' ELSE 'incompleto' END;
SQL_CITAS
)
IFS='|' read -r esquema_vinculaciones esquema_interrupciones esquema_cierre esquema_refuerzos esquema_cierre_servidor esquema_cultura esquema_apoyos esquema_autorizacion <<< "$esquema"
compatible() {
    python3 "$(dirname "$0")/verificar-compatibilidad.py" "$1" "$esquema_vinculaciones" "$esquema_interrupciones" "$esquema_cierre" "$esquema_refuerzos" "$esquema_cierre_servidor" "$esquema_cultura" "$esquema_apoyos" "$esquema_autorizacion" "$esquema_citas"
}
compatible "$artefacto" || exit 1
# Respaldo consistente antes de la intervención; no se sustituye el volumen ni la configuración.
"$(dirname "$0")/respaldo.sh"
destino="/srv/senniaf/releases/$esperado.jar"
if [[ -f $destino ]]; then
    [[ $(sha256sum "$destino" | cut -d' ' -f1) == "$esperado" ]] || { echo 'La versión existente no coincide con su huella; revisar sin sustituirla.' >&2; exit 1; }
else
    install -m 644 -- "$artefacto" "$destino"
fi
activar() {
    ln -sfn "releases/$1.jar" /srv/senniaf/current.jar.nuevo || return 1
    mv -Tf /srv/senniaf/current.jar.nuevo /srv/senniaf/current.jar || return 1
}
comprobar() {
    local ok=0
    for ((intento=0; intento<90; intento++)); do
        if systemctl is-active --quiet senniaf.service && curl -fsS --max-time 2 http://127.0.0.1:8080/ >/dev/null; then
            ok=$((ok+1)); [[ $ok -ge 3 ]] && return 0
        else ok=0; fi
        sleep 1
    done
    return 1
}
systemctl stop senniaf.service
if activar "$esperado" && systemctl start senniaf.service && comprobar; then echo "Servicio actualizado: $esperado. Falta la comprobación autenticada y de navegador."; exit 0; fi
echo 'La nueva versión no está disponible. Intentando recuperar solo el JAR anterior.' >&2
systemctl stop senniaf.service
if [[ -n $anterior && -f $anterior ]]; then
    if ! compatible "$anterior"; then
        echo 'Se conserva la base y el servicio queda detenido. Publica una versión compatible; no se restauraron ni borraron datos.' >&2
        exit 1
    fi
    previo=$(basename "$anterior" .jar)
    if ! activar "$previo" || ! systemctl start senniaf.service || ! comprobar; then
        echo 'La recuperación requiere intervención. No se restauró ni borró la base.' >&2
    fi
fi
exit 1
