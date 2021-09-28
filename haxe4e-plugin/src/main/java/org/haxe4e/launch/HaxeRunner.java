/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.osgi.util.NLS;
import org.haxe4e.localization.Messages;
import org.haxe4e.util.StatusUtils;
import org.haxe4e.util.ui.Dialogs;

import net.sf.jstuff.core.io.Processes;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeRunner {

   public static void launchHxmlFile(final ILaunch launch, final Path haxeCompiler, final Path hxmlFile, final Path workDir,
      final Map<String, String> envVars, final boolean appendEnvVars) {
      final var job = Job.create(NLS.bind(Messages.Launch_RunningFile, hxmlFile), runnable -> {
         try {
            final var proc = Processes.builder(haxeCompiler.toAbsolutePath()) //
               .withArg(hxmlFile.toAbsolutePath()) //
               .withEnvironment(env -> {
                  if (!appendEnvVars) {
                     env.clear();
                  }
                  env.putAll(envVars);
               }) //
               .withWorkingDirectory(workDir) //
               .start();
            launch.addProcess(DebugPlugin.newProcess(launch, proc.getProcess(), Messages.Label_Haxe_Terminal));
         } catch (final IOException ex) {
            Dialogs.showStatus(Messages.Launch_CouldNotRunHaxe, StatusUtils.createError(ex), true);
         }
      });
      job.schedule();
   }

   public static void launchHxmlFile(final Path haxeCompiler, final Path hxmlFile, final Path workDir) {
      final var run = new Launch(null, ILaunchManager.RUN_MODE, null);
      DebugPlugin.getDefault().getLaunchManager().addLaunch(run);
      launchHxmlFile(run, haxeCompiler, hxmlFile, workDir, Collections.emptyMap(), true);
   }

   private HaxeRunner() {
   }
}
