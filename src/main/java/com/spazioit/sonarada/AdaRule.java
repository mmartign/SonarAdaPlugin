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
package com.spazioit.sonarada;

import java.util.Arrays;
import java.util.List;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;

public enum AdaRule {

  LINE_LENGTH(
    "ADA001",
    "Lines should not be too long",
    "Long lines are hard to read and review. Split the expression, declaration, or comment across several lines.",
    RuleType.CODE_SMELL,
    org.sonar.api.rule.Severity.MINOR,
    SoftwareQuality.MAINTAINABILITY,
    org.sonar.api.issue.impact.Severity.LOW,
    CleanCodeAttribute.FORMATTED,
    "5min",
    true,
    List.of("convention", "formatting"),
    List.of(new AdaRuleParam("maximum", "Maximum line length", "Maximum allowed line length.", "120", RuleParamType.INTEGER))
  ),

  TAB_CHARACTER(
    "ADA002",
    "Tab characters should not be used",
    "Tabs make indentation display depend on editor configuration. Use spaces for consistent Ada formatting.",
    RuleType.CODE_SMELL,
    org.sonar.api.rule.Severity.MINOR,
    SoftwareQuality.MAINTAINABILITY,
    org.sonar.api.issue.impact.Severity.LOW,
    CleanCodeAttribute.FORMATTED,
    "2min",
    true,
    List.of("convention", "formatting"),
    List.of()
  ),

  TRAILING_WHITESPACE(
    "ADA003",
    "Trailing whitespace should not be used",
    "Whitespace at the end of a line creates noisy diffs and should be removed.",
    RuleType.CODE_SMELL,
    org.sonar.api.rule.Severity.INFO,
    SoftwareQuality.MAINTAINABILITY,
    org.sonar.api.issue.impact.Severity.INFO,
    CleanCodeAttribute.FORMATTED,
    "1min",
    true,
    List.of("convention", "formatting"),
    List.of()
  ),

  TODO_COMMENT(
    "ADA004",
    "TODO and FIXME comments should be resolved",
    "TODO and FIXME comments mark unfinished work. Track the work outside the code or resolve it before release.",
    RuleType.CODE_SMELL,
    org.sonar.api.rule.Severity.INFO,
    SoftwareQuality.MAINTAINABILITY,
    org.sonar.api.issue.impact.Severity.INFO,
    CleanCodeAttribute.COMPLETE,
    "5min",
    true,
    List.of("todo"),
    List.of()
  ),

  GOTO_STATEMENT(
    "ADA005",
    "goto statements should not be used",
    "The Ada goto statement makes control flow harder to follow. Prefer structured control flow.",
    RuleType.CODE_SMELL,
    org.sonar.api.rule.Severity.MAJOR,
    SoftwareQuality.MAINTAINABILITY,
    org.sonar.api.issue.impact.Severity.MEDIUM,
    CleanCodeAttribute.LOGICAL,
    "15min",
    true,
    List.of("brain-overload"),
    List.of()
  ),

  PRAGMA_SUPPRESS(
    "ADA006",
    "pragma Suppress should not be used",
    "Suppressing Ada runtime checks can hide constraint, range, overflow, or access errors. Keep checks enabled unless the exception is justified and reviewed.",
    RuleType.BUG,
    org.sonar.api.rule.Severity.CRITICAL,
    SoftwareQuality.RELIABILITY,
    org.sonar.api.issue.impact.Severity.HIGH,
    CleanCodeAttribute.TRUSTWORTHY,
    "30min",
    true,
    List.of("cert", "reliability"),
    List.of()
  ),

  SWALLOWED_EXCEPTION(
    "ADA007",
    "when others handlers should not silently ignore exceptions",
    "A handler such as 'when others => null;' hides all unexpected failures. Handle the exception, log it, or re-raise it.",
    RuleType.BUG,
    org.sonar.api.rule.Severity.MAJOR,
    SoftwareQuality.RELIABILITY,
    org.sonar.api.issue.impact.Severity.MEDIUM,
    CleanCodeAttribute.COMPLETE,
    "20min",
    true,
    List.of("error-handling", "reliability"),
    List.of()
  ),

  PACKAGE_USE_CLAUSE(
    "ADA008",
    "Package use clauses should be avoided",
    "Package-level use clauses can obscure the origin of declarations and increase ambiguity. Prefer explicit package qualification or narrow 'use type' clauses.",
    RuleType.CODE_SMELL,
    org.sonar.api.rule.Severity.MINOR,
    SoftwareQuality.MAINTAINABILITY,
    org.sonar.api.issue.impact.Severity.LOW,
    CleanCodeAttribute.CLEAR,
    "10min",
    true,
    List.of("readability"),
    List.of()
  ),

