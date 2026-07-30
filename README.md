# SonarQube Ada Plugin

This repository contains a SonarQube Server plugin that adds static analysis support for Ada source files. The native analysis engine is based on libadalang, with a built-in rule set inspired by the popular AdaControl tool.

## Features

- Ada language registration for `.adb`, `.ads`, and `.ada` files.
- Native Ada analysis with libadalang.
- Built-in Ada quality profile.
- Basic source metrics: lines, non-comment lines, comment lines, functions, and cyclomatic complexity.
- Syntax highlighting for comments, strings, Ada keywords, constants, and pragmas.
- CPD token generation for duplicate-code detection.
- AdaLang_Analyzer report parsing for CSV/CSVX findings produced by Spazio IT's external Ada analyzer.
- Optional AdaControl integration:
  - Run an installed `adactl` executable during analysis.
  - Import pre-generated AdaControl reports.
  - Publish AdaControl findings as Sonar external issues.
- GNATtest result import for native text, AUnit JUnit XML, and AUnit XML reports.
- A curated set of eleven built-in checks inspired by common Ada best practices and AdaControl rules. For a more comprehensive analysis, the AdaControl integration is recommended. The built-in rules include:
  - `ADA001`: Lines should not be too long.
  - `ADA002`: Tab characters should not be used.
  - `ADA003`: Trailing whitespace should not be used.
  - `ADA004`: TODO and FIXME comments should be resolved.
  - `ADA005`: `goto` statements should not be used.
  - `ADA006`: `pragma Suppress` should not be used.
  - `ADA007`: `when others => null` should not swallow exceptions.
  - `ADA008`: Package `use` clauses should be avoided.
  - `ADA009`: Ada files should not be too long.
  - `ADA010`: Ada files should not be too complex.
  - `ADA011`: The `'Address'` attribute should not be used.

## Build

Building the plugin requires JDK 21 or newer and produces Java 21 bytecode.
Verify that Maven uses the expected JDK with `mvn --version` before building.

Install the generated Java API from the local Libadalang and Langkit source
trees into the project-local Maven repository:

```bash
./scripts/install-local-libadalang.sh
```

The installation script retargets the generated Langkit and Libadalang Java
sources to Java 21 before installing them. This includes a compatibility change
for the UTF-32 charset constants that are only available after Java 21. The
upstream source trees are not modified. The script is intentionally pinned to
`/opt/libadalang_26.0.0_75276b8d` and
`/opt/langkit_support_26.0.0_1745168f`; it does not accept alternate
paths or environment-variable overrides. The retargeted artifacts have
source-specific versions, so Maven cannot confuse them with incompatible or
older AdaCore artifacts. Maven is configured to use this project's
`.m2/repository` as its local cache and does not fall back to `~/.m2`.

Libadalang's Java API requires the native `adalang_jni` library. It and its
native dependencies must be built by the matching local toolchain and be
available through `java.library.path` on the scanner/SonarQube host. On macOS,
the plugin also loads Langkit's `langkit_sigsegv_handler` before `adalang_jni`.

### Native libraries on GNU/Linux

Place `libadalang_jni.so` and its native dependencies in a common directory
such as `/opt/libadalang-jni-libs`. GNAT and HotSpot both use `SIGSEGV`, so the
scanner must start with HotSpot's `libjsig.so` signal-chaining library
preloaded and `-XX:+UseSignalChaining` enabled. The included wrapper configures
signal chaining and the native library paths:

```bash
./scripts/sonar-scanner-linux.sh
```

The wrapper automatically uses `/opt/java`, `/opt/sonar-scanner`, and
`/opt/libadalang-jni-libs` when present. Override these defaults with
`JAVA_HOME`, `SONAR_SCANNER_BIN`, or `SONAR_ADA_NATIVE_PATH` when needed. It can
also be linked into a directory on `PATH` to provide a short system-wide
command:

```bash
ln -s /opt/SonarAdaPlugin/scripts/sonar-scanner-linux.sh \
  /usr/local/bin/sonar-scanner-ada
```

Do not disable `sonar.text.activate` or `sonar.scm`: signal chaining allows the
Text/Secrets sensor and SCM publisher to run normally after Ada analysis.

### Native libraries on macOS

The generated Libadalang Java Makefile assumes Linux, and a recent macOS SDK
must be supplied explicitly when using an Alire GNAT toolchain built against an
older SDK. The following recipe targets Apple Silicon with Alire GNAT 15,
Libadalang 26, and JDK 21. Adjust the Libadalang path for another checkout:

