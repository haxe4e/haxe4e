/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import de.sebthom.eclipse.commons.resources.Resources;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.functional.Suppliers;

/**
 * Virtual build file computed based on conventions in case no physical one exists.
 *
 * @author Sebastian Thomschke
 */
public final class LixVirtualBuildFile extends LixBuildFile {

   private final Supplier<BuildFileContent> content = Suppliers.memoize(() -> {
      final var args = new ArrayList<String>();

      // guess source directory
      args.add("-cp");
      args.add(getProject().getFolder("src").exists() ? "src" : ".");

      // guess libraries
      final var libs = getProject().getFolder("haxe_libraries");
      if (libs.exists()) {
         try {
            libs.accept(res -> {
               if (res.getType() == IResource.FILE && "hxml".equals(res.getFileExtension())) {
                  res.getName();
                  args.add("-lib");
                  args.add(Strings.substringBefore(res.getName(), ".hxml"));
               }
               return true;
            }, IResource.DEPTH_ONE, false);
         } catch (final CoreException ex) {
            throw new RuntimeException(ex);
         }
      }

      final var content = new BuildFileContent(args);
      for (final var arg : content.args) {
         if (arg.endsWith(".hxml")) {
            final var member = asNonNull(location.getParent()).findMember(arg);
            if (member instanceof final IFile file && file.exists()) {
               content.includedBuildFiles.add(new HaxeBuildFile(file));
            }
         }
      }
      return content;
   }, (args, ageMS) -> System.currentTimeMillis() - Resources.lastModified(location) > ageMS);

   public LixVirtualBuildFile(final IProject project) {
      super(project.getFile("<virtual>"));
   }

   @Override
   protected BuildFileContent getBuildFileContent() {
      return content.get();
   }

   @Override
   public Set<IPath> getSourcePaths() {
      final var src = getProject().getFolder("src");
      return Set.of(Path.fromOSString(src.exists() ? "src" : "."));
   }
}
