/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;

class AdaLangAnalyzerFileIndexTest {

  @Test
  void resolvesAbsoluteReportPathFromAnotherCheckoutByProjectRelativeSuffix() {
    InputFile inputFile = inputFile(
      "/opt/AdaLang_Analyzer/src/adalang_analyzer-ada_text.adb",
      "src/adalang_analyzer-ada_text.adb");
    AdaLangAnalyzerFileIndex index = new AdaLangAnalyzerFileIndex(
      Path.of("/opt/AdaLang_Analyzer"), List.of(inputFile));

    assertThat(index.find(
      "/Users/mmartign/AdaLang_Analyzer/src/adalang_analyzer-ada_text.adb"))
      .contains(inputFile);
  }

  @Test
  void resolvesADrivePathWrittenOnWindowsByProjectRelativeSuffix() {
    InputFile inputFile = inputFile("/opt/project/src/demo.adb", "src/demo.adb");
    AdaLangAnalyzerFileIndex index = new AdaLangAnalyzerFileIndex(Path.of("/opt/project"), List.of(inputFile));

    assertThat(index.find("C:/Work/project/src/demo.adb")).contains(inputFile);
    assertThat(index.find("C:\\Work\\project\\src\\demo.adb")).contains(inputFile);
  }

  @Test
  void matchesBySuffixAPathThatIsNoPathOnThisHost() {
    // A NUL character makes a path invalid on every host: the lookup must not throw.
    InputFile inputFile = inputFile("/opt/project/src/demo.adb", "src/demo.adb");
    AdaLangAnalyzerFileIndex index = new AdaLangAnalyzerFileIndex(Path.of("/opt/project"), List.of(inputFile));

    assertThat(index.find("/elsewhere/\u0000/src/demo.adb")).contains(inputFile);
    assertThat(index.find("/elsewhere/\u0000/src/other.adb")).isEmpty();
  }

  private static InputFile inputFile(String absolutePath, String relativePath) {
    return (InputFile) Proxy.newProxyInstance(
      InputFile.class.getClassLoader(),
      new Class<?>[] {InputFile.class, IndexedFile.class},
      (proxy, method, arguments) -> switch (method.getName()) {
        case "absolutePath" -> absolutePath;
        case "relativePath" -> relativePath;
        case "toString" -> relativePath;
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }
}
