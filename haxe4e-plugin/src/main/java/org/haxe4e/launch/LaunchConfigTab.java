/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.prefs.HaxeWorkspacePreference;
import org.haxe4e.project.HaxeProjectNature;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Dialogs;

/**
 * @author Sebastian Thomschke
 */
public class LaunchConfigTab extends AbstractLaunchConfigurationTab {

   private LaunchConfigForm form;

   @Override
   public void createControl(final Composite parent) {
      form = new LaunchConfigForm(parent, SWT.NONE);
      setControl(form);
   }

   @Override
   public Image getImage() {
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
         form.selectedProject.set(Projects.getOpenProjectWithNature(projectName, HaxeProjectNature.NATURE_ID));
         form.selectedProject.subscribe(this::updateLaunchConfigurationDialog);

         final var hxmlFile = config.getAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, "");
         form.buildFile.set(hxmlFile);
         form.buildFile.subscribe(this::updateLaunchConfigurationDialog);

         final var altSDK = HaxeWorkspacePreference.getHaxeSDK(config.getAttribute(Constants.LAUNCH_ATTR_HAXE_SDK, ""));
         form.selectedAltSDK.set(altSDK);
         form.selectedAltSDK.subscribe(this::updateLaunchConfigurationDialog);
      } catch (final CoreException ex) {
         Dialogs.showStatus(Messages.Launch_InitializingLaunchConfigTabFailed, Haxe4EPlugin.status().createError(ex), true);
      }
   }

   @Override
   public boolean isValid(final ILaunchConfiguration launchConfig) {
      if (form.selectedProject.get() == null)
         return false;

      return super.isValid(launchConfig);
   }

   @Override
   public void performApply(final ILaunchConfigurationWorkingCopy config) {
      config.setAttribute(Constants.LAUNCH_ATTR_PROJECT, form.selectedProject.get() == null ? null : form.selectedProject.get().getName());
      config.setAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, form.buildFile.get());
      final var altSDK = form.selectedAltSDK.get();
      config.setAttribute(Constants.LAUNCH_ATTR_HAXE_SDK, altSDK == null ? "" : altSDK.getName());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy config) {
   }
}
