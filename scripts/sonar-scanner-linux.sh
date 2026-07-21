#!/usr/bin/env bash
set -euo pipefail

native_path=${SONAR_ADA_NATIVE_PATH:-/opt/libadalang-jni-libs}
scanner_bin=${SONAR_SCANNER_BIN:-sonar-scanner}

if [[ -n "${JAVA_HOME:-}" ]]; then
  java_home=$JAVA_HOME
else
  java_bin=$(command -v java || true)
  if [[ -z "$java_bin" ]]; then
    echo "Unable to locate Java; set JAVA_HOME before running this script." >&2
    exit 1
  fi
  java_bin=$(readlink -f "$java_bin")
  java_home=$(dirname "$(dirname "$java_bin")")
fi

libjsig="$java_home/lib/libjsig.so"
if [[ ! -f "$libjsig" ]]; then
  echo "Unable to locate HotSpot signal-chaining library: $libjsig" >&2
  exit 1
fi
if [[ ! -d "$native_path" ]]; then
  echo "Unable to locate Ada native library directory: $native_path" >&2
  exit 1
fi

export LD_PRELOAD="$libjsig${LD_PRELOAD:+:$LD_PRELOAD}"
export LD_LIBRARY_PATH="$native_path${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export SONAR_SCANNER_OPTS="-Djava.library.path=$native_path${SONAR_SCANNER_OPTS:+ $SONAR_SCANNER_OPTS}"

exec "$scanner_bin" "$@"
