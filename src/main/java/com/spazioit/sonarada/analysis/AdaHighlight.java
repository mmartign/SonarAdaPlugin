package com.spazioit.sonarada.analysis;

public record AdaHighlight(
  int line,
  int startOffset,
  int endOffset,
  AdaHighlightKind kind
) {
}
