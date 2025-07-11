/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.navigation;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxelibJSON;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.util.AbstractResourcesChangedListener;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeDependenciesUpdater extends AbstractResourcesChangedListener {

   public static final String STDLIB_MAGIC_FOLDER_NAME = "!!!haxestdlib";
   public static final String DEPS_MAGIC_FOLDER_NAME = "!!haxedeps";

   public static final HaxeDependenciesUpdater INSTANCE = new HaxeDependenciesUpdater();

   private HaxeDependenciesUpdater() {
   }

   public void onProjectConfigChanged(final IProject project) {
      if (!HaxeProjectNature.hasNature(project))
         return; // ignore

      final var job = new Job("Updating 'Haxe Dependencies' list of project '" + project.getName() + "'...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            return updateProjectDependencies(project, monitor);
         }
      };
      job.setRule(project); // synchronize job execution on project
      job.setPriority(Job.BUILD);
      job.schedule();
   }

   public void onProjectsConfigChanged(final List<IProject> projects) {
      for (final var project : projects) {
         onProjectConfigChanged(project);
      }
   }

   public void removeDependenciesFolder(final IProject project, final @Nullable IProgressMonitor monitor) throws CoreException {
      for (final var folderName : List.of(STDLIB_MAGIC_FOLDER_NAME, DEPS_MAGIC_FOLDER_NAME)) {
         final var folder = project.getFolder(folderName);
         if (folder.exists() && (folder.isVirtual() || folder.isLinked())) {
            folder.delete(false, monitor);
         }
      }
   }

   @Override
   public void resourceChanged(final IResourceChangeEvent event) {
      if (event.getType() != IResourceChangeEvent.POST_CHANGE)
         return;

      final var rootDelta = event.getDelta();
      final var changedProjects = new HashSet<IProject>();
      final IResourceDeltaVisitor visitor = delta -> {
         final var resource = delta.getResource();
         final var project = resource.getProject();
         if (project == null) // e.g. Workspace Root
            return true; // check children

         if (!Projects.hasNature(project, HaxeProjectNature.NATURE_ID))
            return false; // ignore children

         // if the project was just opened update dependencies
         if (resource == project && (delta.getFlags() & IResourceDelta.OPEN) != 0) {
            changedProjects.add(project);
            return false; // no need to check children
         }

         if ((delta.getFlags() & IResourceDelta.CONTENT) == 0)
            return true; // check children

         switch (delta.getKind()) {
            case IResourceDelta.ADDED, IResourceDelta.CHANGED, IResourceDelta.REMOVED:
               break;
            default:
               return true; // check children
         }

         if (resource.getType() != IResource.FILE)
            return true; // check children

         final var prefs = HaxeProjectPreference.get(project);
         if (prefs.getBuildSystem().getBuildFileExtension().equals(resource.getFileExtension()) //
               || HaxelibJSON.FILENAME.equals(resource.getName())) {
            changedProjects.add(project);
         }
         return false; // no need to check children
      };

      try {
         rootDelta.accept(visitor);
      } catch (final CoreException ex) {
         Haxe4EPlugin.log().error(ex);
      }

      for (final IProject p : changedProjects) {
         onProjectConfigChanged(p);
      }
   }

   private IStatus updateProjectDependencies(final IProject project, final IProgressMonitor monitor) {
      try {
         final var prefs = HaxeProjectPreference.get(project);

         final var sdk = prefs.getEffectiveHaxeSDK();
         if (sdk == null)
            return Haxe4EPlugin.status().createError("Cannot update 'Haxe Dependencies' list. Haxe SDK cannot be found!");

         /*
          * create/update haxe stdlib top-level virtual folder
          */
         final var stdLibFolder = project.getFolder(STDLIB_MAGIC_FOLDER_NAME);
         if (stdLibFolder.exists()) {
            if (!stdLibFolder.isLinked())
               return Haxe4EPlugin.status().createError("Cannot update Haxe standard library folder. Physical folder with name '"
                     + STDLIB_MAGIC_FOLDER_NAME + "' exists!");
            if (!asNonNull(stdLibFolder.getLocation()).toFile().toPath().equals(sdk.getStandardLibDir())) {
               stdLibFolder.createLink(sdk.getStandardLibDir().toUri(), IResource.REPLACE, monitor);
            }
         } else {
            stdLibFolder.createLink(sdk.getStandardLibDir().toUri(), IResource.REPLACE, monitor);
         }
         if (!stdLibFolder.isDerived()) {
            stdLibFolder.setDerived(true, monitor);
         }

         /*
          * create/update "Haxe Dependencies" top-level virtual folder
          */
         final var depsFolder = project.getFolder(DEPS_MAGIC_FOLDER_NAME);
         final var buildFile = prefs.getBuildFile();

         // if no build file exists remove the dependencies folder
         if (buildFile == null) {
            if (depsFolder.exists() && depsFolder.isVirtual()) {
               depsFolder.delete(true, monitor);
            }
            return Status.OK_STATUS;
         }

         if (depsFolder.exists()) {
            if (!depsFolder.isVirtual())
               return Haxe4EPlugin.status().createError("Cannot update 'Haxe Dependencies' list. Physical folder with name '"
                     + DEPS_MAGIC_FOLDER_NAME + "' exists!");
         } else {
            depsFolder.create(IResource.VIRTUAL, true, monitor);
         }
         if (!depsFolder.isDerived()) {
            depsFolder.setDerived(true, monitor);
         }

         @SuppressWarnings("null")
         final var depsToCheck = buildFile //
            .getDependencies(sdk, monitor).stream() //
            .collect(Collectors.toMap(d -> d.meta.name + " [" + (d.isDevVersion ? "dev" : d.meta.version) + "]", Function.identity()));

         for (final var folder : depsFolder.members()) {
            if (depsToCheck.containsKey(folder.getName())) {
               final var dep = asNonNullUnsafe(depsToCheck.get(folder.getName()));
               final var rawLoc = folder.getRawLocation();
               if (rawLoc != null && dep.location.equals(rawLoc.toFile().toPath())) {
                  depsToCheck.remove(folder.getName());
               } else {
                  folder.delete(true, monitor); // delete broken folder link
               }
            } else {
               folder.delete(true, monitor); // delete folder link to (now) unused dependency
            }
         }

         for (final var dep : depsToCheck.entrySet()) {
            final var folder = depsFolder.getFolder(dep.getKey());
            folder.createLink(dep.getValue().location.toUri(), IResource.BACKGROUND_REFRESH, monitor);
         }
         return Status.OK_STATUS;
      } catch (final Exception ex) {
         return Haxe4EPlugin.status().createError(ex, "Failed to update 'Haxe Dependencies' list.");
      }
   }
}
