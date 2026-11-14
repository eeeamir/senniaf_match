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
esquema=$(runuser -u postgres -- psql -X -d senniaf_demo -Atqc "SELECT to_regclass('public.vinculaciones') IS NOT NULL, to_regclass('public.interrupciones_convivencia') IS NOT NULL")
IFS='|' read -r esquema_vinculaciones esquema_interrupciones <<< "$esquema"
compatible() {
    python3 "$(dirname "$0")/verificar-compatibilidad.py" "$1" "$esquema_vinculaciones" "$esquema_interrupciones"
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
