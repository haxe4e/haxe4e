/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;
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
public class RunHxmlFileShortcut implements ILaunchShortcut {

   @Override
   public void launch(final IEditorPart editor, final String mode) {
      final var editorInput = editor.getEditorInput();
      if (editorInput instanceof FileEditorInput) {
         final var fileInput = (FileEditorInput) editorInput;
         launchHxmlFile(fileInput.getFile(), mode);
      }
   }

   @Override
   public void launch(final ISelection selection, final String mode) {
      if (selection instanceof IStructuredSelection) {
         final var firstElement = ((IStructuredSelection) selection).getFirstElement();
         if (firstElement instanceof IFile) {
            final var file = (IFile) firstElement;
            launchHxmlFile(file, mode);
         }
      }
   }

   private void launchHxmlFile(final IFile hxmlFile, final String mode) {
      Args.notNull("hxmlFile", hxmlFile);

      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_HAXE_CONFIGURATION_ID);

      final var project = hxmlFile.getProject();
      final var hxmlFilePath = hxmlFile.getFullPath().makeRelativeTo(project.getFullPath()).toPortableString();
      try {
         // use an existing launch config if available
         for (final ILaunchConfiguration cfg : launchMgr.getLaunchConfigurations(launchConfigType)) {
            if (cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName()) //
               && cfg.getAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, "").equalsIgnoreCase(hxmlFilePath) //
            ) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // create a new launch config
         final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName() + " ("
            + hxmlFile.getName() + ")"));
         newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_PROJECT, project.getName());
         newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, hxmlFilePath);
         final var prefs = HaxeProjectPreference.get(project);
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
