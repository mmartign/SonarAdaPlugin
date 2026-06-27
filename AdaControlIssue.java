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
package com.spazioit.sonarada.adacontrol;

import java.util.Locale;

public record AdaControlIssue(
  String file,
  int line,
  int column,
  String key,
  String label,
  String rule,
  String message
) {

  public String ruleId() {
    String source = rule.isBlank() ? key : rule + ":" + label;
    String sanitized = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.:-]+", "_");
    return sanitized.isBlank() ? "adacontrol" : sanitized;
  }

  public String sonarMessage() {
    StringBuilder builder = new StringBuilder();
    builder.append("AdaControl");
    if (!key.isBlank()) {
      builder.append(" ").append(key);
    }
    if (!label.isBlank()) {
      builder.append(" [").append(label).append("]");
    }
    if (!rule.isBlank()) {
      builder.append(" ").append(rule);
    }
    if (!message.isBlank()) {
      builder.append(": ").append(message);
    }
    return builder.toString();
  }
}
