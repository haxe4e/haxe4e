/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.lsp4e.debug.debugmodel.DSPDebugTarget;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate;
import org.haxe4e.util.io.LinePrefixingTeeInputStream;
import org.haxe4e.util.io.LinePrefixingTeeOutputStream;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class LaunchDebugConfig extends DSPLaunchDelegate {

   private static final boolean TRACE_IO = Platform.getDebugBoolean("org.haxe4e/trace/debugserv/io");

   @Override
   public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, final IProgressMonitor monitor)
      throws CoreException {
      super.launch(configuration, mode, launch, monitor);
   }

   @SuppressWarnings("resource")
   @Override
   protected IDebugTarget createDebugTarget(final SubMonitor subMonitor, final Runnable cleanup, final InputStream inputStream,
      final OutputStream outputStream, final ILaunch launch, final Map<String, Object> dspParameters) throws CoreException {
      final var target = TRACE_IO //
         ? new DSPDebugTarget(launch, cleanup, //
            new LinePrefixingTeeInputStream(inputStream, System.out, "SERVER >> "), //
            new LinePrefixingTeeOutputStream(outputStream, System.out, "CLIENT >> "), //
            dspParameters) //
         : new DSPDebugTarget(launch, cleanup, inputStream, outputStream, dspParameters);
      target.initialize(subMonitor.split(80));
      return target;
   }
}
