FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.29.3-alpine
# Plantillas por modo (HTTP/HTTPS), snippet compartido y webroot del desafío ACME.
RUN mkdir -p /etc/nginx/templates /etc/nginx/templates-available /etc/nginx/snippets /var/www/certbot
COPY nginx.conf /etc/nginx/templates-available/http.conf.template
COPY nginx-https.conf /etc/nginx/templates-available/https.conf.template
COPY nginx-locations.conf /etc/nginx/snippets/app-locations.conf
# Selección de modo (15) y vigilante de certificado (40); la imagen oficial ejecuta 20-envsubst entre ambos.
COPY --chmod=755 docker-entrypoint.d/ /docker-entrypoint.d/
# DOMAIN siempre existe (vacío = solo HTTP); envsubst solo sustituye DOMAIN y no toca $host ni $request_uri.
ENV DOMAIN=""
ENV NGINX_ENVSUBST_FILTER="^DOMAIN$"
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80 443
