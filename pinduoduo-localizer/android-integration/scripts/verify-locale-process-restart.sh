#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
APP_ID="com.replit.pinduoduo.localizer.demo"
TEST_ID="${APP_ID}.test"
RUNNER="${TEST_ID}/androidx.test.runner.AndroidJUnitRunner"

cd "$ANDROID_DIR"
./gradlew :demo-app:assembleDebug :demo-app:assembleDebugAndroidTest

adb install -r demo-app/build/outputs/apk/debug/demo-app-debug.apk
adb install -r demo-app/build/outputs/apk/androidTest/debug/demo-app-debug-androidTest.apk

adb shell am instrument -w -e phase write "$RUNNER"
adb shell am force-stop "$APP_ID"
adb shell am start -n "$APP_ID/.MainActivity"
adb shell am instrument -w -e phase verify "$RUNNER"

printf 'Russian locale survived the demo app process restart.\n'
