"""Verify shipped ASS JNI ABI coverage, 64-bit 16 KB compatibility, and embedded notices."""
import argparse
import struct
import zipfile


def check(apk):
    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
        for abi in ("arm64-v8a", "armeabi-v7a", "x86", "x86_64"):
            for library in ("libass.so", "libasskt.so"):
                path = f"lib/{abi}/{library}"
                data = archive.read(path)
                assert data[:4] == b"\x7fELF" and data[5] == 1, path
                bits64 = data[4] == 2
                if not bits64:
                    continue
                offset = struct.unpack_from("<Q", data, 32)[0]
                size, count = struct.unpack_from("<HH", data, 54)
                alignments = [struct.unpack_from("<Q", data, at + 48)[0]
                              for at in range(offset, offset + size * count, size)
                              if struct.unpack_from("<I", data, at)[0] == 1]
                assert alignments and all(value >= 16384 for value in alignments), path
        for component in ("wrapper", "libass", "fribidi", "freetype", "fontconfig", "harfbuzz", "expat", "unibreak"):
            path = f"assets/licenses/libass/{component}.txt"
            assert path in names and len(archive.read(path)) > 200, path
    print("Native subtitles OK: four ABIs, 64-bit 16 KB alignment, eight license notices.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("apk")
    check(parser.parse_args().apk)
