#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "$0")"/.. && pwd)"
lp_file="$root_dir/local.properties"

echo "[setup] Configuring local.properties for Android SDK..."

# 1) Prefer env vars
if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
  sdk_path="$ANDROID_SDK_ROOT"
elif [[ -n "${ANDROID_HOME:-}" ]]; then
  sdk_path="$ANDROID_HOME"
else
  # 2) Try common defaults per OS
  uname_s="$(uname -s 2>/dev/null || echo unknown)"
  case "$uname_s" in
    Darwin)
      sdk_path="$HOME/Library/Android/sdk"
      ;;
    Linux)
      # Android Studio default
      if [[ -d "$HOME/Android/Sdk" ]]; then
        sdk_path="$HOME/Android/Sdk"
      else
        sdk_path="$HOME/Android/sdk"
      fi
      ;;
    *)
      # Windows via Git Bash/Cygwin/MSYS — best effort
      if [[ -n "${USERPROFILE:-}" ]]; then
        sdk_path="${USERPROFILE//\\/\/}/AppData/Local/Android/Sdk"
      else
        sdk_path=""
      fi
      ;;
  esac
fi

if [[ -z "${sdk_path:-}" ]]; then
  echo "[setup] Could not infer SDK path. Set ANDROID_SDK_ROOT or ANDROID_HOME, or edit local.properties manually." >&2
  exit 1
fi

if [[ ! -d "$sdk_path" ]]; then
  echo "[setup] SDK directory not found: $sdk_path" >&2
  echo "[setup] Please install the Android SDK via Android Studio or sdkmanager, then rerun this script." >&2
  exit 2
fi

# Basic sanity check
if [[ ! -d "$sdk_path/platforms" ]]; then
  echo "[setup] SDK found but no platforms installed at: $sdk_path" >&2
  echo "[setup] Open Android Studio > SDK Manager and install required platforms/build-tools." >&2
fi

printf "sdk.dir=%s\n" "$sdk_path" > "$lp_file"
echo "[setup] Wrote $lp_file with sdk.dir=$sdk_path"

echo "[setup] Done. You can now run './gradlew assembleDebug'"

