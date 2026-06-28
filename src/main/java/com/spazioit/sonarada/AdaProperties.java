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

import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

public final class AdaProperties {

  public static final String CATEGORY = "Ada";
  public static final String FILE_SUFFIXES_KEY = "sonar.ada.file.suffixes";
  public static final String DEFAULT_FILE_SUFFIXES = ".adb,.ads,.ada";
  public static final String[] DEFAULT_FILE_SUFFIXES_ARRAY = {".adb", ".ads", ".ada"};

  public static final String ADACONTROL_ENABLED_KEY = "sonar.ada.adacontrol.enabled";
  public static final String ADACONTROL_EXECUTABLE_KEY = "sonar.ada.adacontrol.executable";
  public static final String ADACONTROL_RULES_FILE_KEY = "sonar.ada.adacontrol.rulesFile";
  public static final String ADACONTROL_PROJECT_FILE_KEY = "sonar.ada.adacontrol.projectFile";
  public static final String ADACONTROL_EXTRA_ARGS_KEY = "sonar.ada.adacontrol.extraArgs";
  public static final String ADACONTROL_REPORT_PATHS_KEY = "sonar.ada.adacontrol.reportPaths";
  public static final String ADACONTROL_TIMEOUT_SECONDS_KEY = "sonar.ada.adacontrol.timeoutSeconds";
  public static final String ADACONTROL_FAIL_ON_ERROR_KEY = "sonar.ada.adacontrol.failOnError";

  private AdaProperties() {
  }

  public static List<PropertyDefinition> definitions() {
    return List.of(
      PropertyDefinition.builder(FILE_SUFFIXES_KEY)
        .name("Ada file suffixes")
        .description("Comma-separated list of file suffixes that should be analyzed as Ada.")
        .category(CATEGORY)
        .defaultValue(DEFAULT_FILE_SUFFIXES)
        .type(PropertyType.STRING)
        .build(),
      PropertyDefinition.builder(ADACONTROL_ENABLED_KEY)
        .name("Run AdaControl")
        .description("Run the external AdaControl analyzer during Ada analysis. AdaControl must be installed on the scanner machine.")
        .category(CATEGORY)
        .subCategory("AdaControl")
        .defaultValue("false")
        .type(PropertyType.BOOLEAN)
        .build(),
      PropertyDefinition.builder(ADACONTROL_EXECUTABLE_KEY)
        .name("AdaControl executable")
        .description("Path to the AdaControl executable. Use an absolute path when adactl is not on PATH.")
        .category(CATEGORY)
        .subCategory("AdaControl")
        .defaultValue("adactl")
        .type(PropertyType.STRING)
        .build(),
      PropertyDefinition.builder(ADACONTROL_RULES_FILE_KEY)
        .name("AdaControl rules file")
        .description("Optional AdaControl .aru command file passed with -f.")
        .category(CATEGORY)
        .subCategory("AdaControl")
        .type(PropertyType.STRING)
        .build(),
      PropertyDefinition.builder(ADACONTROL_PROJECT_FILE_KEY)
        .name("AdaControl project file")
        .description("Optional GNAT .gpr project file passed with -p.")
        .category(CATEGORY)
        .subCategory("AdaControl")
        .type(PropertyType.STRING)
        .build(),
      PropertyDefinition.builder(ADACONTROL_EXTRA_ARGS_KEY)
        .name("AdaControl extra arguments")
        .description("Additional command-line arguments appended after input files, for example ASIS options after --.")
        .category(CATEGORY)
        .subCategory("AdaControl")
        .type(PropertyType.STRING)
        .build(),
      PropertyDefinition.builder(ADACONTROL_REPORT_PATHS_KEY)
        .name("AdaControl report paths")
        .description("Comma-separated paths to pre-generated AdaControl CSV reports to import. Reports should contain file,line,column,key,label,rule,message fields.")
        .category(CATEGORY)
        .subCategory("AdaControl")
        .type(PropertyType.STRING)
        .build(),
      PropertyDefinition.builder(ADACONTROL_TIMEOUT_SECONDS_KEY)
        .name("AdaControl timeout")
        .description("Maximum number of seconds to wait for adactl when running it from the scanner.")
        .category(CATEGORY)
        .subCategory("AdaControl")
        .defaultValue("300")
        .type(PropertyType.INTEGER)
        .build(),
      PropertyDefinition.builder(ADACONTROL_FAIL_ON_ERROR_KEY)
        .name("Fail analysis on AdaControl execution errors")
        .description("Fail the Sonar scan when adactl cannot be started, times out, or exits with an execution error. Violation exit codes do not fail the scan.")
        .category(CATEGORY)
        .subCategory("AdaControl")
        .defaultValue("true")
        .type(PropertyType.BOOLEAN)
        .build()
    );
  }
}
