/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.builder;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.haxe4e.Constants;
import org.haxe4e.model.buildsystem.LixVirtualBuildFile;
import org.haxe4e.prefs.HaxeProjectPreference;

/**
 * @author Sebastian Thomschke
 */
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
      final var project = getProject();
      final var prefs = HaxeProjectPreference.get(project);

      final boolean needsBuild;
      switch (kind) {
         case INCREMENTAL_BUILD:
         case AUTO_BUILD:
            if (!prefs.isAutoBuild()) {
               needsBuild = false;
               break;
            }

            final var delta = getDelta(project);
            if (delta == null) {
               needsBuild = true;
            } else if (delta.getAffectedChildren().length == 0) {
               needsBuild = false;
            } else {
               final var hasRelevantFileChange = new MutableBoolean(false);
               delta.accept(subDelta -> {
                  if (hasRelevantFileChange.isTrue())
                     return false; // no further scanning necessary

                  if ((subDelta.getFlags() & IResourceDelta.CONTENT) == 0)
                     return true; // ignore no content change happened

                  switch (subDelta.getKind()) {
                     case IResourceDelta.ADDED:
                     case IResourceDelta.CHANGED:
                     case IResourceDelta.MOVED_FROM:
                     case IResourceDelta.MOVED_TO:
                     case IResourceDelta.REMOVED:
                        break;
                     default:
                        return true; // ignore other delta types
                  }

                  final var resource = subDelta.getResource();
                  switch (resource.getProjectRelativePath().segment(0)) {
                     case "bin":
                     case "build":
                     case "dump":
                     case "output":
                     case "target":
                        return true; // ignore build artifacts
                     default:
                        break;
                  }

                  if (resource.getType() == IResource.FOLDER)
                     return true;

                  final var resourceExt = resource.getFileExtension();
                  switch (resourceExt == null ? "" : resourceExt) {
                     case Constants.HAXE_FILE_EXTENSION:
                     case "json":
                     case "xml":
                        hasRelevantFileChange.setTrue();
                        return false; // no further scanning necessary
                  }
                  if (prefs.getBuildSystem().getBuildFileExtension().equals(resourceExt)) {
                     hasRelevantFileChange.setTrue();
                     return false; // no further scanning necessary
                  }

                  return true;
               });
               needsBuild = hasRelevantFileChange.isTrue();
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
         buildProject(project, prefs, monitor);
      }
      return null;
   }

   private void buildProject(final IProject project, final HaxeProjectPreference prefs, final IProgressMonitor monitor)
      throws CoreException {
      final var haxeSDK = prefs.getEffectiveHaxeSDK();
      if (haxeSDK == null)
         return;

      final var buildFile = prefs.getBuildFile();
      if (buildFile == null || buildFile instanceof LixVirtualBuildFile)
         return;

      final var buildFileProjectRelativePath = buildFile.location.getProjectRelativePath();

      HaxeBuilderConsole.runWithConsole(project, //
         haxeSDK.getCompilerProcessBuilder(false).withArg(buildFileProjectRelativePath.toOSString()), //
         monitor);
   }

   @Override
   public ISchedulingRule getRule(final int kind, final Map<String, String> args) {
      return getProject();
   }
}
