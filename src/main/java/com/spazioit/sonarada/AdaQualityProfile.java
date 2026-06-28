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

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

public final class AdaQualityProfile implements BuiltInQualityProfilesDefinition {

  public static final String PROFILE_NAME = "Ada Recommended";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(PROFILE_NAME, AdaLanguage.KEY)
      .setDefault(true);

    for (AdaRule rule : AdaRule.values()) {
      if (rule.activatedByDefault()) {
        profile.activateRule(AdaRulesDefinition.REPOSITORY_KEY, rule.key());
      }
    }

    profile.done();
  }
}
