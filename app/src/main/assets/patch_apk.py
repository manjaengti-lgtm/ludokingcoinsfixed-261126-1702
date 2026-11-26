#!/usr/bin/env python3
"""Build LudoKing-coins-FIXED.apk from the stock base + its three splits.

Steps, in this exact order, every time:

  1. read stock/inputs.json (fixed merge order, sha256 per input)
  2. merge base.apk, split_config.arm64_v8a.apk, split_config.xxhdpi.apk,
     split_config.en.apk -- first occurrence of a name wins, so base order
     is preserved and later splits only add what is missing
  3. drop ONLY the source META-INF signature files (MANIFEST.MF, *.SF, *.RSA,
     *.DSA, *.EC); everything else, including AndroidManifest.xml, is copied
     byte for byte
  4. overwrite exactly three spots inside lib/arm64-v8a/libil2cpp.so, keeping
     that entry Defl:N and at its original position in the archive

      0x21B9E00  GetChips()        movz x0,#0xc9ff / movk x0,#0x3b9a,lsl #16 / movk x0,#0
      0x22DDC0C  second GetChips() same three instructions
      0x20CA650  IsEnoughCoinsAvailableToPlay()  mov w0,#1 ; ret

     SetChips (0x21B9E6C) is left stock -- asserted, not assumed.
  5. assert 2131 entries and that AndroidManifest.xml hashes like the stock base

RVA -> file offset goes through the PT_LOAD program header that owns the RVA, so
the ELF64 stays valid even with stripped section headers.

Usage:
    python3 patch_apk.py stock/ raw.apk
"""
import hashlib
import json
import pathlib
import struct
import sys
import zipfile

LIB = "lib/arm64-v8a/libil2cpp.so"
MERGE_ORDER = (
    "base.apk",
    "split_config.arm64_v8a.apk",
    "split_config.xxhdpi.apk",
    "split_config.en.apk",
)
PATCHES = (
    (0x21B9E00, "e03f99d24073a7f20000c0f2"),
    (0x22DDC0C, "e03f99d24073a7f20000c0f2"),
    (0x20CA650, "20008052c0035fd6"),
)
SET_CHIPS_RVA = 0x21B9E6C
SET_CHIPS_LEN = 12
EXPECTED_ENTRIES = 2131
PT_LOAD = 1
EM_AARCH64 = 0xB7


def fail(message):
    raise SystemExit("FAIL " + message)


