"""Measure a named device's existing install without clearing accounts or application data.

am start -W measures first display, not complete network loading. gfxinfo measures Android
UI frames, not decoded video/SurfaceView frames. Emulator results are labelled explicitly.
"""
import argparse
import json
import re
import statistics
import subprocess
import time
from datetime import datetime, timezone
from pathlib import Path


def capture(adb, serial, *args):
    return subprocess.run([adb, "-s", serial, *args], check=True, capture_output=True,
                          text=True, encoding="utf-8", errors="replace", timeout=60).stdout


def run(args):
    def shell(*parts):
        return capture(args.adb, args.serial, "shell", *parts)

    if not re.fullmatch(r"[A-Za-z0-9_.]+", args.package):
        raise ValueError("Invalid Android package name")
    component = shell("cmd", "package", "resolve-activity", "--brief", args.package).strip().splitlines()[-1]
    if not re.fullmatch(re.escape(args.package) + r"/[A-Za-z0-9_.$]+", component):
        raise RuntimeError("The installed app has no launchable activity")
    package = shell("dumpsys", "package", args.package)
    def field(pattern):
        match = re.search(pattern, package)
        return match.group(1) if match else None

    report = {
        "measuredAtUtc": datetime.now(timezone.utc).isoformat(),
        "device": {"model": shell("getprop", "ro.product.model").strip(),
                   "api": shell("getprop", "ro.build.version.sdk").strip(),
                   "emulator": shell("getprop", "ro.kernel.qemu").strip() == "1"},
        "app": {"package": args.package, "versionName": field(r"versionName=(\S+)"),
                "versionCode": field(r"versionCode=(\d+)"),
                "debuggable": bool(re.search(r"flags=\[[^\]]*\bDEBUGGABLE\b", package))},
        "coldStartMs": [],
        "settleSeconds": args.settle_seconds,
        "notes": ["First display, not fully loaded library", "Existing accounts and data preserved",
                  "gfxinfo excludes decoded video and separate SurfaceView frames",
                  "Network, cached artwork, host load and emulator GPU affect these results"],
    }
    for _ in range(args.runs):
        shell("am", "force-stop", args.package)
        launched = shell("am", "start", "-W", "-n", component)
        measured = re.search(r"^TotalTime:\s*(\d+)", launched, re.MULTILINE)
        if "Status: ok" not in launched or not measured:
            raise RuntimeError("Android did not report a successful timed cold start")
        report["coldStartMs"].append(int(measured.group(1)))
        time.sleep(args.settle_seconds)
    report["coldStartMedianMs"] = statistics.median(report["coldStartMs"])
    memory = shell("dumpsys", "meminfo", args.package)
    pss = re.search(r"TOTAL PSS:\s*(\d+)", memory)
    if not pss:
        pss = re.search(r"^\s*TOTAL\s+(\d+)\s", memory, re.MULTILINE)
    report["settledPssKiB"] = int(pss.group(1)) if pss else None
    if args.dpad:
        shell("dumpsys", "gfxinfo", args.package, "reset")
        # Focus-only navigation: no select, playback, account switch or destructive action.
        keys = ["20"] * 2 + ["22"] * 12 + ["21"] * 12 + ["19"] * 2
        for key in keys:
            shell("input", "keyevent", key)
            time.sleep(0.25)
        frames = shell("dumpsys", "gfxinfo", args.package)
        frame_metrics = {}
        for name, pattern in {
            "total": r"Total frames rendered:\s*(\d+)",
            "janky": r"Janky frames:\s*(\d+)",
            "p50Ms": r"50th percentile:\s*(\d+)ms",
            "p90Ms": r"90th percentile:\s*(\d+)ms",
            "p95Ms": r"95th percentile:\s*(\d+)ms",
            "p99Ms": r"99th percentile:\s*(\d+)ms",
        }.items():
            match = re.search(pattern, frames)
            frame_metrics[name] = int(match.group(1)) if match else None
        report["focusNavigationFrames"] = frame_metrics
        report["focusNavigationKeys"] = len(keys)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", default="app.reelstack")
    parser.add_argument("--runs", type=int, choices=range(1, 11), default=5)
    parser.add_argument("--settle-seconds", type=float, default=8)
    parser.add_argument("--dpad", action="store_true")
    parser.add_argument("--output", type=Path, required=True)
    options = parser.parse_args()
    if not 1 <= options.settle_seconds <= 60:
        parser.error("--settle-seconds must be between 1 and 60")
    run(options)
