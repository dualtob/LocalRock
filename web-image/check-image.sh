#!/usr/bin/env bash
# Checks the Web App's container image as a black box (ADR 0002): started with SERVER_URL it
# serves the Web App and names that Server in /config.json; started without, /config.json
# reads as "no Server set" (what webAppServer in WebAppConfig.kt makes of it).
#
# Usage: web-image/check-image.sh <image>
# Needs docker, curl and jq. Exits non-zero on the first failed check.
set -euo pipefail

image=${1:?usage: $0 <image>}
server=https://api-roborock.example.com:555
containers=""

fail() { echo "::error::$*" >&2; exit 1; }
cleanup() { for c in $containers; do docker rm -f "$c" >/dev/null 2>&1 || true; done; }
trap cleanup EXIT

# Starts the image with the given `docker run` options and waits until it answers.
# Sets $container and $base (its URL).
start() {
  local port
  container=$(docker run -d -p 127.0.0.1::8080 "$@" "$image")
  containers="$containers $container"
  port=$(docker port "$container" 8080/tcp | head -n1 | sed 's/.*://')
  base="http://127.0.0.1:$port"
  for _ in $(seq 1 50); do
    curl -fsS -o /dev/null "$base/" 2>/dev/null && return
    sleep 0.2
  done
  docker logs "$container" >&2 || true
  fail "the image did not answer on port 8080"
}

# Fetches a path and fails unless it is served with 200 and a Content-Type starting with $2.
expect_type() {
  local url=$1 type=$2 got
  got=$(curl -fsS -o /dev/null -w '%{content_type}' "$url") || fail "$url is not served"
  [[ $got == "$type"* ]] || fail "$url is served as '$got', not $type"
}

# Prints the response headers of a GET for $1 (more curl options may follow), lower-cased and
# without carriage returns.
response_headers() {
  local url=$1; shift
  curl -fsS -o /dev/null -D - "$@" "$url" | tr -d '\r' | tr '[:upper:]' '[:lower:]' || fail "$url is not served"
}

# Succeeds if the headers in $1 hold header $2 exactly once, with the value $3 (lower case).
# Once: nginx only inherits server-level add_header into a location that adds none, so a slip
# there shows up as a missing or doubled header.
has_header() {
  [[ $(grep "^$2:" <<<"$1") == "$2: $3" ]]
}

# "no Server set" the way webAppServer reads config.json: not a JSON object, or serverUrl
# missing, not a string, or blank.
names_no_server() {
  jq -e '(type != "object") or ((.serverUrl | type) != "string") or ((.serverUrl | gsub("^\\s+|\\s+$"; "")) == "")' >/dev/null 2>&1
}

echo "Starting $image with SERVER_URL=$server"
start -e "SERVER_URL=$server"

page=$(curl -fsS "$base/") || fail "/ is not served"
expect_type "$base/" text/html
for link in 'rel="manifest" href="manifest.webmanifest"' 'rel="apple-touch-icon" href="apple-touch-icon.png"' 'rel="icon" type="image/png" sizes="32x32" href="favicon-32.png"'; do
  grep -qF "$link" <<<"$page" || fail "/ is not index.html: it lacks <link $link>"
done

expect_type "$base/manifest.webmanifest" application/manifest+json
manifest=$(curl -fsS "$base/manifest.webmanifest") || fail "manifest.webmanifest is not served"
icons=$(jq -er '.icons[].src' <<<"$manifest") || fail "manifest.webmanifest is not valid JSON with icons"
for icon in $icons apple-touch-icon.png apple-touch-icon-precomposed.png favicon-32.png favicon-16.png; do
  expect_type "$base/$icon" image/png
done
expect_type "$base/favicon.ico" image/

wasms=$(docker exec "$container" find /usr/share/nginx/html -name '*.wasm') || fail "could not look for .wasm files in the image"
[[ -n $wasms ]] || fail "the image holds no .wasm file"
for wasm in $wasms; do
  path=${wasm#/usr/share/nginx/html/}
  expect_type "$base/$path" application/wasm
  # Content-hashed, so a browser may keep them for good; gzipped, a cold start downloads a third.
  headers=$(response_headers "$base/$path" -H 'Accept-Encoding: gzip')
  has_header "$headers" content-encoding gzip || fail "/$path is not served gzipped"
  has_header "$headers" cache-control 'public, max-age=31536000, immutable' || fail "/$path is not served as immutable"
done

# The page and script name the hashed files, so a browser revalidates them on every launch.
for path in "" localrock.js; do
  headers=$(response_headers "$base/$path")
  has_header "$headers" cache-control no-cache || fail "/$path is not served with Cache-Control: no-cache"
done

expect_type "$base/config.json" application/json
got=$(curl -fsS "$base/config.json" | jq -er '.serverUrl') || fail "/config.json has no serverUrl"
[[ $got == "$server" ]] || fail "/config.json names '$got', not $server"

echo "Starting $image without SERVER_URL"
start
config=$(curl -fsS "$base/config.json") || fail "/config.json is not served without SERVER_URL"
names_no_server <<<"$config" || fail "/config.json names a Server without SERVER_URL: $config"

echo "All image checks passed"
