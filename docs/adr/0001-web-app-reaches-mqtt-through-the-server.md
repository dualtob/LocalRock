---
status: superseded by ADR 0002
---

# The Server serves the Web App and its MQTT over WebSockets

Browsers can't open raw TCP sockets, and the Server only offers MQTT over TCP+TLS (its Mosquitto listens on `127.0.0.1` behind a TLS proxy). We'll add two things to the Server: an MQTT-over-WebSocket listener at `/mqtt` on its existing HTTPS port, relayed internally to Mosquitto, and a config option to serve a folder of static files from the Server's own origin. For now these live only on our fork, `dualtob/local_roborock_server`, not upstream `Python-roborock/local_roborock_server`, so running the Web App means running that fork. The fork's Docker image downloads a Web App release from `dualtob/LocalRock` when it's built and serves it, so deploying the Server deploys the Web App. Page, REST API and MQTT share one origin, so the Server needs no CORS and the browser trusts one certificate.

## Considered Options

- **A Bridge shipped with the Web App** (a JVM/Ktor process reusing the shared Kotlin code and Paho, relaying MQTT to the browser over WebSockets). It works with an unmodified Server, but users would have one more process to run. This is the fallback if keeping the Server fork in sync with upstream gets too costly.
- **Unzipping a Web App release into a folder the Server is configured to serve.** Rejected: since we own both forks, a separate manual deploy step buys nothing.
- **Hosting the Web App anywhere (nginx, Docker) with CORS on the Server.** Rejected: it needs CORS and a second trusted certificate.
- **Users add a WebSocket listener to their own Mosquitto.** Rejected: it's fragile, and the bundled Mosquitto only listens on localhost.
