// LocalRock's service worker: keeps the Web App's own files on the phone, so a launch from the
// home screen starts at once instead of waiting on the network. It never touches the Server:
// API calls and MQTT go to another origin and pass straight through.
//
// - config.json always comes from the network: it names the Server, and a changed SERVER_URL
//   must show up at once (ADR 0002). Offline, the last copy is used.
// - The .wasm files are named by their content hash, so a cached one never goes stale.
// - Everything else (index.html, localrock.js, icons, composeResources) is served from the cache
//   and refreshed behind it: a new release is fetched during one launch and runs from the next.

const CACHE = "localrock-v1";
const HASHED_WASM = /\/[0-9a-f]{20}\.wasm$/;

self.addEventListener("install", () => self.skipWaiting());

self.addEventListener("activate", (event) => {
    event.waitUntil((async () => {
        for (const name of await caches.keys()) {
            if (name !== CACHE) await caches.delete(name);
        }
        await self.clients.claim();
    })());
});

self.addEventListener("fetch", (event) => {
    const request = event.request;
    if (request.method !== "GET") return;
    const url = new URL(request.url);
    if (url.origin !== self.location.origin) return;

    if (url.pathname.endsWith("/config.json")) {
        event.respondWith(networkFirst(request));
    } else if (HASHED_WASM.test(url.pathname)) {
        event.respondWith(cacheFirst(request));
    } else {
        event.respondWith(staleWhileRevalidate(event, request, url));
    }
});

async function networkFirst(request) {
    const cache = await caches.open(CACHE);
    try {
        const response = await fetch(request, { cache: "no-store" });
        if (response.ok) await cache.put(request, response.clone());
        return response;
    } catch (e) {
        const cached = await cache.match(request);
        if (cached) return cached;
        throw e;
    }
}

async function cacheFirst(request) {
    const cache = await caches.open(CACHE);
    const cached = await cache.match(request);
    if (cached) return cached;
    const response = await fetch(request);
    if (response.ok) await cache.put(request, response.clone());
    return response;
}

async function staleWhileRevalidate(event, request, url) {
    const cache = await caches.open(CACHE);
    // The page is cached under one key, whatever query string the launch carried.
    const key = request.mode === "navigate" ? new Request(new URL("./", self.location).href) : request;
    const cached = await cache.match(key);
    const refresh = (async () => {
        const response = await fetch(request, { cache: "no-cache" });
        if (response.ok) {
            await cache.put(key, response.clone());
            if (url.pathname.endsWith("/localrock.js")) await pruneWasm(cache, response.clone());
        }
        return response;
    })();
    if (cached) {
        event.waitUntil(refresh.catch(() => undefined));
        return cached;
    }
    return refresh;
}

/** Drops cached .wasm files the newest localrock.js no longer loads, so old builds don't pile up. */
async function pruneWasm(cache, scriptResponse) {
    const script = await scriptResponse.text();
    const wanted = new Set(script.match(/[0-9a-f]{20}\.wasm/g) || []);
    for (const request of await cache.keys()) {
        const name = new URL(request.url).pathname.split("/").pop();
        if (HASHED_WASM.test("/" + name) && !wanted.has(name)) await cache.delete(request);
    }
}
