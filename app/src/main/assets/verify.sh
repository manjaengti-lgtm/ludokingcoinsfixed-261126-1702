#!/usr/bin/env bash
# Every rule here exits non-zero. Nothing is a warning.
# usage: ./verify.sh LudoKing-coins-FIXED.apk stock/base.apk
set -euo pipefail

APK="${1:?usage: verify.sh <built.apk> <stock-base.apk>}"
STOCK="${2:?usage: verify.sh <built.apk> <stock-base.apk>}"
EXPECTED_ENTRIES=2131

fail() { echo "FAIL $*"; exit 1; }
ok() { echo "OK   $*"; }

[ -f "$APK" ] || fail "no such file: $APK"
[ -f "$STOCK" ] || fail "no such file: $STOCK"

entries=$(unzip -l "$APK" | tail -n 1 | awk '{print $2}')
[ "$entries" = "$EXPECTED_ENTRIES" ] || fail "entry count $entries, want $EXPECTED_ENTRIES"
ok "entry count $EXPECTED_ENTRIES"

stock_hash=$(unzip -p "$STOCK" AndroidManifest.xml | sha256sum | cut -d' ' -f1)
built_hash=$(unzip -p "$APK" AndroidManifest.xml | sha256sum | cut -d' ' -f1)
[ "$stock_hash" = "$built_hash" ] || fail "manifest differs from stock"
ok "manifest hash $built_hash"

if unzip -Z1 "$APK" | grep -qE '^META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|DSA|EC))$'; then
  unzip -Z1 "$APK" | grep -E '^META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|DSA|EC))$' | sed 's/^/     /'
  fail "META-INF signature leftovers"
fi
ok "no META-INF signature leftovers"

if unzip -p "$APK" AndroidManifest.xml | strings | grep -q 'extractNativeLibs="false"'; then
  fail "extractNativeLibs is false"
fi
unzip -p "$APK" AndroidManifest.xml | strings | grep -q 'extractNativeLibs' \
  || fail "extractNativeLibs attribute missing from manifest"
ok "extractNativeLibs=true"

if unzip -p "$APK" AndroidManifest.xml | strings | grep -qE 'isSplitRequired|requiredSplitTypes|splitTypes|split="'; then
  fail "split attributes still present"
fi
ok "no split attributes"

abis=$(unzip -Z1 "$APK" | grep '^lib/' | cut -d/ -f2 | sort -u)
[ "$abis" = "arm64-v8a" ] || fail "native libs present for: ${abis//$'\n'/, }"
ok "only arm64-v8a native libs"

zipalign -c -p -v 4 "$APK" > /dev/null || fail "zipalign -c -p 4"
ok "zipalign -c -p 4"

signature_report=$(apksigner verify --verbose "$APK") || fail "signature does not verify"
echo "$signature_report" | grep -q 'Verified using v2 scheme (APK Signature Scheme v2): true' \
  || fail "v2 signature missing"
echo "$signature_report" | grep -q 'Verified using v3 scheme (APK Signature Scheme v3): true' \
  || fail "v3 signature missing"
echo "$signature_report" | grep -q 'Verified using v1 scheme (JAR signing): false' \
  || fail "v1 signature unexpectedly present"
ok "signature verifies, v2+v3 on and v1 off"

unzip -p "$STOCK" lib/arm64-v8a/libil2cpp.so > /tmp/lk_stock.so
unzip -p "$APK" lib/arm64-v8a/libil2cpp.so > /tmp/lk_built.so
python3 - /tmp/lk_stock.so /tmp/lk_built.so <<'PY'
import struct, sys

stock = open(sys.argv[1], "rb").read()
built = open(sys.argv[2], "rb").read()

if len(stock) != len(built):
    raise SystemExit("FAIL libil2cpp.so size %d -> %d" % (len(stock), len(built)))

want = {
    0x21B9E00: "e03f99d24073a7f20000c0f2",
    0x22DDC0C: "e03f99d24073a7f20000c0f2",
    0x20CA650: "20008052c0035fd6",
}
set_chips_rva, set_chips_len = 0x21B9E6C, 12

for data, tag in ((stock, "stock"), (built, "built")):
    if data[:4] != b"\x7fELF" or data[4] != 2:
        raise SystemExit("FAIL %s libil2cpp.so is not ELF64" % tag)
    if struct.unpack_from("<H", data, 0x12)[0] != 0xB7:
        raise SystemExit("FAIL %s libil2cpp.so is not aarch64" % tag)

phoff, = struct.unpack_from("<Q", stock, 0x20)
phentsize, = struct.unpack_from("<H", stock, 0x36)
phnum, = struct.unpack_from("<H", stock, 0x38)

def offset(rva):
    for i in range(phnum):
        b = phoff + i * phentsize
        if struct.unpack_from("<I", stock, b)[0] != 1:
            continue
        p_off, = struct.unpack_from("<Q", stock, b + 0x08)
        p_va, = struct.unpack_from("<Q", stock, b + 0x10)
        p_sz, = struct.unpack_from("<Q", stock, b + 0x20)
        if p_va <= rva < p_va + p_sz:
            return p_off + (rva - p_va)
    raise SystemExit("FAIL RVA %#x is not mapped" % rva)

touched = set()
for rva, hexbytes in want.items():
    payload = bytes.fromhex(hexbytes)
    start = offset(rva)
    before = stock[start:start + len(payload)]
    if before == payload:
        raise SystemExit("FAIL %#x already patched in stock" % rva)
    after = built[start:start + len(payload)]
    if after != payload:
        raise SystemExit("FAIL bytes at %#x: %s" % (rva, after.hex()))
    touched.update(range(start, start + len(payload)))
    print("OK   %#x stock %s -> built %s" % (rva, before.hex(), after.hex()))

sc = offset(set_chips_rva)
sc_stock = stock[sc:sc + set_chips_len]
sc_built = built[sc:sc + set_chips_len]
if sc_stock != sc_built:
    raise SystemExit("FAIL SetChips %#x changed" % set_chips_rva)
touched.update(range(sc, sc + set_chips_len))
print("OK   SetChips %#x unchanged (%s)" % (set_chips_rva, sc_stock.hex()))

stray = [i for i in range(len(stock)) if stock[i] != built[i] and i not in touched]
if stray:
    raise SystemExit("FAIL %d stray byte(s), first at file offset %#x" % (len(stray), stray[0]))
print("OK   no bytes changed outside the three patch sites")
PY

echo "ALL CHECKS PASSED"
