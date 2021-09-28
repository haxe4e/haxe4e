/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import java.io.IOException;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.haxe4e.Constants;
import org.haxe4e.model.HaxeBuildFile;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.util.LOG;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeDependenciesUpdater implements IResourceChangeListener {

   public static final String HAXE_DEPS_MAGIC_FOLDER_NAME = "!!haxedeps";

   public static final HaxeDependenciesUpdater INSTANCE = new HaxeDependenciesUpdater();

   private HaxeDependenciesUpdater() {
   }

   public void onHaxeProjectConfigChanged(final IProject haxeProject) {
      if (HaxeProjectNature.hasNature(haxeProject) != Boolean.TRUE)
         return; // ignore

      final var job = new Job("Updating Haxe dependency tree...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            try {
               /*
                * create haxe dependencies top-level virtual folder
                */
               final var haxedepsFolder = haxeProject.getFolder(HAXE_DEPS_MAGIC_FOLDER_NAME);

               final var prefs = new HaxeProjectPreference(haxeProject);
               final var buildFile = prefs.getEffectiveHaxeBuildFile();

               if (buildFile == null) {
                  if (haxedepsFolder.exists() && haxedepsFolder.isVirtual()) {
                     haxedepsFolder.delete(true, monitor);
                  }
                  return Status.OK_STATUS;
               }

               if (haxedepsFolder.exists()) {
                  if (!haxedepsFolder.isVirtual())
                     return Status.error("Cannot update project dependency tree. Physical folder with name " + HAXE_DEPS_MAGIC_FOLDER_NAME
                        + " exists!");
               } else {
                  haxedepsFolder.create(IResource.VIRTUAL, true, monitor);
               }

               final var depsToCheck = new HaxeBuildFile(buildFile.getLocation().toFile()) //
                  .getDirectDependencies(prefs.getEffectiveHaxeSDK(), monitor).stream() //
                  .collect(Collectors.toMap(d -> d.meta.name + "-" + d.meta.version, Function.identity()));

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
               return Status.OK_STATUS;
            } catch (final CoreException | IOException ex) {
               return Status.error("Failed to update project dependency tree", ex);
            }
         }
      };
      job.setRule(haxeProject); // synchronize job execution on project
      job.setPriority(Job.BUILD);
      job.schedule();
   }

   @Override
   public void resourceChanged(final IResourceChangeEvent event) {
      if (event.getType() != IResourceChangeEvent.POST_CHANGE)
         return;

      final var rootDelta = event.getDelta();
      final var changedProjects = new HashSet<IProject>();
      final IResourceDeltaVisitor visitor = delta -> {
         if ((delta.getFlags() & IResourceDelta.CONTENT) == 0)
            return true; // ignore

         switch (delta.getKind()) {
            case IResourceDelta.CHANGED:
            case IResourceDelta.ADDED:
            case IResourceDelta.REMOVED:
               break;
            default:
               return true; // ignore
         }

         final var resource = delta.getResource();
         final var project = resource.getProject();
         if (!project.hasNature(HaxeProjectNature.NATURE_ID))
            return true; // ignore

         if (Constants.HAXE_BUILD_FILE_EXTENSION.equals(resource.getFileExtension()) || "haxelib.json".equals(resource.getName())) {
            changedProjects.add(project);
         }
         return true;
      };

      try {
         rootDelta.accept(visitor);
      } catch (final CoreException ex) {
         LOG.error(ex);
      }

      for (final IProject p : changedProjects) {
         onHaxeProjectConfigChanged(p);
      }
   }
}
