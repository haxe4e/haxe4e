/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.buildsystem.BuildFile;
import org.haxe4e.model.buildsystem.BuildSystem;
import org.haxe4e.prefs.HaxeWorkspacePreference;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.widget.HaxeBuildFileSelectionGroup;
import org.haxe4e.widget.HaxeProjectSelectionGroup;
import org.haxe4e.widget.HaxeSDKSelectionGroup;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Dialogs;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class LaunchConfigTab extends AbstractLaunchConfigurationTab {

   private MutableObservableRef<@Nullable IProject> selectedProject = lazyNonNull();
   private MutableObservableRef<@Nullable BuildFile> selectedBuildFile = lazyNonNull();
   private MutableObservableRef<@Nullable HaxeSDK> selectedAltSDK = lazyNonNull();

   @Override
   public void createControl(final Composite parent) {
      final var form = new Composite(parent, SWT.NONE);
      form.setLayout(new GridLayout(1, false));

      selectedProject = new HaxeProjectSelectionGroup(form).selectedProject;

      final var grpBuildFile = new HaxeBuildFileSelectionGroup(form);
      selectedBuildFile = grpBuildFile.selectedBuildFile;
      selectedProject.subscribe(grpBuildFile::setProject);

      selectedAltSDK = new HaxeSDKSelectionGroup(form).selectedAltSDK;
      setControl(form);
   }

   @Override
   public String getId() {
      return LaunchConfigTab.class.getName();
   }

   @Override
   public @Nullable Image getImage() {
      return Haxe4EPlugin.get().getImageRegistry().get(Constants.IMAGE_ICON);
   }

   @Override
   public String getName() {
      return Messages.Label_Haxe_Configuration;
   }

   @Override
   public void initializeFrom(final ILaunchConfiguration config) {
      try {
         final var projectName = config.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "");
         final var project = Projects.findOpenProjectWithNature(projectName, HaxeProjectNature.NATURE_ID);
         selectedProject.set(project);
         selectedProject.subscribe(this::updateLaunchConfigurationDialog);

         if (project != null) {
            final var buildFile = config.getAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, "");
            selectedBuildFile.set(BuildSystem.HAXE.toBuildFile(project.getFile(buildFile)));
         }
         selectedBuildFile.subscribe(this::updateLaunchConfigurationDialog);

         final var altSDK = HaxeWorkspacePreference.getHaxeSDK(config.getAttribute(Constants.LAUNCH_ATTR_HAXE_SDK, ""));
         selectedAltSDK.set(altSDK);
         selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_InitializingLaunchConfigTabFailed, Haxe4EPlugin.status().createError(ex), true);
      }
   }

   @Override
   public boolean isValid(final ILaunchConfiguration launchConfig) {
      if (selectedProject.get() == null || selectedBuildFile.get() == null)
         return false;

      return super.isValid(launchConfig);
   }

   @Override
   public void performApply(final ILaunchConfigurationWorkingCopy config) {
      config.setAttribute(Constants.LAUNCH_ATTR_PROJECT, selectedProject.get() == null ? null : asNonNull(selectedProject.get()).getName());
      config.setAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, selectedBuildFile.get() == null ? null
         : asNonNull(selectedBuildFile.get()).getProjectRelativePath().toString());
      final var altSDK = selectedAltSDK.get();
      config.setAttribute(Constants.LAUNCH_ATTR_HAXE_SDK, altSDK == null ? "" : altSDK.getName());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
