#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd "$(dirname "$0")/.." && pwd)
libadalang_dir=${1:-${LIBADALANG_HOME:-/Users/mmartign/libadalang_26.0.0_75276b8d}}
langkit_dir=${2:-${LANGKIT_HOME:-/Users/mmartign/langkit_support_26.0.0_1745168f}}
local_repo="$project_dir/.m2/repository"
staging_dir=$(mktemp -d "${TMPDIR:-/tmp}/sonarada-libadalang.XXXXXX")

cleanup() {
  rm -rf "$staging_dir"
}
trap cleanup EXIT

if [[ ! -f "$libadalang_dir/java/pom.xml" ]]; then
  echo "Missing generated Libadalang Java API: $libadalang_dir/java/pom.xml" >&2
  exit 1
fi

if [[ ! -f "$langkit_dir/langkit/java_support/pom.xml" ]]; then
  echo "Missing Langkit Java support: $langkit_dir/langkit/java_support/pom.xml" >&2
  exit 1
fi

cp -R "$langkit_dir/langkit/java_support" "$staging_dir/langkit-support"
cp -R "$libadalang_dir/java" "$staging_dir/libadalang"

# The generated Makefile builds JNI libraries as part of the Maven package.
# Package the Java API independently; native libraries are supplied by the
# local Libadalang installation at runtime.
if [[ -f "$staging_dir/libadalang/Makefile" ]]; then
  mv "$staging_dir/libadalang/Makefile" "$staging_dir/libadalang/Makefile.disabled"
fi

mvn -f "$staging_dir/langkit-support/pom.xml" \
  -Dmaven.repo.local="$local_repo" \
  -DskipTests install

mvn -f "$staging_dir/libadalang/pom.xml" \
  -Dmaven.repo.local="$local_repo" \
  -DskipTests \
  -DskipShade=true \
  install

echo "Installed local Libadalang Java artifacts in $local_repo"
