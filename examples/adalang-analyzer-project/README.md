# SonarQube AdaLang Analyzer example

This is a minimal Ada project configured to import a report produced by Spazio
IT's `adalang_analyzer` into SonarQube. The sample source intentionally contains
the findings listed in [`reports/adalang-report.csv`](reports/adalang-report.csv).

## Prerequisites

- A running SonarQube instance with the Spazio IT Sonar Ada plugin installed.
- SonarScanner CLI installed on the machine that performs the scan.
- A SonarQube project token exported as `SONAR_TOKEN`.

The included report makes the example runnable without installing the external
analyzer on the scanner machine.

## Run the example

From this directory, run:

```sh
export SONAR_HOST_URL=http://localhost:9000
export SONAR_TOKEN=your-project-token
sonar-scanner
```

The findings appear in SonarQube as external issues whose engine is
`SpazioIT AdaLang Analyzer`.

## Import your own report

Replace the included report, or point the scanner to one or more comma-separated
console-text, CSV, or CSVX files:

```sh
sonar-scanner \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.ada.adalang.reportPaths=build/adalang_report.txt,build/second.csv
```

Console-text reports are the complete output produced by `adalang_analyzer` and
include lines such as `file:line:column: warning: message [Check_Name]`, followed
by rule, advice, and quality details. CSV/CSVX reports use this header and field
order:

```csv
file,line,column,key,label,rule,message
```

Paths inside a report can be absolute or relative to the Sonar project base
directory. Absolute paths generated in another checkout are matched using their
project-relative suffix. Report paths are resolved from the project directory.

## Run the analyzer during the scan

To run a current, compatible `adalang_analyzer` instead of importing a report,
put it on the scanner machine's `PATH` and use:

```properties
sonar.ada.adalang.enabled=true
sonar.ada.adalang.executable=adalang_analyzer
sonar.ada.adalang.checks=No_Goto,No_Raise,Division_By_Zero
```

The executable must support the `-checks=` option and `file:line:column`
diagnostic output. Exit code `1` is accepted when findings were produced.
Report import and direct execution can be enabled together, although the same
finding may then be published twice.

## Configuration

The integration settings are in [`sonar-project.properties`](sonar-project.properties):

- `sonar.ada.adalang.reportPaths` selects comma-separated CSV/CSVX reports.
- `sonar.ada.adalang.enabled=false` prevents the analyzer from also running.
- `sonar.ada.adalang.failOnError=true` makes missing or unreadable reports fail
  the Sonar scan.

GNAT users can build the sample independently with:

```sh
gprbuild -P example.gpr
```

Compilation is not required for SonarScanner analysis.
