# SonarQube Ada Plugin

This repository contains a SonarQube Server plugin that adds static analysis support for Ada source files.

## Features

- Ada language registration for `.adb`, `.ads`, and `.ada` files.
- Native Ada analysis with libadalang.
- Built-in Ada quality profile.
- Basic source metrics: lines, non-comment lines, comment lines, statements, functions, and cyclomatic complexity.
- Syntax highlighting for comments, strings, Ada keywords, constants, and pragmas.
- CPD token generation for duplicate-code detection.
- Optional AdaControl integration:
  - Run an installed `adactl` executable during analysis.
  - Import pre-generated AdaControl CSV/CSVX reports.
  - Publish AdaControl findings as Sonar external issues.
- Ten built-in checks:
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

## Build

```bash
mvn -Dmaven.repo.local=.m2/repository clean package
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

## AdaControl integration

AdaControl is not bundled with this plugin. Install AdaControl separately on the scanner machine, then enable the integration in scanner properties or SonarQube settings.

Run AdaControl from the scanner:

```properties
sonar.ada.adacontrol.enabled=true
sonar.ada.adacontrol.executable=/path/to/adactl
sonar.ada.adacontrol.rulesFile=adacontrol.aru
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

Because AdaControl is GPL-2.0 software, this plugin integrates with it as an external executable/report producer instead of copying AdaControl source code or bundled rule files into this Apache-licensed project.

## License

This plugin is licensed under the Apache License 2.0. See `LICENSE` for details.
