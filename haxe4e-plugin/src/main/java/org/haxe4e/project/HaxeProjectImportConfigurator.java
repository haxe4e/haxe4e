/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.haxe4e.Haxe4EPlugin;

import net.sf.jstuff.core.collection.Sets;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeProjectImportConfigurator implements ProjectConfigurator {

   @Override
   public Set<File> findConfigurableLocations(final File root, final @Nullable IProgressMonitor monitor) {
      final var haxeProjects = new HashSet<File>();

      try {
         Files.walkFileTree(root.toPath(), EnumSet.noneOf(FileVisitOption.class), 2, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
               // ignore hidden folders
               final var parts = dir.iterator();
               while (parts.hasNext()) {
                  final var part = parts.next();
                  if (part.toString().startsWith("."))
                     return FileVisitResult.SKIP_SUBTREE;
               }
               return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
               final var fileName = file.getFileName().toString();
               if (fileName.endsWith(".hxml") || "haxelib.json".equals(fileName)) {
                  haxeProjects.add(file.getParent().toFile());
               }
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (final IOException ex) {
         Haxe4EPlugin.log().error(ex, "Cannot traverse directory tree");
      }
      return haxeProjects;
   }

   @Override
   public boolean shouldBeAnEclipseProject(final IContainer container, final @Nullable IProgressMonitor monitor) {
      return true;
   }

   @Override
   public Set<IFolder> getFoldersToIgnore(final IProject project, final @Nullable IProgressMonitor monitor) {
      final var result = Sets.newHashSet( //
         project.getFolder("bin"), //
         project.getFolder("dump"), //
         project.getFolder("target") //
      );

      try {
         project.accept(res -> {
            if (res.isVirtual() || res.isLinked() || res.isHidden() || res.getType() != IResource.FOLDER)
               return false;

            // ignore all hidden folders
            if (res.getName().startsWith(".")) {
               result.add((IFolder) res);
            }
            return true;
         });
      } catch (final CoreException ex) {
         Haxe4EPlugin.log().error(ex);
      }
      return result;
   }

   @Override
   public boolean canConfigure(final IProject project, final Set<IPath> ignoredPaths, final @Nullable IProgressMonitor monitor) {
      return false;
   }

   @Override
   public void configure(final IProject project, final Set<IPath> ignoredPaths, final @Nullable IProgressMonitor monitor) {
      try {
         HaxeProjectNature.addToProject(project);
      } catch (final CoreException ex) {
         Haxe4EPlugin.log().error(ex);
      }
   }
}
