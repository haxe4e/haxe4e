/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.builder;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.haxe4e.Constants;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.StatusUtils;

import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class HaxeBuilder extends IncrementalProjectBuilder {

   public static final class Context {
      public final IProject project;
      public final IProgressMonitor monitor;
      public final CompletionStage<Void> onTerminated;

      public Context(final IProject project, final IProgressMonitor monitor, final CompletionStage<Void> onTerminated) {
         this.project = project;
         this.monitor = monitor;
         this.onTerminated = onTerminated;
      }
   }

   public static final String ID = "org.haxe4e.builder";

   @Override
   protected IProject[] build(final int kind, final Map<String, String> args, final IProgressMonitor monitor) throws CoreException {
      final boolean needsBuild;
      switch (kind) {
         case INCREMENTAL_BUILD:
         case AUTO_BUILD:
            final var delta = getDelta(getProject());
            if (delta == null) {
               needsBuild = true;
            } else if (delta.getAffectedChildren().length == 0) {
               needsBuild = false;
            } else {
               final boolean[] hasRelevantFileChange = {false};
               delta.accept(subDelta -> {
                  if (hasRelevantFileChange[0])
                     return false; // skip

                  if ((subDelta.getFlags() & IResourceDelta.CONTENT) == 0)
                     return true; // ignore

                  switch (subDelta.getKind()) {
                     case IResourceDelta.CHANGED:
                     case IResourceDelta.ADDED:
                     case IResourceDelta.REMOVED:
                        break;
                     default:
                        return true; // ignore
                  }

                  final var resource = subDelta.getResource();
                  switch (resource.getProjectRelativePath().segment(0)) {
                     case "bin":
                     case "build":
                     case "dump":
                     case "target":
                        return true; // ignore
                     default:
                        break;
                  }

                  final var resourceExt = resource.getFileExtension();
                  if (Constants.HAXE_BUILD_FILE_EXTENSION.equals(resourceExt) //
                     || Constants.HAXE_FILE_EXTENSION.equals(resourceExt)) {
                     hasRelevantFileChange[0] = true;
                  }
                  return true;
               });
               needsBuild = hasRelevantFileChange[0];
            }
            break;
         case CLEAN_BUILD:
         case FULL_BUILD:
            needsBuild = true;
            break;
         default:
            needsBuild = false;
      }

      if (needsBuild) {
         buildProject(getProject(), monitor);
      }
      return null;
   }

   private void buildProject(final IProject project, final IProgressMonitor monitor) throws CoreException {
      final var prefs = new HaxeProjectPreference(project);
      final var haxeSDK = prefs.getEffectiveHaxeSDK();
      if (haxeSDK == null)
         return;

      final var hxmlFile = prefs.getEffectiveHaxeBuildFile();
      if (hxmlFile == null)
         return;

      monitor.setTaskName("Building project '" + project.getName() + "'");

      final var onTerminated = new CompletableFuture<Void>();
      final var console = HaxeBuilderConsole.openConsole(new Context(project, monitor, onTerminated));

      try (var out = console.newMessageStream();
           var err = console.newMessageStream()) {

         out.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR));
         err.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR));

         final var startAt = LocalTime.now();
         final var startAtStr = startAt.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME);
         console.setTitle("<running> " + haxeSDK.getCompilerExecutable() + " (" + startAtStr + ")");

         out.println("Building project '" + project.getName() + "' using '" + hxmlFile.getProjectRelativePath() + "'...");
         out.println();

         final var hasCompilerOutput = new AtomicBoolean(false);
         final var proc = haxeSDK.getCompilerProcessBuilder(false) //
            .withArg(hxmlFile.getProjectRelativePath().toOSString()) //
            .withWorkingDirectory(project.getLocation().toFile()) //
            .withRedirectOutput(line -> {
               out.println(line);
               hasCompilerOutput.set(true);
            }).withRedirectError(line -> {
               err.println(line);
               hasCompilerOutput.set(true);
            }).start();

         while (proc.isAlive()) {
            /*
             * kill process if job was aborted by user
             */
            if (monitor.isCanceled()) {
               proc.terminate() //
                  .waitForExit(2, TimeUnit.SECONDS) //
                  .kill();
               proc.getProcess().descendants().forEach(ProcessHandle::destroy);
               Threads.sleep(1000);
               proc.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
               err.println("Aborted on user request.");
               break;
            }
            Threads.sleep(500);
         }

         final var endAt = LocalTime.now();
         final var endAtStr = endAt.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_TIME);
         console.setTitle("<terminated> " + haxeSDK.getCompilerExecutable() + " (" + startAtStr + " - " + endAtStr + ")");
         if (monitor.isCanceled())
            return;

         if (hasCompilerOutput.get()) {
            out.println();
         }
         if (proc.exitStatus() == 0) {
            out.write("Build successful in ");
         } else {
            out.write("Build");
            out.flush();
            err.write(" failed ");
            out.write("in ");
         }

         var elapsed = ChronoUnit.MILLIS.between(startAt, endAt);
         if (elapsed < 1_000) { // prevent 'Build successful in 0 seconds'
            elapsed = 1_000;
         }
         out.write(DurationFormatUtils.formatDurationWords(elapsed, true, true));
         if (proc.exitStatus() != 0) {
            out.write(" (exit code: " + proc.exitStatus() + ")");
         }
         out.println();

      } catch (final IOException ex) {
         throw new CoreException(StatusUtils.createError(ex, "Failed to run Haxe Builder."));
      } catch (final InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new CoreException(StatusUtils.createError(ex, "Aborted."));
      }
   }

   @Override
   public ISchedulingRule getRule(final int kind, final Map<String, String> args) {
      return getProject();
   }
}
