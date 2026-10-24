# SonarQube Ada Plugin

[![CI](https://github.com/mmartign/SonarAdaPlugin/actions/workflows/ci.yml/badge.svg)](https://github.com/mmartign/SonarAdaPlugin/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fmmartign%2FSonarAdaPlugin%2Fmain%2Fpom.xml&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27version%27%5D&label=version&color=blue)](pom.xml)
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

### Libadalang Java API

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

Some GNAT toolchains link `libadalang_jni.so` against GNU libiconv, whose
headers redefine `iconv_open()` to `libiconv_open()`. glibc provides
`iconv_open()` itself but not that symbol, so hosts without GNU libiconv
installed fail to load the library. `scripts/ensure-libiconv-linux.sh` copies
whichever `libiconv` the JNI library actually needs into the native library
directory so this no longer depends on what a given host has installed.
`scripts/install-local-libadalang.sh` runs it once during setup, and
`scripts/sonar-scanner-linux.sh` runs it again before every scan so the bundle
stays current if the native libraries are updated afterwards.

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

### Package the plugin

```bash
mvn clean package
```

The tests load the native libraries from `/opt/libadalang-jni-libs`. Where they
are elsewhere, as in the macOS recipe above, pass their directories:

```bash
mvn -DargLine="--enable-native-access=ALL-UNNAMED -Djava.library.path=$native_path" clean package
```

Without the native libraries, `mvn -P skip-native-tests clean package` builds
the plugin and runs every test except `LibadalangAnalyzerTest`.

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
cp target/sonar-ada-plugin-1.10.2.jar "$SONARQUBE_HOME/extensions/plugins/"
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

AdaLang Analyzer is a Spazio IT static analyzer for Ada, similar in role to
clang-analyzer, and is under active development. The plugin can run it during
the scan, import reports it wrote earlier, or both. Its findings are published
as Sonar external issues.

This version of the plugin is tested against AdaLang Analyzer 1.10.3. Reports of
earlier versions are still imported.

| Property | Default | Purpose |
| --- | --- | --- |
| `sonar.ada.adalang.enabled` | `false` | Run the analyzer during the scan. |
| `sonar.ada.adalang.executable` | `adalang_analyzer` | Path to the analyzer. Use an absolute path when it is not on `PATH`. |
| `sonar.ada.adalang.preset` | | Preset of checks, passed as `--<preset>`. |
| `sonar.ada.adalang.checks` | | Check names, passed with `-checks=`. |
| `sonar.ada.adalang.projectFile` | | GNAT project file, passed with `-P`. |
| `sonar.ada.adalang.extraArgs` | | Further arguments, passed before the input files. |
| `sonar.ada.adalang.gnatproveLogPaths` | | GNATprove logs, each passed with `--gnatprove-log=`. |
| `sonar.ada.adalang.timeoutSeconds` | `300` | How long to wait for the analyzer. |
| `sonar.ada.adalang.failOnError` | `true` | Fail the scan when the analyzer cannot be run or stops with an error. |
| `sonar.ada.adalang.reportPaths` | | Reports to import. |

`checks`, `gnatproveLogPaths`, and `reportPaths` take comma-separated lists.
Relative paths of the project file, the logs, and the reports are resolved from
the Sonar project base directory.

### Run the analyzer during the scan

```properties
sonar.ada.adalang.enabled=true
sonar.ada.adalang.executable=/path/to/adalang_analyzer
sonar.ada.adalang.preset=verify
sonar.ada.adalang.checks=-Missing_Depends_Contract
sonar.ada.adalang.projectFile=my_project.gpr
sonar.ada.adalang.extraArgs=-XBUILD_MODE=release -complexity-threshold=15
```

**Checks.** `preset` selects one of the analyzer's presets: `recommended`,
`spark`, `verify`, `automotive`, or `do178c=<A|B|C|D>`. `checks` names the
checks to run, such as `No_Goto,No_Raise,Division_By_Zero`; given together with
a preset it refines it, for example `-Missing_Depends_Contract` to disable one
of its checks. When neither is set, the plugin runs the analyzer with
`--recommended`: the analyzer enables no checks by default and would otherwise
silently analyze nothing. Run `adalang_analyzer -list-checks` for the
catalogue.

`checks=*` enables every check of the catalogue, including the coding-standard
checks below and pairs that contradict each other by design (for example
`Use_If_Expression`, which suggests if expressions, and
`Conditional_Expression`, which forbids them). Prefer a preset plus an explicit
list.

**Coding-standard checks.** The analyzer also offers opt-in checks of naming
conventions, layout, restricted constructs, object-oriented design,
representation items, and complexity limits. No preset enables them. Name the
ones your coding standard requires in `checks`, alone or after a preset, and
give their limits and conventions with `-rule-param=<check>.<name>=<value>` in
`extraArgs`:

```properties
sonar.ada.adalang.enabled=true
sonar.ada.adalang.preset=recommended
sonar.ada.adalang.checks=Identifier_Casing,Maximum_Subprogram_Lines,Forbidden_Attribute
sonar.ada.adalang.extraArgs=-rule-param=Identifier_Casing.type=mixed -rule-param=Maximum_Subprogram_Lines.n=200 -rule-param=Forbidden_Attribute.forbidden=Address,Unchecked_Access
```

Their findings are imported like any other finding, under the check's name.

**Project file.** The sources of the project given in `projectFile` are
analyzed together with the Ada files indexed by Sonar, and `--verify` takes the
project's configuration pragmas into account: a function in a project whose
configuration pragmas set `SPARK_Mode` is treated as free of side effects.

**Further arguments.** `extraArgs` takes arguments such as `-X<name>=<value>`
scenario variables, thresholds, or `--baseline=<file>`. Do not pass `--format`,
`--output`, or `-q` there: the plugin reads the analyzer's verbose text output.

**Configuration file.** The analyzer is run from the Sonar project's base
directory, so an `adalang_analyzer.cfg` file at the project root is discovered
as it would be in a manual run. When that file exists, or `extraArgs` names one
with `--config=<file>`, the plugin does not add `--recommended`: the analyzer
reads the file's flags before the command line, and a preset on the command
line would reset the checks and the `--verify` mode the file selected. A
`preset` set in Sonar still overrides the file's, and `checks` still refines
it.

**Errors and warnings.** Exit code `1` is accepted when the output contains
violations. A code `1` result without parseable findings, a timeout, or an
internal error fails the scan; set `sonar.ada.adalang.failOnError=false` to log
a warning instead. The analyzer's own warnings about a run, such as no check
being enabled or no `gnatls` being found on `PATH` (which degrades the checks
that need the Ada runtime packages), are repeated as warnings in the scanner
log.

### Proof obligations

With the `verify` preset the analyzer runs its bounded scalar verification,
which gives each proof obligation one of five outcomes:

| Outcome | In Sonar |
| --- | --- |
| `definite-error`, a proven failure | Reliability issue of high severity |
| `unproved`, an undetermined risk | Reliability issue of medium severity |
| `proved-safe`, `unreachable`, `unsupported` | No issue; counted in the scanner log |

The rule of an issue is made from the kind of its obligation, for example
`proof-obligation:range-check`, `proof-obligation:initialization-check`, or
`proof-obligation:precondition`. Its message gives the outcome, the analysis
method, the reason, and what made the result imprecise.

- **Without `--verify` nothing is proved.** The analyzer attempts no proof and
  reports every obligation that is not a known failure as `unproved`, which
  says nothing about the code; the report states this as the scope `enumerated
  outcomes in current analysis scope; not exhaustive`. Those obligations are
  not imported, only counted in the scanner log. `definite-error` obligations
  are still imported.
- **A `Depends` aspect is never proved.** The analyzer has no route that
  proves one, so every `flow-dependencies` obligation is `unproved`, with the
  method `none`, and each `Depends` aspect has one such issue. A wrong aspect
  is reported by the `Depends_Contract_Mismatch` check.
- **An initialization check is located where the object is read.** JSON
  reports also give the declaration of that object, which becomes a secondary
  location of the issue when it is elsewhere, in the same file or in another
  one Sonar indexes. Console-text output, which is what the plugin reads when
  it runs the analyzer itself, does not carry it.

### GNATprove verdicts

Where the project is also proved with GNATprove, the analyzer can be given the
log of that run, and what GNATprove said of each check is set beside the
analyzer's own proof obligation for it. This needs the `verify` preset and
AdaLang Analyzer 1.8.3 or later. Produce the log with `--report=all
--output=oneline`:

```sh
gnatprove -P my_project.gpr --mode=all --report=all --output=oneline > build/gnatprove.log
```

```properties
sonar.ada.adalang.enabled=true
sonar.ada.adalang.preset=verify
sonar.ada.adalang.projectFile=my_project.gpr
sonar.ada.adalang.gnatproveLogPaths=build/gnatprove.log
```

GNATprove is a separate tool of AdaCore. Neither this plugin nor AdaLang
Analyzer contains it or runs it: the analyzer reads the text GNATprove wrote.

- **The verdict is shown and decides nothing.** It is in the message of each
  proof-obligation issue a check of the log is paired with: `GNATprove:
  proved`, `GNATprove: justified` (accepted by a reviewer, not discharged by a
  prover), or `GNATprove: not-proved`. The log neither adds nor removes an
  issue: an `unproved` obligation is raised whether GNATprove proved its check
  or not, and so is a `definite-error` one.
- **The scanner log has the counts.** It repeats the analyzer's counts of
  GNATprove's verdicts, kept apart from its own results, and warns when a check
  GNATprove proved is a definite error for the analyzer: one of the two tools
  is wrong, or the log is not that of the sources analyzed.
- **Giving the log of the sources analyzed is up to you.** A log names a file
  without its directory and says nothing of the state of the sources it was
  written for, so a log of an earlier state can pair a verdict with the wrong
  obligation.
- **A log the analyzer rejects is an error.** Given a log without `--verify`,
  or a log it cannot read, the analyzer stops with an error, which fails the
  scan unless `sonar.ada.adalang.failOnError=false`.

### Import reports

Import one or more pre-generated reports without running the analyzer:

```properties
sonar.ada.adalang.reportPaths=build/adalang_report.json,build/adalang_report.txt
```

The importer detects the format of each report: the analyzer's complete
console-text output, `--format=json`, `--format=sarif`, or the legacy
semicolon-separated CSV/CSVX format shared with AdaControl. Direct execution
and report import can be enabled together.

Relative source paths inside a report are resolved from the Sonar project base
directory. Absolute source paths from another checkout are matched by their
project-relative suffix. SARIF locations are percent-decoded before matching.

| What a report brings into Sonar | Console text | JSON | SARIF |
| --- | --- | --- | --- |
| Findings: check, message, software quality, severity, location | yes | yes | yes |
| `why` and `evidence` of a finding | yes | yes | yes |
| Rule and advice text of a finding | yes | no | yes |
| Complete source span of a finding | yes | no | no |
| Proof obligations, as issues | yes | yes | no |
| Checked operation and assumptions of an obligation | no | yes | no |
| Declaration of the object of an initialization check | no | yes | no |
| GNATprove verdicts | yes | yes | counts only |

SARIF has no slot for a located proof obligation, so its obligations are
counted and not imported. Findings already matching the analyzer's `--baseline`
are left out of every format, the same way console-text output hides them.

The importer checks the `Violations` (JSON: `newViolations`) and
proof-obligation `Total` summaries of a report against the details it parsed,
when the format reports them. It logs the reported file, violation,
proof-obligation, and skipped-check totals, so that incomplete semantic
analysis remains visible in the scanner log, followed by the number of proof
obligations with each outcome and, for a run that was given a GNATprove log,
the counts of GNATprove's verdicts. The JSON report's list of every check of
the log (`gnatproveChecks`) is not imported.

A report that is missing, empty, unreadable, or inconsistent with its own
totals is skipped with a warning. It does not fail the scan, whatever the value
of `sonar.ada.adalang.failOnError`.

CSV and CSVX reports use these fields:

```text
file,line,column,key,label,rule,message
```

A complete sample project is available in
[`examples/adalang-analyzer-project`](examples/adalang-analyzer-project/README.md).

### Upgrading AdaLang Analyzer

The plugin reports what the analyzer decides, so a new version of the analyzer
changes the issues in Sonar with no change to the plugin:

- **Issue counts are not comparable between analyzer versions.** A newer
  version can raise obligations an earlier one did not, some of them under new
  `proof-obligation:` rules, and decide obligations an earlier one left
  `unproved` or `unsupported`. One it decides can be a `definite-error`, an
  issue of higher severity, and one that was `unsupported`, which is no issue,
  can be `unproved`, which is one.
- **Regenerate imported `--verify` reports with the current analyzer.** Earlier
  versions could report an obligation as proved safe that a legal execution
  violates, and a definite error where there is none. Obligations that were
  wrongly proved safe then appear as unproved or definite-error issues.

[`CHANGELOG.md`](CHANGELOG.md) says what each version changes in Sonar, and the
[analyzer's changelog](https://github.com/mmartign/AdaLang_Analyzer/blob/main/CHANGELOG.md)
what it changes in the analysis.

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

Because AdaControl is GPL-2.0 software, this plugin integrates with it as an external executable/report producer. This avoids any direct linking that would conflict with this plugin's GPL-3.0-or-later license.

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

## License

This plugin is licensed under the GNU General Public License, version 3.0 or later (`GPL-3.0-or-later`).
