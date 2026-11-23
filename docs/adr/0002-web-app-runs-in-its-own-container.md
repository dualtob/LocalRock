---
status: accepted, supersedes ADR-0001
---

# The Web App runs in its own container and calls the Server cross-origin

ADR 0001 baked the Web App into the Server fork's image, so every Web App release needed a "Pin web-vX" commit on `dualtob/local_roborock_server`, and the fork had to know LocalRock's repo, release asset and build layout. We now ship the Web App as its own container image that only serves static files, under its own hostname (e.g. `localrock.example.com` behind the user's reverse proxy), while the Server keeps its own (`api-roborock.example.com:555`). The deployment tells the Web App where the Server is: the container's `SERVER_URL` becomes a `config.json` next to the page, which the Web App reads at startup instead of assuming the Server is its page origin. As before, users can't change the Server in the Web App, and the App's Server setup screen stays App-only. The dependency now runs one way: LocalRock depends on the Server's public surface, and the Server knows nothing about LocalRock.

The Server fork keeps only generic features: MQTT over WebSockets at `/mqtt`, and a CORS allowlist of origins that may call its API. Neither mentions LocalRock. Static-folder serving, the Web App download and its version pins leave the fork. We don't send these features upstream for now, so the Web App still needs the fork.

ADR 0001 rejected cross-origin hosting because the browser would have to trust a second certificate. That no longer applies: the Server's hostname already has a publicly trusted certificate, which the Robots need anyway.

## Considered Options

- **One origin, with a proxy that sends API and `/mqtt` requests to the Server** (in the image or in the user's reverse proxy, possibly under a `/server/` prefix). Rejected: it needs routing rules that know which paths belong to the Server, and it makes the Web App's container more than a file server, all to avoid CORS, which is one small option on the Server.
- **Each browser asks for the Server's address, like the App.** Rejected: every browser, and on iOS the home-screen Web App separately from Safari, would have to type it, where today nobody types anything.
- **The Bridge from ADR 0001** (a Ktor process relaying MQTT). It would work with stock upstream, but while we keep the fork it's one more moving part for no gain. It's still the way out if we drop the fork.
- **Keep the Web App inside the Server image (ADR 0001).** Rejected: it tied Web App releases to Server commits and made the Server depend on LocalRock.
