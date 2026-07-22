/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.gnattest;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

final class GnattestReportParser {

  private static final Pattern TEXT_RESULT = Pattern.compile(
    "^\\s*.+?:\\d+:\\d+:\\s+(?:\\([^)]*\\)\\s+)?(?:info|error):\\s+corresponding test (PASSED|FAILED|CRASHED)\\b.*$",
    Pattern.CASE_INSENSITIVE);
  private static final Pattern TEXT_DURATION = Pattern.compile("\\(([0-9]+(?:\\.[0-9]+)?)\\s*s\\)\\s*$");

  GnattestResults parse(Path report) throws IOException {
    String content = Files.readString(report, StandardCharsets.UTF_8);
    if (content.stripLeading().startsWith("<")) {
      return parseXml(content);
    }
    return parseText(content);
  }

  private static GnattestResults parseText(String content) throws IOException {
    int tests = 0;
    int failures = 0;
    int errors = 0;
    double durationSeconds = 0;

    for (String line : content.split("\\R")) {
      Matcher result = TEXT_RESULT.matcher(line);
      if (!result.matches()) {
        continue;
      }
      tests++;
      switch (result.group(1).toUpperCase(java.util.Locale.ROOT)) {
        case "FAILED" -> failures++;
        case "CRASHED" -> errors++;
        default -> {
          // Passed test.
        }
      }
      Matcher duration = TEXT_DURATION.matcher(line);
      if (duration.find()) {
        durationSeconds += parseNonNegativeDouble(duration.group(1), "test duration");
      }
    }

    if (tests == 0) {
      throw new IOException("Report contains no GNATtest results");
    }
    return new GnattestResults(tests, failures, errors, 0, secondsToMillis(durationSeconds));
  }

  private static GnattestResults parseXml(String content) throws IOException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      var builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new DefaultHandler());
      Document document = builder.parse(new InputSource(new StringReader(content)));
      Element root = document.getDocumentElement();
      return switch (root.getTagName()) {
        case "testsuites" -> parseJunitSuites(root);
        case "testsuite" -> parseJunitSuite(root);
        case "TestRun" -> parseAunitXml(root);
        default -> throw new IOException("Unsupported GNATtest XML root element: " + root.getTagName());
      };
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException("Invalid GNATtest XML: " + e.getMessage(), e);
    }
  }

  private static GnattestResults parseJunitSuites(Element root) throws IOException {
    if (root.hasAttribute("tests")) {
      GnattestResults results = junitAttributes(root);
      if (results.tests() == 0) {
        throw new IOException("JUnit report contains no tests");
      }
      return results;
    }
    GnattestResults total = GnattestResults.empty();
    for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && "testsuite".equals(element.getTagName())) {
        total = total.plus(parseJunitSuite(element));
      }
    }
    if (total.tests() == 0) {
      throw new IOException("JUnit report contains no tests");
    }
    return total;
  }

  private static GnattestResults parseJunitSuite(Element suite) throws IOException {
    GnattestResults results = junitAttributes(suite);
    if (results.tests() == 0) {
      throw new IOException("JUnit report contains no tests");
    }
    return results;
  }

  private static GnattestResults junitAttributes(Element element) throws IOException {
    int tests = requiredIntAttribute(element, "tests");
    int failures = optionalIntAttribute(element, "failures");
    int errors = optionalIntAttribute(element, "errors");
    int skipped = optionalIntAttribute(element, "skipped");
    double seconds = optionalDoubleAttribute(element, "time");
    try {
      return new GnattestResults(tests, failures, errors, skipped, secondsToMillis(seconds));
    } catch (IllegalArgumentException e) {
      throw new IOException("Inconsistent JUnit result totals", e);
    }
  }

  private static GnattestResults parseAunitXml(Element root) throws IOException {
    Element statistics = directChild(root, "Statistics");
    int tests = childInt(statistics, "Tests");
    int failures = childInt(statistics, "Failures");
    int errors = childInt(statistics, "Errors");
    double seconds = root.hasAttribute("elapsed")
      ? parseNonNegativeDouble(root.getAttribute("elapsed"), "elapsed")
      : 0;
    try {
      return new GnattestResults(tests, failures, errors, 0, secondsToMillis(seconds));
    } catch (IllegalArgumentException e) {
      throw new IOException("Inconsistent AUnit result totals", e);
    }
  }

  private static Element directChild(Element parent, String name) throws IOException {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && name.equals(element.getTagName())) {
        return element;
      }
    }
    throw new IOException("Missing XML element: " + name);
  }

  private static int childInt(Element parent, String name) throws IOException {
    return parseNonNegativeInt(directChild(parent, name).getTextContent(), name);
  }

  private static int requiredIntAttribute(Element element, String name) throws IOException {
    if (!element.hasAttribute(name)) {
      throw new IOException("Missing XML attribute: " + name);
    }
    return parseNonNegativeInt(element.getAttribute(name), name);
  }

  private static int optionalIntAttribute(Element element, String name) throws IOException {
    return element.hasAttribute(name) ? parseNonNegativeInt(element.getAttribute(name), name) : 0;
  }

  private static double optionalDoubleAttribute(Element element, String name) throws IOException {
    return element.hasAttribute(name) ? parseNonNegativeDouble(element.getAttribute(name), name) : 0;
  }

  private static int parseNonNegativeInt(String value, String field) throws IOException {
    try {
      int parsed = Integer.parseInt(value.trim());
      if (parsed < 0) {
        throw new NumberFormatException("negative");
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw new IOException("Invalid " + field + " value: " + value, e);
    }
  }

  private static double parseNonNegativeDouble(String value, String field) throws IOException {
    try {
      double parsed = Double.parseDouble(value.trim());
      if (!Double.isFinite(parsed) || parsed < 0) {
        throw new NumberFormatException("not a non-negative finite number");
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw new IOException("Invalid " + field + " value: " + value, e);
    }
  }

  private static long secondsToMillis(double seconds) throws IOException {
    double millis = seconds * 1000;
    if (millis > Long.MAX_VALUE) {
      throw new IOException("GNATtest duration is too large");
    }
    return Math.round(millis);
  }
}
