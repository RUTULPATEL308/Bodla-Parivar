# Dependency audit — 2026-10-18

## Commands and results

| Command | Exit status | Result |
| --- | --- | --- |
| `npm audit --omit=dev` at repository root | 1 | 15 high and 7 moderate findings remain. |
| `admin/npm audit --omit=dev` | 0 | 0 production findings. |
| `./node_modules/.bin/expo.cmd install --check` | 0 | Expo dependency set is compatible. |

## Applied update

The root project was already using Expo SDK 58 and its transitive React Native 0.88.0. Expo was updated from `^58.0.11` to `~58.0.14`, the local Expo CLI's compatible patch version. `react-native` is not a direct root dependency, so it was not independently overridden.

## Remaining npm audit records

`npm audit` reports these high-severity package records: `expo`, `@expo/cli`, `@expo/code-signing-certificates`, `@expo/metro`, `@expo/metro-config`, `@react-native/community-cli-plugin`, `@react-native/virtualized-lists`, `braces`, `metro`, `metro-config`, `metro-file-map`, `metro-transform-worker`, `micromatch`, `node-forge`, and `react-native`.

It reports these moderate-severity package records: `@expo/config`, `@expo/config-plugins`, `@expo/inline-modules`, `@expo/local-build-cache-provider`, `@expo/prebuild-config`, `uuid`, and `xcode`.

The root paths are `expo@58.0.14` -> Expo CLI/config/Metro packages -> Metro/micromatch/braces and `expo@58.0.14` -> React Native 0.88.0 -> React Native CLI/virtualized lists -> Metro. `@expo/cli` also brings `@expo/code-signing-certificates` -> `node-forge`; config plugins bring `xcode` -> `uuid`.

Named advisories in the audit are `GHSA-vfj7-8cjw-p6xm` for `braces`, `GHSA-86w9-cpqp-85rv` for `node-forge`, and `GHSA-w5hq-g745-h8pq` for `uuid`. The audit does not provide a compatible fixed version for the remaining Expo 58 records. Its only proposed aggregate fix is `expo@44.0.6`, labeled semver-major even though it is an older SDK and is incompatible with the installed Expo 58 project. It was deliberately not applied.

This is an unresolved dependency-risk signal. Recheck after the advisory database or Expo publishes a compatible remediation; do not add arbitrary overrides for Metro, React Native, or Expo internals.
