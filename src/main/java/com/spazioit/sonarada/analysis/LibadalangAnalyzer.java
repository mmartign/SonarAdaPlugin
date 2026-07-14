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
package com.spazioit.sonarada.analysis;

import com.adacore.libadalang.Libadalang.*;
import com.spazioit.sonarada.AdaRule;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An analyzer that uses libadalang to perform a full syntactic and
 * semantic analysis of Ada source code.
 */
public final class LibadalangAnalyzer {

  private static final Pattern TRAILING_WHITESPACE = Pattern.compile("[ \\t]+$");
  private static final Pattern TODO_COMMENT = Pattern.compile("(?i)\\b(TODO|FIXME)\\b");
  private static final Set<String> KEYWORDS = Set.of(
    "abort", "abs", "abstract", "accept", "access", "aliased", "all", "and", "array", "at",
    "begin", "body", "case", "constant", "declare", "delay", "delta", "digits", "do", "else",
    "elsif", "end", "entry", "exception", "exit", "for", "function", "generic", "goto", "if", "in",
    "interface", "is", "limited", "loop", "mod", "new", "not", "of", "or", "others", "out", "overriding",
    "package", "parallel", "private", "procedure", "protected", "raise", "range", "record", "rem", "renames",
    "requeue", "return", "reverse", "select", "separate", "some", "subtype", "synchronized", "tagged", "task",
    "terminate", "then", "type", "until", "use", "when", "while", "with", "xor"
  );

  public AdaAnalysis analyze(String source, AdaAnalysisConfig config) {
    try (AnalysisContext context = AnalysisContext.create()) {
      AnalysisUnit unit = context.getUnitFromBuffer(source, "file.adb", "UTF-8", GrammarRule.COMPILATION_RULE);

      AdaMetrics metrics = computeMetrics(unit);
      List<AdaIssue> issues = new ArrayList<>();
      List<AdaHighlight> highlights = generateHighlights(unit);
      List<AdaCpdToken> cpdTokens = generateCpdTokens(unit);

      checkRules(unit, source, config, issues, metrics);

      return new AdaAnalysis(metrics, issues, highlights, cpdTokens);
    }
  }

  private static AdaMetrics computeMetrics(AnalysisUnit unit) {
    Set<Integer> codeLines = new HashSet<>();
    Set<Integer> commentLines = new HashSet<>();
    for (Token token : tokens(unit)) {
      if (token.getKind() == TokenKind.ADA_COMMENT) {
        commentLines.add(token.getSourceLocationRange().start.line);
      } else if (!token.isTrivia()) {
        codeLines.add(token.getSourceLocationRange().start.line);
      }
    }

    List<AdaNode> nodes = nodes(unit.getRoot());
    int lines = unit.getLastToken().getSourceLocationRange().end.line;
    int statements = (int) nodes.stream().filter(NullStmt.class::isInstance).count(); // A rough approximation
    int functions = (int) nodes.stream().filter(SubpBody.class::isInstance).count();
    int complexity = computeComplexity(nodes);

    return new AdaMetrics(lines, codeLines.size(), commentLines.size(), statements, functions, complexity);
  }

  private static int computeComplexity(List<AdaNode> nodes) {
    int complexity = 1;
    for (AdaNode node : nodes) {
      if (node instanceof IfStmt || node instanceof CaseStmt || node instanceof ForLoopStmt || node instanceof WhileLoopStmt ||
          node instanceof IfExpr || node instanceof CaseExpr) {
        complexity++;
      } else if (node instanceof ElsifStmtPart || node instanceof ElsifExprPart) {
        complexity++;
      } else if (node instanceof OpAnd || node instanceof OpAndThen || node instanceof OpOr || node instanceof OpOrElse) {
        complexity++;
      } else if (node instanceof ExceptionHandler) {
        complexity++;
      } else if (node instanceof GotoStmt || node instanceof ExitStmt || node instanceof ReturnStmt || node instanceof RaiseStmt || node instanceof RaiseExpr) {
        // These don't always add to complexity in the standard model, but can be counted
      }
    }
    return complexity;
  }

  private static List<AdaHighlight> generateHighlights(AnalysisUnit unit) {
    List<AdaHighlight> highlights = new ArrayList<>();
    boolean expectPragmaName = false;
    for (Token token : tokens(unit)) {
      String text = token.getText();
      String kind = token.getKind().name;

      if (token.getKind() == TokenKind.ADA_COMMENT) {
        addHighlight(highlights, token, AdaHighlightKind.COMMENT);
      } else if (token.isTrivia()) {
        continue;
      } else if ("pragma".equalsIgnoreCase(text)) {
        addHighlight(highlights, token, AdaHighlightKind.PRAGMA);
        expectPragmaName = true;
      } else if (expectPragmaName) {
        // A pragma name immediately follows the pragma keyword. Highlighting
        // the two tokens separately avoids overlapping SonarQube ranges while
        // still rendering the complete pragma introducer as an annotation.
        addHighlight(highlights, token, AdaHighlightKind.PRAGMA);
        expectPragmaName = false;
      } else if (isStringLiteral(kind)) {
        addHighlight(highlights, token, AdaHighlightKind.STRING);
      } else if (isConstantLiteral(kind, text)) {
        addHighlight(highlights, token, AdaHighlightKind.CONSTANT);
      } else if (KEYWORDS.contains(text.toLowerCase(Locale.ROOT))) {
        addHighlight(highlights, token, AdaHighlightKind.KEYWORD);
      }
    }
    return highlights;
  }

