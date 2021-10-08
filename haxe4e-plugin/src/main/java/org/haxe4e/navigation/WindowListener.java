/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.util.Projects;
import org.haxe4e.widget.HaxeBuildFileToolbarContribution;

/**
 * @author Ian Harrigan
 */
public class WindowListener implements IWindowListener {
   public static final WindowListener INSTANCE = new WindowListener();

   @Override
   public void windowActivated(final IWorkbenchWindow window) {
      if (HaxeBuildFileToolbarContribution.instance != null) {
         HaxeBuildFileToolbarContribution.instance.refresh(Projects.findProjectFromWindow(window, HaxeProjectNature.NATURE_ID));
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
      PlatformUI.getWorkbench().addWindowListener(this);
   }

   public void detatch() {
      PlatformUI.getWorkbench().removeWindowListener(this);
   }
}
