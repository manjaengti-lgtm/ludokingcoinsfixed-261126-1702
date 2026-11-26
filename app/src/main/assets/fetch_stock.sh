#!/usr/bin/env bash
# Put stock/base.apk + the three split_config files into ./stock (or $1).
# Order of attempts: already present -> local .apkm/.xapk bundle -> per-file urls -> asset-pack bundle.
# Nothing is patched here; patch_apk.py does that. sha256 is printed so it can be pinned.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
INPUTS="$SCRIPT_DIR/stock/inputs.json"
DIR="${1:-$PWD/stock}"
PIN=0
for arg in "$@"; do [ "$arg" = "--pin" ] && PIN=1; done

mkdir -p "$DIR"

python3 - "$INPUTS" "$DIR" "$PIN" <<'PY'
import hashlib, json, os, pathlib, shutil, sys, urllib.request, zipfile

inputs_path, out_dir, pin = sys.argv[1], pathlib.Path(sys.argv[2]), sys.argv[3] == "1"
spec = json.loads(pathlib.Path(inputs_path).read_text())
required = [i["name"] for i in spec["inputs"]]
optional = [i["name"] for i in spec.get("optional_inputs", [])]
wanted = required + optional

UA = {"User-Agent": "Mozilla/5.0 (Linux; Android 14) ludoking-coins-fixed"}


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def good(name):
    path = out_dir / name
    if not path.is_file() or path.stat().st_size < 1024:
        return False
    entry = next((i for i in spec["inputs"] + spec.get("optional_inputs", []) if i["name"] == name), {})
    pinned = (entry.get("sha256") or "").strip().lower()
    return not pinned or pinned == sha256(path)


def extract_any(name):
    for bundle in sorted(out_dir.glob("*.apkm")) + sorted(out_dir.glob("*.xapk")) + sorted(out_dir.glob("*.zip")):
        try:
            with zipfile.ZipFile(bundle) as z:
                for member in z.namelist():
                    if pathlib.PurePosixPath(member).name == name:
                        data = z.read(member)
                        if len(data) >= 1024:
                            (out_dir / name).write_bytes(data)
                            print("ok:   %s taken out of %s" % (name, bundle.name))
                            return True
        except zipfile.BadZipFile:
            continue
    return False


def download(url, name):
    target = out_dir / (name + ".part")
    request = urllib.request.Request(url, headers=UA)
    try:
        with urllib.request.urlopen(request, timeout=180) as response, open(target, "wb") as fh:
            shutil.copyfileobj(response, fh)
    except Exception as error:  # noqa: BLE001
        target.unlink(missing_ok=True)
        print("miss: %s <- %s (%s)" % (name, url, error))
        return False
    try:
        with zipfile.ZipFile(target) as z:
            if "AndroidManifest.xml" not in z.namelist():
                raise zipfile.BadZipFile("no AndroidManifest.xml")
    except zipfile.BadZipFile as error:
        target.unlink(missing_ok=True)
        print("miss: %s is not an APK (%s)" % (name, error))
        return False
    target.replace(out_dir / name)
    print("ok:   %s downloaded" % name)
    return True


missing = []
for name in wanted:
    if good(name):
        print("ok:   %s already present" % name)
        continue
    if extract_any(name):
        continue
    entry = next((i for i in spec["inputs"] + spec.get("optional_inputs", []) if i["name"] == name), {})
    if not any(download(url, name) for url in entry.get("urls", [])):
        (out_dir / name).unlink(missing_ok=True)
        if name in required:
            missing.append(name)
        else:
            print("note: optional %s not available, continuing without it" % name)

if missing:
    raise SystemExit(
        "FAIL missing stock input(s): %s\n"
        "Drop an .apkm/.xapk named bundle.apkm into %s, or put the files there by hand, then re-run."
        % (", ".join(missing), out_dir)
    )

for name in required:
    print("sha256 %s  %s" % (sha256(out_dir / name), name))

if pin:
    for entry in spec["inputs"] + spec.get("optional_inputs", []):
        path = out_dir / entry["name"]
        if path.is_file():
            entry["sha256"] = sha256(path)
    pathlib.Path(inputs_path).write_text(json.dumps(spec, indent=2) + "\n")
    print("pinned all sha256 values into", inputs_path)
PY

echo "stock ready in $DIR"
