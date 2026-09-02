#!/usr/bin/env bash
# Idempotent Cloud Agent bootstrap for this GitOps repo.
# Installs the mise-pinned CLI toolchain (.mise.toml) plus flux-local, the
# tool used by the "Flux Local" CI workflow to validate kubernetes/ manifests.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_ROOT}"

MISE_BIN="${HOME}/.local/bin/mise"
FLUX_LOCAL_VENV="${HOME}/.venv/flux-local"
FLUX_LOCAL_VERSION="8.4.0"
PYTHON_VERSION="3.13"
# Pin mise itself so identical commits always bootstrap the same tooling. The
# mise.run installer honours MISE_VERSION and verifies the release checksum for
# the pinned version.
MISE_VERSION="v2026.9.0"

# 1. Install mise (single static binary) if it is not already present.
if [ ! -x "${MISE_BIN}" ] && ! command -v mise >/dev/null 2>&1; then
  curl -fsSL https://mise.run | MISE_VERSION="${MISE_VERSION}" sh
fi
export PATH="${HOME}/.local/bin:${PATH}"

# 2. Install the toolchain pinned in .mise.toml (task, flux, kustomize,
#    kubeconform, sops, age, helm, kubectl, talosctl, talhelper, ...).
mise trust --yes "${REPO_ROOT}/.mise.toml"
mise install --yes

# 3. Userspace WireGuard (no kernel module in Cloud Agent VMs). Pinned
#    Ubuntu 24.04 packages so install stays deterministic.
sudo -n apt-get update -qq
sudo -n apt-get install -y --no-install-recommends \
  iproute2=6.1.0-1ubuntu6.4 \
  wireguard-tools=1.0.20210914-1ubuntu4 \
  wireguard-go=0.0.20230223-1ubuntu0.24.04.2

# 4. flux-local >= 8 needs Python >= 3.13; use a mise-managed standalone
#    interpreter (WireGuard packages above are the only apt dependency).
mise install --yes "python@${PYTHON_VERSION}"
if [ ! -x "${FLUX_LOCAL_VENV}/bin/python" ]; then
  mise exec "python@${PYTHON_VERSION}" -- python3 -m venv "${FLUX_LOCAL_VENV}"
fi
"${FLUX_LOCAL_VENV}/bin/pip" install --quiet --upgrade pip
"${FLUX_LOCAL_VENV}/bin/pip" install --quiet "flux-local==${FLUX_LOCAL_VERSION}"

# 5. Put the toolchain on PATH for every future shell (mise shims work in
#    non-interactive shells; flux-local from its venv). Point kubectl/sops/
#    talosctl at the gitignored credential paths from .mise.toml. Guarded
#    so re-runs do not duplicate the block, and placed before .bashrc's
#    interactive guard so it applies to login and non-login shells alike.
BASHRC="${HOME}/.bashrc"
MARKER="# >>> home-ops cloud-agent PATH >>>"
if [ -f "${BASHRC}" ] && ! grep -qF "${MARKER}" "${BASHRC}"; then
  TMP="$(mktemp)"
  {
    echo "${MARKER}"
    echo 'export PATH="$HOME/.local/bin:$HOME/.local/share/mise/shims:$HOME/.venv/flux-local/bin:$PATH"'
    echo "export KUBECONFIG=\"${REPO_ROOT}/kubeconfig\""
    echo "export SOPS_AGE_KEY_FILE=\"${REPO_ROOT}/age.key\""
    echo "export TALOSCONFIG=\"${REPO_ROOT}/talos/clusterconfig/talosconfig\""
    echo "# <<< home-ops cloud-agent PATH <<<"
    echo ""
    cat "${BASHRC}"
  } >"${TMP}"
  mv "${TMP}" "${BASHRC}"
fi

echo "home-ops environment ready: mise toolchain + flux-local ${FLUX_LOCAL_VERSION} installed."
