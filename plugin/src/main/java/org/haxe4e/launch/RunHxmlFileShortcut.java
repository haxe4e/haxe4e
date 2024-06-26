/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.buildsystem.HaxeBuildFile;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;

/**
 * @author Sebastian Thomschke
 */
public class RunHxmlFileShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof final FileEditorInput fileInput) {
         launchHxmlFile(new HaxeBuildFile(fileInput.getFile()), mode);
      }
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      if (selection instanceof final IStructuredSelection structuredSelection) {
         final var firstElement = structuredSelection.getFirstElement();
         if (firstElement instanceof final IFile file) {
            launchHxmlFile(new HaxeBuildFile(file), mode);
         }
      }
   }

   private void launchHxmlFile(final HaxeBuildFile hxmlFile, final String mode) {
      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_HAXE_CONFIGURATION_ID);

      final var project = hxmlFile.getProject();
      try {
         // use an existing launch config if available
         for (final var cfg : launchMgr.getLaunchConfigurations(launchConfigType)) {
            if (cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName()) //
                  && cfg.getAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, "").equalsIgnoreCase(hxmlFile.getProjectRelativePath()) //
            ) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // create a new launch config
         final var newLaunchConfig = LaunchConfigurations.create(hxmlFile);
         final String groupId = "debug".equals(mode) ? IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP : Constants.LAUNCH_HAXE_GROUP;
         if (Window.OK == DebugUITools.openLaunchConfigurationDialog(UI.getShell(), newLaunchConfig, groupId, null)) {
            newLaunchConfig.doSave();
         }
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_CreatingLaunchConfigFailed, Haxe4EPlugin.status().createError(ex), true);
      }
   }
}
