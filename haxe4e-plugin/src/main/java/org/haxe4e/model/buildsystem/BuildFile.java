/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

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

   public abstract Set<Haxelib> getDirectDependencies(HaxeSDK haxeSDK, IProgressMonitor monitor);

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
