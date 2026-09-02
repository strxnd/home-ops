#!/usr/bin/env python3
"""Normalize Cloud Agent credential secrets into the gitignored files.

Never prints secret values. Handles multiline YAML, literal \\n, base64, and
YAML that a secrets form flattened onto one line.
"""
from __future__ import annotations

import base64
import os
import re
import sys
from pathlib import Path

KUBE_KEYS = [
    "certificate-authority-data",
    "client-certificate-data",
    "client-key-data",
    "current-context",
    "insecure-skip-tls-verify",
    "certificate-authority",
    "apiVersion",
    "preferences",
    "clusters",
    "contexts",
    "cluster",
    "context",
    "namespace",
    "server",
    "users",
    "user",
    "kind",
    "name",
]

AGE_RE = re.compile(r"AGE-SECRET-KEY-1[A-Z0-9]+")
TALHELPER_MARKERS = (
    "clusterName:",
    "talosVersion:",
    "kubernetesVersion:",
    "additionalApiServerCertSans:",
    "installDisk:",
)
TALOSCONFIG_MARKERS = ("contexts:", "endpoints:", "context:")


def _maybe_b64(value: str) -> str | None:
    compact = "".join(value.split())
    if len(compact) < 16 or not re.fullmatch(r"[A-Za-z0-9+/]+=*", compact):
        return None
    try:
        decoded = base64.b64decode(compact, validate=True).decode("utf-8")
    except (ValueError, UnicodeDecodeError):
        return None
    return decoded


def _unescape(value: str) -> str:
    if "\\n" in value and "\n" not in value.strip():
        return value.replace("\\n", "\n")
    return value


def _line_count(value: str) -> int:
    return len(value.strip().splitlines())


def normalize_kubeconfig(value: str) -> str:
    value = _unescape(value.strip())
    decoded = _maybe_b64(value)
    if decoded and ("apiVersion:" in decoded or "clusters:" in decoded):
        value = decoded.strip()
    if _line_count(value) > 2 and "apiVersion:" in value:
        return value if value.endswith("\n") else value + "\n"

    pat = re.compile(r"(?P<list> - )?(?P<key>" + "|".join(re.escape(k) for k in KUBE_KEYS) + r"):")
    matches = list(pat.finditer(value))
    if not matches:
        return value if value.endswith("\n") else value + "\n"

    tokens: list[tuple[bool, str, str]] = []
    for i, match in enumerate(matches):
        end = matches[i + 1].start() if i + 1 < len(matches) else len(value)
        tokens.append((bool(match.group("list")), match.group("key"), value[match.end() : end].strip()))

    lines: list[str] = []
    section = ""
    for is_list, key, val in tokens:
        if key in {"apiVersion", "kind", "preferences", "current-context"}:
            lines.append(f"{key}: {val}".rstrip())
            continue
        if key in {"clusters", "contexts", "users"}:
            section = key
            lines.append(f"{key}:")
            continue
        if section == "clusters":
            if key == "cluster":
                lines.append("- cluster:")
            elif key in {
                "certificate-authority-data",
                "certificate-authority",
                "server",
                "insecure-skip-tls-verify",
            }:
                lines.append(f"    {key}: {val}")
            elif key == "name":
                lines.append(f"  name: {val}")
        elif section == "contexts":
            if key == "context":
                lines.append("- context:")
            elif key in {"cluster", "user", "namespace"}:
                lines.append(f"    {key}: {val}")
            elif key == "name":
                lines.append(f"  name: {val}")
        elif section == "users":
            if key == "name":
                lines.append(f"- name: {val}")
            elif key == "user":
                lines.append("  user:")
            elif key in {"client-certificate-data", "client-key-data", "token"}:
                lines.append(f"    {key}: {val}")
    return "\n".join(lines) + "\n"


def normalize_age_key(value: str) -> str:
    value = _unescape(value.strip())
    decoded = _maybe_b64(value)
    if decoded and AGE_RE.search(decoded):
        value = decoded.strip()
    match = AGE_RE.search(value)
    if not match:
        return value if value.endswith("\n") else value + "\n"
    if _line_count(value) > 1 and value.lstrip().startswith("#"):
        return value if value.endswith("\n") else value + "\n"
    return match.group(0) + "\n"


def looks_like_talhelper(value: str) -> bool:
    return sum(marker in value for marker in TALHELPER_MARKERS) >= 2


def looks_like_talosconfig(value: str) -> bool:
    return all(marker in value for marker in ("context:", "contexts:")) and not looks_like_talhelper(value)


def _yaml_list_items(raw: str) -> list[str]:
    return [part.strip() for part in re.split(r"\s*-\s*", raw.strip()) if part.strip()]


def _unflatten_talosconfig(value: str) -> str | None:
    match = re.search(
        r"context:\s*(\S+)\s+contexts:\s*(\S+):\s*endpoints:\s*(.*?)"
        r"(?:\s+nodes:\s*(.*?))?\s+ca:\s*(\S+)\s+crt:\s*(\S+)\s+key:\s*(\S+)\s*$",
        value,
    )
    if not match:
        return None
    ctx, ctx2, endpoints_raw, nodes_raw, ca, crt, key = match.groups()
    lines = [f"context: {ctx}", "contexts:", f"  {ctx2}:", "    endpoints:"]
    for endpoint in _yaml_list_items(endpoints_raw):
        lines.append(f"      - {endpoint}")
    if nodes_raw:
        lines.append("    nodes:")
        for node in _yaml_list_items(nodes_raw):
            lines.append(f"      - {node}")
    lines.extend([f"    ca: {ca}", f"    crt: {crt}", f"    key: {key}"])
    return "\n".join(lines) + "\n"


