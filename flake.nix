{
  description = "zig-tracy development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11";
    zig-overlay.url = "github:mitchellh/zig-overlay";
    zig-overlay.inputs.nixpkgs.follows = "nixpkgs";

    zls-flake.url = "github:zigtools/zls";
    zls-flake.inputs.nixpkgs.follows = "nixpkgs";

    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, zig-overlay, flake-utils, zls-flake }:
  flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        zls = zls-flake.packages.${system}.zls;
        zig = zig-overlay.packages.${system}.master-2025-01-12;
      in {
        devShells.default = pkgs.mkShell {
          nativeBuildInputs = [
            zig
            zls
            pkgs.lldb_16
          ];
        };
      });
}