```bash
libadalang_dir=/Users/mmartign/libadalang_26.0.0_75276b8d
sdk_root=$(xcrun --sdk macosx --show-sdk-path)
java_home="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"

cd "$libadalang_dir"
gnat_prefix=$(alr exec -- sh -c 'printf %s "$GNAT_NATIVE_ALIRE_PREFIX"')
gnat_lib="$gnat_prefix/lib"
alire_root=$(dirname "$(dirname "$gnat_prefix")")
alire_builds_dir="$alire_root/builds"

alr exec -- env \
  SDKROOT="$sdk_root" \
  C_INCLUDE_PATH="$sdk_root/usr/include:/opt/homebrew/include" \
  LIBRARY_PATH="$sdk_root/usr/lib:$gnat_lib:/opt/homebrew/lib" \
  gprbuild -p -P libadalang.gpr \
  -XLIBADALANG_LIBRARY_TYPE=relocatable \
  -XLIBADALANG_BUILD_MODE=prod \
  -XLIBRARY_TYPE=relocatable \
  -XGPR_LIBRARY_TYPE=relocatable \
  -XXMLADA_BUILD=relocatable
```

Build the JNI bridge using JDK 21. The Libadalang distribution normally already
contains the generated JNI headers under `java/jni`; rerun Libadalang's Java
generation first if those headers are absent. Embedding the dependency
directories as runtime paths avoids relying on `DYLD_LIBRARY_PATH`, which macOS
may remove before launching Java:

```bash
cd /Users/mmartign/SonarAdaPlugin

rpath_flags=$(find \
  "$alire_builds_dir" "$gnat_prefix" "$libadalang_dir" \
  -name '*.dylib' -exec dirname {} \; | sort -u | \
  awk '{printf " -Wl,-rpath,%s", $0}')

make -B -C "$libadalang_dir/java" \
  JAVA_HOME="$java_home" \
  JNI_INCLUDE="$java_home/include/darwin" \
  LIB_FILE_NAME=libadalang_jni.dylib \
  C_OPT="-fPIC -g -Wall -O0 -Werror \
    -I$java_home/include -I$java_home/include/darwin \
    -I$libadalang_dir/src" \
  LD_OPT="-dynamiclib -fPIC -Wl,-headerpad_max_install_names \
    -L$libadalang_dir/lib/relocatable/prod$rpath_flags"
```

Verify the two entry libraries and run tests with their directories exposed
to the forked test JVM:

```bash
file \
  "$libadalang_dir/lib/relocatable/prod/libadalang.dylib" \
  "$libadalang_dir/java/jni/libadalang_jni.dylib"

native_path="$libadalang_dir/java/jni:$libadalang_dir/lib/relocatable/prod"
mvn -DargLine="--enable-native-access=ALL-UNNAMED -Djava.library.path=$native_path" test
```

Supply the same `java.library.path` directories to the JVM that runs the
SonarScanner in production.

```bash
mvn -DargLine="--enable-native-access=ALL-UNNAMED -Djava.library.path=$native_path" clean package
```

The plugin JAR is created under `target/`.

## Install

Copy the generated JAR to the SonarQube Server plugin directory and restart SonarQube:

```bash
cp target/sonar-ada-plugin-0.1.0.jar "$SONARQUBE_HOME/extensions/plugins/"
```

## Analyze an Ada project

Create a `sonar-project.properties` file in the Ada project:

```properties
sonar.projectKey=my-ada-project
sonar.projectName=My Ada Project
sonar.sources=src
sonar.sourceEncoding=UTF-8
```

Run the SonarScanner as usual. Files with `.adb`, `.ads`, and `.ada` suffixes are indexed as Ada by default.

## Configuration

The default Ada suffixes can be changed in SonarQube settings with:

```properties
sonar.ada.file.suffixes=.adb,.ads,.ada
```

Rule thresholds such as line length, file length, and complexity are configured as rule parameters in the Ada quality profile.

## Spazio IT AdaLang Analyzer

AdaLang Analyzer is a Spazio IT static analyzer for Ada, similar in role to clang-analyzer, and is under active development. The plugin can run it directly and publish its findings as Sonar external issues:

```properties
sonar.ada.adalang.enabled=true
sonar.ada.adalang.executable=/Users/mmartign/AdaLang_Analyzer/bin/adalang_analyzer
sonar.ada.adalang.checks=*
sonar.ada.adalang.timeoutSeconds=300
```

