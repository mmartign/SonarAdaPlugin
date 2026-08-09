#!/usr/bin/env bash
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
native_path=${SONAR_ADA_NATIVE_PATH:-/opt/libadalang-jni-libs}
if [[ -n "${SONAR_SCANNER_BIN:-}" ]]; then
  scanner_bin=$SONAR_SCANNER_BIN
elif [[ -x /opt/sonar-scanner/bin/sonar-scanner ]]; then
  scanner_bin=/opt/sonar-scanner/bin/sonar-scanner
else
  scanner_bin=sonar-scanner
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  java_home=$JAVA_HOME
elif [[ -f /opt/java/lib/libjsig.so ]]; then
  java_home=/opt/java
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

SONAR_ADA_NATIVE_PATH="$native_path" "$script_dir/ensure-libiconv-linux.sh"

export LD_PRELOAD="$libjsig${LD_PRELOAD:+:$LD_PRELOAD}"
export LD_LIBRARY_PATH="$native_path${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export SONAR_SCANNER_OPTS="-XX:+UseSignalChaining -Djava.library.path=$native_path${SONAR_SCANNER_OPTS:+ $SONAR_SCANNER_OPTS}"

exec "$scanner_bin" "$@"
