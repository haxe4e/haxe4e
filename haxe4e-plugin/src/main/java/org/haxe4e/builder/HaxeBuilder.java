/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.builder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
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

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class HaxeBuilder extends IncrementalProjectBuilder {

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
      final var console = HaxeBuilderConsole.openConsole(project);

      try (var out = console.newOutputStream();
           var err = console.newOutputStream()) {

         out.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR));
         err.setColor(DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR));

         out.write("Building project '" + project.getName() + "': haxe " + hxmlFile.getProjectRelativePath() + "");
         out.write(" (haxe v" + haxeSDK.getVersion() + ")");
         out.write(Strings.NEW_LINE);
         out.write(Strings.NEW_LINE);

         final var sw = new StopWatch();
         sw.start();

         final AtomicBoolean hasCompilerOutput = new AtomicBoolean(false);
         final var proc = haxeSDK.getCompilerProcessBuilder(false) //
            .withArg(hxmlFile.getLocation().toOSString()) //
            .withWorkingDirectory(project.getLocation().toFile()) //
            .withRedirectOutput(line -> {
               try {
                  out.write(line);
                  out.write(Strings.NEW_LINE);
               } catch (final IOException e) {
                  e.printStackTrace();
               }
               hasCompilerOutput.set(true);
            })
            .withRedirectError(line -> {
               try {
                  err.write(line);
                  err.write(Strings.NEW_LINE);
               } catch (final IOException e) {
                  e.printStackTrace();
               }
               hasCompilerOutput.set(true);
            })
            .start();
         proc.waitForExit();

         sw.stop();

         if (hasCompilerOutput.get()) {
            out.write(Strings.NEW_LINE);
         }
         if (proc.exitStatus() == 0) {
            out.write("Build successful in ");
         } else {
            out.write("Build");
            out.flush();
            err.write(" failed ");
            out.write("in ");
         }
         out.write(DurationFormatUtils.formatDurationWords(sw.getTime(), true, true));
         if (proc.exitStatus() != 0) {
            out.write(" (exit code: " + proc.exitStatus() + ")");
         }
         out.write(Strings.NEW_LINE);

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