Use `sonar.ada.adalang.checks=*` to enable every available check, or provide a comma-separated subset such as `No_Goto,No_Raise,Division_By_Zero`. In the current analyzer version, omitting this property leaves all checks disabled. Exit code `1` is accepted when the output contains violations. A code `1` result without parseable findings, timeouts, and internal errors fail the scan by default; set `sonar.ada.adalang.failOnError=false` to log a warning instead.

Import one or more pre-generated reports without running the analyzer. Both the
analyzer's complete console-text output and CSV/CSVX are supported:

```properties
sonar.ada.adalang.reportPaths=build/adalang_report.txt,build/adalang-report.csv
```

Multiple report paths can be separated with commas. Relative report paths and
relative source paths inside reports are resolved from the Sonar project base
directory. Absolute source paths from another checkout are matched by their
project-relative suffix. Direct execution and report import can be enabled
together.

For console reports, the importer preserves the check identifier, rule and
advice text, software quality, severity, file, line, column, and the complete
source caret span. Structured proof obligations are also imported: unproved
obligations become reliability issues containing their obligation kind,
analysis method, reason, and imprecision detail. The importer checks both the
`Violations` and proof-obligation `Total` summaries against the parsed details.
It logs the reported file, violation, proof-obligation, and skipped-check
coverage totals so incomplete semantic analysis remains visible in scanner logs.
CSV and CSVX reports use these fields:

```text
file,line,column,key,label,rule,message
```

A complete sample project is available in
[`examples/adalang-analyzer-project`](examples/adalang-analyzer-project/README.md).

## AdaControl integration

AdaControl is not bundled with this plugin. Install AdaControl separately on the scanner machine, then enable the integration in scanner properties or SonarQube settings.

Run AdaControl from the scanner:

```properties
sonar.ada.adacontrol.enabled=true
sonar.ada.adacontrol.executable=/path/to/adactl
sonar.ada.adacontrol.rulesFile=adactl.aru
sonar.ada.adacontrol.projectFile=my_project.gpr
sonar.ada.adacontrol.extraArgs=-- -gnat12
sonar.ada.adacontrol.timeoutSeconds=300
```

Import an existing AdaControl report instead:

```properties
sonar.ada.adacontrol.reportPaths=build/adacontrol-report.csv
```

Expected report fields are the standard AdaControl CSV/CSVX fields:

```text
file,line,column,key,label,rule,message
```

AdaControl findings are imported as Sonar external issues with engine id `AdaControl`. AdaControl exit code `1` means controls were triggered and does not fail the scan. Execution errors and timeouts fail the scan by default; set `sonar.ada.adacontrol.failOnError=false` to log them as warnings instead.

`sonar.ada.adacontrol.extraArgs` is appended after the input file list, which is why ASIS/compiler options can be supplied as `-- -gnat12`.

## GNATtest integration

Run GNATtest and its generated test driver before SonarScanner, save the test
output, and configure one or more reports:

```properties
sonar.ada.gnattest.reportPaths=build/gnattest.txt,build/gnattest-junit.xml
```

The importer accepts GNATtest's native text reporter, AUnit's JUnit reporter,
and AUnit's XML reporter. Results are aggregated into Sonar's test count,
failures, errors (crashed tests), skipped tests, and execution time metrics.
Relative report paths are resolved from the Sonar project base directory.

For native GNATtest output, redirect the test driver output to a file. Add
`--test-duration` when generating the harness to include execution times:

```bash
gnattest -Pexample.gpr --test-duration
gprbuild -Pobj/gnattest/harness/test_driver.gpr
obj/gnattest/harness/test_runner > build/gnattest.txt
```

For JUnit XML, generate the harness with AUnit's JUnit reporter and redirect its
output instead:

```bash
gnattest -Pexample.gpr --reporter=JUnit --test-duration
gprbuild -Pobj/gnattest/harness/test_driver.gpr
obj/gnattest/harness/test_runner > build/gnattest-junit.xml
```

Malformed, missing, empty, or inconsistent reports fail the scan by default.
Set `sonar.ada.gnattest.failOnError=false` to log and skip invalid reports.

Because AdaControl is GPL-2.0 software, this plugin integrates with it as an external executable/report producer. This avoids any direct linking that would conflict with this plugin's GPL-3.0-or-later license.

## License

This plugin is licensed under the GNU General Public License, version 3.0 or later (`GPL-3.0-or-later`).
