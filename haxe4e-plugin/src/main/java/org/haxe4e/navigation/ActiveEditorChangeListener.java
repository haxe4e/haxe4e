/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.widget.HaxeBuildFileToolbarContribution;

import de.sebthom.eclipse.commons.ui.Projects;

/**
 * @author Ian Harrigan
 */
public final class ActiveEditorChangeListener implements IPartListener2 {
   public static final ActiveEditorChangeListener INSTANCE = new ActiveEditorChangeListener();

   @Override
   public void partBroughtToTop(final IWorkbenchPartReference partRef) {
      if (HaxeBuildFileToolbarContribution.instance != null) {
         HaxeBuildFileToolbarContribution.instance.refresh(Projects.getActiveProjectWithNature(partRef, HaxeProjectNature.NATURE_ID));
      }
   }

   public void attach() {
      for (final var window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
         window.getPartService().addPartListener(this);
      }
   }

   public void detatch() {
      for (final var window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
         window.getPartService().removePartListener(this);
      }
   }
}
