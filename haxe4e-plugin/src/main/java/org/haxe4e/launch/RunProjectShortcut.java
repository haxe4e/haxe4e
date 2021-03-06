/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.prefs.HaxeProjectPreference;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public class RunProjectShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      IProject project = null;
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof IFileEditorInput) {
         project = ((IFileEditorInput) editorInput).getFile().getProject();
      }

      launchProject(project, mode);
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      IProject project = null;
      if (selection instanceof StructuredSelection) {
         final var firstElement = ((StructuredSelection) selection).getFirstElement();
         if (firstElement instanceof IResource) {
            project = ((IResource) firstElement).getProject();
         }
      }

      launchProject(project, mode);
   }

   private void launchProject(final IProject project, final String mode) {
      Args.notNull("project", project);

      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_HAXE_CONFIGURATION_ID);

      try {
         // search most recently launched configs for a matching one
         for (final var launch : launchMgr.getLaunches()) {
            final var cfg = launch.getLaunchConfiguration();
            if (cfg.getType().equals(launchConfigType) //
               && cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName()) //
            ) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // search all created launch configs for a matching one
         for (final var cfg : launchMgr.getLaunchConfigurations(launchConfigType)) {
            if (cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName())) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // create a new launch config
         final var prefs = HaxeProjectPreference.get(project);
         final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName()));
         newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_PROJECT, project.getName());
         final var buildFileToLaunch = prefs.getBuildFile();
         if (buildFileToLaunch != null) {
            newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, buildFileToLaunch.getProjectRelativePath());
         }

         final var altSDK = prefs.getAlternateHaxeSDK();
         if (altSDK != null) {
            newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_HAXE_SDK, altSDK.getName());
         }

         if (Window.OK == DebugUITools.openLaunchConfigurationDialog(UI.getShell(), newLaunchConfig, Constants.LAUNCH_HAXE_GROUP, null)) {
            newLaunchConfig.doSave();
         }
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_CreatingLaunchConfigFailed, Haxe4EPlugin.status().createError(ex), true);
      }
   }
}