  private static boolean isStringLiteral(String tokenKind) {
    return "String".equals(tokenKind) || tokenKind.startsWith("Format_String_");
  }

  private static boolean isConstantLiteral(String tokenKind, String tokenText) {
    return "Integer".equals(tokenKind)
      || "Decimal".equals(tokenKind)
      || "Char".equals(tokenKind)
      || "true".equalsIgnoreCase(tokenText)
      || "false".equalsIgnoreCase(tokenText)
      || "null".equalsIgnoreCase(tokenText);
  }

  private static void addHighlight(List<AdaHighlight> highlights, Token token, AdaHighlightKind kind) {
    highlights.add(new AdaHighlight(
      token.getSourceLocationRange().start.line,
      token.getSourceLocationRange().start.column - 1,
      token.getSourceLocationRange().end.column - 1,
      kind
    ));
  }

  private static List<AdaCpdToken> generateCpdTokens(AnalysisUnit unit) {
    List<AdaCpdToken> cpdTokens = new ArrayList<>();
    for (Token token : tokens(unit)) {
      if (token.isTrivia()) {
        continue;
      }
      String image = switch (token.getKind()) {
        case ADA_INTEGER, ADA_DECIMAL -> "$NUMBER";
        default -> token.getText().toLowerCase(Locale.ROOT);
      };
      cpdTokens.add(new AdaCpdToken(
        image,
        token.getSourceLocationRange().start.line,
        token.getSourceLocationRange().start.column - 1,
        token.getSourceLocationRange().end.column - 1
      ));
    }
    return cpdTokens;
  }

  private void checkRules(AnalysisUnit unit, String source, AdaAnalysisConfig config, List<AdaIssue> issues, AdaMetrics metrics) {
    checkLineBasedRules(source, config, issues);
    checkNodeBasedRules(unit, config, issues);
    checkFileThresholds(unit, metrics, config, issues);
  }

  private void checkLineBasedRules(String source, AdaAnalysisConfig config, List<AdaIssue> issues) {
    String[] lines = source.split("\\r\\n|\\r|\\n", -1);
    for (int i = 0; i < lines.length; i++) {
      int lineNumber = i + 1;
      String lineText = lines[i];

      if (config.isActive(AdaRule.LINE_LENGTH)) {
        int maximum = config.intParam(AdaRule.LINE_LENGTH, "maximum");
        if (lineText.length() > maximum) {
          issues.add(new AdaIssue(AdaRule.LINE_LENGTH.key(), lineNumber, maximum, lineText.length(), "Split this " + lineText.length() + "-character line so it does not exceed " + maximum + " characters."));
        }
      }

      if (config.isActive(AdaRule.TAB_CHARACTER)) {
        int tab = lineText.indexOf('\t');
        if (tab >= 0) {
          issues.add(new AdaIssue(AdaRule.TAB_CHARACTER.key(), lineNumber, tab, tab + 1, "Replace this tab character with spaces."));
        }
      }

      if (config.isActive(AdaRule.TRAILING_WHITESPACE)) {
        Matcher trailing = TRAILING_WHITESPACE.matcher(lineText);
        if (trailing.find()) {
          issues.add(new AdaIssue(AdaRule.TRAILING_WHITESPACE.key(), lineNumber, trailing.start(), trailing.end(), "Remove this trailing whitespace."));
        }
      }
    }
  }

