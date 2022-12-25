/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.RefreshTab;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
public class LaunchConfigTabGroup extends AbstractLaunchConfigurationTabGroup {

   @Override
   public void createTabs(final ILaunchConfigurationDialog dialog, final String mode) {
      setTabs(new LaunchConfigTab(), new RefreshTab(), new EnvironmentTab(), new CommonTab());
   }

   @Override
   public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
      super.setDefaults(configuration);

      LaunchConfigurations.initialize(configuration);
   }
}