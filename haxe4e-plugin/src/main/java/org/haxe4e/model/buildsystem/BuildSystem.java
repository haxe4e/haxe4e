/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model.buildsystem;

import static java.util.Collections.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Sebastian Thomschke
 */
public enum BuildSystem {

   /**
    * https://haxe.org/manual/compiler-usage-hxml.html
    */
   HAXE("hxml", Arrays.asList("build.hxml", "tests.hxml"), Arrays.asList("extraParams.hxml")),

   /**
    * https://github.com/haxelime/lime
    */
   LIME("xml", Arrays.asList("project.xml", "include.xml"), emptyList());

   @Nullable
   public static BuildSystem guessBuildSystemOfBuildFile(final File location) {
      return guessBuildSystemOfBuildFile(location.toPath());
   }

   @Nullable
   public static BuildSystem guessBuildSystemOfBuildFile(final Path location) {
      final var path = location.toAbsolutePath().toString();

      for (final var buildSystem : BuildSystem.values()) {
         for (final var defaultBuildFileName : buildSystem.getDefaultBuildFileNames()) {
            if (path.endsWith(defaultBuildFileName))
               return buildSystem;
         }
      }

      for (final var buildSystem : BuildSystem.values()) {
         if (path.endsWith("." + buildSystem.getBuildFileExtension()))
            return buildSystem;
      }
      return null;
   }

   private final String buildFileExtension;
   private final SortedSet<String> defaultBuildFileNames;
   private final SortedSet<String> ignorableBuildFileNames;

   BuildSystem(final String buildFileExtension, final List<String> defaultBuildFileNames, final List<String> ignorableBuildFileNames) {
      this.buildFileExtension = buildFileExtension;
      this.defaultBuildFileNames = unmodifiableSortedSet(new TreeSet<>(defaultBuildFileNames));
      this.ignorableBuildFileNames = unmodifiableSortedSet(new TreeSet<>(ignorableBuildFileNames));
   }

   public String getBuildFileExtension() {
      return buildFileExtension;
   }

   public List<BuildFile> getBuildFiles(final IProject project, final boolean excludeIgnorableBuildFiles) throws CoreException {
      final var buildFiles = new ArrayList<BuildFile>();
      project.accept(res -> {
         if (res.isVirtual() || res.isLinked())
            return false;

         if (res instanceof IFile && buildFileExtension.equals(res.getFileExtension())) {
            if (!excludeIgnorableBuildFiles || !ignorableBuildFileNames.contains(res.getName())) {
               buildFiles.add(toBuildFile((IFile) res));
            }
         }
         return true;
      });

      return buildFiles;
   }

   public SortedSet<String> getDefaultBuildFileNames() {
      return defaultBuildFileNames;
   }

   public BuildFile toBuildFile(final IFile buildFilePath) {
      switch (this) {
         case HAXE:
            return new HaxeBuildFile(buildFilePath);
         case LIME:
            return new LimeBuildFile(buildFilePath);
         default:
            throw new UnsupportedOperationException("Unsupported build-system: " + this);
      }
   }
}
