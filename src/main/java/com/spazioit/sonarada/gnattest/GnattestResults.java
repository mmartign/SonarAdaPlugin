/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.gnattest;

record GnattestResults(int tests, int failures, int errors, int skipped, long durationMillis) {

  GnattestResults {
    if (tests < 0 || failures < 0 || errors < 0 || skipped < 0 || durationMillis < 0
      || failures + errors + skipped > tests) {
      throw new IllegalArgumentException("Invalid GNATtest result totals");
    }
  }

  static GnattestResults empty() {
    return new GnattestResults(0, 0, 0, 0, 0);
  }

  GnattestResults plus(GnattestResults other) {
    return new GnattestResults(
      Math.addExact(tests, other.tests),
      Math.addExact(failures, other.failures),
      Math.addExact(errors, other.errors),
      Math.addExact(skipped, other.skipped),
      Math.addExact(durationMillis, other.durationMillis));
  }
}
