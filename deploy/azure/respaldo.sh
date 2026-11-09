#!/usr/bin/env bash
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo 'Ejecutar como root en el servidor remoto dedicado.' >&2; exit 1; }
umask 077
install -d -m 700 /var/backups/senniaf
archivo=$(mktemp /var/backups/senniaf/"$(date -u +%Y%m%dT%H%M%SZ)".XXXXXX.dump)
# Acceso local por peer; no imprime ni necesita contraseñas en argumentos.
if ! runuser -u postgres -- pg_dump -Fc -d senniaf_demo > "$archivo"; then
    echo 'Respaldo incompleto: no usar para recuperación.' >&2; exit 1
fi
pg_restore --list "$archivo" >/dev/null
sha256sum "$archivo"
# Configuración opcional sin claves; acceso Blob mediante identidad administrada de Azure.
if [[ -f /etc/senniaf/respaldo.env ]]; then
    set -a
    source /etc/senniaf/respaldo.env
    set +a
    /usr/local/lib/senniaf/subir-respaldo.py "$archivo"
fi
