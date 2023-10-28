/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNullUnsafe;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.Haxelib;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
public abstract class BuildFile {

   public final BuildSystem buildSystem;
   public final IFile location;

   protected BuildFile(final BuildSystem buildSystem, final IFile location) {
      this.buildSystem = buildSystem;
      this.location = location;
   }

   public boolean exists() {
      return location.exists();
   }

   public Collection<Haxelib> getDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) {
      final var deps = new HashMap<String, Haxelib>();
      for (final var dep : getDirectDependencies(haxeSDK, monitor)) {
         deps.put(dep.meta.name, dep);

         // transitive dependencies
         dep.getDependencies(haxeSDK, monitor).forEach(i -> deps.putIfAbsent(i.meta.name, i));
      }
      return deps.values();
   }

   public abstract Collection<Haxelib> getDirectDependencies(HaxeSDK haxeSDK, IProgressMonitor monitor);

   public IProject getProject() {
      return asNonNullUnsafe(location.getProject());
   }

   public String getProjectRelativePath() {
      return location.getProjectRelativePath().toString();
   }

   public abstract Set<IPath> getSourcePaths();

   @Override
   public String toString() {
      return Strings.toString(this, //
         "project", getProject().getName(), //
         "path", getProjectRelativePath() //
      );
   }
}
