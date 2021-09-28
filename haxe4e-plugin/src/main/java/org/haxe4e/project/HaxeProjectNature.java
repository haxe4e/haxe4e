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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.navigation.HaxeDependenciesUpdater;
import org.haxe4e.util.LOG;

import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectNature implements IProjectNature {

   /**
    * @return null if status cannot be determine
    */
   public static Boolean hasNature(final IProject project) {
      if (project == null)
         return false;

      try {
         return project.hasNature(NATURE_ID);
      } catch (final CoreException ex) {
         LOG.error(ex);
         return null; // CHECKSTYLE:IGNORE .*
      }
   }

   public static final class AddNatureHandler extends AbstractHandler {

      @Override
      public Object execute(final ExecutionEvent event) throws ExecutionException {
         final var currentSelection = HandlerUtil.getCurrentSelection(event);
         if (currentSelection instanceof IStructuredSelection) {
            final var selectedElement = ((IStructuredSelection) currentSelection).getFirstElement();
            final var resource = Platform.getAdapterManager().getAdapter(selectedElement, IResource.class);
            if (resource != null) {
               final var project = resource.getProject();
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
            final var resource = Platform.getAdapterManager().getAdapter(selectedElement, IResource.class);
            if (resource != null) {
               final var project = resource.getProject();
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
      HaxeDependenciesUpdater.INSTANCE.onHaxeProjectConfigChanged(project);
   }

   public static void removeFromProject(final IProject project) throws CoreException {
      Args.notNull("project", project);
      final var projectConfig = project.getDescription();
      final var natures = projectConfig.getNatureIds();
      if (ArrayUtils.contains(natures, NATURE_ID)) {
         projectConfig.setNatureIds(ArrayUtils.removeElement(natures, NATURE_ID));
         project.setDescription(projectConfig, null);
      }
      final var haxeDepsFolder = project.getFolder(HaxeDependenciesUpdater.HAXE_DEPS_MAGIC_FOLDER_NAME);
      if (haxeDepsFolder.exists() && haxeDepsFolder.isVirtual()) {
         haxeDepsFolder.delete(false, new NullProgressMonitor());
      }
   }

   private IProject project;

   @Override
   public void configure() throws CoreException {
      // invoked when the nature is added
   }

   @Override
   public void deconfigure() throws CoreException {
      // invoked when the nature is removed
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