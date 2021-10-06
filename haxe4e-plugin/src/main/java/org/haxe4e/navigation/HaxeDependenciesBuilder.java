/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.haxe4e.Constants;
import org.haxe4e.model.HaxeBuildFile;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.util.StatusUtils;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeDependenciesBuilder extends IncrementalProjectBuilder {

   public static final String HAXE_DEPS_MAGIC_FOLDER_NAME = "!!haxedeps";

   public static final String ID = "org.haxe4e.builder.dependencies";

   public static void onHaxeProjectConfigChanged(final IProject haxeProject) {
      if (HaxeProjectNature.hasNature(haxeProject) != Boolean.TRUE)
         return; // ignore

      final var job = new Job("Updating 'Haxe Dependencies' list...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            try {
               updateHaxeProjectDependencies(haxeProject, monitor);
               return Status.OK_STATUS;
            } catch (final CoreException ex) {
               return ex.getStatus();
            }
         }
      };
      job.setRule(haxeProject); // synchronize job execution on project
      job.setPriority(Job.BUILD);
      job.schedule();
   }

   public static void removeDependenciesFolder(final IProject haxeProject, final IProgressMonitor monitor) throws CoreException {
      final var haxeDepsFolder = haxeProject.getFolder(HAXE_DEPS_MAGIC_FOLDER_NAME);
      if (haxeDepsFolder.exists() && haxeDepsFolder.isVirtual()) {
         haxeDepsFolder.delete(false, monitor);
      }
   }

   private static void updateHaxeProjectDependencies(final IProject project, final IProgressMonitor monitor) throws CoreException {
      try {
         /*
          * create haxe dependencies top-level virtual folder
          */
         final var haxedepsFolder = project.getFolder(HAXE_DEPS_MAGIC_FOLDER_NAME);

         final var prefs = new HaxeProjectPreference(project);
         final var buildFile = prefs.getEffectiveHaxeBuildFile();

         if (buildFile == null) {
            if (haxedepsFolder.exists() && haxedepsFolder.isVirtual()) {
               haxedepsFolder.delete(true, monitor);
            }
            return;
         }

         if (haxedepsFolder.exists()) {
            if (!haxedepsFolder.isVirtual())
               throw new CoreException(StatusUtils.createError("Cannot update project dependency tree. Physical folder with name "
                  + HAXE_DEPS_MAGIC_FOLDER_NAME + " exists!"));
         } else {
            haxedepsFolder.create(IResource.VIRTUAL, true, monitor);
         }

         final var depsToCheck = new HaxeBuildFile(buildFile.getLocation().toFile()) //
            .getDirectDependencies(prefs.getEffectiveHaxeSDK(), monitor).stream() //
            .collect(Collectors.toMap(d -> d.meta.name + " [" + (d.isDevVersion ? "dev" : d.meta.version) + "]", Function.identity()));

         for (final var folder : haxedepsFolder.members()) {
            final var dep = depsToCheck.get(folder.getName());
            if (dep != null && dep.location.equals(folder.getRawLocation().toFile().toPath())) {
               depsToCheck.remove(folder.getName());
            } else {
               // delete obsolete or broken link
               folder.delete(true, monitor);
            }
         }
         for (final var dep : depsToCheck.entrySet()) {
            final var folder = haxedepsFolder.getFolder(dep.getKey());
            folder.createLink(dep.getValue().location.toUri(), IResource.BACKGROUND_REFRESH, monitor);
         }
      } catch (final CoreException | IOException ex) {
         throw new CoreException(StatusUtils.createError(ex, "Failed to update project dependency tree"));
      }
   }

   @Override
   protected IProject[] build(final int kind, final Map<String, String> args, final IProgressMonitor monitor) throws CoreException {
      final boolean needsUpdate;
      switch (kind) {
         case INCREMENTAL_BUILD:
         case AUTO_BUILD:
            final var delta = getDelta(getProject());
            if (delta == null) {
               needsUpdate = true;
            } else if (delta.getAffectedChildren().length == 0) {
               needsUpdate = false;
            } else {
               final boolean[] hasConfigChange = {false};
               delta.accept(subDelta -> {
                  if (hasConfigChange[0])
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
                  if (Constants.HAXE_BUILD_FILE_EXTENSION.equals(resource.getFileExtension()) || "haxelib.json".equals(resource
                     .getName())) {
                     hasConfigChange[0] = true;
                  }
                  return true;
               });
               needsUpdate = hasConfigChange[0];
            }
            break;
         case CLEAN_BUILD:
         case FULL_BUILD:
            needsUpdate = true;
            break;
         default:
            needsUpdate = false;
      }

      if (needsUpdate) {
         updateHaxeProjectDependencies(getProject(), monitor);
      }
      return null;
   }

   @Override
   public ISchedulingRule getRule(final int kind, final Map<String, String> args) {
      return getProject();
   }
}
