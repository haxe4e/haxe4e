/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.builder.HaxeBuilder;
import org.haxe4e.navigation.HaxeDependenciesUpdater;

import de.sebthom.eclipse.commons.resources.Projects;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectNature implements IProjectNature {

   public static final class AddNatureHandler extends AbstractHandler {

      @Override
      public Object execute(final ExecutionEvent event) throws ExecutionException {
         final var currentSelection = HandlerUtil.getCurrentSelection(event);
         if (currentSelection instanceof IStructuredSelection) {
            final var selectedElement = ((IStructuredSelection) currentSelection).getFirstElement();
            final var project = Projects.adapt(selectedElement);
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
      public Object execute(final ExecutionEvent event) throws ExecutionException {
         final var currentSelection = HandlerUtil.getCurrentSelection(event);
         if (currentSelection instanceof IStructuredSelection) {
            final var selectedElement = ((IStructuredSelection) currentSelection).getFirstElement();
            final var project = Projects.adapt(selectedElement);
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
      Args.notNull("project", project);
      final var projectConfig = project.getDescription();
      final var natures = projectConfig.getNatureIds();
      if (!ArrayUtils.contains(natures, NATURE_ID)) {
         projectConfig.setNatureIds(ArrayUtils.add(natures, NATURE_ID));
         project.setDescription(projectConfig, null);
      }
   }

   /**
    * @return null if status cannot be determine
    */
   public static Boolean hasNature(final IProject project) {
      if (project == null)
         return false;

      try {
         return project.hasNature(NATURE_ID);
      } catch (final CoreException ex) {
         Haxe4EPlugin.log().error(ex);
         return null; // CHECKSTYLE:IGNORE .*
      }
   }

   public static void removeFromProject(final IProject project) throws CoreException {
      Args.notNull("project", project);
      final var projectConfig = project.getDescription();
      final var natures = projectConfig.getNatureIds();
      if (ArrayUtils.contains(natures, NATURE_ID)) {
         projectConfig.setNatureIds(ArrayUtils.removeElement(natures, NATURE_ID));
         project.setDescription(projectConfig, null);
      }
   }

   private IProject project;

   private void addBuilder(final String builderId) throws CoreException {
      final var desc = project.getDescription();
      final var commands = desc.getBuildSpec();

      for (final var command : commands) {
         if (command.getBuilderName().equals(builderId))
            return;
      }
      final var command = desc.newCommand();
      command.setBuilderName(builderId);
      desc.setBuildSpec(ArrayUtils.add(commands, command));
      project.setDescription(desc, null);
   }

   @Override
   public void configure() throws CoreException {
      HaxeDependenciesUpdater.INSTANCE.onHaxeProjectConfigChanged(project);
      addBuilder(HaxeBuilder.ID);
   }

   @Override
   public void deconfigure() throws CoreException {
      HaxeDependenciesUpdater.INSTANCE.removeDependenciesFolder(project, new NullProgressMonitor());
      removeBuilder(HaxeBuilder.ID);
   }

   @Override
   public IProject getProject() {
      return project;
   }

   private void removeBuilder(final String builderId) throws CoreException {
      final var desc = project.getDescription();
      final var commands = desc.getBuildSpec();
      for (final var command : commands) {
         if (command.getBuilderName().equals(builderId)) {
            desc.setBuildSpec(ArrayUtils.removeElement(commands, command));
            project.setDescription(desc, null);
            return;
         }
      }
   }

   @Override
   public void setProject(final IProject project) {
      this.project = project;
   }
}
