/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.haxe4e.Constants;
import org.haxe4e.localization.Messages;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.StatusUtils;
import org.haxe4e.util.ui.Dialogs;
import org.haxe4e.util.ui.UI;

import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public class RunProjectShortcut implements ILaunchShortcut {

   private String guessBuildFileToLaunch(final IProject project) throws CoreException {
      final var defaultBuildFile = project.getFile(Constants.DEFAULT_HAXE_BUILD_FILE);
      if (defaultBuildFile != null)
         return Constants.DEFAULT_HAXE_BUILD_FILE;

      final var foundFiles = new ArrayList<IFile>();
      project.accept(res -> {
         if (res.getType() == IResource.PROJECT)
            return true;
         if (res.getType() == IResource.FILE //
            && Constants.HAXE_BUILD_FILE_EXTENSION.equals(((IFile) res).getFileExtension())) {
            foundFiles.add((IFile) res);
         }
         return false;
      });

      return foundFiles.isEmpty() ? null : foundFiles.get(1).getName();
   }

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
         for (final ILaunchConfiguration cfg : launchMgr.getLaunchConfigurations(launchConfigType)) {
            if (cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName())) {
               DebugUITools.launch(cfg, mode);
               return;
            }
         }

         // create a new launch config
         final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName()));
         newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_PROJECT, project.getName());
         final var buildFileToLaunch = guessBuildFileToLaunch(project);
         if (buildFileToLaunch != null) {
            newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, buildFileToLaunch);
         }
         final var prefs = new HaxeProjectPreference(project);
         final var altSDK = prefs.getAlternateHaxeSDK();
         if (altSDK != null) {
            newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_HAXE_SDK, altSDK.getName());
         }

         if (Window.OK == DebugUITools.openLaunchConfigurationDialog(UI.getShell(), newLaunchConfig, Constants.LAUNCH_HAXE_GROUP, null)) {
            newLaunchConfig.doSave();
         }
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_CreatingLaunchConfigFailed, StatusUtils.createError(ex), true);
      }
   }
}
