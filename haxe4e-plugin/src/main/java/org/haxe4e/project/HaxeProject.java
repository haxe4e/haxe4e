/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.haxe4e.Constants;
import org.haxe4e.prefs.HaxeProjectPreference;

/**
 * @author Ian Harrigan
 */
public class HaxeProject {
   private IProject project;

   public HaxeProject(final IProject project) {
      this.project = project;
   }

   public HaxeProjectPreference getPrefs() {
      return new HaxeProjectPreference(project);
   }

   public List<String> getBuildFiles() throws CoreException {
      return getBuildFiles(false);
   }

   public List<String> getBuildFiles(final boolean allFiles) throws CoreException {
      final var buildFiles = new ArrayList<String>();
      final var projectFullPath = project.getFullPath();
      project.accept(res -> {
         if (res.isVirtual() || res.isLinked())
            return false;

         if (res instanceof IFile //
            && Constants.HAXE_BUILD_FILE_EXTENSION.equals(res.getFileExtension()) //
         ) {
            final var fullPath = res.getFullPath().makeRelativeTo(projectFullPath).toPortableString();
            if (allFiles || !allFiles && !fullPath.contains("/")) {
               buildFiles.add(fullPath);
            }
         }
         return true;
      });

      return buildFiles;
   }

   public ILaunchConfiguration getOrCreateLaunchConfiguration() throws CoreException {
      if (project == null)
         return null;

      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_HAXE_CONFIGURATION_ID);

      // search most recently launched configs for a matching one
      for (final var launch : launchMgr.getLaunches()) {
         final var cfg = launch.getLaunchConfiguration();
         if (cfg.getType().equals(launchConfigType) //
            && cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName()) //
         )
            return cfg;
      }

      // search all created launch configs for a matching one
      for (final ILaunchConfiguration cfg : launchMgr.getLaunchConfigurations(launchConfigType)) {
         if (cfg.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "").equalsIgnoreCase(project.getName()))
            return cfg;
      }

      // create a new launch config
      final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName()));
      newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_PROJECT, project.getName());
      final var prefs = new HaxeProjectPreference(project);
      final var altSDK = prefs.getAlternateHaxeSDK();
      if (altSDK != null) {
         newLaunchConfig.setAttribute(Constants.LAUNCH_ATTR_HAXE_SDK, altSDK.getName());
      }

      return newLaunchConfig;
   }

   public void setBuildFile(final String buildFile) {
      getPrefs().setHaxeBuildFile(buildFile);
      getPrefs().save();
   }
}
