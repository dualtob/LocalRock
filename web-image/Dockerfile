# The Web App's container image (ADR 0002): stock nginx serving the Web App's files over plain
# HTTP on port 8080, with TLS left to a reverse proxy. At start, nginx's template mechanism
# renders config.json from SERVER_URL; without SERVER_URL it names no Server.
#
# Built from the repository root after `./gradlew :composeApp:wasmJsBrowserDistribution`:
#   docker build -f web-image/Dockerfile -t localrock-web .
FROM nginx:1.31.9-alpine@sha256:fb4658584bd9a916ffb6f6e365f97c25cfdb2923801f551ea08356e695f14ff7

# Defined but empty, so the template renders a blank serverUrl instead of a literal ${SERVER_URL}.
ENV SERVER_URL=""
# Render templates into the web root instead of conf.d; config.json is the only template.
ENV NGINX_ENVSUBST_OUTPUT_DIR=/usr/share/nginx/html

COPY web-image/default.conf /etc/nginx/conf.d/default.conf
COPY web-image/config.json.template /etc/nginx/templates/config.json.template
COPY composeApp/build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/
# Gzipped copies for gzip_static (default.conf); -k keeps the originals for clients without gzip.
RUN find /usr/share/nginx/html -type f \( -name '*.wasm' -o -name '*.js' -o -name '*.html' -o -name '*.json' -o -name '*.webmanifest' \) \
        -exec gzip -9 -k {} +

LABEL org.opencontainers.image.source=https://github.com/dualtob/LocalRock
LABEL org.opencontainers.image.description="LocalRock Web App"

EXPOSE 8080
