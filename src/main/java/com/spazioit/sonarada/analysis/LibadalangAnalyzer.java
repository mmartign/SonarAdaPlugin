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

import com.adacore.libadalang.AdaNode;
import com.adacore.libadalang.AttributeRef;
import com.adacore.libadalang.AnalysisResult;
import com.adacore.libadalang.CaseExpr;
import com.adacore.libadalang.CaseStmt;
import com.adacore.libadalang.Comment;
import com.adacore.libadalang.ExitStmt;
import com.adacore.libadalang.ForLoopStmt;
import com.adacore.libadalang.GotoStmt;
import com.adacore.libadalang.IfExpr;
import com.adacore.libadalang.IfStmt;
import com.adacore.libadalang.IterateStmt;
import com.adacore.libadalang.LangkitCharacter;
import com.adacore.libadalang.LangkitChoiceOthers;
import com.adacore.libadalang.LangkitAnalysisContext;
import com.adacore.libadalang.LangkitAnalysisUnit;
import com.adacore.libadalang.LangkitBaseNode;
import com.adacore.libadalang.LangkitBasicDecl;
import com.adacore.libadalang.LangkitElsifExprPart;
import com.adacore.libadalang.LangkitElsifStmtPart;
import com.adacore.libadalang.LangkitExceptionHandler;
import com.adacore.libadalang.LangkitFileUnitProvider;
import com.adacore.libadalang.LangkitNullStmt;
import com.adacore.libadalang.LangkitSubpBody;
import com.adacore.libadalang.LangkitToken;
import com.adacore.libadalang.Libadalang;
import java.nio.charset.StandardCharsets;
import com.adacore.libadalang.LogicAnd;
import com.adacore.libadalang.LogicOr;
import com.adacore.libadalang.PragmaNode;
import com.adacore.libadalang.RaiseExpr;
import com.adacore.libadalang.RaiseStmt;
import com.adacore.libadalang.ReturnStmt;
import com.adacore.libadalang.SubpBody;
import com.adacore.libadalang.Token;
import com.adacore.libadalang.Trivia;
import com.adacore.libadalang.UsePackageClause;
import com.adacore.libadalang.WhileLoopStmt;
import com.spazioit.sonarada.AdaRule;
import java.util.ArrayList;
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

  public AdaAnalysis analyze(String source, AdaAnalysisConfig config) {
    try (
      LangkitAnalysisContext context = Libadalang.createContext(
        null, null, null, null, StandardCharsets.UTF_8, true
      );
      LangkitFileUnitProvider unitProvider = context.getUnitProvider()
    ) {
      LangkitAnalysisUnit unit = unitProvider.getUnitFromBuffer("file.adb", source, StandardCharsets.UTF_8);

      AdaMetrics metrics = computeMetrics(unit);
      List<AdaIssue> issues = new ArrayList<>();
      List<AdaHighlight> highlights = generateHighlights(unit);
      List<AdaCpdToken> cpdTokens = generateCpdTokens(unit);

      checkRules(unit, source, config, issues, metrics);

      return new AdaAnalysis(metrics, issues, highlights, cpdTokens);
    }
  }

  private static AdaMetrics computeMetrics(LangkitAnalysisUnit unit) {
    int lines = unit.getEndLoc().getLine();
    int ncloc = (int) unit.getTriviaManager().getLinesWithTokens().stream().count();
    int commentLines = (int) unit.getTriviaManager().getLinesWithComments().stream().count();
    int statements = unit.getRoot().stream(LangkitNullStmt.class).toArray().length; // A rough approximation
    int functions = unit.getRoot().stream(LangkitSubpBody.class).toArray().length;
    int complexity = computeComplexity(unit.getRoot());

    return new AdaMetrics(lines, ncloc, commentLines, statements, functions, complexity);
  }

  private static int computeComplexity(AdaNode root) {
    int complexity = 1;
    for (AdaNode node : root.stream()) {
      if (node instanceof IfStmt || node instanceof CaseStmt || node instanceof ForLoopStmt || node instanceof WhileLoopStmt ||
          node instanceof IfExpr || node instanceof CaseExpr) {
        complexity++;
      } else if (node instanceof LangkitElsifStmtPart || node instanceof LangkitElsifExprPart) {
        complexity++;
      } else if (node instanceof LogicAnd || node instanceof LogicOr) {
        complexity++;
      } else if (node instanceof LangkitExceptionHandler) {
        complexity++;
      } else if (node instanceof GotoStmt || node instanceof ExitStmt || node instanceof ReturnStmt || node instanceof RaiseStmt || node instanceof RaiseExpr || node instanceof IterateStmt) {
        // These don't always add to complexity in the standard model, but can be counted
      }
    }
    return complexity;
  }

  private static List<AdaHighlight> generateHighlights(LangkitAnalysisUnit unit) {
    List<AdaHighlight> highlights = new ArrayList<>();
    for (Trivia trivia : unit.getTriviaManager().getTrivia()) {
      if (trivia instanceof Comment) {
        highlights.add(new AdaHighlight(trivia.getStartLoc().getLine(), trivia.getStartLoc().getCol() - 1, trivia.getEndLoc().getCol(), AdaHighlightKind.COMMENT));
      }
    }
    for (Token token : unit.getTokens()) {
      if (token.getKind().equals("StringLit")) {
        highlights.add(new AdaHighlight(token.getStartLoc().getLine(), token.getStartLoc().getCol() - 1, token.getEndLoc().getCol(), AdaHighlightKind.STRING));
      } else if (token.isKeyword()) {
        highlights.add(new AdaHighlight(token.getStartLoc().getLine(), token.getStartLoc().getCol() - 1, token.getEndLoc().getCol(), AdaHighlightKind.KEYWORD));
      }
    }
    return highlights;
  }

  private static List<AdaCpdToken> generateCpdTokens(LangkitAnalysisUnit unit) {
    List<AdaCpdToken> cpdTokens = new ArrayList<>();
    for (Token token : unit.getTokens()) {
      String image = token.getKind().equals("Number") ? "$NUMBER" : token.getText().toLowerCase(Locale.ROOT);
      cpdTokens.add(new AdaCpdToken(image, token.getStartLoc().getLine(), token.getStartLoc().getCol() - 1, token.getEndLoc().getCol()));
    }
    return cpdTokens;
  }

  private void checkRules(LangkitAnalysisUnit unit, String source, AdaAnalysisConfig config, List<AdaIssue> issues, AdaMetrics metrics) {
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

  private void checkNodeBasedRules(LangkitAnalysisUnit unit, AdaAnalysisConfig config, List<AdaIssue> issues) {
    for (AdaNode node : unit.getRoot().stream()) {
      if (config.isActive(AdaRule.GOTO_STATEMENT) && node instanceof GotoStmt) {
        addIssue(issues, AdaRule.GOTO_STATEMENT, node, "Replace this goto with structured control flow.");
      }

      if (config.isActive(AdaRule.PRAGMA_SUPPRESS) && node instanceof PragmaNode pragma) {
        if ("suppress".equalsIgnoreCase(pragma.fPragmaName.getText())) {
          addIssue(issues, AdaRule.PRAGMA_SUPPRESS, pragma, "Remove this pragma Suppress or justify it outside the main quality profile.");
        }
      }

      if (config.isActive(AdaRule.PACKAGE_USE_CLAUSE) && node instanceof UsePackageClause) {
        addIssue(issues, AdaRule.PACKAGE_USE_CLAUSE, node, "Prefer explicit package qualification instead of a package use clause.");
      }

      if (config.isActive(AdaRule.SWALLOWED_EXCEPTION) && node instanceof LangkitExceptionHandler handler) {
        boolean hasOthers = handler.fChoices.stream().anyMatch(c -> c instanceof LangkitChoiceOthers);
        boolean hasNullStmt = handler.fStmts.stream().anyMatch(s -> s instanceof LangkitNullStmt);
        if (hasOthers && hasNullStmt) {
          addIssue(issues, AdaRule.SWALLOWED_EXCEPTION, handler, "Handle this exception, log it, or re-raise it instead of silently ignoring it.");
        }
      }

      if (config.isActive(AdaRule.NO_ADDRESS_ATTRIBUTE) && node instanceof AttributeRef attr) {
        if ("address".equalsIgnoreCase(attr.fAttribute.getText())) {
          addIssue(issues, AdaRule.NO_ADDRESS_ATTRIBUTE, node, "Avoid using the 'Address' attribute.");
        }
      }
    }

    if (config.isActive(AdaRule.TODO_COMMENT)) {
      for (Trivia trivia : unit.getTriviaManager().getTrivia()) {
        if (trivia instanceof Comment) {
          Matcher todo = TODO_COMMENT.matcher(trivia.getText());
          if (todo.find()) {
            int line = trivia.getStartLoc().getLine();
            int startCol = trivia.getStartLoc().getCol() - 1 + todo.start();
            int endCol = startCol + todo.group(1).length();
            issues.add(new AdaIssue(AdaRule.TODO_COMMENT.key(), line, startCol, endCol, "Resolve this " + todo.group(1).toUpperCase(Locale.ROOT) + " comment."));
          }
        }
      }
    }
  }

  private void checkFileThresholds(LangkitAnalysisUnit unit, AdaMetrics metrics, AdaAnalysisConfig config, List<AdaIssue> issues) {
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
    LangkitToken start = node.getTokenStart();
    LangkitToken end = node.getTokenEnd();
    if (start != null && end != null) {
      issues.add(new AdaIssue(
        rule.key(),
        start.getStartLoc().getLine(),
        start.getStartLoc().getCol() - 1,
        end.getEndLoc().getCol(),
        message
      ));
    }
  }

  private static int firstCodeLine(LangkitAnalysisUnit unit) {
    return unit.getTriviaManager().getLinesWithTokens().stream().findFirst().orElse(1);
  }
}