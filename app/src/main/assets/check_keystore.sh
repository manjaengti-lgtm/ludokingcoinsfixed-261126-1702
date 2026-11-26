#!/usr/bin/env bash
# The keystore must be binary and must carry DN CN/OU/O/L/ST=Arin, C=IN.
set -euo pipefail

KS="${1:?usage: check_keystore.sh <keystore>}"
ALIAS="${LK_KEY_ALIAS:-ludoking}"
STOREPASS="${LK_STORE_PASS:-ludoking}"
[ -f "$KS" ] || { echo "FAIL no such keystore: $KS"; exit 1; }
[ -s "$KS" ] || { echo "FAIL keystore is empty: $KS"; exit 1; }

magic=$(head -c 4 "$KS" | od -An -tx1 | tr -d ' \n')
case "$magic" in
  3082*|feedfeed|cececece) ;;
  *) echo "FAIL $KS is not a binary keystore (magic $magic)"; exit 1 ;;
esac

report=$(keytool -list -v -keystore "$KS" -storepass "$STOREPASS" -alias "$ALIAS")
for field in "CN=Arin" "OU=Arin" "O=Arin" "L=Arin" "ST=Arin" "C=IN"; do
  echo "$report" | grep -q "$field" || { echo "FAIL owner field missing: $field"; exit 1; }
done
echo "$report" | grep -q 'PrivateKeyEntry' || { echo "FAIL alias $ALIAS has no private key"; exit 1; }
echo "OK   keystore is binary PKCS12 with DN CN/OU/O/L/ST=Arin, C=IN and a private key"
