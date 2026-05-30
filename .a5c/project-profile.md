# Project Profile: home-ops

GitOps home operations repository for a single-node Talos Kubernetes homelab cluster. The project aims to build enterprise-grade self-hosted infrastructure, maintain future-proof infrastructure skills, and replace big-tech/subscription services with privacy-respecting alternatives.

> Last updated: 2026-05-30T01:25:07Z | Version: 1

## Goals

- **infrastructure** [high]: Develop enterprise-grade homelab infrastructure using GitOps, Talos, Kubernetes, Flux, and strong operational practices. (active)
- **skills** [high]: Maintain useful future infrastructure, security, automation, and self-hosting skills through practical operation of the cluster. (active)
- **privacy** [high]: Replace big-tech and subscription services with self-hosted, privacy-respecting, low-lock-in alternatives where practical. (active)
- **reliability** [high]: Keep the single-node cluster understandable, recoverable, observable, and safely change-managed. (active)

## Tech Stack

### Languages

- YAML (primary manifests)
- Bash (automation scripts)
- Markdown (documentation)
- JSON5 (Renovate config)

### Frameworks

- Kubernetes
- Talos Linux
- Flux CD
- Kustomize
- Helm
- Helmfile
- External Secrets Operator
- SOPS/age
- Renovate
- GitHub Actions

### Infrastructure

- Cilium [networking]
- Envoy Gateway [ingress]
- Cloudflare Tunnel [ingress]
- ExternalDNS [dns]
- 1Password Connect [secrets]
- local-path-provisioner [storage]
- kube-prometheus-stack/Grafana/Victoria Logs/Fluent Bit/Gatus [observability]

**Build tools:** task, kustomize, kubeconform, flux-local, helm, helmfile, talhelper, sops, age, yq, jq

**Package managers:** mise/aqua, Renovate, Helm/OCI chart sources

## Architecture

**Pattern:** Infrastructure-as-code GitOps monorepo for a single-node Talos Kubernetes cluster.
**Data flow:** Local declarative manifest changes are validated, reviewed, committed to Git, and then applied by Flux after push/merge. HelmRelease resources pull charts from HelmRepository/OCIRepository sources. Secrets are provided through SOPS or External Secrets backed by 1Password Connect.

### Modules

| Module | Path | Description |
|--------|------|-------------|
| Flux root | `kubernetes/flux/cluster` | Root cluster Kustomization for kubernetes/apps |
| Kubernetes apps | `kubernetes/apps` | Namespace-grouped application definitions |
| Talos | `talos` | Talos and talhelper configuration and patches |
| Bootstrap | `bootstrap` | Bootstrap Helmfile and encrypted bootstrap material |
| Tasks/scripts | `Taskfile.yaml .taskfiles scripts` | Operational command wrappers and bootstrap helpers |
| Automation | `.github .renovaterc.json5` | GitHub Actions and Renovate automation |

**Entry points:** `Taskfile.yaml`, `kubernetes/flux/cluster/ks.yaml`, `kubernetes/apps/*/kustomization.yaml`, `talos/talconfig.yaml`, `.github/workflows/flux-local.yaml`

## Team

- **Kumar Aarav** (owner/operator): infrastructure design, GitOps changes, validation, self-hosted app operations, privacy/security decisions
- **Dad** (collaborator/reviewer): household stakeholder, review/support when useful

## Workflows

### development

Direct-to-main local workflow with self-review gate and manual commit/push/apply decisions.
**Triggers:** user request

1. git status --short
2. inspect relevant files
3. make minimal declarative change
4. run safe local validation
5. review git diff
6. commit/push only after explicit user approval

### GitOps apply

Flux applies committed repository state to the cluster after push/merge.
**Triggers:** manual push/merge

1. commit to main
2. push
3. Flux reconciles
4. observe result if explicitly requested

### secret management

Prefer ExternalSecret + 1Password Connect for new app credentials; avoid secret decryption and raw Secret commits.
**Triggers:** new app credentials

1. identify required keys
2. create ExternalSecret manifest
3. call out placeholder/assumed item names
4. validate locally

### cluster-affecting operations

