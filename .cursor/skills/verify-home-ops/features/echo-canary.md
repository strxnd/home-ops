# Echo canary

Echo canary lets an operator hit the deployed `http-https-echo` workload the way traffic reaches it on the LAN, and confirm the pod answers JSON for `/healthz` and `/`.

## Sub-features

- `echo-discover` reads the live HTTPRoute hostname and the `envoy-external` load-balancer IP.
- `echo-healthz` returns HTTP 200 JSON for `GET /healthz`.
- `echo-root` returns HTTP 200 JSON for `GET /`.
- `echo-pod` shows `os.hostname` starting with `echo-`.

## How to get to it (user POV)

- Browse `https://echo.<SECRET_DOMAIN>/` from a browser that can pass Cloudflare (external hostname on `envoy-external`).
- From the LAN, open the same hostname against Envoy at `192.168.20.130`.
- From a Cloud Agent, run `.cursor/skills/verify-home-ops/bin/verify-home-ops echo-http`.
- Fallback: `kubectl -n default port-forward svc/echo 18080:80` then `curl http://127.0.0.1:18080/healthz`.

## Driving it with verify-home-ops

Preconditions:

- `verify-home-ops doctor` prints `CLUSTER_READY=yes`.
- A run exists (`verify-home-ops launch`).
- Flux objects `default/echo` (Kustomization and HelmRelease) are `Ready=True`.
- Deployment `echo` in `default` is `1/1`.

- **Discover.** Run `.cursor/skills/verify-home-ops/bin/verify-home-ops echo-http`. The helper reads `httproute/echo` in `default` and `svc/envoy-external` in `network`. It does not decrypt SOPS.
- **Healthz.** The same command `GET`s `https://<host>/healthz` with `--resolve <host>:443:<lb-ip>`. HTTP status is `200`. JSON `path` is `/healthz` and `method` is `GET`.
- **Root.** The same command `GET`s `https://<host>/`. HTTP status is `200`. JSON `path` is `/`.
- **Pod identity.** JSON `os.hostname` starts with `echo-`. Artifact `echo-http.txt` records host, LB IP, and statuses. Bodies are in `echo-healthz.json` and `echo-root.json`.
- **Proof.** Keep those three files. A public Cloudflare HTML page titled `Just a moment...` with status `403` is a skipped external path, not a pass.

## Gotchas

- Direct `curl https://echo.<domain>/` from this Cloud Agent hits Cloudflare and returns 403. That is not the echo app. Use Envoy `--resolve` or port-forward.
- Discover the hostname from the HTTPRoute. Do not run `sops --decrypt` on `cluster-secrets.sops.yaml` to learn `SECRET_DOMAIN`.
- Echo reflects request headers. Do not treat reflected `x-forwarded-for` as a secret leak in chat; still avoid pasting full header dumps when they include client IPs you do not need.
- `kubectl -n default port-forward` is a valid fallback. Record its PID in the run `pids` file so cleanup can stop that PID only.
- Grafana, Gatus, LibreChat, and other HTTPRoutes are different apps. A 200 from `grafana` does not prove echo.
