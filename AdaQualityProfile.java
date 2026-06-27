/*
 * Copyright (C) 2026 Spazio IT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a a copy of the License at
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
