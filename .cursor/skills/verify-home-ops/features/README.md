# home-ops verification map

This directory is the maintained source for verifying the operator-facing behavior of the home-ops GitOps repo. Read this index before driving, then use the matching feature file as the recipe.

## Baseline preconditions

- Run commands from the repo root with mise shims and flux-local on `PATH`.
- Run `verify-home-ops launch` and export `HOME_OPS_VERIFY_RUN_ID` from its `READY` line (or rely on `artifacts/.latest`).
- Run `verify-home-ops doctor` and require `LOCAL_READY=yes`.
- For `cluster-readiness` and `echo-canary`, also require `CLUSTER_READY=yes` (`kubeconfig` present, `wg0` up, API at `192.168.20.100:6443` reachable).
- Never decrypt SOPS. Never print credential files. Never run `task reconcile`, bootstrap, or Talos apply/upgrade/reset unless the user confirmed that exact command.
- The live cluster is shared production. Do not double-drive mutations. Local validation may run in parallel with distinct run IDs.

## Driving conventions

- Start every recipe from the baseline unless its preconditions say otherwise.
- Treat every command as literal. Keep namespace names and Flux object names unchanged.
- Prefer `verify-home-ops` subcommands over ad-hoc kubectl that dumps secrets.
- Restore nothing on the cluster after a read-only drive. Remove only this run’s scratch on cleanup. Keep artifacts.

## Proof and skip reporting

- Capture the command and the resulting state, not only the final line.
- Local proof includes the kustomize kinds list and the kubeconform or flux-local summary.
- Cluster proof includes Ready columns for the objects named in the feature.
- HTTP proof includes status `200`, JSON `path` / `method`, and an `echo-` pod hostname.
- Record the feature ID and entry point with every artifact.
- Report an unreachable path with the attempted command and the unmet precondition (`CLUSTER_READY=no`, missing `flux-local`, etc.).
- Do not report a skipped entry point as verified through a different path. A Cloudflare 403 is not echo success.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the user-visible behavior. It then uses exactly four H2 sections in this order.

1. `Sub-features` lists short IDs with one line for each behavior.
2. `How to get to it (user POV)` lists every user entry point.
3. `Driving it with verify-home-ops` starts with `Preconditions:` and uses labeled bullets that pair each user action with an exact command and observable result.
4. `Gotchas` lists traps that can waste or invalidate a verification run.

Keep implementation details out of the map. Name only user paths, stable handles, required state, commands, and observable proof.

## Features

- [Local manifest validate](./local-manifest-validate.md) covers `kustomize build` and `kubeconform` on namespace kustomizations.
- [Flux-local test](./flux-local-test.md) covers the CI-equivalent `flux-local test` against `kubernetes/flux/cluster`.
- [Cluster readiness](./cluster-readiness.md) covers read-only node, Flux Kustomization, HelmRelease, and HTTPRoute status.
- [Echo canary](./echo-canary.md) covers the deployed echo HTTP service through Envoy on the LAN.
