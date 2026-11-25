// LocalRock's service worker: keeps the Web App's own files on the phone, so a launch from the
// home screen starts at once instead of waiting on the network. It never touches the Server:
// API calls and MQTT go to another origin (ADR 0002) and pass straight through.
//
// - config.json comes from the network first: it names the Server, and a changed SERVER_URL must
//   show up at once. Offline, the last copy is used.
// - The .wasm files are named by their content hash, so a cached one never goes stale.
// - Everything else (the page, localrock.js, icons, composeResources) is served from the cache
//   and refreshed behind it: a new release is fetched during one launch and runs from the next.
//   A new localrock.js is stored only once the .wasm it loads is stored, so that next launch
//   works offline too; the page gets the script as soon as it arrives either way. Cached .wasm that neither the newest script nor the running one loads
//   is dropped.
// - The first launch ran before this worker existed, so installing stores the page, script,
//   .wasm and config.json up front, and the page then reports what else it loaded until the
//   worker took over (index.html). Without that, a second launch would still need the network.

const CACHE = "localrock-v1";
const WASM_NAME = /[0-9a-f]{20}\.wasm/g;
const WASM_PATH = /\/[0-9a-f]{20}\.wasm$/;
const PAGE = new URL("./", self.location).href;
const SCRIPT = new URL("localrock.js", PAGE).href;
const CONFIG = new URL("config.json", PAGE).href;
/** .wasm downloads on their way, by URL, so the page and the worker never fetch one twice at once. */
const wasmOnItsWay = new Map();
const SHELL = ["manifest.webmanifest", "favicon.ico", "favicon-16.png", "favicon-32.png",
    "apple-touch-icon.png", "apple-touch-icon-precomposed.png", "icon-192.png", "icon-512.png"];

self.addEventListener("install", (event) => {
    event.waitUntil((async () => {
        const cache = await caches.open(CACHE);
        // The page, script and .wasm must all be stored, or the install fails and is tried again
        // on the next launch. The rest is nice to have.
        await Promise.all([
            fetchAndStore(cache, PAGE).then((response) => {
                if (!response.ok || response.redirected) throw new Error(`${PAGE}: ${response.status}`);
            }),
            storeScript(cache),
            ...[CONFIG, ...SHELL.map((name) => new URL(name, PAGE).href)].map((url) =>
                fetchAndStore(cache, url).catch(() => undefined)),
        ]);
        await self.skipWaiting();
    })());
});

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
    const path = url.origin + url.pathname;

    if (request.mode === "navigate") {
        // The page is kept under one key, whatever query string the launch carried.
        event.respondWith(staleWhileRevalidate(event, PAGE, (cache) => fetchAndStore(cache, PAGE)));
    } else if (path === CONFIG) {
        event.respondWith(networkFirst());
    } else if (WASM_PATH.test(url.pathname)) {
        event.respondWith(cacheFirst(request));
    } else if (path === SCRIPT) {
        event.respondWith(staleWhileRevalidate(event, SCRIPT, refreshScript));
    } else {
        event.respondWith(staleWhileRevalidate(event, request, (cache) => fetchAndStore(cache, request)));
    }
});

// The page's list of files it loaded before this worker took over (index.html): those not stored
// yet, composeResources for one, are stored now.
self.addEventListener("message", (event) => {
    const data = event.data;
    if (!data || data.type !== "loaded" || !Array.isArray(data.urls)) return;
    event.waitUntil((async () => {
        const cache = await caches.open(CACHE);
        for (const href of data.urls) {
            const url = new URL(href, PAGE);
            if (url.origin !== self.location.origin || url.href === CONFIG) continue;
            if (url.pathname.endsWith("/sw.js") || await cache.match(url.href)) continue;
            await fetchAndStore(cache, url.href).catch(() => undefined);
        }
    })());
});

/** Fetches [request] past the browser's HTTP cache and, if it worked, stores it. */
async function fetchAndStore(cache, request) {
    const response = await fetch(request, { cache: "no-cache" });
    // A redirected response can't answer a navigation, so it is never kept.
    if (response.ok && !response.redirected) await cache.put(request, response.clone());
    return response;
}

