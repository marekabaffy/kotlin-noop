#!/usr/bin/env bash
# Checks a Maven repository folder for the files a release needs: every file signed, every jar of code with the license.
set -euo pipefail

repo=$1
version=$2
problems=0

problem() {
  echo "$1" >&2
  problems=$((problems + 1))
}

for artifact in kotlinnoop-annotations kotlinnoop-ksp; do
  base="$repo/com/marekabaffy/kotlinnoop/$artifact/$version/$artifact-$version"

  for suffix in .jar -sources.jar -javadoc.jar .pom .module; do
    file="$base$suffix"
    if [ ! -f "$file" ]; then
      problem "Missing $file"
      continue
    fi
    [ -f "$file.asc" ] || problem "Missing signature for $file"
    # The javadoc jar is an empty placeholder. The listing is read in full first, because with pipefail
    # a grep that stops at its first match can fail the pipeline.
    if [ "$suffix" = .jar ] || [ "$suffix" = -sources.jar ]; then
      listing=$(unzip -l "$file")
      [[ "$listing" == *META-INF/LICENSE* ]] || problem "Missing license in $file"
    fi
  done
done

if [ "$problems" -gt 0 ]; then
  echo "$problems problem(s) found" >&2
  exit 1
fi
echo "All release files for $version are present, signed and licensed"
