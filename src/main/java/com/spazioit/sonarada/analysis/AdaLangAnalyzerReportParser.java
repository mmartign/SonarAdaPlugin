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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AdaLangAnalyzerReportParser {

  public List<AdaLangAnalyzerIssue> parse(Path reportPath) throws IOException {
    List<AdaLangAnalyzerIssue> issues = new ArrayList<>();
    for (String line : Files.readAllLines(reportPath, StandardCharsets.UTF_8)) {
      parseLine(line).ifPresent(issues::add);
    }
    return List.copyOf(issues);
  }

  public java.util.Optional<AdaLangAnalyzerIssue> parseLine(String line) {
    if (line == null || line.isBlank()) {
      return java.util.Optional.empty();
    }

    char separator = chooseSeparator(line);
    List<String> fields = parseDelimited(line, separator);
    if (fields.size() != 7 && !line.startsWith("\"")) {
      fields = parseDelimited("\"" + line, separator);
    }

    if (fields.size() != 7 || isHeader(fields)) {
      return java.util.Optional.empty();
    }

    Integer lineNumber = parsePositiveInteger(fields.get(1));
    Integer column = parsePositiveInteger(fields.get(2));
    if (lineNumber == null || column == null) {
      return java.util.Optional.empty();
    }

    return java.util.Optional.of(new AdaLangAnalyzerIssue(
      fields.get(0).trim(),
      lineNumber,
      column,
      fields.get(3).trim(),
      fields.get(4).trim(),
      fields.get(5).trim(),
      fields.get(6).trim()
    ));
  }

  private static char chooseSeparator(String line) {
    int commas = count(line, ',');
    int semicolons = count(line, ';');
    return semicolons > commas ? ';' : ',';
  }

  private static int count(String value, char expected) {
    int count = 0;
    boolean quoted = false;
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current == '"') {
        if (quoted && index + 1 < value.length() && value.charAt(index + 1) == '"') {
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (!quoted && current == expected) {
        count++;
      }
    }
    return count;
  }

  private static List<String> parseDelimited(String line, char separator) {
    List<String> fields = new ArrayList<>();
    StringBuilder currentField = new StringBuilder();
    boolean quoted = false;

    for (int index = 0; index < line.length(); index++) {
      char current = line.charAt(index);
      if (current == '"') {
        if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
          currentField.append('"');
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (!quoted && current == separator) {
        fields.add(currentField.toString());
        currentField.setLength(0);
      } else {
        currentField.append(current);
      }
    }

    fields.add(currentField.toString());
    return fields;
  }

  private static boolean isHeader(List<String> fields) {
    return "file".equalsIgnoreCase(fields.get(0).trim())
      && "line".equalsIgnoreCase(fields.get(1).trim())
      && "column".equalsIgnoreCase(fields.get(2).trim());
  }

  private static Integer parsePositiveInteger(String value) {
    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
