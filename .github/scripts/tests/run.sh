#!/bin/bash
# Runs every *.test.sh next to this file. Exits non-zero if any test file fails; prints "0 tests" and exits 0 when there is none.
dir=$(cd "$(dirname "$0")" && pwd)
found=0
failed=0
for t in "$dir"/*.test.sh; do
  [ -e "$t" ] || continue
  found=$((found + 1))
  echo "== $(basename "$t")"
  if ! bash "$t"; then
    failed=$((failed + 1))
  fi
done
if [ "$found" -eq 0 ]; then
  echo "0 tests"
  exit 0
fi
echo "$found test file(s), $failed failed"
[ "$failed" -eq 0 ]
