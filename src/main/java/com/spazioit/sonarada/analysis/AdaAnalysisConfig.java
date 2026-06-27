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