Live cluster/Talos/bootstrap commands are gated by explicit confirmation.
**Triggers:** explicit operational request

1. explain command impact
2. ask for confirmation
3. run only if approved
4. summarize results

## Processes

- **Plan-first infrastructure change** (`undefined`, undefined) - Research chart/app, inspect repo patterns, draft plan, ask decisions, implement only after sufficient clarity.
- **Safe GitOps app deployment** (`undefined`, undefined) - Add namespace/app manifests following existing Flux/Kustomize/ExternalSecret conventions and validate locally.
- **Secret-safe troubleshooting** (`undefined`, undefined) - Diagnose without decrypting or printing secrets unless explicitly confirmed.

## Tools

### Linting

- undefined
- undefined
- undefined

### Testing

- undefined
- undefined

### Formatting

- undefined
- undefined
- undefined

## Services

- **Flux** (gitops)
- **Talos** (cluster-os)
- **Cloudflare** (dns/tunnel)
- **UniFi** (network/dns)
- **1Password Connect** (secret-backend)
- **Renovate** (dependency-automation)

## CI/CD

**Provider:** GitHub Actions
**Config files:** `.github/workflows/flux-local.yaml`, `.github/workflows/label-sync.yaml`, `.github/workflows/labeler.yaml`

### Pipelines

- **flux-local** (trigger: pull_request)
  Stages: flux-local test -> flux-local diff

## Pain Points

- **high** [security]: Secret handling spans SOPS, age, 1Password Connect, and External Secrets; unsafe reads/decrypts could leak credentials.
  - Remediation: Keep strict confirmation gates and prefer ExternalSecret for new app credentials.
- **medium** [validation]: Some Kubernetes correctness depends on cluster state even when local rendering passes.
  - Remediation: Run kustomize/kubeconform/flux-local locally where possible and use live commands only with confirmation.
- **medium** [operations]: Single-node cluster means storage and workload changes can have outsized operational impact.

## Bottlenecks

- Peacesmp HelmRelease is high-churn and may benefit from careful validation/review. at kubernetes/apps/games/peacesmp/app/helmrelease.yaml (high)
  Impact: medium
- Mixed SOPS and ExternalSecret patterns require careful secret strategy selection. at kubernetes/apps/** (recurring)
  Impact: high
- Taskfile exposes live and destructive cluster/Talos operations. at Taskfile.yaml .taskfiles/** (persistent)
  Impact: high

## Conventions

### Naming

- **apps:** kubernetes/apps/<namespace>/<app>/app plus <app>/ks.yaml
- **flux:** metadata.name should match app directory
- **secrets:** ExternalSecret preferred for new credentials; *.sops.yaml only when explicitly requested

### Git

- **branchStrategy:** direct-to-main
- **reviewProcess:** self-review gate
- **releaseCadence:** manual merge/apply
- **commitPrefixes:** feat:, fix:, chore: without scope unless asked

**Import order:** namespace.yaml before child app ks.yaml > app kustomization resources listed explicitly

**Error handling:** Explain risks and ask before destructive/live/secret operations; keep blockers in-progress rather than pretending success.

**Testing:** Prefer safe local validation: kustomize build, kubeconform, flux-local; do not run live cluster commands without confirmation.

### Additional Rules

- Start with git status --short
- Do not overwrite user changes
- Do not decrypt or reveal secrets
- Do not commit raw Kubernetes Secret values
- Pin chart/image/dependency versions
- Review git diff after edits
- Commit/push only when asked

## Repositories

- **home-ops** [`/home/kumaraarav/dev/home-ops`]

## CLAUDE.md Instructions

- Follow AGENTS.md as the source of truth for repository safety and workflow.
- For Kubernetes changes, inspect existing app patterns before editing and prefer ExternalSecret for new credentials.
- Do not run live cluster, Talos, bootstrap, or secret decryption commands without explicit confirmation.
- Use direct, structured, precise summaries and call out risks/placeholders.

## Installed Extensions

- Skills: babysit, plan, project-install, doctor, retrospect, cleanup
- Agents: general-purpose, Explore, Plan, code-reviewer, security-engineer
- Processes: cradle/project-install, cradle/bugfix, cradle/feature-implementation-contribute
