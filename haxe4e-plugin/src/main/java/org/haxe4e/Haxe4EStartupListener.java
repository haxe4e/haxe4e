/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;
import org.haxe4e.navigation.ActiveEditorChangeListener;
import org.haxe4e.navigation.HaxeDependenciesUpdater;
import org.haxe4e.project.HaxeProjectNature;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public class Haxe4EStartupListener implements IStartup {

   @Override
   public void earlyStartup() {
      // workaround for "IResourceChangeListener adding at IDE startup" https://www.eclipse.org/forums/index.php/t/87906/
      ResourcesPlugin.getWorkspace().addResourceChangeListener(HaxeDependenciesUpdater.INSTANCE, IResourceChangeEvent.POST_CHANGE);

      // refresh dependencies when workbench first starts
      HaxeDependenciesUpdater.INSTANCE.onProjectsConfigChanged(Projects.getOpenProjectsWithNature(HaxeProjectNature.NATURE_ID).toList());

      ActiveEditorChangeListener.INSTANCE.attach();
   }
}
