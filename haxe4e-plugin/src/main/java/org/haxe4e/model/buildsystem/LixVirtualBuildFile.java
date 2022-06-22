/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model.buildsystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.io.RuntimeIOException;

/**
 * Virtual build file computed based on conventions in case no physical one exists.
 *
 * @author Sebastian Thomschke
 */
public final class LixVirtualBuildFile extends LixBuildFile {

   public LixVirtualBuildFile(final IProject project) {
      super(project.getFile("<virtual>"));
   }

   @Override
   public List<String> parseArgs() throws RuntimeIOException {
      final var args = new ArrayList<String>();

      // guess source directory
      args.add("-cp");
      args.add(location.getProject().getFolder("src").exists() ? "src" : ".");

      // guess libraries
      final var libs = location.getProject().getFolder("haxe_libraries");
      if (libs.exists()) {
         try {
            libs.accept(res -> {
               if (res.getType() == IResource.FILE && res.getFileExtension().equals("hxml")) {
                  res.getName();
                  args.add("-lib");
                  args.add(Strings.substringBefore(res.getName(), ".hxml"));
                  return true;
               }
               return true;
            }, IResource.DEPTH_ONE, false);
         } catch (final CoreException ex) {
            throw new RuntimeException(ex);
         }
      }

      return args;
   }

   @Override
   public Set<Path> getSourcePaths() throws RuntimeIOException {
      return Set.of(Paths.get(location.getProject().getFolder("src").exists() ? "src" : "."));
   }
}
