/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import com.spazioit.sonarada.AdaProperties;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Configuration;

class AdaLangAnalyzerSensorTest {

  @Test
  void importsARealConsoleReportWithGnatproveVerdicts(@TempDir Path projectDirectory) throws IOException {
    // The complete output of AdaLang Analyzer 1.8.5 run with --verify -v --gnatprove-log on the
    // analyzer's own tests/gnatprove_import fixture, with the checkout directory shortened.
    Files.write(projectDirectory.resolve("adalang.txt"), readResource("adalanganalyzer/gnatprove-import-console-output.txt"));

    List<SavedIssue> issues = importReport(projectDirectory, "adalang.txt",
      "tests/gnatprove_import/sample.ads", "tests/gnatprove_import/sample.adb");

    // Three findings, and the seven obligations that are unproved or a definite error. The
    // addition at sample.ads:18:7, which only GNATprove proved, is one of them.
    assertThat(issues).hasSize(10);
    assertThat(issues)
      .filteredOn(issue -> issue.ruleId().startsWith("proof-obligation:"))
      .extracting(issue -> issue.primary().file() + ":" + issue.primary().range(), SavedIssue::ruleId, SavedIssue::severity)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple(
          "tests/gnatprove_import/sample.ads:13:13", "proof-obligation:division-by-zero", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple(
          "tests/gnatprove_import/sample.ads:13:6", "proof-obligation:integer-overflow", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple(
          "tests/gnatprove_import/sample.ads:18:6", "proof-obligation:integer-overflow", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple(
          "tests/gnatprove_import/sample.adb:11:21", "proof-obligation:division-by-zero", Severity.CRITICAL),
        org.assertj.core.groups.Tuple.tuple(
          "tests/gnatprove_import/sample.adb:5:16", "proof-obligation:range-check", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple(
          "tests/gnatprove_import/sample.adb:11:16", "proof-obligation:integer-overflow", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple(
          "tests/gnatprove_import/sample.adb:11:16", "proof-obligation:range-check", Severity.MAJOR));
    assertThat(issues)
      .extracting(issue -> issue.primary().message())
      .anyMatch(message -> message.startsWith("Proof obligation [integer-overflow] unproved")
        && message.contains("GNATprove: proved"))
      .anyMatch(message -> message.startsWith("Proof obligation [division-by-zero] definite-error")
        && message.contains("GNATprove: not-proved"));
  }

  @Test
  void importsTheLengthChecksOfARealConsoleReport(@TempDir Path projectDirectory) throws IOException {
    // The complete output of AdaLang Analyzer 1.8.5 run with --verify -v on the analyzer's own
    // tests/verification_length_check_state.adb, with the checkout directory shortened.
    Files.write(projectDirectory.resolve("adalang.txt"), readResource("adalanganalyzer/length-check-console-output.txt"));

    List<SavedIssue> issues = importReport(projectDirectory, "adalang.txt", "tests/verification_length_check_state.adb");

    // Three findings and the nineteen unproved obligations: 1.8.5 proves two that 1.8.4 did not.
    assertThat(issues).hasSize(22);
    // The five length checks that are not proved. An assignment to a slice has one at its value
    // and one at its ":=", three columns before.
    assertThat(issues)
      .filteredOn(issue -> issue.ruleId().equals("proof-obligation:length-check"))
      .extracting(issue -> issue.primary().range(), SavedIssue::severity)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple("18:26", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("18:23", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("29:27", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("29:24", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("42:47", Severity.MAJOR));
    assertThat(issues)
      .filteredOn(issue -> issue.ruleId().equals("proof-obligation:length-check"))
      .extracting(issue -> issue.primary().message())
      .containsOnly("Proof obligation [length-check] unproved. Method: abstract-interpretation"
        + ". Why: length-check failure is not established, but absence is not proved"
        + ". Imprecision: the two lengths are not known to be equal");
  }

  @Test
  void importsTheReadsOfAnOutActualItsCalleeMayNotHaveWritten(@TempDir Path projectDirectory) throws IOException {
    // The complete text output and --format=json report of AdaLang Analyzer 1.8.5 run with
    // --verify on the analyzer's own tests/verification_fp117_unwritten_out.adb, the text with
    // the checkout directory shortened. Up to 1.8.4 the four reads were proved safe, and so
    // were no issue: an out actual was taken to be initialized after a call whose callee has a
    // path that returns without writing the parameter.
    Files.write(projectDirectory.resolve("adalang.txt"), readResource("adalanganalyzer/unwritten-out-console-output.txt"));
    Files.write(projectDirectory.resolve("adalang.json"), readResource("adalanganalyzer/unwritten-out-report.json"));

    List<SavedIssue> fromText = importReport(projectDirectory, "adalang.txt", "tests/verification_fp117_unwritten_out.adb");
    List<SavedIssue> fromJson = importReport(projectDirectory, "adalang.json", "tests/verification_fp117_unwritten_out.adb");

    // Eighteen findings, the definite error and the twelve unproved obligations.
    assertThat(fromText).hasSize(31);
    assertThat(fromJson).hasSize(31);

    // The out parameter each of four callees does not write on every path, then the read of
    // the actual after the call of each: an error after the callee that never writes it.
    List<SavedIssue> initializations = fromJson.stream()
      .filter(issue -> issue.ruleId().equals("proof-obligation:initialization-check"))
      .toList();
    assertThat(initializations)
      .extracting(issue -> issue.primary().range(), SavedIssue::severity)
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple("10:29", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("20:24", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("30:30", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("40:29", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("69:11", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("76:11", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("83:11", Severity.MAJOR),
        org.assertj.core.groups.Tuple.tuple("90:11", Severity.CRITICAL));
    assertThat(initializations.getLast().primary().message()).isEqualTo(
      "Proof obligation [initialization-check] definite-error. Operation: Swallowed. Method: flow-analysis"
        + ". Why: object is uninitialized on every incoming path"
        + ". Evidence: initialization => false");

    // The JSON report says where the object read is declared; a parameter is its own declaration.
    assertThat(initializations)
      .extracting(SavedIssue::secondary)
      .containsExactly(
        List.of(), List.of(), List.of(), List.of(),
        List.of(new Location("tests/verification_fp117_unwritten_out.adb", "66:6", "Declaration of Looped")),
        List.of(new Location("tests/verification_fp117_unwritten_out.adb", "73:6", "Declaration of Jumped")),
        List.of(new Location("tests/verification_fp117_unwritten_out.adb", "80:6", "Declaration of Blocked")),
        List.of(new Location("tests/verification_fp117_unwritten_out.adb", "87:6", "Declaration of Swallowed")));

    // The text output gives the same issues at the same places, without the declarations.
    assertThat(fromText)
      .extracting(SavedIssue::ruleId, SavedIssue::severity, issue -> issue.primary().range())
      .containsExactlyElementsOf(fromJson.stream()
        .map(issue -> org.assertj.core.groups.Tuple.tuple(issue.ruleId(), issue.severity(), issue.primary().range()))
        .toList());
    assertThat(fromText).allMatch(issue -> issue.secondary().isEmpty());
  }

  @Test
  void pointsAnInitializationIssueAtTheDeclarationOfItsObject(@TempDir Path projectDirectory) throws IOException {
    // Obligations copied verbatim from AdaLang Analyzer 1.8.3 --verify JSON reports: a read of
    // an object declared in another file, a read the analyzer found uninitialized, an out
    // parameter located at its own declaration, and a Depends aspect, which has no subject.
    Files.writeString(projectDirectory.resolve("adalang.json"), """
      {
        "analysisConfiguration": {"toolVersion": "1.8.3", "selectedPreset": "verify", "skippedChecks": 0},
        "filesScanned": 5,
        "newViolations": 0,
        "proofSummary": {"scope": "bounded scalar verification; unsupported boundaries are explicit", \
      "total": 4, "provedSafe": 0, "definiteError": 1, "unproved": 3, "unreachable": 0, "unsupported": 0},
        "findings": [],
        "proofObligations": [
          {"id": "proof/v1/8d9babb695256398", "kind": "initialization-check", "status": "unproved", \
      "method": "flow-analysis", "file": "tests/verification_call_frame.adb", "line": 5, "column": 10, \
      "operation": "Total", "assumptions": "", "abstractState": "", \
      "explanation": "object initialization is not established", \
      "imprecisionSource": "incoming paths disagree or object is external", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none", \
      "subject": {"file": "tests/verification_call_frame.ads", "line": 7, "column": 4}},
          {"id": "proof/v1/050bb272e3fce383", "kind": "initialization-check", "status": "definite-error", \
      "method": "flow-analysis", "file": "tests/verification_fp099_function_out_actual.adb", "line": 32, \
      "column": 12, "operation": "Never", "assumptions": "", "abstractState": "initialization => false", \
      "explanation": "object is uninitialized on every incoming path", "imprecisionSource": "", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none", \
      "subject": {"file": "tests/verification_fp099_function_out_actual.adb", "line": 19, "column": 4}},
          {"id": "proof/v1/a2ac0f34d9d9ad0d", "kind": "initialization-check", "status": "unproved", \
      "method": "flow-analysis", "file": "tests/verification_forward_goto.ads", "line": 24, "column": 45, \
      "operation": "Unset_Result", "assumptions": "", "abstractState": "", \
      "explanation": "out parameter is not established as initialized at the normal exit", \
      "imprecisionSource": "some path to the exit does not assign the whole parameter", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none", \
      "subject": {"file": "tests/verification_forward_goto.ads", "line": 24, "column": 45}},
          {"id": "proof/v1/dda1f8dd910d3305", "kind": "flow-dependencies", "status": "unproved", "method": "none", \
      "file": "tests/verification_flow_contracts.adb", "line": 40, "column": 11, "operation": "Depends of Scaled", \
      "assumptions": "", "abstractState": "", \
      "explanation": "the Depends aspect is not shown to be the dependencies of the subprogram", \
      "imprecisionSource": "information flow is not yet analyzed to the point of proof", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none"}
        ]
      }
      """);

    List<SavedIssue> issues = importReport(projectDirectory, "adalang.json",
      "tests/verification_call_frame.adb", "tests/verification_call_frame.ads",
      "tests/verification_fp099_function_out_actual.adb", "tests/verification_forward_goto.ads",
      "tests/verification_flow_contracts.adb");

    assertThat(issues).extracting(SavedIssue::ruleId).containsExactly(
      "proof-obligation:initialization-check", "proof-obligation:initialization-check",
      "proof-obligation:initialization-check", "proof-obligation:flow-dependencies");

    SavedIssue readOfAnotherFilesObject = issues.get(0);
    assertThat(readOfAnotherFilesObject.primary())
      .extracting(Location::file, Location::range)
      .containsExactly("tests/verification_call_frame.adb", "5:9");
    assertThat(readOfAnotherFilesObject.secondary()).containsExactly(
      new Location("tests/verification_call_frame.ads", "7:3", "Declaration of Total"));

    SavedIssue uninitializedRead = issues.get(1);
    assertThat(uninitializedRead.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(uninitializedRead.secondary()).containsExactly(
      new Location("tests/verification_fp099_function_out_actual.adb", "19:3", "Declaration of Never"));

    // The declaration is where the issue already is.
    assertThat(issues.get(2).primary().range()).isEqualTo("24:44");
    assertThat(issues.get(2).secondary()).isEmpty();

    SavedIssue dependsAspect = issues.get(3);
    assertThat(dependsAspect.severity()).isEqualTo(Severity.MAJOR);
    assertThat(dependsAspect.primary())
      .extracting(Location::file, Location::range)
      .containsExactly("tests/verification_flow_contracts.adb", "40:10");
    assertThat(dependsAspect.secondary()).isEmpty();
  }

  @Test
  void leavesOutASubjectInAFileSonarDoesNotIndex(@TempDir Path projectDirectory) throws IOException {
    Files.writeString(projectDirectory.resolve("adalang.json"), """
      {
        "findings": [],
        "proofObligations": [
          {"id": "proof/v1/8d9babb695256398", "kind": "initialization-check", "status": "unproved", \
      "method": "flow-analysis", "file": "tests/verification_call_frame.adb", "line": 5, "column": 10, \
      "operation": "Total", "assumptions": "", "abstractState": "", \
      "explanation": "object initialization is not established", \
      "imprecisionSource": "incoming paths disagree or object is external", "reasonCode": "", \
      "blockingExpression": "", "inlinePath": "", "configurationId": "none", \
      "subject": {"file": "tests/verification_call_frame.ads", "line": 7, "column": 4}}
        ]
      }
      """);

    List<SavedIssue> issues = importReport(projectDirectory, "adalang.json", "tests/verification_call_frame.adb");

    assertThat(issues).hasSize(1);
    assertThat(issues.getFirst().secondary()).isEmpty();
  }

  private static List<SavedIssue> importReport(Path projectDirectory, String report, String... relativePaths) {
    List<SavedIssue> issues = new ArrayList<>();
    List<InputFile> inputFiles = new ArrayList<>();
    for (String relativePath : relativePaths) {
      inputFiles.add(inputFile(projectDirectory, relativePath));
    }
    new AdaLangAnalyzerSensor().execute(sensorContext(
      projectDirectory, Map.of(AdaProperties.ADALANG_ANALYZER_REPORT_PATHS_KEY, report), inputFiles, issues));
    return issues;
  }

  private static byte[] readResource(String name) throws IOException {
    try (InputStream stream = AdaLangAnalyzerSensorTest.class.getClassLoader().getResourceAsStream(name)) {
      if (stream == null) {
        throw new IllegalStateException("Missing test resource: " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
    }
  }

  private record Location(String file, String range, String message) {
  }

  private record SavedIssue(String ruleId, Severity severity, Location primary, List<Location> secondary) {
  }

  private static SensorContext sensorContext(
    Path baseDir, Map<String, String> settings, List<InputFile> inputFiles, List<SavedIssue> issues
  ) {
    Configuration configuration = proxy(Configuration.class, "Configuration", (proxy, method, arguments) ->
      switch (method.getName()) {
        case "get" -> Optional.ofNullable(settings.get(arguments[0]));
        case "getBoolean" -> Optional.ofNullable(settings.get(arguments[0])).map(Boolean::parseBoolean);
        case "getInt" -> Optional.ofNullable(settings.get(arguments[0])).map(Integer::parseInt);
        case "hasKey" -> settings.containsKey(arguments[0]);
        default -> throw new UnsupportedOperationException(method.getName());
      });
    // The predicates select the Ada main files, which is all this file system has.
    FilePredicates predicates = proxy(FilePredicates.class, "FilePredicates", (proxy, method, arguments) -> null);
    FileSystem fileSystem = proxy(FileSystem.class, "FileSystem", (proxy, method, arguments) ->
      switch (method.getName()) {
        case "baseDir" -> baseDir.toFile();
        case "predicates" -> predicates;
        case "inputFiles" -> inputFiles;
        default -> throw new UnsupportedOperationException(method.getName());
      });
    return proxy(SensorContext.class, "SensorContext", (proxy, method, arguments) ->
      switch (method.getName()) {
        case "config" -> configuration;
        case "fileSystem" -> fileSystem;
        case "newExternalIssue" -> newExternalIssue(issues);
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }

  private static InputFile inputFile(Path baseDir, String relativePath) {
    return proxy(InputFile.class, relativePath, (proxy, method, arguments) ->
      switch (method.getName()) {
        case "absolutePath" -> baseDir.resolve(relativePath).toString();
        case "relativePath" -> relativePath;
        case "lines" -> 100;
        // Named after its start: the line and the zero-based offset in it.
        case "newRange" -> proxy(TextRange.class, arguments[0] + ":" + arguments[1], AdaLangAnalyzerSensorTest::unsupported);
        case "selectLine" -> proxy(TextRange.class, arguments[0] + ":line", AdaLangAnalyzerSensorTest::unsupported);
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }

  private static NewExternalIssue newExternalIssue(List<SavedIssue> issues) {
    String[] ruleId = new String[1];
    Severity[] severity = new Severity[1];
    Location[] primary = new Location[1];
    List<Location> secondary = new ArrayList<>();
    return proxy(NewExternalIssue.class, "NewExternalIssue", (proxy, method, arguments) -> {
      switch (method.getName()) {
        case "ruleId" -> ruleId[0] = (String) arguments[0];
        case "severity" -> severity[0] = (Severity) arguments[0];
        case "at" -> primary[0] = location(arguments[0]);
        case "addLocation" -> secondary.add(location(arguments[0]));
        case "newLocation" -> {
          return proxy(NewIssueLocation.class, "NewIssueLocation", new LocationRecorder());
        }
        case "save" -> {
          issues.add(new SavedIssue(ruleId[0], severity[0], primary[0], List.copyOf(secondary)));
          return null;
        }
        default -> {
          // engineId, type, cleanCodeAttribute, addImpact, and remediationEffortMinutes.
        }
      }
      return proxy;
    });
  }

  private static Location location(Object newIssueLocation) {
    LocationRecorder recorder = (LocationRecorder) ((Named) Proxy.getInvocationHandler(newIssueLocation)).handler();
    return new Location(recorder.file, recorder.range, recorder.message);
  }

  private static final class LocationRecorder implements InvocationHandler {
    private String file;
    private String range;
    private String message;

    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) {
      switch (method.getName()) {
        case "on" -> file = arguments[0].toString();
        case "at" -> range = arguments[0].toString();
        case "message" -> message = (String) arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      }
      return proxy;
    }
  }

  private static Object unsupported(Object proxy, Method method, Object[] arguments) {
    throw new UnsupportedOperationException(method.getName());
  }

  /** A proxy that answers the {@link Object} methods itself, by its name and identity. */
  private static <T> T proxy(Class<T> type, String name, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, new Named(name, handler)));
  }

  private record Named(String name, InvocationHandler handler) implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
      return switch (method.getName()) {
        case "toString" -> name;
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> handler.invoke(proxy, method, arguments);
      };
    }
  }
}
