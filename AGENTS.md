# AGENTS.md

Guidance for pi and other coding agents working in this repository.

## Repository Purpose

This is a home operations GitOps repository for a single-node Talos Kubernetes cluster. Infrastructure and applications are managed as code using Flux, Renovate, GitHub Actions, SOPS, and age.

Flux reconciles the `kubernetes/` directory from Git into the cluster. Most day-to-day changes should be made as declarative manifests, reviewed with `git diff`, validated locally when possible, then committed and pushed for Flux to apply.

## Important Directories and Files

- `README.md` — high-level architecture and repo overview.
- `Taskfile.yaml` — main task entrypoint.
- `.taskfiles/bootstrap/Taskfile.yaml` — cluster/app bootstrap tasks.
- `.taskfiles/talos/Taskfile.yaml` — Talos generation, apply, upgrade, and reset tasks.
- `.mise.toml` — pinned CLI tools and environment variables.
- `kubernetes/apps/` — Flux-managed applications grouped by namespace/category.
- `kubernetes/apps/*/kustomization.yaml` — namespace-level Kustomize entries that include `namespace.yaml` and child app `ks.yaml` files.
- `kubernetes/flux/cluster/ks.yaml` — Flux Kustomization that points at `./kubernetes/apps` and applies common defaults.
- `kubernetes/components/sops/` — SOPS-related Kustomize component.
- `talos/` — Talos cluster configuration and generated config inputs.
- `bootstrap/` and `scripts/` — bootstrap support files and scripts.
- `.github/` — repository automation metadata.

## Common Commands

Prefer read-only/local commands first:

```sh
git status --short
git diff
task --list
find kubernetes/apps -maxdepth 2 -name kustomization.yaml -print | sort
```

Useful project tasks:

```sh
task reconcile              # Force Flux reconcile; touches the cluster
task bootstrap:apps         # Bootstrap apps; touches the cluster
task bootstrap:talos        # Bootstrap Talos; destructive/cluster-affecting
task talos:generate-config  # Generate Talos config locally
task talos:apply-node IP=... MODE=auto
task talos:upgrade-node IP=...
task talos:upgrade-k8s
task talos:reset            # Destructive
```

Before suggesting or running cluster-affecting commands, explain what they do and ask for explicit confirmation.

## GitOps Workflow

1. Inspect the relevant area under `kubernetes/apps/` or `talos/`.
2. Follow existing layout and naming conventions.
3. Make the smallest declarative change possible.
4. Run safe local checks where available.
5. Review `git diff`.
6. Commit and push.
7. Reconcile with Flux only after the user confirms cluster access is intended.

Application namespaces generally have this pattern:

```text
kubernetes/apps/<area>/
├── namespace.yaml
├── kustomization.yaml
└── <app>/
    └── ks.yaml
```

Child app directories commonly contain Flux `Kustomization`, `HelmRelease`, `HelmRepository`/source references, config, and secrets references.

## Secrets and Safety Rules

Do not reveal, decrypt, print, or modify secrets unless the user explicitly asks and confirms the exact operation.

Treat these as sensitive:

- `age.key`
- `kubeconfig`
- `.sops.yaml`
- `*.sops.yaml`
- Talos secrets and generated Talos configs
- Cluster credentials, tokens, API keys, kubeconfigs, and 1Password references

Safe handling expectations:

- Do not run `sops --decrypt` unless explicitly requested and confirmed.
- Do not print secret values to chat or logs.
- Do not commit raw Kubernetes `Secret` values.
- Prefer `ExternalSecret` or encrypted SOPS-managed files following existing repo patterns.
- Placeholder secret item names, such as `REPLACE_WITH_...`, should be called out before commit.
- Be careful with `task bootstrap:*`, `task talos:*`, `kubectl`, `flux`, and `talosctl`; these can affect live infrastructure.

## Commit Message Conventions

When suggesting or creating commits, prefer plain Conventional Commit-style prefixes without scopes:

- `feat: ...` for adding a new app, service, capability, or meaningful behavior.
- `fix: ...` for broken behavior, regressions, security fixes, or incorrect configuration.
- `chore: ...` for maintenance, cleanup, migrations, dependency/repo upkeep, and routine infrastructure changes.

Do not add scope parentheses such as `feat(app): ...` or `fix(container): ...` unless the user explicitly asks for them. The user's default preference is `chore: ...` when a change does not clearly need `feat:` or `fix:`.

Examples:

```sh
feat: add gatus monitoring
fix: correct unifi dns secret reference
chore: migrate unifi-dns to external secrets
```

## Editing Conventions

- Preserve YAML document starts (`---`) where present.
- Keep manifests concise and consistent with neighboring files.
- Prefer existing repo patterns over introducing new structure.
- Use Kustomize resources/components consistently.
- Keep namespace-level `kustomization.yaml` files sorted or grouped consistently with existing entries.
- For Flux resources, check existing `ks.yaml` and `HelmRelease` examples before creating new ones.
- Avoid broad rewrites; make targeted edits.

## Agent Behavior

- Start by checking `git status --short` so existing user changes are visible.
- Read nearby files before editing.
- Ask before overwriting or restructuring user work.
- For infrastructure changes, explain risk and validation steps.
- Prefer local validation over live-cluster operations.
- After edits, summarize changed files and suggest next safe commands.
