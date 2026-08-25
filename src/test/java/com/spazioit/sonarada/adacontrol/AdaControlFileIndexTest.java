/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adacontrol;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;

class AdaControlFileIndexTest {

  @Test
  void resolvesInputFileByAbsolutePath() {
    InputFile inputFile = inputFile("/project/src/demo.adb", "src/demo.adb");
    AdaControlFileIndex index = new AdaControlFileIndex(Path.of("/project"), List.of(inputFile));

    assertThat(index.find("/project/src/demo.adb")).contains(inputFile);
  }

  @Test
  void resolvesInputFileByRelativePathWhenAbsolutePathDoesNotMatch() {
    InputFile inputFile = inputFile("/opt/build/src/demo.adb", "src/demo.adb");
    AdaControlFileIndex index = new AdaControlFileIndex(Path.of("/some/other/dir"), List.of(inputFile));

    assertThat(index.find("src\\demo.adb")).contains(inputFile);
  }

  @Test
  void fallsBackToUnambiguousFileName() {
    InputFile inputFile = inputFile("/project/src/utils/demo.adb", "src/utils/demo.adb");
    AdaControlFileIndex index = new AdaControlFileIndex(Path.of("/project"), List.of(inputFile));

    assertThat(index.find("obj/demo.adb")).contains(inputFile);
  }

  @Test
  void returnsEmptyForAmbiguousFileName() {
    InputFile moduleA = inputFile("/project/moduleA/demo.adb", "moduleA/demo.adb");
    InputFile moduleB = inputFile("/project/moduleB/demo.adb", "moduleB/demo.adb");
    AdaControlFileIndex index = new AdaControlFileIndex(Path.of("/project"), List.of(moduleA, moduleB));

    assertThat(index.find("obj/demo.adb")).isEmpty();
  }

  @Test
  void returnsEmptyForBlankOrNullReportPath() {
    AdaControlFileIndex index = new AdaControlFileIndex(Path.of("/project"), List.of());

    assertThat(index.find(null)).isEmpty();
    assertThat(index.find("")).isEmpty();
    assertThat(index.find("   ")).isEmpty();
  }

  @Test
  void returnsEmptyWhenNothingMatches() {
    InputFile inputFile = inputFile("/project/src/demo.adb", "src/demo.adb");
    AdaControlFileIndex index = new AdaControlFileIndex(Path.of("/project"), List.of(inputFile));

    assertThat(index.find("src/unrelated.adb")).isEmpty();
  }

  private static InputFile inputFile(String absolutePath, String relativePath) {
    return (InputFile) Proxy.newProxyInstance(
      InputFile.class.getClassLoader(),
      new Class<?>[] {InputFile.class, IndexedFile.class},
      (proxy, method, arguments) -> switch (method.getName()) {
        case "absolutePath" -> absolutePath;
        case "relativePath" -> relativePath;
        case "path" -> Path.of(absolutePath);
        case "toString" -> relativePath;
        case "hashCode" -> System.identityHashCode(proxy);
        case "equals" -> proxy == arguments[0];
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }
}
