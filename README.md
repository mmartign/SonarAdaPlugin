# SonarQube Ada Plugin

[![CI](https://github.com/mmartign/SonarAdaPlugin/actions/workflows/ci.yml/badge.svg)](https://github.com/mmartign/SonarAdaPlugin/actions/workflows/ci.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

This repository contains a SonarQube Server plugin that adds static analysis support for Ada source files. The native analysis engine is based on libadalang, with a built-in rule set inspired by the popular AdaControl tool.

This plugin and [AdaLang_Analyzer](https://github.com/mmartign/AdaLang_Analyzer) are packaged and distributed together as part of the [Spazio IT SAFe Toolset](https://spazioit.com/pages_en/sol_inf_en/code_quality_en/safe-toolset-en/), a ready-to-run static analysis environment for safety-critical C, C++, and Ada codebases.

## Features

- Ada language registration for `.adb`, `.ads`, and `.ada` files.
- Native Ada analysis with libadalang.
- Built-in Ada quality profile.
- Basic source metrics: lines, non-comment lines, comment lines, functions, and cyclomatic complexity.
- Syntax highlighting for comments, strings, Ada keywords, constants, and pragmas.
- CPD token generation for duplicate-code detection.
- AdaLang_Analyzer integration: run the analyzer directly, or import its console-text, JSON, SARIF, or legacy CSV/CSVX reports.
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

Some GNAT toolchains link `libadalang_jni.so` against GNU libiconv, whose
headers redefine `iconv_open()` to `libiconv_open()`. glibc provides
`iconv_open()` itself but not that symbol, so hosts without GNU libiconv
installed fail to load the library. `scripts/ensure-libiconv-linux.sh` copies
whichever `libiconv` the JNI library actually needs into the native library
directory so this no longer depends on what a given host has installed.
`scripts/install-local-libadalang.sh` runs it once during setup, and
`scripts/sonar-scanner-linux.sh` runs it again before every scan so the bundle
stays current if the native libraries are updated afterwards.

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

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) builds and tests the
plugin on every push and pull request, except for the native libadalang/JNI
path: `LibadalangAnalyzerTest` is excluded via the `skip-native-tests` Maven
profile, since the CI runner has the libadalang Java bindings on the compile
classpath but not the native `adalang_jni`/`langkit_sigsegv_handler`
libraries described above. Every other unit test runs for real. Locally,
`mvn test` still runs the full suite, native tests included, once the native
libraries are set up per this README.

The libadalang/langkit_support Java bindings jars are published as assets on
the [`ci-deps-libadalang-26.0.0-75276b8d-java21`](https://github.com/mmartign/SonarAdaPlugin/releases/tag/ci-deps-libadalang-26.0.0-75276b8d-java21)
release for the workflow to download. When `libadalang.version` in `pom.xml`
changes, rebuild the bindings locally per this README, publish a new release
with the new jars, and update the version/tag in
`.github/workflows/ci.yml` to match.

## Install

Copy the generated JAR to the SonarQube Server plugin directory and restart SonarQube:

```bash
cp target/sonar-ada-plugin-1.6.2.jar "$SONARQUBE_HOME/extensions/plugins/"
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

Use `sonar.ada.adalang.checks=*` to enable every available check, or provide a comma-separated subset such as `No_Goto,No_Raise,Division_By_Zero`. Exit code `1` is accepted when the output contains violations. A code `1` result without parseable findings, timeouts, and internal errors fail the scan by default; set `sonar.ada.adalang.failOnError=false` to log a warning instead.

Select one of the analyzer's presets with `sonar.ada.adalang.preset`: `recommended`, `spark`, `verify`, `automotive`, or `do178c=<A|B|C|D>`. The value is passed as `--<preset>`. `verify` runs the analyzer's bounded scalar verification, which classifies each proof obligation as proved safe, definite error, unproved, unreachable, or unsupported; with any other preset nothing is proved, and every obligation that is not a known failure is reported as unproved. A `checks` list given together with a preset refines it, for example `-Missing_Depends_Contract` to disable one of its checks:

```properties
sonar.ada.adalang.enabled=true
sonar.ada.adalang.preset=verify
sonar.ada.adalang.checks=-Missing_Depends_Contract
sonar.ada.adalang.projectFile=my_project.gpr
sonar.ada.adalang.extraArgs=-XBUILD_MODE=release -complexity-threshold=15
```

AdaLang Analyzer also offers opt-in coding-standard checks: naming conventions, layout, restricted constructs, object-oriented design, representation items, and complexity limits. No preset enables them. Name the ones your coding standard requires in `sonar.ada.adalang.checks`, alone or after a preset, and give their limits and conventions with `-rule-param=<check>.<name>=<value>` in `sonar.ada.adalang.extraArgs`:

```properties
sonar.ada.adalang.enabled=true
sonar.ada.adalang.preset=recommended
sonar.ada.adalang.checks=Identifier_Casing,Maximum_Subprogram_Lines,Forbidden_Attribute
sonar.ada.adalang.extraArgs=-rule-param=Identifier_Casing.type=mixed -rule-param=Maximum_Subprogram_Lines.n=200 -rule-param=Forbidden_Attribute.forbidden=Address,Unchecked_Access
```

Their findings are imported like any other AdaLang Analyzer finding, under the check's name. With these checks in the catalogue, `sonar.ada.adalang.checks=*` enables every one of them as well, including pairs that contradict each other by design (for example `Use_If_Expression`, which suggests if expressions, and `Conditional_Expression`, which forbids them); prefer a preset plus an explicit list. Run `adalang_analyzer -list-checks` for the catalogue.

AdaLang Analyzer enables no checks by default, so when neither `preset` nor `checks` is set the plugin runs it with `--recommended` instead of silently analyzing nothing.

`sonar.ada.adalang.projectFile` passes a GNAT project file with `-P`. The project's sources are analyzed together with the Ada files indexed by Sonar, and `--verify` takes the project's configuration pragmas into account: a function in a project whose configuration pragmas set `SPARK_Mode` is treated as free of side effects. `sonar.ada.adalang.extraArgs` passes further arguments before the input files, such as `-X<name>=<value>` scenario variables, thresholds, or `--baseline=<file>`. Do not pass `--format`, `--output`, or `-q` there: the plugin reads the analyzer's verbose text output.

The analyzer is run from the Sonar project's base directory, so an `adalang_analyzer.cfg` file placed at the project root is auto-discovered the same way it would be from a manual command-line run. When that file exists, or `extraArgs` names one with `--config=<file>`, the plugin does not add `--recommended`: the analyzer reads the file's flags before the command line, and a preset on the command line would reset the checks and the `--verify` mode the file selected. A `preset` set in Sonar still overrides the file's, and `checks` still refines it.

The analyzer's own warnings about a run, such as no check being enabled or no `gnatls` being found on `PATH` (which degrades the checks that need the Ada runtime packages), are repeated as warnings in the scanner log.

Import one or more pre-generated reports without running the analyzer. The
analyzer's complete console-text output, `--format=json`, and `--format=sarif`
are all supported, plus the legacy semicolon-separated CSV/CSVX format shared
with AdaControl; the importer detects the format of each report automatically:

```properties
sonar.ada.adalang.reportPaths=build/adalang_report.json,build/adalang_report.txt
```

Multiple report paths can be separated with commas. Relative report paths and
relative source paths inside reports are resolved from the Sonar project base
directory. Absolute source paths from another checkout are matched by their
project-relative suffix. SARIF locations are percent-decoded before matching.
Direct execution and report import can be enabled together.

For console and SARIF reports, the importer preserves the check identifier,
rule and advice text (SARIF: from the run's rule catalog), software quality,
severity, file, line, column, and (console only) the complete source caret
span. All three structured formats also carry the analyzer's `why`/`evidence`
explanation for a finding, when present. JSON reports additionally carry
`explanation`/`evidence` per finding directly. Findings already matching the
analyzer's `--baseline` are excluded from every format, the same way
console-text output hides them.

Structured proof obligations are imported from console-text and JSON reports
(SARIF has no slot for them). Both `definite-error` (a proven failure) and
`unproved` (an undetermined risk) obligations become reliability issues
containing their obligation kind, analysis method, reason, and imprecision
detail; JSON reports additionally carry the checked operation and any
assumptions the proof relied on. `proved-safe`, `unreachable`, and
`unsupported` obligations are not findings and are not imported.

`unproved` obligations are imported only from a `--verify` run. Without
`--verify` the analyzer attempts no proof and reports every obligation that is
not a known failure as `unproved`, which says nothing about the code; the
report states this as the scope `enumerated outcomes in current analysis scope;
not exhaustive`. Those obligations are counted in the scanner log instead, and
`definite-error` obligations are still imported.

The importer checks both the `Violations` (or
JSON's `newViolations`) and proof-obligation `Total` summaries against the
parsed details, when the format reports them. It logs the reported file,
violation, proof-obligation, and skipped-check coverage totals so incomplete
semantic analysis remains visible in scanner logs, followed by the number of
proof obligations with each outcome.

Reports of AdaLang Analyzer 1.6.2 are supported in all three formats. Its
`--verify` results differ from those of 1.6.0 and earlier, which could report
an obligation as proved safe that a legal execution violates (see the
analyzer's changelog for 1.6.1). Regenerate imported `--verify` reports with
1.6.1 or later: obligations that were wrongly proved safe then appear as
unproved or definite-error issues.
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
