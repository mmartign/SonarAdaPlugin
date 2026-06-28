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
package com.spazioit.sonarada;

import java.util.Arrays;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Language;

public final class AdaLanguage implements Language {

  public static final String KEY = "ada";
  public static final String NAME = "Ada";

  private final Configuration configuration;

  public AdaLanguage(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String[] getFileSuffixes() {
    String configured = configuration.get(AdaProperties.FILE_SUFFIXES_KEY).orElse(AdaProperties.DEFAULT_FILE_SUFFIXES);
    String[] suffixes = Arrays.stream(configured.split(","))
      .map(String::trim)
      .filter(suffix -> !suffix.isEmpty())
      .map(AdaLanguage::normalizeSuffix)
      .distinct()
      .toArray(String[]::new);
    return suffixes.length == 0 ? AdaProperties.DEFAULT_FILE_SUFFIXES_ARRAY : suffixes;
  }

  private static String normalizeSuffix(String suffix) {
    return suffix.startsWith(".") ? suffix : "." + suffix;
  }
}
