#!/usr/bin/env bash
set -euo pipefail

# Some GNAT toolchains link libadalang_jni.so against GNU libiconv, whose
# iconv.h re-defines iconv_open() to libiconv_open(). glibc provides
# iconv_open() itself but not that symbol, so a host without GNU libiconv
# installed fails to load the library with an unresolved libiconv_open.
#
# This bundles whatever libiconv the JNI library actually needs into the same
# directory as libadalang_jni.so, which is already first on LD_LIBRARY_PATH /
# java.library.path (see pom.xml and sonar-scanner-linux.sh). That makes
# native-library loading independent of whether the host happens to have GNU
# libiconv installed system-wide.

if [[ "$(uname -s)" != "Linux" ]]; then
  # macOS ships /usr/lib/libiconv.2.dylib system-wide; nothing to bundle.
  exit 0
fi

native_path=${SONAR_ADA_NATIVE_PATH:-/opt/libadalang-jni-libs}
jni_lib="$native_path/libadalang_jni.so"

if [[ ! -f "$jni_lib" ]]; then
  echo "ensure-libiconv-linux.sh: $jni_lib not found, skipping libiconv bundling." >&2
  exit 0
fi

if ! command -v ldd >/dev/null; then
  echo "ensure-libiconv-linux.sh: ldd not available, skipping libiconv bundling." >&2
  exit 0
fi

iconv_line=$(ldd "$jni_lib" | grep -E 'libiconv\.so' || true)

if [[ -z "$iconv_line" ]]; then
  # Built against glibc's own iconv_open; no separate libiconv needed.
  exit 0
fi

soname=$(awk '{print $1}' <<< "$iconv_line")

if [[ -f "$native_path/$soname" ]]; then
  # Already bundled by a previous run.
  exit 0
fi

resolved=$(awk '{print $3}' <<< "$iconv_line")

if [[ "$resolved" == "not" || -z "$resolved" || ! -f "$resolved" ]]; then
  # ldd reported "=> not found"; look for an installed copy via ldconfig.
  resolved=$(ldconfig -p 2>/dev/null | grep "$soname" | awk '{print $NF}' | head -n1 || true)
fi

if [[ -z "$resolved" || ! -f "$resolved" ]]; then
  echo "ensure-libiconv-linux.sh: $jni_lib requires $soname but no copy was found on this host." >&2
  echo "Install GNU libiconv (e.g. the 'libiconv' or 'libiconv1' package) or copy $soname into $native_path manually." >&2
  exit 1
fi

cp "$resolved" "$native_path/$soname"
echo "ensure-libiconv-linux.sh: bundled $resolved as $native_path/$soname."
