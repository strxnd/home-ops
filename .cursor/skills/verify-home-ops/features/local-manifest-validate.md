# Local manifest validate

Local manifest validate lets an operator render a namespace kustomization and schema-check the result without talking to the cluster.

## Sub-features

- `kustomize-build-one` renders `kubernetes/apps/<namespace>` to YAML.
- `kustomize-build-all` renders every namespace kustomization.
- `kubeconform-one` schema-checks one rendered namespace after stripping SOPS metadata.
- `kubeconform-all` schema-checks every namespace the same way.

## How to get to it (user POV)

- From the repo root, run `kustomize build kubernetes/apps/<namespace>` as documented in `AGENTS.md`.
- Run `kubeconform -strict -summary` on that build.
- Run the same checks through `verify-home-ops kustomize-build` and `verify-home-ops kubeconform`.

## Driving it with verify-home-ops

Preconditions:

- `verify-home-ops doctor` prints `LOCAL_READY=yes`.
- A run exists (`verify-home-ops launch`).
- The namespace directory has `kustomization.yaml`. Cluster credentials are not required.

- **Build one namespace.** Render `default`. Run `.cursor/skills/verify-home-ops/bin/verify-home-ops kustomize-build default`. Exit code `0`. Stdout lists `Namespace default`, `Secret cluster-secrets`, and `Kustomization echo`. Artifact `kustomize-default.kinds.txt` has those three rows.
- **Schema-check one namespace.** Run `.cursor/skills/verify-home-ops/bin/verify-home-ops kubeconform default`. Exit code `0`. Report contains `Invalid: 0` and skips the Flux `Kustomization` (no schema). The helper wrote `kustomize-default.nosops.yaml` with no `sops:` key.
- **Build another namespace.** After an edit under `kubernetes/apps/network`, run `.cursor/skills/verify-home-ops/bin/verify-home-ops kustomize-build network`. Exit code `0` and `kustomize-network.kinds.txt` includes `Namespace network` plus the Flux `Kustomization` rows for cloudflare-dns, cloudflare-tunnel, envoy-gateway, k8s-gateway, and unifi-dns.
- **Build all namespaces.** Run `.cursor/skills/verify-home-ops/bin/verify-home-ops kustomize-build all`. Exit code `0` and eight `kustomize-*.kinds.txt` files exist.
- **Proof.** Keep the kinds list and kubeconform summary. Do not paste Secret `stringData` or `ENC[...]` values into chat.

## Gotchas

- Raw `kubeconform -strict -summary` on the unstripped build fails: `Secret cluster-secrets` has additional property `sops`, and Flux `Kustomization` has no default schema. Use the helper, or strip `sops:` and pass `-ignore-missing-schemas`.
- Namespace builds include the SOPS component. That is expected. Do not decrypt to make kubeconform pass.
- `kustomize build kubernetes/flux/cluster` is not this feature and fails (no `kustomization.yaml` there). Use flux-local-test for that path.
- `all` writes eight artifacts. Drive the namespace you changed unless you intend a full sweep.
