package com.spazioit.sonarada.adacontrol;

import java.util.ArrayList;
import java.util.List;

public final class AdaControlArgumentParser {

  public List<String> parse(String arguments) {
    if (arguments == null || arguments.isBlank()) {
      return List.of();
    }

    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    Character quote = null;
    boolean escaped = false;

    for (int index = 0; index < arguments.length(); index++) {
      char ch = arguments.charAt(index);
      if (escaped) {
        current.append(ch);
        escaped = false;
      } else if (ch == '\\') {
        escaped = true;
      } else if (quote != null) {
        if (ch == quote) {
          quote = null;
        } else {
          current.append(ch);
        }
      } else if (ch == '\'' || ch == '"') {
        quote = ch;
      } else if (Character.isWhitespace(ch)) {
        addCurrent(result, current);
      } else {
        current.append(ch);
      }
    }

    if (escaped) {
      current.append('\\');
    }
    addCurrent(result, current);
    return List.copyOf(result);
  }

  private static void addCurrent(List<String> result, StringBuilder current) {
    if (current.length() > 0) {
      result.add(current.toString());
      current.setLength(0);
    }
  }
}
