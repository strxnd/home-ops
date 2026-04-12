<div align="center">

<a href="https://github.com/strxnd"><img src="https://github.com/strxnd.png" align="center" width="175px" height="175px"/></a>

### <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f680/512.gif" alt="🚀" width="16" height="16"> My Home Operations Repository <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f6a7/512.gif" alt="🚧" width="16" height="16">

_... managed with Flux, Renovate, and GitHub Actions_ <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f916/512.gif" alt="🤖" width="16" height="16">

</div>

<div align="center">

[![Talos](https://img.shields.io/badge/Talos-blue?style=for-the-badge&logo=talos&logoColor=white)](https://www.talos.dev/)&nbsp;&nbsp;
[![Kubernetes](https://img.shields.io/badge/Kubernetes-blue?style=for-the-badge&logo=kubernetes&logoColor=white)](https://kubernetes.io/)&nbsp;&nbsp;
[![Flux](https://img.shields.io/badge/Flux-blue?style=for-the-badge&logo=flux&logoColor=white)](https://fluxcd.io/)&nbsp;&nbsp;
[![Renovate](https://img.shields.io/github/actions/workflow/status/strxnd/home-ops/renovate.yaml?branch=main&label=&logo=renovatebot&style=for-the-badge&color=blue)](https://github.com/strxnd/home-ops/actions/workflows/renovate.yaml)

</div>

---

## <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f4a1/512.gif" alt="💡" width="20" height="20"> Overview

This is a monorepo for my home infrastructure and Kubernetes cluster, built on the [cluster-template](https://github.com/onedr0p/cluster-template) by [onedr0p](https://github.com/onedr0p). I try to adhere to Infrastructure as Code (IaC) and GitOps practices using tools like [Kubernetes](https://kubernetes.io/), [Flux](https://github.com/fluxcd/flux2), [Renovate](https://github.com/renovatebot/renovate), and [GitHub Actions](https://github.com/features/actions).

---

## <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f331/512.gif" alt="🌱" width="20" height="20"> Kubernetes

My Kubernetes cluster is deployed with [Talos](https://www.talos.dev) on a single node. Workloads and storage share the same available resources on the node.

### Core Components

- **Networking**: [cilium](https://github.com/cilium/cilium) provides eBPF-based networking with L2 announcements. [envoy-gateway](https://gateway.envoyproxy.io/) handles external and internal ingress. [cloudflared](https://github.com/cloudflare/cloudflared) secures ingress traffic via Cloudflare, and [external-dns](https://github.com/kubernetes-sigs/external-dns) keeps DNS records in sync automatically.
- **Security & Secrets**: [cert-manager](https://github.com/cert-manager/cert-manager) automates SSL/TLS certificate management. Secrets are encrypted with [SOPS](https://github.com/getsops/sops) and [age](https://github.com/FiloSottile/age).
- **Storage**: [local-path-provisioner](https://github.com/rancher/local-path-provisioner) provides persistent volumes backed by the node's local NVMe storage.
- **Monitoring**: [kube-prometheus-stack](https://github.com/prometheus-community/helm-charts) for metrics and alerting, [Grafana](https://grafana.com/) for dashboards, [Victoria Logs](https://docs.victoriametrics.com/victorialogs/) and [Fluent Bit](https://fluentbit.io/) for log aggregation, and [Gatus](https://gatus.io/) for uptime monitoring.

### GitOps

[Flux](https://github.com/fluxcd/flux2) watches the [kubernetes](./kubernetes/) folder and makes changes to the cluster based on the state of my Git repository. It recursively searches for top level `kustomization.yaml` files in each directory. Each of these files contain a namespace resource as well as one or more Flux kustomization (`ks.yaml`). The Flux kustomizations control HelmRelease or other app resources.

[Renovate](https://github.com/renovatebot/renovate) scans my whole repo for out of date depedencies, automatically creating PRs when found. Merging these PRs triggers Flux to apply the changes to the cluster.

### Directories

This Git repository contains the following directories under [Kubernetes](./kubernetes/).

```sh
📁 kubernetes
├── 📁 apps       # applications
└── 📁 flux       # flux system configuration
```

---

## <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f30e/512.gif" alt="🌎" width="20" height="20"> DNS

There are two instances of [ExternalDNS](https://github.com/kubernetes-sigs/external-dns) running in my cluster. The first one for syncing internal DNS records to my `Unifi Cloud Gateway Fiber` using the [ExternalDNS webhook provider for UniFi](https://github.com/kashalls/external-dns-unifi-webhook), while the other one syncs public DNS to my `Cloudflare` domain. Which one handles a record depends on the ingress class being other `internal` for internal resolution only and `external` for public access.

---

## <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/2699_fe0f/512.gif" alt="⚙" width="20" height="20"> Hardware

<details>
  <summary>Click here to see my server rack</summary>

  <p align="center"><img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f3d7_fe0f/512.gif" alt="🏗️" width="40" height="40"></p>
  <p align="center"><i>TBD — rack photos coming soon!</i> <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f440/512.gif" alt="👀" width="16" height="16"></p>

</details>

| Device                      | Num | OS Disk Size | Ram              | OS    | Function           |
|-----------------------------|-----|--------------|------------------|-------|--------------------|
| Minisforum MS-02 Ultra      | 1   | 2TB NVMe     | 192GB DDR5 ECC   | Talos | Kubernetes         |
| UniFi Cloud Gateway Fiber   | 1   | -            | -                | -     | Router / Firewall  |
| UniFi Pro XG 8 PoE          | 1   | -            | -                | -     | Core Switch        |
| UniFi U7 Pro XG             | 1   | -            | -                | -     | Wi-Fi AP           |

### Networking

<details>
  <summary>Click here to see my high-level network diagram</summary>

  <p align="center"><img src="https://github.com/user-attachments/assets/17b8b010-dceb-4405-a907-ed2273942021" alt="Network Diagram"></p>

</details>

---

## <img src="https://fonts.gstatic.com/s/e/notoemoji/latest/1f64f/512.gif" alt="🙏" width="20" height="20"> Gratitude and Thanks

Thanks to [onedr0p](https://github.com/onedr0p) for the [onedr0p/cluster-template](https://github.com/onedr0p/cluster-template).

---
