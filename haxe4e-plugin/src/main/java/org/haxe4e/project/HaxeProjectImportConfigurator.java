/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.haxe4e.Constants;
import org.haxe4e.util.LOG;

import net.sf.jstuff.core.collection.Sets;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectImportConfigurator implements ProjectConfigurator {

   @Override
   public Set<File> findConfigurableLocations(final File root, final IProgressMonitor monitor) {
      final Set<File> haxeProjects = new HashSet<>();

      try {
         Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
               // ignore hidden folders
               if (dir.getFileName().startsWith("."))
                  return FileVisitResult.SKIP_SUBTREE;
               return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
               switch (file.getFileName().toString()) {
                  case Constants.DEFAULT_HAXE_BUILD_FILE:
                  case "haxelib.json":
                     haxeProjects.add(file.getParent().toFile());
               }
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (final IOException ex) {
         LOG.error(ex, "Cannot traverse directory tree");
      }
      return haxeProjects;
   }

   @Override
   public boolean shouldBeAnEclipseProject(final IContainer container, final IProgressMonitor monitor) {
      return true;
   }

   @Override
   public Set<IFolder> getFoldersToIgnore(final IProject project, final IProgressMonitor monitor) {
      return Sets.newHashSet( //
         project.getFolder(".git"), //
         project.getFolder("bin"), //
         project.getFolder("dump"), //
         project.getFolder("target") //
      );
   }

   @Override
   public boolean canConfigure(final IProject project, final Set<IPath> ignoredPaths, final IProgressMonitor monitor) {
      // Do nothing, as everything is handled in ListenerService
      return false;
   }

   @Override
   public void configure(final IProject project, final Set<IPath> ignoredPaths, final IProgressMonitor monitor) {
      try {
         HaxeProjectNature.addToProject(project);
      } catch (final CoreException ex) {
         LOG.error(ex);
      }
   }
}
