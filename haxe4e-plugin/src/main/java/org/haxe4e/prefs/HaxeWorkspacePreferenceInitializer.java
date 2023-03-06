/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeWorkspacePreferenceInitializer extends AbstractPreferenceInitializer {

   @Override
   public void initializeDefaultPreferences() {
      HaxeWorkspacePreference.STORE.setDefault(HaxeWorkspacePreference.PREFKEY_WARNED_NO_SDK_REGISTERED, false);
   }
}
