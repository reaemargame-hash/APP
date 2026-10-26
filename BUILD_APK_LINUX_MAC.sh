#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/android"
if ! command -v gradle >/dev/null 2>&1; then
  echo 'Gradle 8.9 not found. Use GitHub Actions workflow instead.' >&2
  exit 1
fi
if [ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
  echo 'ANDROID_HOME / ANDROID_SDK_ROOT not set. Install Android SDK 35.' >&2
  exit 1
fi
gradle --no-daemon :app:assembleDebug
printf 'APK: %s\n' "$(pwd)/app/build/outputs/apk/debug/app-debug.apk"
