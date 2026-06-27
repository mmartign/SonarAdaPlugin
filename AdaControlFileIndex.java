package com.spazioit.sonarada.adacontrol;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;

@SuppressWarnings("deprecation")
final class AdaControlFileIndex {

  private final Map<String, InputFile> byAbsolutePath = new HashMap<>();
  private final Map<String, InputFile> byRelativePath = new HashMap<>();
  private final Map<String, InputFile> byFileName = new HashMap<>();
  private final Map<String, Boolean> ambiguousFileNames = new HashMap<>();
  private final Path baseDir;

  AdaControlFileIndex(Path baseDir, Iterable<InputFile> inputFiles) {
    this.baseDir = baseDir.normalize();
    for (InputFile inputFile : inputFiles) {
      IndexedFile indexedFile = inputFile;
      byAbsolutePath.put(normalizePath(Path.of(indexedFile.absolutePath())), inputFile);
      byRelativePath.put(normalizeTextPath(indexedFile.relativePath()), inputFile);
      String fileName = indexedFile.path().getFileName().toString();
      if (byFileName.containsKey(fileName)) {
        ambiguousFileNames.put(fileName, true);
      } else {
        byFileName.put(fileName, inputFile);
      }
    }
  }

  Optional<InputFile> find(String reportPath) {
    if (reportPath == null || reportPath.isBlank()) {
      return Optional.empty();
    }

    Path candidate = Path.of(reportPath);
    String absoluteKey = normalizePath(candidate.isAbsolute() ? candidate : baseDir.resolve(candidate));
    InputFile absoluteMatch = byAbsolutePath.get(absoluteKey);
    if (absoluteMatch != null) {
      return Optional.of(absoluteMatch);
    }

    InputFile relativeMatch = byRelativePath.get(normalizeTextPath(reportPath));
    if (relativeMatch != null) {
      return Optional.of(relativeMatch);
    }

    String fileName = candidate.getFileName() == null ? reportPath : candidate.getFileName().toString();
    if (!ambiguousFileNames.containsKey(fileName)) {
      return Optional.ofNullable(byFileName.get(fileName));
    }

    return Optional.empty();
  }

  private static String normalizePath(Path path) {
    return normalizeTextPath(path.normalize().toString());
  }

  private static String normalizeTextPath(String path) {
    return path.replace('\\', '/');
  }
}
