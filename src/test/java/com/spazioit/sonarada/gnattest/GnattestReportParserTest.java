/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.gnattest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GnattestReportParserTest {

  private final GnattestReportParser parser = new GnattestReportParser();

  @Test
  void parsesNativeGnattestTextIncludingNamesAndDurations(@TempDir Path directory) throws IOException {
    Path report = write(directory, "gnattest.txt", """
      bar.ads:9:5: info: corresponding test PASSED (0.125 s)
      foo.ads:7:4: (Foo.Test_Not_Equal) error: corresponding test FAILED: Value not equal (foo-test_data-tests.adb:45)
      foo.ads:9:5: error: corresponding test CRASHED: CONSTRAINT_ERROR : range check failed
        Traceback:
          frame
      """);

    assertThat(parser.parse(report)).isEqualTo(new GnattestResults(3, 1, 1, 0, 125));
  }

  @Test
  void parsesAunitJunitXml(@TempDir Path directory) throws IOException {
    Path report = write(directory, "junit.xml", """
      <?xml version="1.0" encoding="utf-8"?>
      <testsuites skipped="1" tests="4" failures="1" errors="1" time="0.0425">
        <testsuite name="Foo" skipped="1" tests="4" failures="1" errors="1" time="0.0425"/>
      </testsuites>
      """);

    assertThat(parser.parse(report)).isEqualTo(new GnattestResults(4, 1, 1, 1, 43));
  }

  @Test
  void sumsJunitSuitesWhenRootDoesNotHaveTotals(@TempDir Path directory) throws IOException {
    Path report = write(directory, "junit.xml", """
      <testsuites>
        <testsuite name="Foo" tests="2" failures="1" errors="0" time="0.01"/>
        <testsuite name="Bar" tests="3" failures="0" errors="1" skipped="1" time="0.02"/>
      </testsuites>
      """);

    assertThat(parser.parse(report)).isEqualTo(new GnattestResults(5, 1, 1, 1, 30));
  }

  @Test
  void parsesAunitXml(@TempDir Path directory) throws IOException {
    Path report = write(directory, "aunit.xml", """
      <TestRun elapsed="0.995880">
        <Statistics>
          <Tests>4</Tests>
          <FailuresTotal>3</FailuresTotal>
          <Failures>2</Failures>
          <Errors>1</Errors>
        </Statistics>
      </TestRun>
      """);

    assertThat(parser.parse(report)).isEqualTo(new GnattestResults(4, 2, 1, 0, 996));
  }

  @Test
  void rejectsReportsWithoutResults(@TempDir Path directory) throws IOException {
    Path report = write(directory, "empty.txt", "GNATtest execution started\n");

    assertThatIOException().isThrownBy(() -> parser.parse(report))
      .withMessageContaining("no GNATtest results");
  }

  @Test
  void rejectsDoctypeDeclarations(@TempDir Path directory) throws IOException {
    Path report = write(directory, "unsafe.xml", """
      <!DOCTYPE testsuites [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
      <testsuites tests="1" failures="0" errors="0" skipped="0" time="0">&xxe;</testsuites>
      """);

    assertThatIOException().isThrownBy(() -> parser.parse(report))
      .withMessageContaining("Invalid GNATtest XML");
  }

  private static Path write(Path directory, String name, String contents) throws IOException {
    return Files.writeString(directory.resolve(name), contents);
  }
}
