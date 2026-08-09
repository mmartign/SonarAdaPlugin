/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal recursive-descent JSON reader for AdaLang Analyzer's JSON and SARIF reports.
 * Objects parse to {@code Map<String, Object>}, arrays to {@code List<Object>}, and
 * scalars to {@code String}, {@code Double}, {@code Boolean}, or {@code null}.
 */
final class AdaLangAnalyzerJson {

  private final String text;
  private int pos;

  private AdaLangAnalyzerJson(String text) {
    this.text = text;
  }

  static Object parse(String text) {
    AdaLangAnalyzerJson parser = new AdaLangAnalyzerJson(text);
    parser.skipWhitespace();
    Object value = parser.readValue();
    parser.skipWhitespace();
    return value;
  }

  static String stringOf(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? "" : String.valueOf(value);
  }

  static int intOf(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value instanceof Number number ? number.intValue() : -1;
  }

  static boolean boolOf(Map<String, Object> map, String key) {
    return Boolean.TRUE.equals(map.get(key));
  }

  @SuppressWarnings("unchecked")
  static List<Object> listOf(Object value) {
    return value instanceof List<?> list ? (List<Object>) list : List.of();
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> mapOf(Object value) {
    return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
  }

  private Object readValue() {
    char c = peek();
    return switch (c) {
      case '{' -> readObject();
      case '[' -> readArray();
      case '"' -> readString();
      case 't', 'f' -> readBoolean();
      case 'n' -> readNull();
      default -> readNumber();
    };
  }

  private Map<String, Object> readObject() {
    Map<String, Object> result = new LinkedHashMap<>();
    expect('{');
    skipWhitespace();
    if (peek() == '}') {
      pos++;
      return result;
    }
    while (true) {
      skipWhitespace();
      String key = readString();
      skipWhitespace();
      expect(':');
      skipWhitespace();
      result.put(key, readValue());
      skipWhitespace();
      char c = next();
      if (c == '}') {
        break;
      }
      if (c != ',') {
        throw new IllegalStateException("Expected ',' or '}' at position " + (pos - 1));
      }
    }
    return result;
  }

  private List<Object> readArray() {
    List<Object> result = new ArrayList<>();
    expect('[');
    skipWhitespace();
    if (peek() == ']') {
      pos++;
      return result;
    }
    while (true) {
      skipWhitespace();
      result.add(readValue());
      skipWhitespace();
      char c = next();
      if (c == ']') {
        break;
      }
      if (c != ',') {
        throw new IllegalStateException("Expected ',' or ']' at position " + (pos - 1));
      }
    }
    return result;
  }

  private String readString() {
    expect('"');
    StringBuilder result = new StringBuilder();
    while (true) {
      char c = next();
      if (c == '"') {
        break;
      }
      if (c == '\\') {
        char escaped = next();
        switch (escaped) {
          case '"' -> result.append('"');
          case '\\' -> result.append('\\');
          case '/' -> result.append('/');
          case 'b' -> result.append('\b');
          case 'f' -> result.append('\f');
          case 'n' -> result.append('\n');
          case 'r' -> result.append('\r');
          case 't' -> result.append('\t');
          case 'u' -> {
            String hex = text.substring(pos, pos + 4);
            pos += 4;
            result.append((char) Integer.parseInt(hex, 16));
          }
          default -> throw new IllegalStateException("Invalid escape \\" + escaped);
        }
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  private Boolean readBoolean() {
    if (text.startsWith("true", pos)) {
      pos += 4;
      return Boolean.TRUE;
    }
    if (text.startsWith("false", pos)) {
      pos += 5;
      return Boolean.FALSE;
    }
    throw new IllegalStateException("Invalid literal at position " + pos);
  }

  private Object readNull() {
    if (text.startsWith("null", pos)) {
      pos += 4;
      return null;
    }
    throw new IllegalStateException("Invalid literal at position " + pos);
  }

  private Double readNumber() {
    int start = pos;
    while (pos < text.length() && "-+.eE0123456789".indexOf(text.charAt(pos)) >= 0) {
      pos++;
    }
    if (pos == start) {
      throw new IllegalStateException("Invalid number at position " + pos);
    }
    return Double.parseDouble(text.substring(start, pos));
  }

  private void skipWhitespace() {
    while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
      pos++;
    }
  }

  private char peek() {
    if (pos >= text.length()) {
      throw new IllegalStateException("Unexpected end of JSON input");
    }
    return text.charAt(pos);
  }

  private char next() {
    char c = peek();
    pos++;
    return c;
  }

  private void expect(char expected) {
    char actual = next();
    if (actual != expected) {
      throw new IllegalStateException("Expected '" + expected + "' but found '" + actual + "' at position " + (pos - 1));
    }
  }
}
