# AGENTS.md

Guidance for pi and other coding agents working in this repository.

## Repository Purpose

This is a home operations GitOps repository for a single-node Talos Kubernetes cluster. Infrastructure and applications are managed as code with Flux, Renovate, GitHub Actions, External Secrets Operator, 1Password Connect, SOPS, and age.

Flux reconciles `kubernetes/` from Git into the cluster. Most changes should be declarative manifests, locally validated where possible, reviewed with `git diff`, then committed and pushed for Flux to apply.

## Architecture and Important Paths

- `README.md` — high-level repo, cluster, DNS, hardware, and GitOps overview.
- `Taskfile.yaml` — main Task entrypoint; includes bootstrap and Talos taskfiles.
- `.mise.toml` — pinned CLI tools and sensitive env var paths (`KUBECONFIG`, `SOPS_AGE_KEY_FILE`, `TALOSCONFIG`).
- `kubernetes/flux/cluster/ks.yaml` — root Flux `Kustomization` for `./kubernetes/apps`; applies common SOPS and HelmRelease defaults.
- `kubernetes/apps/` — Flux-managed apps grouped by namespace/category (`ai`, `cert-manager`, `default`, `external-secrets`, `flux-system`, `games`, `kube-system`, `network`, `observability`, `openclaw`, `selfhosted`).
- `kubernetes/apps/*/kustomization.yaml` — namespace-level Kustomize entries with `namespace.yaml`, optional components, and child app `ks.yaml` files.
- `kubernetes/components/sops/` — legacy SOPS component and encrypted cluster secret manifests; do not use for new app secrets unless explicitly asked.
- `kubernetes/apps/external-secrets/` — External Secrets Operator and 1Password Connect integration; preferred secret pattern for new app credentials.
- `talos/` — Talos/talhelper config, generated cluster config location, version pins, and machine patches.
- `bootstrap/` — bootstrap inputs, including encrypted SOPS/age material.
- `scripts/` and `scripts/lib/` — bootstrap/support scripts.
- `.taskfiles/bootstrap/Taskfile.yaml` — cluster and app bootstrap tasks.
- `.taskfiles/talos/Taskfile.yaml` — Talos generate/apply/upgrade/reset tasks.
- `.github/` — repo automation; `flux-local.yaml` validates/diffs Kubernetes changes in PRs.
- `.a5c/` — optional Babysitter project metadata and quality gates; see `.a5c/README.md`. Non-Babysitter users can ignore it.

## Common Commands

Prefer read-only/local commands first:

```sh
git status --short
git diff
task --list
find kubernetes/apps -maxdepth 2 -name kustomization.yaml -print | sort
```

Useful local validation commands when available:

```sh
kustomize build kubernetes/apps/<namespace> >/tmp/kustomize-build.yaml
kubeconform -strict -summary /tmp/kustomize-build.yaml
# CI uses flux-local against kubernetes/flux/cluster for Kubernetes changes
```

Useful project tasks:

```sh
task reconcile              # Force Flux reconcile; touches the cluster
task bootstrap:apps         # Bootstrap apps; touches the cluster
task bootstrap:talos        # Bootstrap Talos; destructive/cluster-affecting
task talos:generate-config  # Generate Talos config locally; needs SOPS age key
task talos:apply-node IP=... MODE=auto
task talos:upgrade-node IP=...
task talos:upgrade-k8s
task talos:reset            # Destructive
```

Before suggesting or running any cluster-affecting command, explain what it does and ask for explicit confirmation.

## GitOps / Infrastructure Workflow

1. Start with `git status --short` and avoid overwriting user changes.
2. Inspect nearby manifests, existing app layout, and relevant task/config files before editing.
3. Make the smallest declarative change possible under `kubernetes/` or `talos/`.
4. Follow existing naming, namespace, Kustomize, Flux, HelmRelease, and ExternalSecret patterns.
5. Run safe local validation where practical; prefer local checks over live-cluster commands.
6. Review `git diff` and call out any risky, destructive, or placeholder values.
7. Commit and push only when asked. Flux reconciliation should happen only after explicit confirmation.

Application namespaces generally follow this pattern:

```text
kubernetes/apps/<namespace>/
├── namespace.yaml
├── kustomization.yaml
└── <app>/
    └── ks.yaml
```

Child app directories commonly contain Flux `Kustomization`, `HelmRelease`, source references, config, and secret references.

## Secrets and Cluster Safety

Do not reveal, decrypt, print, or modify secrets unless the user explicitly asks and confirms the exact operation.

Treat these as sensitive:

- `age.key`
- `kubeconfig`
- `.sops.yaml`
- `*.sops.yaml`
- `talos/clusterconfig/*`, Talos secrets, and generated Talos configs
- Cluster credentials, tokens, API keys, kubeconfigs, Cloudflare/UniFi credentials, and 1Password references

Safe handling expectations:

- Do not run `sops --decrypt` unless explicitly requested and confirmed.
- Do not print secret values, kubeconfigs, Talos secrets, or decrypted manifests to chat/logs.
- Do not commit raw Kubernetes `Secret` values.
- For new app credentials, prefer `ExternalSecret` backed by the `onepassword-connect` `ClusterSecretStore`.
- Some older manifests still use SOPS, but do not create new `*.sops.yaml` app secrets unless the user explicitly asks.
- ExternalSecret item/key placeholders such as `REPLACE_WITH_...`, missing 1Password item names, or assumed property names must be called out before commit.
- Be careful with `task bootstrap:*`, `task talos:*`, `kubectl`, `flux`, `helm`, `helmfile`, and `talosctl`; these can affect live infrastructure or external services.

## Editing Conventions

- Preserve YAML document starts (`---`) where present.
- Keep YAML concise and consistent with neighboring files.
- Prefer existing repo structure over introducing new patterns.
- Use Kustomize resources/components consistently; keep namespace `kustomization.yaml` files sorted or grouped like nearby files.
- For Flux resources, inspect existing `ks.yaml`, `HelmRelease`, source, and `externalsecret.yaml` examples before creating new ones.
- Avoid broad rewrites; make targeted edits.
- Always pin chart, image, and dependency versions/tags; do not use `latest` because Renovate cannot reliably manage latest tags in this repo.
- Do not add scope parentheses in commit messages unless asked. Prefer `feat:`, `fix:`, or `chore:`; use `chore:` for routine maintenance.

## Optional Babysitter Support

This repository includes optional Babysitter project metadata under `.a5c/`. Agents and humans not using Babysitter can ignore it. Babysitter-specific processes must not override the repository safety rules in this file.

## Agent Behavior

- Read relevant files before editing and summarize changed paths afterward.
- Ask before destructive operations, live-cluster access, secret decryption, or restructuring.
- Explain infrastructure risk and validation steps for cluster/Talos changes.
- After edits, suggest safe next commands rather than running cluster-affecting commands automatically.
