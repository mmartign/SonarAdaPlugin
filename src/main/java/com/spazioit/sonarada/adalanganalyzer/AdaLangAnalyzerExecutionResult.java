/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

record AdaLangAnalyzerExecutionResult(int exitCode, String output, boolean timedOut) {
}
