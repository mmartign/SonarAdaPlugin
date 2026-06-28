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

import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RulesDefinition;

public final class AdaRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "ada";
  public static final String REPOSITORY_NAME = "Ada Analyzer";

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, AdaLanguage.KEY)
      .setName(REPOSITORY_NAME);

    for (AdaRule adaRule : AdaRule.values()) {
      NewRule rule = repository.createRule(adaRule.key());
      rule
        .setName(adaRule.ruleName())
        .addDescriptionSection(RuleDescriptionSection.builder()
          .sectionKey("default")
          .htmlContent("<p>" + escapeHtml(adaRule.description()) + "</p>")
          .build())
        .setType(adaRule.type())
        .setSeverity(adaRule.severity())
        .setStatus(RuleStatus.READY)
        .setCleanCodeAttribute(adaRule.cleanCodeAttribute())
        .addDefaultImpact(adaRule.softwareQuality(), adaRule.impactSeverity())
        .setDebtRemediationFunction(rule.debtRemediationFunctions().constantPerIssue(adaRule.remediationCost()))
        .setActivatedByDefault(adaRule.activatedByDefault())
        .addTags(adaRule.tags());

      for (AdaRuleParam param : adaRule.params()) {
        rule.createParam(param.key())
          .setName(param.name())
          .setDescription(param.description())
          .setDefaultValue(param.defaultValue())
          .setType(param.type());
      }
    }

    repository.done();
  }

  private static String escapeHtml(String value) {
    return value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;");
  }
}
