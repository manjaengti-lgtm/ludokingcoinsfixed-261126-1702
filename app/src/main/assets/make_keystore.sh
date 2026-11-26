#!/usr/bin/env bash
# Creates app/signing.keystore (real PKCS12 binary) with the mandated DN.
# Never overwrites an existing keystore.
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
TARGET="${1:-$SCRIPT_DIR/../../../signing.keystore}"
ALIAS="${LK_KEY_ALIAS:-ludoking}"
STORE_PASS="${LK_STORE_PASS:-ludoking}"
KEY_PASS="${LK_KEY_PASS:-ludoking}"
if [ -f "$TARGET" ] && [ -s "$TARGET" ]; then
  echo "keystore already present: $TARGET"
  exit 0
fi

mkdir -p "$(dirname "$TARGET")"
keytool -genkeypair \
  -keystore "$TARGET" \
  -storetype PKCS12 \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 10950 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
  -dname "CN=Arin, OU=Arin, O=Arin, L=Arin, ST=Arin, C=IN"

file "$TARGET"
echo "wrote $TARGET (alias $ALIAS)"
