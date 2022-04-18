/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.navigation;

import java.util.HashSet;
import java.util.List;
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
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.project.HaxeProjectNature;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeDependenciesUpdater implements IResourceChangeListener {

   public static final String HAXE_STDLIB_MAGIC_FOLDER_NAME = "!!!haxestdlib";
   public static final String HAXE_DEPS_MAGIC_FOLDER_NAME = "!!haxedeps";

   public static final HaxeDependenciesUpdater INSTANCE = new HaxeDependenciesUpdater();

   private HaxeDependenciesUpdater() {
   }

   public void onHaxeProjectConfigChanged(final IProject haxeProject) {
      if (HaxeProjectNature.hasNature(haxeProject) != Boolean.TRUE)
         return; // ignore

      final var job = new Job("Updating 'Haxe Dependencies' list...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            return updateHaxeProjectDependencies(haxeProject, monitor);
         }
      };
      job.setRule(haxeProject); // synchronize job execution on project
      job.setPriority(Job.BUILD);
      job.schedule();
   }

   public void onHaxeProjectsConfigChanged(final List<IProject> haxeProjects) {
      final var job = new Job("Updating Haxe dependency tree...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            for (final var haxeProject : haxeProjects) {
               if (HaxeProjectNature.hasNature(haxeProject) == Boolean.TRUE) {
                  final var status = updateHaxeProjectDependencies(haxeProject, monitor);
                  if (status != Status.OK_STATUS) {
                     Haxe4EPlugin.log().error(status);
                  }
               }
            }
            return Status.OK_STATUS;
         }
      };
      job.setPriority(Job.BUILD);
      job.schedule();
   }

   public void removeDependenciesFolder(final IProject haxeProject, final IProgressMonitor monitor) throws CoreException {
      final var haxeStdLibFolder = haxeProject.getFolder(HAXE_STDLIB_MAGIC_FOLDER_NAME);
      if (haxeStdLibFolder.exists() && (haxeStdLibFolder.isVirtual() || haxeStdLibFolder.isLinked())) {
         haxeStdLibFolder.delete(false, monitor);
      }

      final var haxeDepsFolder = haxeProject.getFolder(HAXE_DEPS_MAGIC_FOLDER_NAME);
      if (haxeDepsFolder.exists() && (haxeDepsFolder.isVirtual() || haxeDepsFolder.isLinked())) {
         haxeDepsFolder.delete(false, monitor);
      }
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

         final var prefs = HaxeProjectPreference.get(project);
         if (prefs.getBuildSystem().getBuildFileExtension().equals(resource.getFileExtension()) //
            || "haxelib.json".equals(resource.getName())) {
            changedProjects.add(project);
         }
         return true;
      };

      try {
         rootDelta.accept(visitor);
      } catch (final CoreException ex) {
         Haxe4EPlugin.log().error(ex);
      }

      for (final IProject p : changedProjects) {
         onHaxeProjectConfigChanged(p);
      }
   }

   private IStatus updateHaxeProjectDependencies(final IProject haxeProject, final IProgressMonitor monitor) {
      try {
         final var prefs = HaxeProjectPreference.get(haxeProject);

         final var haxeSDK = prefs.getEffectiveHaxeSDK();
         if (haxeSDK == null)
            throw new IllegalStateException("Haxe SDK cannot be found.");

         /*
          * create haxe stdlib top-level virtual folder
          */
         final var haxeStdLibFolder = haxeProject.getFolder(HAXE_STDLIB_MAGIC_FOLDER_NAME);
         if (haxeStdLibFolder.exists()) {
            if (!haxeStdLibFolder.isLinked())
               return Haxe4EPlugin.status().createError("Cannot update Haxe standard library folder. Physical folder with name "
                  + HAXE_STDLIB_MAGIC_FOLDER_NAME + " exists!");
         } else {
            haxeStdLibFolder.createLink(haxeSDK.getStandardLibDir().toUri(), IResource.REPLACE, monitor);
         }

         /*
          * create haxe dependencies top-level virtual folder
          */
         final var haxeDepsFolder = haxeProject.getFolder(HAXE_DEPS_MAGIC_FOLDER_NAME);

         final var buildFile = prefs.getBuildFile();

         // if no build file exists remove the dependencies folder
         if (buildFile == null) {
            if (haxeDepsFolder.exists() && haxeDepsFolder.isVirtual()) {
               haxeDepsFolder.delete(true, monitor);
            }
            return Status.OK_STATUS;
         }

         if (haxeDepsFolder.exists()) {
            if (!haxeDepsFolder.isVirtual())
               return Haxe4EPlugin.status().createError("Cannot update Haxe dependencies list. Physical folder with name "
                  + HAXE_DEPS_MAGIC_FOLDER_NAME + " exists!");
         } else {
            haxeDepsFolder.create(IResource.VIRTUAL, true, monitor);
         }

         final var depsToCheck = buildFile //
            .getDirectDependencies(haxeSDK, monitor).stream() //
            .collect(Collectors.toMap(d -> d.meta.name + " [" + (d.isDevVersion ? "dev" : d.meta.version) + "]", Function.identity()));

         for (final var folder : haxeDepsFolder.members()) {
            if (depsToCheck.containsKey(folder.getName())) {
               final var dep = depsToCheck.get(folder.getName());
               if (folder.getRawLocation() != null && dep.location.equals(folder.getRawLocation().toFile().toPath())) {
                  depsToCheck.remove(folder.getName());
               } else {
                  folder.delete(true, monitor); // delete broken folder link
               }
            } else {
               folder.delete(true, monitor); // delete folder link to (now) unused dependency
            }
         }

         for (final var dep : depsToCheck.entrySet()) {
            final var folder = haxeDepsFolder.getFolder(dep.getKey());
            folder.createLink(dep.getValue().location.toUri(), IResource.BACKGROUND_REFRESH, monitor);
         }
         return Status.OK_STATUS;
      } catch (final Exception ex) {
         return Haxe4EPlugin.status().createError(ex, "Failed to Haxe dependencies list.");
      }
   }

}
