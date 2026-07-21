#!/usr/bin/env bash
set -euo pipefail

if (( $# != 0 )); then
  echo "Usage: $0" >&2
  echo "Dependency paths are pinned in this script and cannot be overridden." >&2
  exit 2
fi

project_dir=$(cd "$(dirname "$0")/.." && pwd)
libadalang_dir=/opt/libadalang_26.0.0_75276b8d
langkit_dir=/opt/langkit_support_26.0.0_1745168f
local_repo="$project_dir/.m2/repository"
staging_dir=$(mktemp -d "${TMPDIR:-/tmp}/sonarada-libadalang.XXXXXX")
java_release=21
libadalang_version=26.0.0-75276b8d-java21
langkit_version=26.0.0-1745168f-java21

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

# Libadalang 26 generates its Java artifacts for Java 24. Retarget the staged
# copies so that every class packaged with the SonarQube plugin runs on Java 21.
for pom_file in \
  "$staging_dir/langkit-support/pom.xml" \
  "$staging_dir/libadalang/pom.xml"; do
  perl -0pi -e \
    "s{<release>\\s*[0-9]+\\s*</release>}{<release>$java_release</release>}g" \
    "$pom_file"

  if ! grep -q "<release>$java_release</release>" "$pom_file"; then
    echo "Unable to set Java release $java_release in $pom_file" >&2
    exit 1
  fi

done

LANGKIT_VERSION="$langkit_version" perl -0pi -e \
  's{<version>\s*0\.1\s*</version>}{<version>$ENV{LANGKIT_VERSION}</version>}g' \
  "$staging_dir/langkit-support/pom.xml"

LIBADALANG_VERSION="$libadalang_version" LANGKIT_VERSION="$langkit_version" perl -0pi -e \
  's{<version>\s*0\.1\s*</version>}{<version>$ENV{LIBADALANG_VERSION}</version>}; s{(<artifactId>langkit_support</artifactId>\s*<version>)\s*0\.1\s*(</version>)}{$1$ENV{LANGKIT_VERSION}$2}' \
  "$staging_dir/libadalang/pom.xml"

if ! grep -q "<version>$langkit_version</version>" "$staging_dir/langkit-support/pom.xml"; then
  echo "Unable to set Langkit version $langkit_version" >&2
  exit 1
fi

if ! grep -q "<version>$libadalang_version</version>" "$staging_dir/libadalang/pom.xml" || \
   ! grep -q "<version>$langkit_version</version>" "$staging_dir/libadalang/pom.xml"; then
  echo "Unable to set Libadalang dependency versions" >&2
  exit 1
fi

# StandardCharsets gained named UTF-32 constants after Java 21. Charset lookup
# provides the same standard encodings on Java 21 and later.
libadalang_java="$staging_dir/libadalang/src/main/java/com/adacore/libadalang/Libadalang.java"
if [[ -f "$libadalang_java" ]]; then
  perl -pi -e \
    's/StandardCharsets\.UTF_32BE/Charset.forName("UTF-32BE")/g; s/StandardCharsets\.UTF_32LE/Charset.forName("UTF-32LE")/g' \
    "$libadalang_java"
fi

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

echo "Installed Java $java_release bindings from:"
echo "  $libadalang_dir ($libadalang_version)"
echo "  $langkit_dir ($langkit_version)"
