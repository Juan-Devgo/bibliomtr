#!/bin/sh
# Ciclo de emisión y renovación del certificado de Let's Encrypt (HTTP-01 por webroot).
# Corre dentro del contenedor certbot (imagen certbot/certbot, shell /bin/sh de Alpine).
# - DOMAIN vacío: no ejecuta certbot (el stack funciona solo por HTTP).
# - Sin certificado: certonly; si falla, reintenta a los 30 min (4 intentos por hora).
# - Con certificado: renew cada 12 h (2 intentos por día).
# - CERTBOT_STAGING=1 usa el entorno de pruebas de Let's Encrypt.
# Cada ciclo (60 s) actualiza /tmp/certbot-alive, que lee el healthcheck del servicio.

WEBROOT=/var/www/certbot
ALIVE=/tmp/certbot-alive
LIVE_DIR=/etc/letsencrypt/live
CYCLE_SECONDS=60
RETRY_SECONDS=1800
RENEW_SECONDS=43200

log() {
  echo "certbot-loop: $*"
}

DOMAIN="${DOMAIN:-}"
EMAIL="${CERTBOT_EMAIL:-}"
STAGING_FLAG=""
if [ "${CERTBOT_STAGING:-0}" = "1" ]; then
  STAGING_FLAG="--staging"
fi

if [ -z "$DOMAIN" ]; then
  log "DOMAIN vacío: no se emite ni se renueva ningún certificado (solo HTTP)"
  while true; do
    touch "$ALIVE"
    sleep "$CYCLE_SECONDS"
  done
fi

if [ -z "$EMAIL" ]; then
  log "CERTBOT_EMAIL vacío: defina CERTBOT_EMAIL en .env para emitir el certificado"
fi

next_issue=0
next_renew=0

while true; do
  touch "$ALIVE"
  now="$(date +%s)"

  if [ -s "$LIVE_DIR/$DOMAIN/fullchain.pem" ]; then
    if [ "$now" -ge "$next_renew" ]; then
      log "renovando (solo se renueva si faltan menos de 30 días)"
      certbot renew --webroot -w "$WEBROOT" --non-interactive \
        || log "la renovación falló; se reintenta en 12 h"
      next_renew=$((now + RENEW_SECONDS))
    fi
  elif [ -n "$EMAIL" ] && [ "$now" -ge "$next_issue" ]; then
    log "emitiendo certificado para $DOMAIN ${STAGING_FLAG}"
    # shellcheck disable=SC2086
    if certbot certonly --webroot -w "$WEBROOT" -d "$DOMAIN" \
      --email "$EMAIL" --agree-tos --no-eff-email --non-interactive \
      --keep-until-expiring $STAGING_FLAG; then
      log "certificado emitido para $DOMAIN"
    else
      log "no se pudo emitir (¿DNS sin propagar o límite de Let's Encrypt?); se reintenta en 30 min"
      next_issue=$((now + RETRY_SECONDS))
    fi
  fi

  touch "$ALIVE"
  sleep "$CYCLE_SECONDS"
done