def normalize_talosconfig(value: str) -> str | None:
    value = _unescape(value.strip())
    decoded = _maybe_b64(value)
    if decoded and (looks_like_talosconfig(decoded) or looks_like_talhelper(decoded)):
        value = decoded.strip()
    if looks_like_talhelper(value):
        return None
    if _line_count(value) > 2 and looks_like_talosconfig(value):
        return value if value.endswith("\n") else value + "\n"
    unflattened = _unflatten_talosconfig(value)
    if unflattened:
        return unflattened
    if looks_like_talosconfig(value):
        return value if value.endswith("\n") else value + "\n"
    return value if value.endswith("\n") else value + "\n"


WG_KEYS = (
    "PrivateKey",
    "Address",
    "DNS",
    "ListenPort",
    "MTU",
    "PublicKey",
    "PresharedKey",
    "AllowedIPs",
    "Endpoint",
    "PersistentKeepalive",
)
WG_SPLIT_ALLOWED_IPS = "192.168.20.0/24, 192.168.0.0/24"
WG_KEY_RE = re.compile(
    r"(?P<key>" + "|".join(WG_KEYS) + r")\s*=\s*(?P<value>.*?)(?=(?:\s+(?:" + "|".join(WG_KEYS) + r")\s*=|\s*\[|$))",
    re.DOTALL,
)


def normalize_wireguard(value: str) -> str | None:
    value = _unescape(value.strip())
    decoded = _maybe_b64(value)
    if decoded and "[Interface]" in decoded and "[Peer]" in decoded:
        value = decoded.strip()
    if "[Interface]" not in value or "[Peer]" not in value:
        return None

    sections: list[tuple[str, list[tuple[str, str]]]] = []
    parts = re.split(r"\[(Interface|Peer)\]", value)
    # split yields [preamble, name, body, name, body, ...]
    for i in range(1, len(parts), 2):
        name = parts[i]
        body = parts[i + 1] if i + 1 < len(parts) else ""
        pairs = [(m.group("key"), " ".join(m.group("value").split())) for m in WG_KEY_RE.finditer(body)]
        sections.append((name, pairs))

    if not any(name == "Interface" for name, _ in sections) or not any(name == "Peer" for name, _ in sections):
        return None

    lines: list[str] = []
    for name, pairs in sections:
        if lines:
            lines.append("")
        lines.append(f"[{name}]")
        seen = set()
        for key, val in pairs:
            if key == "DNS":
                continue
            if key == "AllowedIPs":
                val = WG_SPLIT_ALLOWED_IPS
            lines.append(f"{key} = {val}")
            seen.add(key)
        if name == "Peer" and "AllowedIPs" not in seen:
            lines.append(f"AllowedIPs = {WG_SPLIT_ALLOWED_IPS}")
        if name == "Peer" and "PersistentKeepalive" not in seen:
            lines.append("PersistentKeepalive = 25")
    return "\n".join(lines) + "\n"


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(f".{path.name}.tmp")
    tmp.write_text(content)
    tmp.chmod(0o600)
    tmp.replace(path)


def main() -> int:
    repo = Path(os.environ.get("REPO_ROOT") or Path(__file__).resolve().parent.parent)
    wrote = 0
    missing = 0

    kube = os.environ.get("HOME_OPS_KUBECONFIG", "")
    if kube:
        write_file(repo / "kubeconfig", normalize_kubeconfig(kube))
        wrote += 1
    else:
        print(f"start: HOME_OPS_KUBECONFIG is unset; {repo / 'kubeconfig'} not written", file=sys.stderr)
        missing += 1

    age = os.environ.get("HOME_OPS_AGE_KEY", "")
    if age:
        write_file(repo / "age.key", normalize_age_key(age))
        wrote += 1
    else:
        print(f"start: HOME_OPS_AGE_KEY is unset; {repo / 'age.key'} not written", file=sys.stderr)
        missing += 1

    talos = os.environ.get("HOME_OPS_TALOSCONFIG", "")
    if talos:
        normalized = normalize_talosconfig(talos)
        dest = repo / "talos" / "clusterconfig" / "talosconfig"
        if normalized is None:
            print(
                "start: HOME_OPS_TALOSCONFIG looks like talhelper cluster/nodes YAML, "
                f"not a talosconfig; {dest} not written",
                file=sys.stderr,
            )
            missing += 1
        else:
            write_file(dest, normalized)
            wrote += 1
    else:
        print("start: HOME_OPS_TALOSCONFIG is unset; talosconfig not written", file=sys.stderr)
        missing += 1

    wg = os.environ.get("HOME_OPS_WIREGUARD_CONF", "")
    wg_dest = repo / ".private" / "wg0.conf"
    if wg:
        normalized = normalize_wireguard(wg)
        if normalized is None:
            print("start: HOME_OPS_WIREGUARD_CONF is not a WireGuard config; wg0.conf not written", file=sys.stderr)
            missing += 1
        else:
            write_file(wg_dest, normalized)
            wrote += 1
    else:
        print(f"start: HOME_OPS_WIREGUARD_CONF is unset; {wg_dest} not written", file=sys.stderr)
        missing += 1

    print(f"start: wrote {wrote} credential file(s); {missing} secret(s) unset or skipped")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
