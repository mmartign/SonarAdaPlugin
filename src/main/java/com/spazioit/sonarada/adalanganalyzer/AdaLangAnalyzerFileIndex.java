/*
 * Copyright (C) 2026 Spazio IT
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.spazioit.sonarada.adalanganalyzer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;

@SuppressWarnings("deprecation")
final class AdaLangAnalyzerFileIndex {
  private final Map<String, InputFile> paths = new HashMap<>();
  private final Map<String, InputFile> relativePaths = new HashMap<>();
  private final Path baseDir;

  AdaLangAnalyzerFileIndex(Path baseDir, Iterable<InputFile> inputFiles) {
    this.baseDir = baseDir.normalize();
    for (InputFile inputFile : inputFiles) {
      IndexedFile indexed = inputFile;
      paths.put(normalize(indexed.absolutePath()), inputFile);
      paths.put(normalize(indexed.relativePath()), inputFile);
      relativePaths.put(normalize(indexed.relativePath()), inputFile);
    }
  }

  Optional<InputFile> find(String path) {
    if (path == null || path.isBlank()) {
      return Optional.empty();
    }
    InputFile direct = paths.get(normalize(path));
    if (direct != null) {
      return Optional.of(direct);
    }
    Path candidate = Path.of(path);
    InputFile resolved = paths.get(normalize(candidate.isAbsolute() ? candidate : baseDir.resolve(candidate)));
    if (resolved != null) {
      return Optional.of(resolved);
    }

    String normalized = normalize(path);
    return relativePaths.entrySet().stream()
      .filter(entry -> normalized.endsWith("/" + entry.getKey()))
      .map(Map.Entry::getValue)
      .findFirst();
  }

  private static String normalize(Path path) {
    return normalize(path.normalize().toString());
  }

  private static String normalize(String path) {
    return path.replace('\\', '/');
  }
}
