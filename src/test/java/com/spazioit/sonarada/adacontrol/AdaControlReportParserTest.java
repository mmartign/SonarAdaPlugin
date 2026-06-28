/*
 * Copyright (C) 2026 Spazio IT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.spazioit.sonarada.adacontrol;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdaControlReportParserTest {

  private final AdaControlReportParser parser = new AdaControlReportParser();

  @Test
  void parsesAdaControlCsvLine() {
    Optional<AdaControlIssue> parsed = parser.parseLine("\"src/demo.adb\",\"12\",\"4\",\"Error\",\"No_Goto\",\"STATEMENTS\",\"use of goto\"");

    assertThat(parsed).isPresent();
    AdaControlIssue issue = parsed.orElseThrow();
    assertThat(issue.file()).isEqualTo("src/demo.adb");
    assertThat(issue.line()).isEqualTo(12);
    assertThat(issue.column()).isEqualTo(4);
    assertThat(issue.key()).isEqualTo("Error");
    assertThat(issue.label()).isEqualTo("No_Goto");
    assertThat(issue.rule()).isEqualTo("STATEMENTS");
    assertThat(issue.message()).isEqualTo("use of goto");
    assertThat(issue.ruleId()).isEqualTo("statements:no_goto");
  }

  @Test
  void parsesAdaControlCsvxLine() {
    Optional<AdaControlIssue> parsed = parser.parseLine("\"src/demo.adb\";\"3\";\"9\";\"Found\";\"Search\";\"USAGE\";\"not called\"");

    assertThat(parsed).isPresent();
    assertThat(parsed.orElseThrow().message()).isEqualTo("not called");
  }

  @Test
  void skipsHeaderAndMalformedLines() {
    assertThat(parser.parseLine("\"file\",\"line\",\"column\",\"key\",\"label\",\"rule\",\"message\"")).isEmpty();
    assertThat(parser.parseLine("Counts summary:")).isEmpty();
    assertThat(parser.parseLine("\"src/demo.adb\",\"x\",\"4\",\"Error\",\"Label\",\"RULE\",\"message\"")).isEmpty();
  }

  @Test
  void acceptsAdaControlLegacyMissingOpeningQuote() {
    Optional<AdaControlIssue> parsed = parser.parseLine("src/demo.adb\",\"2\",\"4\",\"Error\",\"Label\",\"DECLARATIONS\",\"message\"");

    assertThat(parsed).isPresent();
    assertThat(parsed.orElseThrow().file()).isEqualTo("src/demo.adb");
  }
}
