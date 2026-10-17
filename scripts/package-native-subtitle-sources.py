"""Archive the exact libass vendor tree, including every pinned recursive submodule.

Reads Git objects instead of copying a worktree: no credentials, .git directories,
build outputs or untracked files enter the archive. Long upstream filenames also work
on Windows. The caller supplies a clone whose submodule commits have been fetched.
"""
import argparse
import gzip
import hashlib
import io
import json
import subprocess
import tarfile
from pathlib import Path

ROOT_COMMIT = "504f64a325059ab5188c4c9d0eddc99c9356a23a"
ARCHIVE_ROOT = "spole-native-subtitles"
BUILDING = """# Native undertekstar: kjelde og ny lenking

Dette arkivet inneheld libass-android 0.5.1 med alle rekursive submodular på dei
commitane som hovudtreet peikar på. SOURCE-MANIFEST.json identifiserer kvar del.
Ingen Git-katalogar, signeringsnøklar eller Spole-kontoar er tekne med.

Bygg på Linux med JDK 17 eller nyare, Android SDK 36, NDK 28.1.13356709 og CMake
3.22.1. Set ANDROID_HOME til SDK-mappa. Frå libass-android/:

    bash gradlew :lib_ass:assembleRelease :lib_ass_kt:assembleRelease

Endra kjelde i lib_ass/src/main/cpp/libass-cmake/src/ eller JNI/Kotlin-wrapperen
etter behov. Bygg på nytt med same kommando. Kvar modul legg AAR-en under
build/outputs/aar/. Ingen Maven-publisering eller privat signeringsnøkkel trengst.

For å lenkje Spole med dei endra biblioteka: hent Spole-kjelda som høyrer til den
distribuerte APK-en, legg begge bygde AAR-filene i app/libs/, og byt dei to
io.github.peerless2012-avhengigheitene i app/build.gradle.kts med implementation(files(...)).
Begge AAR-ane må vere med: JNI-wrapperen og sjølve libass-biblioteket. Den eksisterande
Spole/Media3-integrasjonen skal framleis byggjast. Bygg assembleDebug for eigen
installasjon, eller signer assembleRelease med din eigen Android-nøkkel. Ein annan
signeringsnøkkel kan ikkje oppdatere ei offisiell Spole-installering direkte.

Køyr dei native undertekstprøvene og scripts/check-native-subtitles.py på den nye APK-en.
Dette kjeldearkivet dokumenterer innhaldet; det er ikkje eit krav om byteidentisk
resultat frå ulike NDK-/Java-/Gradle-miljø. Lisensane ligg i dei opphavlege kjeldetrea.
"""


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], check=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def package(checkout, output):
    manifest = []
    output.parent.mkdir(parents=True, exist_ok=True)
    # Exclusive creation preserves an earlier archive until the caller chooses a new path.
    with output.open("xb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", filename="", mtime=0) as zipped:
        with tarfile.open(fileobj=zipped, mode="w|", format=tarfile.PAX_FORMAT) as target:
            def add_bytes(name, data):
                entry = tarfile.TarInfo(f"{ARCHIVE_ROOT}/{name}")
                entry.size = len(data); entry.mode = 0o644; entry.mtime = 0
                target.addfile(entry, io.BytesIO(data))

            def add_repo(repo, commit, relative):
                # archive uses the pinned object, never a possibly dirty/partial checkout.
                source = io.BytesIO(git(repo, "archive", "--format=tar", commit))
                manifest.append({"path": relative, "commit": commit})
                with tarfile.open(fileobj=source, mode="r:") as upstream:
                    for member in upstream:
                        if member.name.startswith("/") or ".." in Path(member.name).parts:
                            raise ValueError("Unsafe upstream archive path")
                        data = upstream.extractfile(member) if member.isfile() else None
                        member.name = f"{ARCHIVE_ROOT}/{relative}/{member.name}"
                        member.uid = member.gid = 0; member.uname = member.gname = ""
                        member.mtime = 0; member.pax_headers = {}
                        target.addfile(member, data)
                for entry in git(repo, "ls-tree", "-r", "-z", commit).split(b"\0"):
                    if not entry:
                        continue
                    metadata, path = entry.split(b"\t", 1)
                    mode, kind, sha = metadata.decode().split()
                    if mode == "160000" and kind == "commit":
                        subpath = path.decode("utf-8")
                        add_repo(repo / subpath, sha, f"{relative}/{subpath}")

            add_repo(checkout, ROOT_COMMIT, "libass-android")
            add_bytes("BUILDING.md", BUILDING.encode("utf-8"))
            add_bytes("SOURCE-MANIFEST.json", (json.dumps(manifest, indent=2) + "\n").encode())
    with output.open("rb") as archive:
        digest = hashlib.file_digest(archive, "sha256").hexdigest()
    print(f"{digest}  {output.name}")
    print(f"{len(manifest)} pinned repositories, {output.stat().st_size} bytes")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("checkout", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    package(args.checkout.resolve(), args.output.resolve())
