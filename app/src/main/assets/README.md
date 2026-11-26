# LudoKing-coins-FIXED

Stock **10.5.0.406** (`com.ludo.king`, versionCode 10500406), arm64-v8a, rebuilt as one APK.

## What actually changes

Exactly three spots inside `lib/arm64-v8a/libil2cpp.so`:

| RVA | Function | Written |
| --- | --- | --- |
| `0x21B9E00` | `GetChips()` | `movz x0,#0xc9ff` / `movk x0,#0x3b9a,lsl #16` / `movk x0,#0` |
| `0x22DDC0C` | second `GetChips()` | same three instructions |
| `0x20CA650` | `IsEnoughCoinsAvailableToPlay()` | `mov w0,#1` ; `ret` |

`SetChips()` at `0x21B9E6C` is **untouched** — the getters lie on top of it, which is why
buying things still debits normally. Nothing else in the archive may differ; `verify.sh`
diffs stock against built and fails on a single stray byte.

## Pipeline (fixed order, no shortcuts)

```
stock/{base, split_config.arm64_v8a, split_config.xxhdpi, split_config.en}.apk
  -> patch_apk.py  merge in that order, drop ONLY META-INF signatures,
                   replace ONLY libil2cpp.so, keep it Defl:N and in place,
                   assert 2131 entries + manifest sha == stock
  -> zipalign -p -f 4
  -> apksigner sign --v1 false --v2 true --v3 true
  -> verify.sh     hard-fail on every rule
```

## Stock inputs

Not committed (third-party binaries, too big for git). `stock/inputs.json` pins the order
and a sha256 per file; `patch_apk.py` refuses any file that does not match. Pull them once
with `adb shell pm path com.ludo.king` + `adb pull`, or let CI fetch the release they are
hosted on, then fill the `sha256` fields.

## Signing key

`check_keystore.sh` runs before signing and demands `CN/OU/O/L/ST=Arin, C=IN`, the alias
`ludoking` and a working password, so a re-created key can never drift from the pinned DN.

## Verify by hand

```
bash app/src/main/assets/check_keystore.sh signing.keystore
python3 app/src/main/assets/patch_apk.py stock/ raw.apk
zipalign -p -f 4 raw.apk aligned.apk
apksigner sign --ks signing.keystore --ks-key-alias ludoking \
  --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled true \
  --out LudoKing-coins-FIXED.apk aligned.apk
bash app/src/main/assets/verify.sh LudoKing-coins-FIXED.apk stock/base.apk
