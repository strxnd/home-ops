#!/usr/bin/env bash
# Materialize gitignored cluster credentials from Cloud Agent secrets.
# Expected Runtime Secrets (file contents, not paths):
#   HOME_OPS_KUBECONFIG      -> ./kubeconfig
#   HOME_OPS_AGE_KEY         -> ./age.key
#   HOME_OPS_TALOSCONFIG     -> ./talos/clusterconfig/talosconfig
#   HOME_OPS_WIREGUARD_CONF  -> ./.private/wg0.conf (split-tunnel AllowedIPs)
# Never print secret values.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export REPO_ROOT

python3 "${REPO_ROOT}/.cursor/normalize-cloud-secrets.py"

export KUBECONFIG="${REPO_ROOT}/kubeconfig"
export SOPS_AGE_KEY_FILE="${REPO_ROOT}/age.key"
export TALOSCONFIG="${REPO_ROOT}/talos/clusterconfig/talosconfig"

WG_CONF="${REPO_ROOT}/.private/wg0.conf"
if [ -f "${WG_CONF}" ]; then
  if ! command -v wg-quick >/dev/null 2>&1 || ! command -v wireguard-go >/dev/null 2>&1; then
    echo "start: wireguard-tools/wireguard-go missing; skip tunnel" >&2
  else
    sudo -n mkdir -p /etc/wireguard
    sudo -n cp "${WG_CONF}" /etc/wireguard/wg0.conf
    sudo -n chmod 600 /etc/wireguard/wg0.conf
    if sudo -n wg show wg0 >/dev/null 2>&1; then
      echo "start: wg0 already up"
    else
      # Discard wg-quick stdout; it prints interface addresses.
      if sudo -n env WG_QUICK_USERSPACE_IMPLEMENTATION=wireguard-go wg-quick up wg0 >/tmp/wg-quick.out 2>/tmp/wg-quick.err; then
        echo "start: wg0 up (userspace wireguard-go, split-tunnel LAN only)"
      else
        echo "start: wg-quick failed; see /tmp/wg-quick.err (no secrets expected)" >&2
      fi
    fi
  fi
fi

# Do not fail the environment when secrets are absent so local flux-local /
# kustomize work still boots. kubectl/sops/talosctl need the files present.
exit 0