/** config.json: the network's answer, or the last copy when the network or the image is down. */
async function networkFirst() {
    const cache = await caches.open(CACHE);
    try {
        const response = await fetch(CONFIG, { cache: "no-store" });
        if (response.ok) await cache.put(CONFIG, response.clone());
        if (response.status < 500) return response;
        return (await cache.match(CONFIG)) || response;
    } catch (e) {
        const cached = await cache.match(CONFIG);
        if (cached) return cached;
        throw e;
    }
}

/** A .wasm: the stored copy, else stored now (or by the download already on its way) and served. */
async function cacheFirst(request) {
    const cache = await caches.open(CACHE);
    const cached = await cache.match(request);
    if (cached) return cached;
    try {
        await storeWasmFile(cache, request.url);
        const stored = await cache.match(request);
        if (stored) return stored;
    } catch (e) {
        // Below, the page gets the network's own answer, error status and all.
    }
    return fetch(request);
}

/**
 * The cached copy at once, while [refresh] fetches the next one behind it; the network's answer
 * when nothing is cached yet.
 */
async function staleWhileRevalidate(event, key, refresh) {
    const cache = await caches.open(CACHE);
    const cached = await cache.match(key);
    const refreshed = refresh(cache, cached, (work) => event.waitUntil(work.catch(() => undefined)));
    if (cached) {
        event.waitUntil(refreshed.catch(() => undefined));
        return cached;
    }
    return refreshed;
}

/**
 * Fetches localrock.js and returns it at once. Behind it, through [keepAlive], stores the .wasm it
 * loads and only then the script, and drops cached .wasm that neither it nor the script running
 * now ([served], the cached copy) loads. On a first launch nothing is cached, so the page waits for
 * this answer: it must not also wait for the .wasm, nor fail when one of them does.
 */
async function refreshScript(cache, served, keepAlive) {
    const servedText = served ? served.clone().text() : Promise.resolve("");
    const response = await fetch(SCRIPT, { cache: "no-cache" });
    if (!response.ok) return response;
    const toStore = response.clone();
    keepAlive((async () => {
        const text = await storeWasm(cache, toStore.clone());
        await cache.put(SCRIPT, toStore);
        await pruneWasm(cache, text + await servedText);
    })());
    return response;
}

/** On install: stores localrock.js and the .wasm it loads. */
async function storeScript(cache) {
    const response = await fetch(SCRIPT, { cache: "no-cache" });
    if (!response.ok) throw new Error(`${SCRIPT}: ${response.status}`);
    await storeWasm(cache, response.clone());
    await cache.put(SCRIPT, response);
}

/** Stores each .wasm the script in [response] loads that isn't stored yet; returns the script. */
async function storeWasm(cache, response) {
    const text = await response.text();
    const names = new Set(text.match(WASM_NAME) || []);
    await Promise.all([...names].map(async (name) => {
        const url = new URL(name, SCRIPT).href;
        if (await cache.match(url)) return;
        await storeWasmFile(cache, url);
    }));
    return text;
}

/** Downloads and stores one .wasm, joining the download of it already on its way if there is one. */
function storeWasmFile(cache, url) {
    let storing = wasmOnItsWay.get(url);
    if (!storing) {
        storing = (async () => {
            const wasm = await fetch(url);
            if (!wasm.ok) throw new Error(`${url}: ${wasm.status}`);
            await cache.put(url, wasm);
        })().finally(() => wasmOnItsWay.delete(url));
        wasmOnItsWay.set(url, storing);
    }
    return storing;
}

/** Drops cached .wasm that [scripts] don't name, so old builds don't pile up. */
async function pruneWasm(cache, scripts) {
    const wanted = new Set(scripts.match(WASM_NAME) || []);
    for (const request of await cache.keys()) {
        const name = new URL(request.url).pathname.split("/").pop();
        if (WASM_PATH.test("/" + name) && !wanted.has(name)) await cache.delete(request);
    }
}
