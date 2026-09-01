#!/usr/bin/env bash
# Materialize gitignored cluster credentials from Cloud Agent secrets.
# Expected Runtime Secrets (file contents, not paths):
#   HOME_OPS_KUBECONFIG   -> ./kubeconfig
#   HOME_OPS_AGE_KEY      -> ./age.key
#   HOME_OPS_TALOSCONFIG  -> ./talos/clusterconfig/talosconfig
# Never print secret values.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

write_secret_file() {
  local dest="$1"
  local value="$2"
  local dest_dir tmp
  dest_dir="$(dirname "${dest}")"
  mkdir -p "${dest_dir}"
  tmp="$(mktemp -p "${dest_dir}" ".$(basename "${dest}").XXXXXX")"
  # Preserve the secret body and guarantee a terminating newline.
  printf '%s\n' "${value%$'\n'}" >"${tmp}"
  chmod 600 "${tmp}"
  mv -f "${tmp}" "${dest}"
}

wrote=0
missing=0

if [ -n "${HOME_OPS_KUBECONFIG:-}" ]; then
  write_secret_file "${REPO_ROOT}/kubeconfig" "${HOME_OPS_KUBECONFIG}"
  wrote=$((wrote + 1))
else
  echo "start: HOME_OPS_KUBECONFIG is unset; ${REPO_ROOT}/kubeconfig not written" >&2
  missing=$((missing + 1))
fi

if [ -n "${HOME_OPS_AGE_KEY:-}" ]; then
  write_secret_file "${REPO_ROOT}/age.key" "${HOME_OPS_AGE_KEY}"
  wrote=$((wrote + 1))
else
  echo "start: HOME_OPS_AGE_KEY is unset; ${REPO_ROOT}/age.key not written" >&2
  missing=$((missing + 1))
fi

if [ -n "${HOME_OPS_TALOSCONFIG:-}" ]; then
  write_secret_file "${REPO_ROOT}/talos/clusterconfig/talosconfig" "${HOME_OPS_TALOSCONFIG}"
  wrote=$((wrote + 1))
else
  echo "start: HOME_OPS_TALOSCONFIG is unset; talosconfig not written" >&2
  missing=$((missing + 1))
fi

export KUBECONFIG="${REPO_ROOT}/kubeconfig"
export SOPS_AGE_KEY_FILE="${REPO_ROOT}/age.key"
export TALOSCONFIG="${REPO_ROOT}/talos/clusterconfig/talosconfig"

echo "start: wrote ${wrote} credential file(s); ${missing} secret(s) unset"
# Do not fail the environment when secrets are absent so local flux-local /
# kustomize work still boots. kubectl/sops/talosctl need the files present.
exit 0
