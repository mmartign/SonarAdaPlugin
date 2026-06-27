/*
 * Copyright (C) 2026 Spazio IT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
