#!/bin/sh
# Vigilante en segundo plano: cada 60 s compara el modo de nginx y el cksum del certificado.
# Si cambian (emisión inicial HTTP -> HTTPS, o renovación), repite la selección y el envsubst,
# valida con nginx -t (si falla, restaura la configuración anterior) y ejecuta nginx -s reload,
# sin detener ni reiniciar el contenedor. Con DOMAIN vacío termina de inmediato.
set -eu

if [ -z "${DOMAIN:-}" ]; then
  echo "40-watch-certificate: DOMAIN vacío; no se vigila el certificado"
  exit 0
fi

SELECT=/docker-entrypoint.d/15-select-nginx-mode.sh
ENVSUBST=/docker-entrypoint.d/20-envsubst-on-templates.sh
CERT_DIR="${NGINX_CERT_DIR:-/etc/letsencrypt/live}"
CONF=/etc/nginx/conf.d/default.conf
TEMPLATE=/etc/nginx/templates/default.conf.template
INTERVAL="${CERT_WATCH_INTERVAL:-60}"

current_state() {
  mode="$("$SELECT" --print)"
  cert="$CERT_DIR/${DOMAIN}/fullchain.pem"
  if [ -s "$cert" ]; then
    sum="$(cksum < "$cert")"
  else
    sum="sin-certificado"
  fi
  echo "$mode $sum"
}

watch_loop() {
  set +e
  last="$(current_state)"
  while true; do
    sleep "$INTERVAL"
    now="$(current_state)"
    if [ "$now" = "$last" ]; then
      continue
    fi
    echo "40-watch-certificate: cambio detectado ($last -> $now); se recarga nginx"
    backup="$(mktemp -d)"
    cp "$CONF" "$backup/default.conf"
    cp "$TEMPLATE" "$backup/default.conf.template"
    "$SELECT" && "$ENVSUBST"
    if nginx -t; then
      if nginx -s reload; then
        last="$now"
      else
        echo "40-watch-certificate: nginx -s reload falló" >&2
      fi
    else
      echo "40-watch-certificate: la nueva configuración no es válida; se restaura la anterior" >&2
      cp "$backup/default.conf" "$CONF"
      cp "$backup/default.conf.template" "$TEMPLATE"
    fi
    rm -rf "$backup"
  done
}

watch_loop </dev/null &
echo "40-watch-certificate: vigilando el certificado de ${DOMAIN} cada ${INTERVAL} s"
