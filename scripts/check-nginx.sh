#!/usr/bin/env bash
# Valida la configuración de nginx de la imagen `web` con `nginx -t` dentro de un contenedor,
# en modo HTTP (sin dominio y con dominio sin certificado) y en modo HTTPS (certificado autofirmado
# temporal). No usa AWS. Requiere Docker y openssl. Uso: scripts/check-nginx.sh
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

IMAGE=bibliomtr-web-check
TEST_DOMAIN=prueba.example

for cmd in docker openssl; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "Falta '$cmd' en el PATH." >&2; exit 1; }
done

CERT_ROOT="$(mktemp -d)"
trap 'rm -rf "$CERT_ROOT"' EXIT

echo "==> Construyendo la imagen web ($IMAGE)"
docker build -q -t "$IMAGE" web >/dev/null

echo "==> Generando un certificado autofirmado temporal para $TEST_DOMAIN"
mkdir -p "$CERT_ROOT/live/$TEST_DOMAIN"
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj "/CN=$TEST_DOMAIN" \
  -keyout "$CERT_ROOT/live/$TEST_DOMAIN/privkey.pem" \
  -out "$CERT_ROOT/live/$TEST_DOMAIN/fullchain.pem" 2>/dev/null
chmod -R a+rX "$CERT_ROOT"
chmod a+r "$CERT_ROOT/live/$TEST_DOMAIN/"*.pem

# Ejecuta `nginx -t` tras los scripts de entrada y comprueba que se eligió el modo esperado.
check_mode() {
  local expected="$1" label="$2"
  shift 2
  local output
  echo "==> nginx -t: $label"
  if ! output="$(docker run --rm --add-host api:127.0.0.1 "$@" "$IMAGE" nginx -t 2>&1)"; then
    echo "$output" >&2
    echo "FALLO: nginx -t en $label" >&2
    exit 1
  fi
  if ! grep -q "modo $expected" <<<"$output"; then
    echo "$output" >&2
    echo "FALLO: se esperaba el modo $expected en $label" >&2
    exit 1
  fi
  echo "    correcto (modo $expected)"
}

check_mode http "HTTP, DOMAIN vacío"
check_mode http "HTTP, DOMAIN definido sin certificado" -e "DOMAIN=$TEST_DOMAIN"
check_mode https "HTTPS, DOMAIN con certificado" -e "DOMAIN=$TEST_DOMAIN" -v "$CERT_ROOT:/etc/letsencrypt:ro"

echo "nginx -t correcto en los modos HTTP y HTTPS"
