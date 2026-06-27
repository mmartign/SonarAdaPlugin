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
package com.spazioit.sonarada.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.spazioit.sonarada.AdaRule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link LibadalangAnalyzer}.
 */
class LibadalangAnalyzerTest {

  private final LibadalangAnalyzer analyzer = new LibadalangAnalyzer();

  /**
   * Verifies that the analyzer correctly detects a range of built-in rule violations
   * from a single piece of Ada source code.
   */
  @Test
  void detectsBuiltInIssuesInAdaCode() {
    String source = """
      procedure Demo is
      begin
         goto Done; -- TODO: remove
         pragma Suppress(All_Checks);
         use Ada.Text_IO;
      <<Done>>
         null;
      exception
         when others => null;
      end Demo;
      """;

    AdaAnalysis analysis = analyzer.analyze(source, AdaAnalysisConfig.allRules());

    assertThat(ruleKeys(analysis)).contains(
      AdaRule.GOTO_STATEMENT.key(),
      AdaRule.PRAGMA_SUPPRESS.key(),
      AdaRule.PACKAGE_USE_CLAUSE.key(),
      AdaRule.SWALLOWED_EXCEPTION.key(),
      AdaRule.TODO_COMMENT.key()
    );
  }

  /**
   * Verifies that rule parameters, such as the maximum line length, are correctly
   * applied during analysis.
   */
  @Test
  void appliesRuleParameters() {
    AdaAnalysisConfig config = new AdaAnalysisConfig(
      Set.of(AdaRule.LINE_LENGTH.key()),
      Map.of(AdaAnalysisConfig.parameterKey(AdaRule.LINE_LENGTH, "maximum"), "10")
    );

    AdaAnalysis analysis = analyzer.analyze("01234567890\nshort\n", config);

    assertThat(analysis.issues())
      .extracting(AdaIssue::ruleKey)
      .containsExactly(AdaRule.LINE_LENGTH.key());
  }

  /**
   * Verifies that the analyzer computes basic code metrics correctly.
   */
  @Test
  void computesBasicMetrics() {
    String source = """
      package body Demo is
         -- one comment line
         procedure P is
         begin
            if Ready then
               null;
            end if;
         end P;
      end Demo;
      """;

    AdaMetrics metrics = analyzer.analyze(source, AdaAnalysisConfig.allRules()).metrics();

    assertThat(metrics.lines()).isEqualTo(9);
    assertThat(metrics.commentLines()).isEqualTo(1);
    assertThat(metrics.functions()).isEqualTo(1);
    assertThat(metrics.complexity()).isEqualTo(2);
    assertThat(metrics.ncloc()).isEqualTo(8);
  }

  /**
   * Helper method to extract rule keys from an analysis result for easier assertions.
   * @param analysis The analysis result.
   * @return A list of rule keys for all found issues.
   */
  private static List<String> ruleKeys(AdaAnalysis analysis) {
    return analysis.issues().stream().map(AdaIssue::ruleKey).toList();
  }
}