{
  description = "zig-tracy development environment";

  inputs = {
    zig-overlay.url = "github:mitchellh/zig-overlay";
    zls-flake.url = "github:zigtools/zls";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, zig-overlay, flake-utils, zls-flake }:
  flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        zls = zls-flake.packages.${system}.zls;
        zig = zig-overlay.packages.${system}.master-2025-01-16;
      in {
        devShells.default = pkgs.mkShell {
          nativeBuildInputs = [
            zig
            zls
          ];
        };
      });
}

