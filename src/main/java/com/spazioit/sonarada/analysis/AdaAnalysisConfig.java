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

import com.spazioit.sonarada.AdaRule;
import com.spazioit.sonarada.AdaRuleParam;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdaAnalysisConfig {

  private final Set<String> activeRuleKeys;
  private final Map<String, String> parameters;

  public AdaAnalysisConfig(Set<String> activeRuleKeys, Map<String, String> parameters) {
    this.activeRuleKeys = Set.copyOf(activeRuleKeys);
    this.parameters = Map.copyOf(parameters);
  }

  public static AdaAnalysisConfig allRules() {
    return new AdaAnalysisConfig(
      Arrays.stream(AdaRule.values()).map(AdaRule::key).collect(Collectors.toSet()),
      new HashMap<>()
    );
  }

  public boolean isActive(AdaRule rule) {
    return activeRuleKeys.contains(rule.key());
  }

  public int intParam(AdaRule rule, String key) {
    AdaRuleParam param = rule.param(key);
    String value = parameters.getOrDefault(parameterKey(rule, key), param.defaultValue());
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return Integer.parseInt(param.defaultValue());
    }
  }

  public static String parameterKey(AdaRule rule, String key) {
    return rule.key() + ":" + key;
  }
}
