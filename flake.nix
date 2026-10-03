{
  description = "Development tools for home-ops";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs =
    { nixpkgs, ... }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forAllSystems (
        system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
        in
        {
          default = pkgs.mkShell {
            packages = with pkgs; [
              age
              bashInteractive
              cilium-cli
              cloudflared
              fluxcd
              gh
              git
              go-task
              helmfile
              jq
              kubeconform
              kubectl
              kubernetes-helm
              kustomize
              sops
              talhelper
              talosctl
              which
              yq-go
            ];

            shellHook = ''
              repo_root="$(git rev-parse --show-toplevel)"
              export KUBECONFIG="$repo_root/kubeconfig"
              export SOPS_AGE_KEY_FILE="$repo_root/age.key"
              export TALOSCONFIG="$repo_root/talos/clusterconfig/talosconfig"
              unset repo_root
            '';
          };
        }
      );

      formatter = forAllSystems (system: nixpkgs.legacyPackages.${system}.nixfmt);
    };
}
