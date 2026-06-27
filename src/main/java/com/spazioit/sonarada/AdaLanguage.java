/*
 * Copyright (C) 2026 Spazio IT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
