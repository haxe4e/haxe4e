/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.project;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.builder.HaxeBuilder;
import org.haxe4e.navigation.HaxeDependenciesUpdater;

import de.sebthom.eclipse.commons.resources.Projects;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectNature implements IProjectNature {

   public static final class AddNatureHandler extends AbstractHandler {

      @Override
      public @Nullable Object execute(final ExecutionEvent event) throws ExecutionException {
         if (HandlerUtil.getCurrentSelection(event) instanceof final IStructuredSelection currentSelection) {
            final var project = Projects.adapt(currentSelection.getFirstElement());
            if (project != null) {
               try {
                  addToProject(project);
                  return Status.OK_STATUS;
               } catch (final CoreException ex) {
                  throw new ExecutionException(ex.getMessage(), ex);
               }
            }
         }
         return Status.CANCEL_STATUS;
      }
   }

   public static final class RemoveNatureHandler extends AbstractHandler {

      @Override
      public @Nullable Object execute(final ExecutionEvent event) throws ExecutionException {
         if (HandlerUtil.getCurrentSelection(event) instanceof final IStructuredSelection currentSelection) {
            final var project = Projects.adapt(currentSelection.getFirstElement());
            if (project != null) {
               try {
                  removeFromProject(project);
                  return Status.OK_STATUS;
               } catch (final CoreException ex) {
                  throw new ExecutionException(ex.getMessage(), ex);
               }
            }
         }
         return Status.CANCEL_STATUS;
      }
   }

   public static final String NATURE_ID = Haxe4EPlugin.PLUGIN_ID + ".project.nature";

   public static void addToProject(final IProject project) throws CoreException {
      Projects.addNature(project, NATURE_ID, null);
   }

   public static boolean hasNature(final @Nullable IProject project) {
      return Projects.hasNature(project, NATURE_ID);
   }

   public static void removeFromProject(final IProject project) throws CoreException {
      Projects.removeNature(project, NATURE_ID, null);
   }

   private IProject project = lateNonNull();

   @Override
   public void configure() throws CoreException {
      HaxeDependenciesUpdater.INSTANCE.onProjectConfigChanged(project);
      Projects.addBuilder(project, HaxeBuilder.ID, null);
   }

   @Override
   public void deconfigure() throws CoreException {
      HaxeDependenciesUpdater.INSTANCE.removeDependenciesFolder(project, null);
      Projects.removeBuilder(project, HaxeBuilder.ID, null);
   }

   @Override
   public IProject getProject() {
      return project;
   }

   @Override
   public void setProject(final IProject project) {
      this.project = project;
   }
}
