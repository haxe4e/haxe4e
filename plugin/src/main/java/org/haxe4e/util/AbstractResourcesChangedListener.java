/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.util;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * @author Sebastian Thomschke
 */
public abstract class AbstractResourcesChangedListener implements IResourceChangeListener {

   public void install() {
      ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
   }

   public void uninstall() {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
   }
}
