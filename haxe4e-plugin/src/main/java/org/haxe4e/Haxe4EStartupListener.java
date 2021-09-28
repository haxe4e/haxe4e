/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;
import org.haxe4e.navigation.HaxeDependenciesUpdater;

/**
 * @author Sebastian Thomschke
 */
public class Haxe4EStartupListener implements IStartup {

   @Override
   public void earlyStartup() {
      // workaround for "IResourceChangeListener adding at IDE startup" https://www.eclipse.org/forums/index.php/t/87906/
      ResourcesPlugin.getWorkspace().addResourceChangeListener(HaxeDependenciesUpdater.INSTANCE, IResourceChangeEvent.POST_CHANGE);
   }
}
