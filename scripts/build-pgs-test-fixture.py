"""Build an original synthetic PGS bitmap in the existing synthetic test video."""
from pathlib import Path
import struct
import subprocess

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/androidTest/assets/player"
WORK = ROOT / "app/build/pgs-fixture"
WORK.mkdir(parents=True, exist_ok=True)


def segment(seconds, kind, data):
    return b"PG" + struct.pack(">IIBH", int(seconds * 90000), 0, kind, len(data)) + data


def composition(number, objects):
    return struct.pack(">HHBHBBBB", 320, 180, 0x10, number, 0x80 if number == 0 else 0, 0, 0, len(objects)) + b"".join(objects)


# A white 160 x 40 rectangle at (80, 120), with an explicit transparent palette entry.
# Generated here; no downloaded film, soundtrack, font or subtitle is included.
pixels = bytes([0, 0xC0, 160, 1, 0, 0]) * 40
bitmap = struct.pack(">HH", 160, 40) + pixels
object_data = struct.pack(">HBB", 0, 0, 0xC0) + len(bitmap).to_bytes(3, "big") + bitmap
object_ref = struct.pack(">HBBHH", 0, 0, 0, 80, 120)
def display(seconds, number):
    return (segment(seconds, 0x16, composition(number, [object_ref]))
            + segment(seconds, 0x17, struct.pack(">BBHHHH", 1, 0, 80, 120, 160, 40))
            + segment(seconds, 0x14, bytes([0, 0, 0, 16, 128, 128, 0, 1, 235, 128, 128, 255]))
            + segment(seconds, 0x15, object_data) + segment(seconds, 0x80, b""))


# A second cue after the seek target proves decoding continues after a seek. Matroska seeks
# to a video keyframe; a cue before that point is not guaranteed to be repeated by Media3.
data = display(0.5, 0) + display(9.5, 1)
data += segment(18, 0x16, composition(2, [])) + segment(18, 0x80, b"")
sup = WORK / "synthetic.sup"
sup.write_bytes(data)
(ASSETS / "synthetic.sup").write_bytes(data)
subprocess.run([
    "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
    "-i", str(ASSETS / "video.mp4"), "-i", str(sup),
    "-map", "0:v:0", "-map", "0:a:0", "-map", "1:s:0", "-c", "copy",
    "-metadata:s:s:0", "language=nor", str(ASSETS / "pgs.mkv"),
], check=True)
subprocess.run([
    "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
    "-i", str(ASSETS / "video.mp4"), "-i", str(ASSETS / "styled.ass"),
    "-map", "0:v:0", "-map", "0:a:0", "-map", "1:s:0", "-c", "copy",
    "-disposition:s:0", "default", str(ASSETS / "styled.mkv"),
], check=True)
subprocess.run([
    "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
    "-i", str(ASSETS / "pgs.mkv"), "-map", "0", "-c", "copy", "-c:s", "dvdsub",
    "-disposition:s:0", "default", str(ASSETS / "dvd.mkv"),
], check=True)
for name in ("dvd", "pgs"):
    subprocess.run([
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-copyts",
        "-i", str(ASSETS / f"{name}.mkv"), "-map", "0:s:0", "-c", "copy",
        "-f", "matroska", str(ASSETS / f"{name}.mks"),
    ], check=True)
