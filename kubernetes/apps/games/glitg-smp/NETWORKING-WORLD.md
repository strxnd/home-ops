# GLITG SMP Networking And World Runbook

## Addresses

- LAN server address: `192.168.20.12:25565`
- Public player join address: `glitg.${SECRET_DOMAIN}`
- Protocol: TCP
- Existing router forward: WAN TCP `25565` to `192.168.20.12:25565`

The Minecraft Pod uses the single node's host network, so Paper binds directly
to `192.168.20.12:25565` while retaining an empty `server-ip`. This avoids
claiming the node IP as a Cilium LoadBalancer virtual IP. Do not replace the
existing router forward. The `DNSEndpoint` keeps the
`glitg` A record DNS-only and pointed at `${SECRET_PUBLIC_IP}`.

## World Safety

The image writes `level-seed=-3085124641489919468` from `SEED` before it
generates a new `glitg-smp` world. It does not delete an existing PVC/world.

Before the first production start, inspect whether a retained world exists. If
it does, determine whether it is staging or production and verify its actual
seed. If the intended production world has the wrong seed, back it up before
regeneration; do not delete it blindly. After startup, verify the actual
Overworld seed in-game or from the server console and record the result in the
deployed checklist. Verify that the Nether and End are the dimensions created
from that same world.

The bundled datapack centres each dimension's border at `0, 0` and sets it to
`4000`; confirm `/worldborder get` in the Overworld reports a 4,000-block
diameter (a 4,000 x 4,000 playable area).

## Required Manual Validation

1. From another LAN device, ping/list/join `192.168.20.12:25565` with a
   Minecraft 1.21.11 client and confirm Paper is not blocked by a server
   firewall.
2. Verify `glitg.${SECRET_DOMAIN}` resolves to the expected WAN IPv4 and is
   not HTTP-proxied.
3. From a genuinely external network, list/ping/join
   `glitg.${SECRET_DOMAIN}` without appending a port. A LAN failure against
   the public name can be a missing hairpin-NAT feature; it does not prove the
   external forward failed.

Do not mark networking complete until the external client test has passed. If
an external test is unavailable, mark it **manual verification required** in
the checklist.
