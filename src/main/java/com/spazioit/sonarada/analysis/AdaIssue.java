package com.spazioit.sonarada.analysis;

public record AdaIssue(
  String ruleKey,
  int line,
  int startOffset,
  int endOffset,
  String message
) {
}
