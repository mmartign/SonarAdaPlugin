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

Building the plugin requires JDK 24 or newer. Verify that Maven uses the
expected JDK with `mvn --version` before building.

Install the generated Java API from the local Libadalang and Langkit source
trees into the project-local Maven repository:

```bash
./scripts/install-local-libadalang.sh \
  /Users/mmartign/libadalang_26.0.0_75276b8d \
  /Users/mmartign/langkit_support_26.0.0_1745168f
```

The plugin resolves `com.adacore:libadalang:0.1` from `.m2/repository`; the
obsolete remote AdaCore Maven repository is not used.

Libadalang's Java API also requires the native `langkit_sigsegv_handler` and
`adalang_jni` libraries. They must be built by the local Libadalang toolchain
and be available through `java.library.path` on the scanner/SonarQube host.

### Native libraries on macOS

The generated Libadalang Java Makefile assumes Linux, and a recent macOS SDK
must be supplied explicitly when using an Alire GNAT toolchain built against an
older SDK. The following recipe was verified on Apple Silicon with Alire GNAT
15, Libadalang 26, and JDK 25. Adjust the first two paths for another checkout:

```bash
libadalang_dir=/Users/mmartign/libadalang_26.0.0_75276b8d
langkit_dir=/Users/mmartign/langkit_support_26.0.0_1745168f
sdk_root=$(xcrun --sdk macosx --show-sdk-path)
java_home="$(brew --prefix openjdk)/libexec/openjdk.jdk/Contents/Home"

cd "$libadalang_dir"
gnat_prefix=$(alr exec -- sh -c 'printf %s "$GNAT_NATIVE_ALIRE_PREFIX"')
gnat_lib="$gnat_prefix/lib"
adalib_library=$(find "$gnat_lib/gcc" -type f -name 'libgnarl-*.dylib' -print -quit)
adalib_dir=$(dirname "$adalib_library")
alire_root=$(dirname "$(dirname "$gnat_prefix")")
alire_builds_dir="$alire_root/builds"

alr exec -- env \
  SDKROOT="$sdk_root" \
  LIBRARY_PATH="$sdk_root/usr/lib:$gnat_lib:/opt/homebrew/lib" \
  gprbuild -p \
  -P "$langkit_dir/sigsegv_handler/langkit_sigsegv_handler.gpr"

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

Generate the JNI header using the project-local Maven repository, then build
the JNI bridge. Embedding the dependency directories as runtime paths avoids
relying on `DYLD_LIBRARY_PATH`, which macOS may remove before launching Java:

```bash
cd /Users/mmartign/SonarAdaPlugin

mvn -f "$libadalang_dir/java/pom.xml" \
  -Dmaven.repo.local="$PWD/.m2/repository" \
  -DskipTests compile

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

sigsegv_library="$langkit_dir/sigsegv_handler/lib/liblangkit_sigsegv_handler.dylib"
if ! otool -l "$sigsegv_library" | grep -F "path $adalib_dir " >/dev/null; then
  install_name_tool -add_rpath "$adalib_dir" "$sigsegv_library"
fi
```

Verify the three entry libraries and run tests with their directories exposed
to the forked test JVM:

```bash
file \
  "$langkit_dir/sigsegv_handler/lib/liblangkit_sigsegv_handler.dylib" \
  "$libadalang_dir/lib/relocatable/prod/libadalang.dylib" \
  "$libadalang_dir/java/jni/libadalang_jni.dylib"

native_path="$langkit_dir/sigsegv_handler/lib:$libadalang_dir/java/jni:$libadalang_dir/lib/relocatable/prod"
mvn -DargLine="--enable-native-access=ALL-UNNAMED -Djava.library.path=$native_path" test
```

Supply the same `java.library.path` directories to the JVM that runs the
SonarScanner in production.

```bash
mvn clean package
```

The plugin JAR is created under `target/`.

## Install

Copy the generated JAR to the SonarQube Server plugin directory and restart SonarQube:

```bash
cp target/sonar-ada-plugin-0.1.0-SNAPSHOT.jar "$SONARQUBE_HOME/extensions/plugins/"
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

The existing `AdaLangAnalyzerReportParser` can also parse pre-generated CSV/CSVX reports. Expected report fields are:

```text
file,line,column,key,label,rule,message
```

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

## License

This plugin is licensed under the GNU General Public License, version 3.0 or later (`GPL-3.0-or-later`).
