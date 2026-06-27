package com.spazioit.sonarada.analysis;

public record AdaCpdToken(
  String image,
  int line,
  int startOffset,
  int endOffset
) {
}
