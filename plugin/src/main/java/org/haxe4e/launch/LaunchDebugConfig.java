/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.launch;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNullUnsafe;

import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.lsp4e.debug.debugmodel.TransportStreams;
import org.eclipse.lsp4e.debug.debugmodel.TransportStreams.DefaultTransportStreams;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate;
import org.haxe4e.prefs.HaxeWorkspacePreference;
import org.haxe4e.util.io.VSCodeJsonRpcLineTracing;
import org.haxe4e.util.io.VSCodeJsonRpcLineTracing.Source;

import net.sf.jstuff.core.io.stream.LineCapturingInputStream;
import net.sf.jstuff.core.io.stream.LineCapturingOutputStream;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class LaunchDebugConfig extends DSPLaunchDelegate {

   @Override
   @SuppressWarnings("resource")
   @NonNullByDefault({})
   protected IDebugTarget createDebugTarget(final SubMonitor mon, final Supplier<TransportStreams> streamsSupplier, final ILaunch launch,
         final Map<String, Object> dspParameters) throws CoreException {
      final var isTraceIOVerbose = HaxeWorkspacePreference.isDAPTraceIOVerbose();
      return isTraceIOVerbose || HaxeWorkspacePreference.isDAPTraceIO() //
            ? super.createDebugTarget(mon, (Supplier<TransportStreams>) () -> {
               final var streams = streamsSupplier.get();
               return new DefaultTransportStreams( //
                  new LineCapturingInputStream(asNonNullUnsafe(streams.in), line -> VSCodeJsonRpcLineTracing.traceLine(Source.SERVER_OUT,
                     line, isTraceIOVerbose)), //
                  new LineCapturingOutputStream(asNonNullUnsafe(streams.out), line -> VSCodeJsonRpcLineTracing.traceLine(Source.CLIENT_OUT,
                     line, isTraceIOVerbose)));
            }, launch, dspParameters)
            : super.createDebugTarget(mon, streamsSupplier, launch, dspParameters);
   }
}
