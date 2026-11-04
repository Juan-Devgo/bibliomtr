#!/bin/sh
# Elige el modo de nginx y copia su plantilla a /etc/nginx/templates/default.conf.template,
# que el script oficial 20-envsubst-on-templates.sh convierte en /etc/nginx/conf.d/default.conf.
# Modo https: DOMAIN definido y fullchain.pem y privkey.pem presentes. Cualquier otro caso: modo http.
# Con el argumento --print solo imprime el modo (lo usa 40-watch-certificate.sh).
set -eu

AVAILABLE_DIR="${NGINX_TEMPLATES_AVAILABLE:-/etc/nginx/templates-available}"
TEMPLATE_DIR="${NGINX_ENVSUBST_TEMPLATE_DIR:-/etc/nginx/templates}"
CERT_DIR="${NGINX_CERT_DIR:-/etc/letsencrypt/live}"
domain="${DOMAIN:-}"

detect_mode() {
  if [ -z "$domain" ]; then
    echo http
    return
  fi
  case "$domain" in
    *[!A-Za-z0-9.-]*)
      echo "15-select-nginx-mode: DOMAIN con caracteres no válidos; se usa el modo http" >&2
      echo http
      return
      ;;
  esac
  if [ -s "$CERT_DIR/$domain/fullchain.pem" ] && [ -s "$CERT_DIR/$domain/privkey.pem" ]; then
    echo https
  else
    echo http
  fi
}

mode="$(detect_mode)"

if [ "${1:-}" = "--print" ]; then
  echo "$mode"
  exit 0
fi

mkdir -p "$TEMPLATE_DIR"
cp "$AVAILABLE_DIR/$mode.conf.template" "$TEMPLATE_DIR/default.conf.template"
echo "15-select-nginx-mode: modo $mode (DOMAIN='${domain}')"
