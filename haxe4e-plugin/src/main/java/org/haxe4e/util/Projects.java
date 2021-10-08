package org.haxe4e.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.EditorReference;
import org.eclipse.ui.part.FileEditorInput;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class Projects {

   public static IProject findProject(final String name, final String natureId) {
      if (Strings.isEmpty(name))
         return null;
      final var project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
      if (!project.exists())
         return null;

      try {
         if (natureId != null && !project.hasNature(natureId))
            return null;
      } catch (final CoreException e) {
         return null;
      }

      return project;
   }

   public static List<IProject> getProjectsWithNature(final String natureId) {
      final var projects = new ArrayList<IProject>();
      for (final var project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
         try {
            if (project.isOpen() && project.hasNature(natureId)) {
               projects.add(project);
            }
         } catch (final CoreException ex) {
            LOG.error(ex);
         }
      }
      return projects;
   }

   public static IProject findProjectFromPartRef(final IWorkbenchPartReference partRef) {
      return findProjectFromPartRef(partRef, null);
   }
   
   public static IProject findProjectFromPartRef(final IWorkbenchPartReference partRef, final String natureId) {
      IProject project = null;
      try {
         if (partRef instanceof EditorReference) {
            final EditorReference editorRef = (EditorReference) partRef;
            final IEditorInput editorInput = editorRef.getEditorInput();
            if (editorInput instanceof FileEditorInput) {
               final FileEditorInput fileEditorInput = (FileEditorInput) editorInput;
               project = fileEditorInput.getFile().getProject();
               if (natureId != null && !project.hasNature(natureId)) {
                  project = null;
               }
            }
         }
      } catch (final Exception ex) {
         LOG.error(ex);
      }
      return project;
   }
   
   public static IProject findProjectFromWindow(final IWorkbenchWindow window) {
      return findProjectFromWindow(window, null);
   }
   
   public static IProject findProjectFromWindow(final IWorkbenchWindow window, final String natureId) {
      IProject project = null;
      try {
         final ISelection iselection = window.getSelectionService().getSelection();
         if (iselection instanceof IStructuredSelection) {
            final IStructuredSelection selection = (IStructuredSelection) iselection;             
            final Object firstElement = selection.getFirstElement();
            if (firstElement instanceof IResource) {
               project = ((IResource) firstElement).getProject();                
            }
         } else {
            final IWorkbenchPage activePage = window.getActivePage();
            final IEditorPart activeEditor = activePage.getActiveEditor();
            if (activeEditor != null) {
               final IEditorInput input = activeEditor.getEditorInput();
               project = input.getAdapter(IProject.class);
               if (project == null) {
                  final IResource resource = input.getAdapter(IResource.class);
                  if (resource != null) {
                     project = resource.getProject();
                  }
               }
            }
         }
         
         if (natureId != null && project != null && !project.hasNature(natureId)) {
            project = null;
         }
      } catch (final Exception ex) {
         LOG.error(ex);
      }
      
      return project;
   }
   
   private Projects() {
   }
}
