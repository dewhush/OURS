#!/usr/bin/env bash
# Stamp cache-busting ?v=<hash> onto local CSS/JS references inside web/*.html.
#
# The version token is derived from the sha256 of the referenced file itself, so it
# can never go stale and can never be circular (no html references itself).
# Idempotent: re-running replaces any existing ?v= token.
#
# Run this before pushing web/ to the VPS:
#   bash deploy/stamp-web-assets.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEB_DIR="${ROOT_DIR}/web"

cd "$WEB_DIR"

shopt -s nullglob
stamped=0

for html in *.html; do
  # Local (no scheme, no protocol-relative) .js/.css targets referenced by this file.
  refs="$(grep -oE '(href|src)="[^"]+\.(js|css)(\?[^"]*)?"' "$html" \
    | sed -E 's/^(href|src)="//; s/"$//' \
    | grep -vE '^([a-z]+:)?//' \
    | sed -E 's/\?.*$//' \
    | sort -u || true)"

  [ -z "$refs" ] && continue

  before="$(sha256sum "$html" | cut -d' ' -f1)"

  while IFS= read -r path; do
    [ -z "$path" ] && continue
    file="${path#/}"
    [ -f "$file" ] || continue
    vsn="$(sha256sum "$file" | cut -c1-12)"
    # Regex-escape the path for sed ERE (only . and / are special here).
    esc="$(printf '%s' "$path" | sed -E 's/[./]/\\&/g')"
    sed -i -E "s@(href|src)=\"${esc}(\\?v=[0-9a-f]+)?\"@\1=\"${path}?v=${vsn}\"@g" "$html"
  done <<< "$refs"

  after="$(sha256sum "$html" | cut -d' ' -f1)"
  if [ "$before" != "$after" ]; then
    echo "stamped ${html}"
    stamped=$((stamped + 1))
  fi
done

echo "stamp-web-assets: ${stamped} html file(s) updated."
