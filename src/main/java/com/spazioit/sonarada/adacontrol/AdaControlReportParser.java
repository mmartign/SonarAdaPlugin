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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AdaControlReportParser {

  public List<AdaControlIssue> parse(Path reportPath) throws IOException {
    List<AdaControlIssue> issues = new ArrayList<>();
    for (String line : Files.readAllLines(reportPath, StandardCharsets.UTF_8)) {
      parseLine(line).ifPresent(issues::add);
    }
    return List.copyOf(issues);
  }

  public java.util.Optional<AdaControlIssue> parseLine(String line) {
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

    return java.util.Optional.of(new AdaControlIssue(
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