  FILE_LENGTH(
    "ADA009",
    "Ada files should not be too long",
    "Large Ada files are harder to understand and review. Split unrelated units or responsibilities into smaller files.",
    RuleType.CODE_SMELL,
    org.sonar.api.rule.Severity.MAJOR,
    SoftwareQuality.MAINTAINABILITY,
    org.sonar.api.issue.impact.Severity.MEDIUM,
    CleanCodeAttribute.FOCUSED,
    "20min",
    true,
    List.of("brain-overload"),
    List.of(new AdaRuleParam("maximum", "Maximum file length", "Maximum allowed physical lines in a file.", "300", RuleParamType.INTEGER))
  ),

  FILE_COMPLEXITY(
    "ADA010",
    "Ada files should not be too complex",
    "High cyclomatic complexity indicates too many independent execution paths. Split complex logic into smaller subprograms.",
    RuleType.CODE_SMELL,
    org.sonar.api.rule.Severity.MAJOR,
    SoftwareQuality.MAINTAINABILITY,
    org.sonar.api.issue.impact.Severity.MEDIUM,
    CleanCodeAttribute.FOCUSED,
    "20min",
    true,
    List.of("brain-overload"),
    List.of(new AdaRuleParam("maximum", "Maximum complexity", "Maximum allowed cyclomatic complexity for a file.", "20", RuleParamType.INTEGER))
  ),

  NO_ADDRESS_ATTRIBUTE(
    "ADA011",
    "The 'Address' attribute should not be used",
    "Using the 'Address' attribute can lead to non-portable code and memory safety issues. Prefer safer, higher-level language constructs.",
    RuleType.BUG,
    org.sonar.api.rule.Severity.MAJOR,
    SoftwareQuality.RELIABILITY,
    org.sonar.api.issue.impact.Severity.HIGH,
    CleanCodeAttribute.TRUSTWORTHY,
    "10min",
    true,
    List.of("cert", "portability", "unsafe"),
    List.of()
  )

  ;

  private final String key;
  private final String name;
  private final String description;
  private final RuleType type;
  private final String severity;
  private final SoftwareQuality softwareQuality;
  private final org.sonar.api.issue.impact.Severity impactSeverity;
  private final CleanCodeAttribute cleanCodeAttribute;
  private final String remediationCost;
  private final boolean activatedByDefault;
  private final List<String> tags;
  private final List<AdaRuleParam> params;

  AdaRule(
    String key,
    String name,
    String description,
    RuleType type,
    String severity,
    SoftwareQuality softwareQuality,
    org.sonar.api.issue.impact.Severity impactSeverity,
    CleanCodeAttribute cleanCodeAttribute,
    String remediationCost,
    boolean activatedByDefault,
    List<String> tags,
    List<AdaRuleParam> params
  ) {
    this.key = key;
    this.name = name;
    this.description = description;
    this.type = type;
    this.severity = severity;
    this.softwareQuality = softwareQuality;
    this.impactSeverity = impactSeverity;
    this.cleanCodeAttribute = cleanCodeAttribute;
    this.remediationCost = remediationCost;
    this.activatedByDefault = activatedByDefault;
    this.tags = tags;
    this.params = params;
  }

  public String key() {
    return key;
  }

  public String ruleName() {
    return name;
  }

  public String description() {
    return description;
  }

  public RuleType type() {
    return type;
  }

  public String severity() {
    return severity;
  }

  public SoftwareQuality softwareQuality() {
    return softwareQuality;
  }

  public org.sonar.api.issue.impact.Severity impactSeverity() {
    return impactSeverity;
  }

  public CleanCodeAttribute cleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  public String remediationCost() {
    return remediationCost;
  }

  public boolean activatedByDefault() {
    return activatedByDefault;
  }

  public String[] tags() {
    return tags.toArray(String[]::new);
  }

  public List<AdaRuleParam> params() {
    return params;
  }

  public AdaRuleParam param(String key) {
    return params.stream()
      .filter(param -> param.key().equals(key))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown parameter '" + key + "' for rule " + this.key));
  }

  public static AdaRule byKey(String key) {
    return Arrays.stream(values())
      .filter(rule -> rule.key.equals(key))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown Ada rule: " + key));
  }
}
