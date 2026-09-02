#!/usr/bin/env bash
# Materialize gitignored cluster credentials from Cloud Agent secrets.
# Expected Runtime Secrets (file contents, not paths):
#   HOME_OPS_KUBECONFIG   -> ./kubeconfig
#   HOME_OPS_AGE_KEY      -> ./age.key
#   HOME_OPS_TALOSCONFIG  -> ./talos/clusterconfig/talosconfig
# Never print secret values.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export REPO_ROOT

python3 "${REPO_ROOT}/.cursor/normalize-cloud-secrets.py"

export KUBECONFIG="${REPO_ROOT}/kubeconfig"
export SOPS_AGE_KEY_FILE="${REPO_ROOT}/age.key"
export TALOSCONFIG="${REPO_ROOT}/talos/clusterconfig/talosconfig"

# Do not fail the environment when secrets are absent so local flux-local /
# kustomize work still boots. kubectl/sops/talosctl need the files present.
exit 0
