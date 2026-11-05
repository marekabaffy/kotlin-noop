#!/usr/bin/env bash
# Prints the CHANGELOG.md section for a version, and fails if it has none.
set -euo pipefail

version=$1

notes=$(awk -v heading="## $version" '
  $0 == heading { found = 1; next }
  found && /^## / { exit }
  found { print }
' CHANGELOG.md | sed -e '/./,$!d')

if [ -z "$notes" ]; then
  echo "CHANGELOG.md has no notes under '## $version'" >&2
  exit 1
fi

printf '%s\n' "$notes"
