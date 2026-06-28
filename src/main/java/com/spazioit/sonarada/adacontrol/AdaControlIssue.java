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
