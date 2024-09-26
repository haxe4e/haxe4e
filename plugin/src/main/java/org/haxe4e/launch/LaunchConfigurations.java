/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.launch;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.haxe4e.Constants;
import org.haxe4e.model.buildsystem.BuildFile;
import org.haxe4e.prefs.HaxeProjectPreference;

/**
 * @author Sebastian Thomschke
 */
public abstract class LaunchConfigurations {

   public static ILaunchConfigurationWorkingCopy create(final BuildFile buildFile) throws CoreException {
      final var project = buildFile.getProject();

      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_HAXE_CONFIGURATION_ID);
      final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName() + " ("
            + buildFile.location.getName() + ")"));
      LaunchConfigurations.initialize(newLaunchConfig, buildFile);
      return newLaunchConfig;
   }

   public static ILaunchConfigurationWorkingCopy create(final IProject project) throws CoreException {
      final var prefs = HaxeProjectPreference.get(project);
      final var buildFileToLaunch = prefs.getBuildFile();
      if (buildFileToLaunch != null)
         return create(buildFileToLaunch);

      final var launchMgr = DebugPlugin.getDefault().getLaunchManager();
      final var launchConfigType = launchMgr.getLaunchConfigurationType(Constants.LAUNCH_HAXE_CONFIGURATION_ID);
      final var newLaunchConfig = launchConfigType.newInstance(null, launchMgr.generateLaunchConfigurationName(project.getName()));
      LaunchConfigurations.initialize(newLaunchConfig, project);
      return newLaunchConfig;
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config) {
      config.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
      config.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);
      config.setAttribute(IDebugUIConstants.ATTR_FAVORITE_GROUPS, List.of(Constants.LAUNCH_HAXE_GROUP));
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final BuildFile buildFile) {
      initialize(config, buildFile.getProject());
      config.setAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, buildFile.getProjectRelativePath().toString());
   }

   public static void initialize(final ILaunchConfigurationWorkingCopy config, final IProject project) {
      initialize(config);
      config.setAttribute(Constants.LAUNCH_ATTR_PROJECT, project.getName());
      final var altSDK = HaxeProjectPreference.get(project).getAlternateHaxeSDK();
      if (altSDK != null) {
         config.setAttribute(Constants.LAUNCH_ATTR_HAXE_SDK, altSDK.getName());
      }
   }
}
