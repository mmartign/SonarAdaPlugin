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

import org.sonar.api.Plugin;

public final class AdaPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtensions(AdaProperties.definitions());
    context.addExtensions(
      AdaLanguage.class,
      AdaRulesDefinition.class,
      AdaQualityProfile.class,
      AdaSensor.class,
      com.spazioit.sonarada.adacontrol.AdaControlSensor.class
    );
  }
}
