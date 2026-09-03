# Cluster readiness

Cluster readiness lets an operator confirm the single-node Talos cluster is reachable over WireGuard and that Flux objects are Ready, without changing cluster state.

## Sub-features

- `cluster-api` reaches the Kubernetes API at `192.168.20.100:6443`.
- `cluster-node` shows node `ms-02-ultra` Ready.
- `cluster-flux` lists Flux Kustomizations and HelmReleases as Ready.
- `cluster-routes` lists HTTPRoute hostnames.

## How to get to it (user POV)

- On a machine with `kubeconfig` and LAN or WireGuard access, run `kubectl get nodes` and `kubectl get kustomizations,helmreleases -A`.
- Run `task --list` and notice `reconcile` (do not run it for this feature).
- Run `.cursor/skills/verify-home-ops/bin/verify-home-ops cluster-status`.

## Driving it with verify-home-ops

Preconditions:

- `verify-home-ops doctor` prints `LOCAL_READY=yes` and `CLUSTER_READY=yes`.
- A run exists (`verify-home-ops launch`).
- `wg0` is already up from `.cursor/start.sh`. Do not bring the tunnel down.

- **API and node.** Run `.cursor/skills/verify-home-ops/bin/verify-home-ops cluster-status`. Exit code `0`. `cluster-status.txt` shows node `ms-02-ultra` with `STATUS` `Ready` and `ROLES` containing `control-plane`.
- **Flux Kustomizations.** In the same output, every `kustomizations.kustomize.toolkit.fluxcd.io` row has `READY` `True`, including `flux-system/cluster-apps`, `flux-system/flux-system`, and `default/echo`.
- **HelmReleases.** Every `helmreleases.helm.toolkit.fluxcd.io` row has `READY` `True`, including `default/echo`.
- **HTTPRoutes.** The HTTPRoute table includes `default/echo` (a hostname), `observability/gatus`, and `observability/grafana-httproute`.
- **Proof.** Keep `cluster-status.txt`. Do not follow this feature with `task reconcile` unless the user asked to mutate the cluster.

## Gotchas

- `CLUSTER_READY=no` means stop. Local features still work; do not claim cluster readiness via flux-local.
- The API server is `192.168.20.100`. WireGuard must allow `192.168.20.0/24`. A missing `HOME_OPS_WIREGUARD_CONF` looks like a dead cluster.
- An idle `wg0` can miss the first kubectl. Doctor retries `/readyz` three times. If it still fails, ping `192.168.20.100` before declaring the cluster dead.
- Never `kubectl get secret` or dump `kubeconfig` to prove this feature.
- `task reconcile` is a different, mutating entry point. A Ready table taken after an unsolicited reconcile is not this feature.
- Client `kubectl` may be newer than the server (mise pins `1.36.x`, the node has been seen on `v1.35.2`). A version skew warning is not unreadiness.
