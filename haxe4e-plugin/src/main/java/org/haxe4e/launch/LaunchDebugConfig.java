/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate;

/**
 * @author Sebastian Thomschke
 */
public class LaunchDebugConfig extends DSPLaunchDelegate {

   @Override
   public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, final IProgressMonitor monitor)
      throws CoreException {
      super.launch(configuration, mode, launch, monitor);
   }
}
