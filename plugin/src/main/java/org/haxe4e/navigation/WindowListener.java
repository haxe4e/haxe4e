/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Ian Harrigan
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.navigation;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.widget.HaxeBuildFileToolbarContribution;

import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Ian Harrigan
 */
public class WindowListener implements IWindowListener {
   public static final WindowListener INSTANCE = new WindowListener();

   @Override
   public void windowActivated(final IWorkbenchWindow window) {
      if (HaxeBuildFileToolbarContribution.instance != null) {
         HaxeBuildFileToolbarContribution.instance.refresh(UI.getActiveProjectWithNature(window, HaxeProjectNature.NATURE_ID));
      }
   }

   @Override
   public void windowDeactivated(final IWorkbenchWindow window) {
   }

   @Override
   public void windowClosed(final IWorkbenchWindow window) {
   }

   @Override
   public void windowOpened(final IWorkbenchWindow window) {
   }

   public void attach() {
      UI.getWorkbench().addWindowListener(this);
   }

   public void detatch() {
      UI.getWorkbench().removeWindowListener(this);
   }
}