  private void checkNodeBasedRules(AnalysisUnit unit, AdaAnalysisConfig config, List<AdaIssue> issues) {
    for (AdaNode node : nodes(unit.getRoot())) {
      if (config.isActive(AdaRule.GOTO_STATEMENT) && node instanceof GotoStmt) {
        addIssue(issues, AdaRule.GOTO_STATEMENT, node, "Replace this goto with structured control flow.");
      }

      if (config.isActive(AdaRule.PRAGMA_SUPPRESS) && node instanceof PragmaNode pragma) {
        if ("suppress".equalsIgnoreCase(pragma.fId().getText())) {
          addIssue(issues, AdaRule.PRAGMA_SUPPRESS, pragma, "Remove this pragma Suppress or justify it outside the main quality profile.");
        }
      }

      if (config.isActive(AdaRule.PACKAGE_USE_CLAUSE) && node instanceof UsePackageClause) {
        addIssue(issues, AdaRule.PACKAGE_USE_CLAUSE, node, "Prefer explicit package qualification instead of a package use clause.");
      }

      if (config.isActive(AdaRule.SWALLOWED_EXCEPTION) && node instanceof ExceptionHandler handler) {
        boolean hasOthers = containsInstance(handler.fHandledExceptions(), OthersDesignator.class);
        boolean hasNullStmt = containsInstance(handler.fStmts(), NullStmt.class);
        if (hasOthers && hasNullStmt) {
          addIssue(issues, AdaRule.SWALLOWED_EXCEPTION, handler, "Handle this exception, log it, or re-raise it instead of silently ignoring it.");
        }
      }

      if (config.isActive(AdaRule.NO_ADDRESS_ATTRIBUTE) && node instanceof AttributeRef attr) {
        if ("address".equalsIgnoreCase(attr.fAttribute().getText())) {
          addIssue(issues, AdaRule.NO_ADDRESS_ATTRIBUTE, node, "Avoid using the 'Address' attribute.");
        }
      }
    }

    if (config.isActive(AdaRule.TODO_COMMENT)) {
      for (Token token : tokens(unit)) {
        if (token.getKind() == TokenKind.ADA_COMMENT) {
          Matcher todo = TODO_COMMENT.matcher(token.getText());
          if (todo.find()) {
            int line = token.getSourceLocationRange().start.line;
            int startCol = token.getSourceLocationRange().start.column - 1 + todo.start();
            int endCol = startCol + todo.group(1).length();
            issues.add(new AdaIssue(AdaRule.TODO_COMMENT.key(), line, startCol, endCol, "Resolve this " + todo.group(1).toUpperCase(Locale.ROOT) + " comment."));
          }
        }
      }
    }
  }

  private void checkFileThresholds(AnalysisUnit unit, AdaMetrics metrics, AdaAnalysisConfig config, List<AdaIssue> issues) {
    if (config.isActive(AdaRule.FILE_LENGTH)) {
      int maximum = config.intParam(AdaRule.FILE_LENGTH, "maximum");
      if (metrics.lines() > maximum) {
        int line = Math.min(metrics.lines(), maximum + 1);
        issues.add(new AdaIssue(
          AdaRule.FILE_LENGTH.key(),
          line,
          0,
          1,
          "This Ada file has " + metrics.lines() + " lines, which is greater than the allowed " + maximum + " lines."
        ));
      }
    }

    if (config.isActive(AdaRule.FILE_COMPLEXITY)) {
      int maximum = config.intParam(AdaRule.FILE_COMPLEXITY, "maximum");
      if (metrics.complexity() > maximum) {
        int line = firstCodeLine(unit);
        issues.add(new AdaIssue(
          AdaRule.FILE_COMPLEXITY.key(),
          line,
          0,
          1,
          "This Ada file has cyclomatic complexity " + metrics.complexity() + ", which is greater than the allowed " + maximum + "."
        ));
      }
    }
  }

  private static void addIssue(List<AdaIssue> issues, AdaRule rule, AdaNode node, String message) {
    Token start = node.tokenStart();
    Token end = node.tokenEnd();
    if (!start.isNone() && !end.isNone()) {
      issues.add(new AdaIssue(
        rule.key(),
        start.getSourceLocationRange().start.line,
        start.getSourceLocationRange().start.column - 1,
        end.getSourceLocationRange().end.column - 1,
        message
      ));
    }
  }

  private static int firstCodeLine(AnalysisUnit unit) {
    return tokens(unit).stream()
      .filter(token -> !token.isTrivia())
      .mapToInt(token -> token.getSourceLocationRange().start.line)
      .findFirst()
      .orElse(1);
  }

  private static List<Token> tokens(AnalysisUnit unit) {
    List<Token> result = new ArrayList<>();
    for (Token token = unit.getFirstToken(); !token.isNone(); token = token.next()) {
      result.add(token);
    }
    return result;
  }

  private static List<AdaNode> nodes(AdaNode root) {
    List<AdaNode> result = new ArrayList<>();
    Deque<AdaNode> pending = new ArrayDeque<>();
    pending.push(root);
    while (!pending.isEmpty()) {
      AdaNode node = pending.pop();
      result.add(node);
      AdaNode[] children = node.children();
      for (int i = children.length - 1; i >= 0; i--) {
        pending.push(children[i]);
      }
    }
    return result;
  }

  private static boolean containsInstance(Iterable<? extends AdaNode> nodes, Class<?> type) {
    for (AdaNode node : nodes) {
      if (type.isInstance(node)) {
        return true;
      }
    }
    return false;
  }
}
