# Omarchy VM

This deploys VM infrastructure only: KubeVirt, CDI, a blank system disk, and the
Omarchy 4.5.0 x86 installer. Complete the OS installation interactively. There is
no cloud-init, guest account, package configuration, T3 setup, or guest remote
desktop service declared here.

## Deployment

The namespace Kustomization is discovered by the existing Flux app tree. After
the changes are pushed, Flux installs KubeVirt v1.9.1 and CDI v1.66.1 in their own
namespaces. The Omarchy resources wait for both operators to report Available.
The VM starts after CDI imports the approximately 6.1 GiB installer ISO.

The Talos node must expose `/dev/kvm` with hardware virtualization enabled in
BIOS. It was present on `ms-02-ultra` during preparation. No Talos machine
configuration changes are included. KubeVirt requires privileged host access;
the VM namespace also permits privileged workloads.

The VM has 8 virtual CPUs (2 CPUs requested, capped at 8), 32 GiB guest RAM, a
250 GiB local-path system PVC, and UEFI firmware. The disk is thin
provisioned on the same node as the rest of the cluster; the PVC size does not
reserve physical disk capacity. Leave room for other workloads and the importer.

## Install through the console

Open [omarchy.kumaraarav.dev](https://omarchy.kumaraarav.dev) from the lab network.
The internal Envoy HTTPS gateway serves a noVNC console connected to KubeVirt.
The web console works before installation and during boot without guest software.
It has full keyboard/display access with no separate console login, so access is
limited to the internal gateway; it is not routed through the public gateway.

The console service account can only read the `omarchy` VMI and open its VNC
subresource. An init container fetches checksum-verified virtctl v1.9.1; the
stock bonigarcia/novnc v1.3.0 image supplies the web client and WebSocket proxy.
Nothing is installed in the VM.
One console connection is supported at a time; reconnect after a VM restart.

For direct access through the Kubernetes API instead:

Install `virtctl` matching KubeVirt v1.9.1 and a VNC viewer such as TigerVNC on
your own computer. Using this repository's kubeconfig:

```sh
kubectl -n virtualization get vm,vmi,dv,pvc
virtctl -n virtualization vnc omarchy
```

If automatic viewer launch is unavailable, run:

```sh
virtctl -n virtualization vnc omarchy --proxy-only
```

Connect your VNC viewer to the local address/port printed by that command. The
console is tunneled through the Kubernetes API; no public VNC service is created.

Select the blank 250 GiB VirtIO disk in the Omarchy installer. Guest credentials,
disk encryption, and all other installation choices are entered manually. Disk
encryption may require console unlock after each cold boot.

UEFI variables are ephemeral: persistent NVRAM failed with a file permission
error on this local-path setup. Omarchy's installer enables the standard EFI
fallback boot path, so the installed disk can boot without saved firmware entries.

The system disk has first boot priority. Before installation, firmware falls
back to the ISO; after installation, it boots the system disk. If necessary, use
the UEFI boot menu to select the installer. The ISO can remain attached or be
removed later from the VM's `disks` and `volumes` in Git.

The VM uses an emulated VirtIO display without GPU passthrough or 3D
acceleration. Omarchy's graphical session may need manual software-rendering
adjustments. A booting installer/console does not establish desktop performance.

## Lifecycle and storage

Guest networking uses pod-network masquerading for outbound access. No guest LAN
IP or SSH service is provisioned. The browser console's ingress is outside the VM,
and the VNC console remains available without guest software.

Flux owns `runStrategy: Always`; change it to `Halted` in Git for a persistent
stop. Reboot from inside the guest as needed. Guest shutdown alone causes an
automatic restart.

The system PVC and installer DataVolume are protected from Flux pruning.
Removing the VM manifests stops/deletes the VM but keeps those disks.
Explicit PVC deletion is destructive
because the existing local-path storage class uses reclaim policy Delete.
There is no backup configured, and node maintenance takes the VM offline.

## References

- [KubeVirt on Talos](https://docs.siderolabs.com/talos/v1.14/advanced-guides/install-kubevirt)
- [KubeVirt installation](https://kubevirt.io/user-guide/cluster_admin/installation/)
- [Omarchy installation](https://omarchy.org/manual/getting-started/)
