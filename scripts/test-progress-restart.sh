#!/usr/bin/env bash
set -euo pipefail
# Only an explicitly selected isolated instrumentation device; never the real-account AVDs.
serial="${1:?Pass the isolated test emulator serial}"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'An emulator serial is required' >&2; exit 2; }
case "$serial" in emulator-5560|emulator-5564) echo 'Real-account review devices are forbidden' >&2; exit 2;; esac
adb_bin="${ADB:-adb}"
log_dir="app/build/outputs/progress-restart/$serial"
mkdir -p "$log_dir"
for phase in seed verify; do
  "$adb_bin" -s "$serial" shell am force-stop app.reelstack.debug
  "$adb_bin" -s "$serial" shell am instrument -w \
    -e class app.reelstack.PlaybackProgressRestartTest \
    -e progressRestartPhase "$phase" \
    app.reelstack.debug.test/app.reelstack.SpoleTestRunner | tee "$log_dir/$phase.txt"
  grep -q 'OK (1 test)' "$log_dir/$phase.txt"
done