def sha256_file(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def is_signature(name):
    if not name.startswith("META-INF/"):
        return False
    tail = name[len("META-INF/"):]
    if "/" in tail or not tail:
        return False
    return tail == "MANIFEST.MF" or tail.endswith((".SF", ".RSA", ".DSA", ".EC"))


def load_inputs(folder):
    manifest = pathlib.Path(folder) / "inputs.json"
    if not manifest.is_file():
        fail("missing %s" % manifest)
    data = json.loads(manifest.read_text())
    listed = [item["name"] for item in data["inputs"]]
    if tuple(listed) != MERGE_ORDER:
        fail("inputs.json order %s != fixed order %s" % (listed, list(MERGE_ORDER)))
    paths = []
    for item in data["inputs"]:
        path = pathlib.Path(folder) / item["name"]
        if not path.is_file():
            fail("missing stock input " + str(path))
        actual = sha256_file(path)
        expected = (item.get("sha256") or "").strip().lower()
        if expected and expected != actual:
            fail("sha256 mismatch for %s: %s != %s" % (path.name, actual, expected))
        if not expected:
            print("note: %s has no pinned sha256, is %s -- pin it" % (path.name, actual))
        else:
            print("ok: %s matches its pinned sha256" % path.name)
        paths.append(path)
    return paths


def rva_to_offset(data, rva):
    e_phoff, = struct.unpack_from("<Q", data, 0x20)
    e_phentsize, = struct.unpack_from("<H", data, 0x36)
    e_phnum, = struct.unpack_from("<H", data, 0x38)
    for index in range(e_phnum):
        base = e_phoff + index * e_phentsize
        p_type, = struct.unpack_from("<I", data, base)
        if p_type != PT_LOAD:
            continue
        p_offset, = struct.unpack_from("<Q", data, base + 0x08)
        p_vaddr, = struct.unpack_from("<Q", data, base + 0x10)
        p_filesz, = struct.unpack_from("<Q", data, base + 0x20)
        if p_vaddr <= rva < p_vaddr + p_filesz:
            return p_offset + (rva - p_vaddr)
    fail("RVA %#x is not inside any PT_LOAD segment" % rva)


def patch_library(blob):
    if blob[:4] != b"\x7fELF" or blob[4] != 2:
        fail("libil2cpp.so is not an ELF64 file")
    machine, = struct.unpack_from("<H", blob, 0x12)
    if machine != EM_AARCH64:
        fail("libil2cpp.so is not aarch64 (e_machine=%#x)" % machine)
    untouched = bytes(blob[rva_to_offset(blob, SET_CHIPS_RVA):
                           rva_to_offset(blob, SET_CHIPS_RVA) + SET_CHIPS_LEN])
    for rva, hexbytes in PATCHES:
        offset = rva_to_offset(blob, rva)
        payload = bytes.fromhex(hexbytes)
        before = bytes(blob[offset:offset + len(payload)])
        if before == payload:
            fail("RVA %#x is already patched -- wrong stock file?" % rva)
        blob[offset:offset + len(payload)] = payload
        print("patched %#x -> file offset %#x (was %s)" % (rva, offset, before.hex()))
    after = bytes(blob[rva_to_offset(blob, SET_CHIPS_RVA):
                       rva_to_offset(blob, SET_CHIPS_RVA) + SET_CHIPS_LEN])
    if after != untouched:
        fail("SetChips at %#x changed" % SET_CHIPS_RVA)
    print("ok: SetChips %#x untouched (%s)" % (SET_CHIPS_RVA, untouched.hex()))


def merge(paths):
    seen = set()
    merged = []
    for path in paths:
        with zipfile.ZipFile(path) as source:
            for info in source.infolist():
                name = info.filename
                if is_signature(name):
                    continue
                if name in seen:
                    continue
                seen.add(name)
                merged.append((info, source.read(name) if not info.is_dir() else b""))
    return merged


def main(folder, target):
    paths = load_inputs(folder)
    merged = merge(paths)

    stock_manifest = b""
    with zipfile.ZipFile(paths[0]) as base:
        stock_manifest = base.read("AndroidManifest.xml")
    stock_manifest_sha = hashlib.sha256(stock_manifest).hexdigest()

    lib_index = None
    for index, (info, data) in enumerate(merged):
        if info.filename == LIB:
            lib_index = index
            break
    if lib_index is None:
        fail("merged archive has no " + LIB)

    blob = bytearray(merged[lib_index][1])
    patch_library(blob)
    lib_info = merged[lib_index][0]
    merged[lib_index] = (lib_info, bytes(blob))

    with zipfile.ZipFile(target, "w", allowZip64=True) as out:
        for info, data in merged:
            clone = zipfile.ZipInfo(info.filename, date_time=info.date_time)
            clone.compress_type = info.compress_type
            clone.external_attr = info.external_attr
            clone.internal_attr = info.internal_attr
            clone.create_system = info.create_system
            out.writestr(clone, data)

    with zipfile.ZipFile(target) as check:
        names = check.namelist()
        if len(names) != EXPECTED_ENTRIES:
            fail("entry count %d, want %d" % (len(names), EXPECTED_ENTRIES))
        built_manifest_sha = hashlib.sha256(check.read("AndroidManifest.xml")).hexdigest()
        if built_manifest_sha != stock_manifest_sha:
            fail("AndroidManifest.xml differs from stock")
        lib_entry = check.getinfo(LIB)
        if lib_entry.compress_type != zipfile.ZIP_DEFLATED:
            fail("libil2cpp.so is no longer Defl:N")
        if lib_entry.compress_type != lib_info.compress_type:
            fail("libil2cpp.so compression method changed")
        leftovers = [n for n in names if is_signature(n)]
        if leftovers:
            fail("META-INF signature leftovers: %s" % leftovers)
        ordered = names.index(LIB)

    print("ok: %d entries (want %d)" % (EXPECTED_ENTRIES, EXPECTED_ENTRIES))
    print("ok: AndroidManifest.xml sha256 %s == stock" % stock_manifest_sha)
    print("ok: libil2cpp.so still Defl:N at entry %d" % ordered)
    print("wrote", target)
    print("next: zipalign -p -f 4, apksigner v2+v3 (v1 off), then verify.sh")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        fail("usage: patch_apk.py <stock-dir> <out.apk>")
    main(sys.argv[1], sys.argv[2])
