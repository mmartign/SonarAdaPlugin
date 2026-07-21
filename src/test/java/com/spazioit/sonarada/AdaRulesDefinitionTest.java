/*
 * Copyright (C) 2026 Spazio IT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.spazioit.sonarada;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.rule.RulesDefinition;

class AdaRulesDefinitionTest {

  @Test
  void registersEveryRuleWithAnHtmlDescription() {
    RulesDefinition.Context context = new RulesDefinition.Context();

    new AdaRulesDefinition().define(context);

    RulesDefinition.Repository repository = context.repository(AdaRulesDefinition.REPOSITORY_KEY);
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(AdaRule.values().length);
    assertThat(repository.rules())
      .allSatisfy(rule -> assertThat(rule.htmlDescription()).isNotBlank());
  }
}
