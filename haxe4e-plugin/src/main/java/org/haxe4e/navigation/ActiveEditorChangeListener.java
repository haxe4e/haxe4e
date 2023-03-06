/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Ian Harrigan
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.navigation;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.widget.HaxeBuildFileToolbarContribution;

import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Ian Harrigan
 */
public final class ActiveEditorChangeListener implements IPartListener2 {
   public static final ActiveEditorChangeListener INSTANCE = new ActiveEditorChangeListener();

   public void attach() {
      for (final var window : UI.getWorkbench().getWorkbenchWindows()) {
         window.getPartService().addPartListener(this);
      }
   }

   public void detach() {
      for (final var window : UI.getWorkbench().getWorkbenchWindows()) {
         window.getPartService().removePartListener(this);
      }
   }

   @Override
   public void partBroughtToTop(final IWorkbenchPartReference partRef) {
      if (HaxeBuildFileToolbarContribution.instance != null) {
         HaxeBuildFileToolbarContribution.instance.refresh(UI.getActiveProjectWithNature(partRef, HaxeProjectNature.NATURE_ID));
      }
   }
}
