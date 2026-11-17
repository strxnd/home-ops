# Hermes Agent

The official Hermes desktop container runs its supervised gateway and built-in
Webapp at `https://hermes.<domain>` through `envoy-internal`. Access requires the
home network or a VPN into it. The external gateway does not publish Hermes.

The container starts as root for its official s6 bootstrap and runs Hermes as its
unprivileged user. The desktop image includes Bot Screen's Linux desktop packages;
no Docker socket, host desktop, or separate Cua Spaces service is mounted.

## Sign in

The username is `kumar`. External Secrets generates the password and session
signing secret once into the immutable `ai/hermes-auth` Secret. To copy the
password on macOS without printing it:

```sh
kubectl -n ai get secret hermes-auth \
  -o jsonpath='{.data.HERMES_DASHBOARD_BASIC_AUTH_PASSWORD}' | base64 --decode | pbcopy
```

This uses External Secrets' built-in password generator rather than depending on
a manually created 1Password item. The Secret is retained if its ExternalSecret
is removed. Intentional password rotation requires replacing the immutable
Secret and restarting Hermes.

## State and configuration

The retained 50 GiB `local-path` PVC `hermes-data` at `/opt/data` contains Hermes
configuration, provider logins, profiles, conversations, memories, skills,
routines, browser state, and files. Configure these in Hermes; Git does not
define them.

The init container makes the data directory private (required by Hermes's control
socket) and copies only the Webapp listener and reverse-proxy settings on the
first boot. It never replaces an existing `config.yaml`. Ingress is restricted
to the internal Envoy proxy; this makes the bounded pod CIDR in
`dashboard.trusted_proxies` appropriate for forwarded HTTPS headers.

Update Hermes by changing the pinned container image through Git, not by running
`hermes update` inside the container. PVC retention is not an off-node backup.

Optional tools install through Hermes's package manager into the persistent data
directory. Computer use can prepare its pinned driver with
`hermes computer-use install`; its Linux support is currently alpha. No driver or
agent settings are patched by this deployment.

## Official references

- [Hermes container deployment](https://hermes-agent.nousresearch.com/docs/user-guide/docker)
- [Bot Screen](https://hermes-agent.nousresearch.com/docs/user-guide/features/bot-screen)
- [External Secrets password generator](https://external-secrets.io/latest/api/generator/password/)
- [External Secrets credential lifecycle](https://external-secrets.io/latest/api/externalsecret/#createdonce)
