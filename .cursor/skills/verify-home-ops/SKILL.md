---
name: verify-home-ops
description: Verify the home-ops GitOps repo — local kustomize/kubeconform/flux-local checks, read-only cluster status over WireGuard, and the echo HTTP canary. Use when proving kubernetes/ or Taskfile changes, or when an agent needs to drive the operator CLI and live cluster the way a user would.
---

# Verify home-ops

This repo is not a local app server. The operator surface is the GitOps CLI (`task`, `kustomize`, `kubeconform`, `flux-local`) plus, when Cloud Agent secrets and WireGuard are up, a single shared Talos cluster (`kubectl`, `flux`) and HTTP canaries behind Envoy.

Read `features/README.md` before driving. Drive the feature file that matches the change. Do not invent a shorter path.

## Launch

There is no long-lived process to start. Launch means: toolchain on `PATH`, repo env vars set, a disposable run directory created.

```sh
export PATH="$HOME/.local/bin:$HOME/.local/share/mise/shims:$HOME/.venv/flux-local/bin:$PATH"
export KUBECONFIG="$PWD/kubeconfig"
export SOPS_AGE_KEY_FILE="$PWD/age.key"
export TALOSCONFIG="$PWD/talos/clusterconfig/talosconfig"
.cursor/skills/verify-home-ops/bin/verify-home-ops launch
```

Ready when stdout contains `READY run_id=` and `verify-home-ops doctor` prints `LOCAL_READY=yes`.

`CLUSTER_READY=yes` is optional. It requires `./kubeconfig` plus `wg0` (brought up by `.cursor/start.sh` from `HOME_OPS_WIREGUARD_CONF`). Local kustomize / kubeconform / flux-local do not need the cluster.

Teardown is `verify-home-ops cleanup`. That removes `/tmp/home-ops-verify-$RUN_ID` only. It must not kill `wg0`, must not delete `kubeconfig` / `age.key` / `talosconfig`, and must not delete artifacts.

## Doctor

Run this first whenever anything looks off:

```sh
.cursor/skills/verify-home-ops/bin/verify-home-ops doctor
```

It is read-only. Require:

- `task`, `kustomize` (v5.7.x), `kubeconform` (v0.7.x), `flux`, `kubectl`, `yq`, `jq` on `PATH` (mise shims from `.mise.toml`)
- `flux-local` at `$HOME/.venv/flux-local/bin/flux-local` (installed by `.cursor/install.sh`, pinned `8.4.0`)
- `LOCAL_READY=yes`

For cluster-backed features also require:

- `kubeconfig_present=yes` (do not print the file)
- `wg0=up`
- `cluster_api=reachable` against `https://192.168.20.100:6443`
- `CLUSTER_READY=yes`

Refuse to drive a cluster feature when `CLUSTER_READY=no`. Local features may still run.

## Drive

Harness: `verify-home-ops`. Stable handles are Task names, namespace paths, Flux object names, HTTPRoute names, and HTTP paths — not coordinates.

```sh
.cursor/skills/verify-home-ops/bin/verify-home-ops kustomize-build default
.cursor/skills/verify-home-ops/bin/verify-home-ops kubeconform default
.cursor/skills/verify-home-ops/bin/verify-home-ops flux-local-test
.cursor/skills/verify-home-ops/bin/verify-home-ops cluster-status
.cursor/skills/verify-home-ops/bin/verify-home-ops echo-http
```

Namespaces the helper accepts: `ai`, `cert-manager`, `default`, `external-secrets`, `flux-system`, `kube-system`, `network`, `observability`, or `all`.

Operator entry points that exist but are **out of scope** for this harness (mutating / destructive):

- `task reconcile` — forces Flux to pull Git; touches the live cluster
- `task bootstrap:apps` / `task bootstrap:talos` — bootstrap
- `task talos:apply-node` / `task talos:upgrade-node` / `task talos:upgrade-k8s` / `task talos:reset` — Talos mutation / destroy

Do not run those unless the user explicitly confirmed the exact operation.

The cluster is a single production node (`ms-02-ultra`). Two agents must not mutate it at once. Read-only `cluster-status` and `echo-http` may overlap. Local validation may run in parallel under different `HOME_OPS_VERIFY_RUN_ID` values.

Public `https://echo.<domain>/` hits Cloudflare and returns a bot-challenge 403 from this environment. The real user path for the canary is Envoy’s LAN load balancer (`envoy-external` at `192.168.20.130`) with `--resolve`, or `kubectl -n default port-forward svc/echo`. Discover the hostname from `httproute/echo` — do not decrypt `cluster-secrets.sops.yaml`.

`kustomize build kubernetes/flux/cluster` fails: that directory has only `ks.yaml`. Flux and `flux-local` still consume it. Do not “fix” that during verification.

## Evidence

Write proof under `.cursor/skills/verify-home-ops/artifacts/<run-id>/`. Cleanup must leave that tree in place.

Proof standards:

- Exercise the operator path (`kustomize` / `kubeconform` / `flux-local` / `kubectl` / HTTP to echo). Do not treat a Git diff or an internal setter as proof.
- Capture the command and the resulting state (exit code, pytest summary, resource Ready columns, HTTP status + JSON path/method/pod), not only a final “ok”.
- Side effects: local builds must produce a YAML file whose kinds/names match the namespace kustomization. Echo must show a pod name starting with `echo-`. Flux objects must stay `Ready=True` after a read-only check.
- Never `sops --decrypt`. Never print `kubeconfig`, `age.key`, `talosconfig`, or `wg0.conf`. Never `kubectl get secret -o yaml`. Kustomize output may contain `ENC[...]` blobs that are already in Git; summarize kinds/names on stdout instead of dumping Secret values.
- `kubeconform -strict -summary` on a raw namespace build fails: the SOPS `Secret` has a `sops:` key, and Flux `Kustomization` has no default schema. The helper strips `sops:` only (does not decrypt) and passes `-ignore-missing-schemas`. That is the working local check. CI’s real gate is `flux-local test`.
- Public Cloudflare 403 is not an echo failure. Prove via Envoy `--resolve` or port-forward.

Copy a short transcript the user should see into `/opt/cursor/artifacts/` when this environment has that directory.

## Cleanup

```sh
.cursor/skills/verify-home-ops/bin/verify-home-ops cleanup
```

Kills only PIDs recorded in `/tmp/home-ops-verify-$RUN_ID/pids` (port-forwards this run started). Removes that scratch directory. Leaves `.cursor/skills/verify-home-ops/artifacts/<run-id>/` untouched. Does not call `wg-quick down`, `pkill`, or delete credential files.

## Helpers

`bin/verify-home-ops` is executable. Invocation is always from the repo root as shown above. Subcommands: `launch`, `doctor`, `kustomize-build`, `kubeconform`, `flux-local-test`, `cluster-status`, `echo-http`, `cleanup`.
