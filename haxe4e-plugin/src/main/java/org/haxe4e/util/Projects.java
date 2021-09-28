package org.haxe4e.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
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

   private Projects() {
   }
}
