# Flux-local test

Flux-local test lets an operator run the same build-and-inflate check GitHub Actions runs on `kubernetes/**` pull requests, without applying anything to the cluster.

## Sub-features

- `flux-local-test-all` builds every Flux Kustomization under `kubernetes/flux/cluster` with Helm inflation.
- `flux-local-get` lists the root Flux Kustomizations the tester will walk.

## How to get to it (user POV)

- Open a PR that touches `kubernetes/**` and wait for the `Flux Local` workflow (`flux-local test --enable-helm --all-namespaces --path kubernetes/flux/cluster -v`).
- From the repo root, run that same `flux-local test` command locally.
- Run `.cursor/skills/verify-home-ops/bin/verify-home-ops flux-local-test`.

## Driving it with verify-home-ops

Preconditions:

- `verify-home-ops doctor` prints `LOCAL_READY=yes` and `flux_local=present`.
- A run exists (`verify-home-ops launch`).
- Network is available so Helm charts can be pulled. Cluster credentials are not required.

- **List roots.** Run `flux-local get kustomizations --path kubernetes/flux/cluster`. Exit code `0`. The table includes `cluster-apps` at `kubernetes/apps`, plus `flux-instance` and `flux-operator`.
- **Run the CI test.** Run `.cursor/skills/verify-home-ops/bin/verify-home-ops flux-local-test`. Exit code `0`. Log `flux-local-test.log` contains a pytest summary with `passed` and no `FAILED` rows. Expect on the order of 50 items (one Kustomization check and one HelmRelease inflate per app).
- **Proof.** Keep the pytest summary tail (the `N passed in …s` line) and the `PASSED` count. A deprecation banner about flate/konflate is expected on stderr and is not a failure.

## Gotchas

- `kustomize build kubernetes/flux/cluster` fails because that directory has only `ks.yaml`. `flux-local` still walks the Flux `Kustomization` CRs. Do not add a kustomization.yaml just to make `kustomize build` work unless the user asked.
- `--enable-helm` talks to OCI registries. An empty or firewalled network looks like a product failure.
- Pin stays `flux-local==8.4.0` to match `.github/workflows/flux-local.yaml` and `.cursor/install.sh`. A different version is not this feature.
- Do not treat `flux-local diff` (PR comment helper) as this test. Diff needs two checkouts.
