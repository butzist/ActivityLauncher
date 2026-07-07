{
  description = "Activity Launcher";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    flake-root.url = "github:srid/flake-root";
    jailed-agents.url = "github:andersonjoseph/jailed-agents";
  };

  outputs = inputs @ {
    nixpkgs,
    flake-parts,
    jailed-agents,
    ...
  }:
    flake-parts.lib.mkFlake {inherit inputs;} {
      systems = ["x86_64-linux" "aarch64-darwin"];
      imports = [
        inputs.flake-root.flakeModule
      ];
      perSystem = {
        config,
        system,
        ...
      }: let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            android_sdk.accept_license = true;
            allowUnfree = true;
          };
        };

        jlib = jailed-agents.lib.${system};
        combinators = jlib.internals.jail.combinators;

        python = pkgs.python314.withPackages (ps: [
          ps.google-api-python-client
          ps.google-auth
          ps.google-auth-oauthlib
        ]);

        androidSdk = pkgs.androidenv.composeAndroidPackages {
          platformVersions = ["37"];
          buildToolsVersions = ["37.0.0"];
          cmakeVersions = [];
          includeEmulator = false;
          includeSystemImages = false;
          includeSources = false;
        };

        androidSdkPath = "${androidSdk.androidsdk}/libexec/android-sdk";

        androidPkgs = [
          pkgs.openjdk21_headless
          pkgs.android-tools
          androidSdk.androidsdk
          pkgs.coreutils
          pkgs.bash
          pkgs.gradle
          pkgs.findutils
          pkgs.which
        ];

        agentPkgs = androidPkgs ++ [python];
      in {
        devShells.default = pkgs.mkShell {
          name = "dev-shell";
          packages =
            agentPkgs
            ++ [
              (jlib.makeJailedOpencode {
                name = "opencode";
                extraPkgs = agentPkgs;
                extraReadwriteDirs = [
                  "~/.android"
                  "~/.gradle"
                  "~/.emulator_console_auth_token"
                ];
                baseJailOptions = with combinators;
                  jlib.commonJailOptions
                  ++ [
                    (readwrite (noescape "\"$FLAKE_ROOT\""))
                  ];
              })
            ];
          inputsFrom = [config.flake-root.devShell];
          shellHook = ''
            export ANDROID_SDK_ROOT="${androidSdkPath}"
            export ANDROID_HOME="$ANDROID_SDK_ROOT"
            export ANDROID_AVD_HOME="$HOME/.android/avd"
            export GRADLE_OPTS="-Dorg.gradle.daemon=false"

            gradlew() {
              ${pkgs.coreutils}/bin/env sh "$FLAKE_ROOT/gradlew" "$@"
            }

            # ADB server fix: ensure it talks to the outside emulator
            adb kill-server 2>/dev/null; adb start-server 2>/dev/null || true

            echo "Activity Launcher dev shell"
            echo "Run: gradlew app:assembleOssNoadsDebug"
            echo "Run: gradlew app:testOssNoadsDebugUnitTest"
            echo "Run: gradlew app:connectedCheck          # requires emulator"
            echo "Run: adb devices                         # list connected devices"
            echo "Run: python scripts/update-listing.py --help"
            echo ""
            echo "ADB: $(adb --version 2>&1 | head -1)"
          '';
        };

        packages.update-listing = pkgs.writeShellApplication {
          name = "update-listing";
          text = ''
            exec ${python}/bin/python ${./scripts/update-listing.py} "$@"
          '';
        };
      };
    };
}
