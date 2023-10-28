/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.Haxelib;
import org.w3c.dom.Element;

import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.exception.Exceptions;
import net.sf.jstuff.core.io.RuntimeIOException;
import net.sf.jstuff.xml.DOMFile;
import net.sf.jstuff.xml.XMLException;

/**
 * @author Sebastian Thomschke
 */
public class LimeBuildFile extends BuildFile {

   public LimeBuildFile(final IFile location) {
      super(BuildSystem.LIME, location);
   }

   @Override
   public Set<Haxelib> getDirectDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) throws RuntimeIOException,
      XMLException {
      final var deps = new LinkedHashSet<Haxelib>();
      final var domFile = parseFile();
      for (final var srcNode : domFile.findNodes("/project/haxelib")) {
         final var libName = ((Element) srcNode).getAttribute("name");
         if (libName != null && !libName.isBlank()) {
            final var libVer = ((Element) srcNode).getAttribute("version");
            try {
               deps.add(Haxelib.from(haxeSDK, libName, libVer, monitor));
            } catch (final IOException ex) {
               Haxe4EPlugin.log().error(ex);
               UI.run(() -> new NotificationPopup(ex.getMessage()).open());
            }
         }
      }
      return deps;
   }

   @Override
   public Set<IPath> getSourcePaths() throws RuntimeIOException, XMLException {
      final var paths = new LinkedHashSet<IPath>();
      final var domFile = parseFile();
      for (final var srcNode : domFile.findNodes("/project/source")) {
         final var path = ((Element) srcNode).getAttribute("path");
         if (path != null && !path.isBlank()) {
            try {
               paths.add(org.eclipse.core.runtime.Path.fromOSString(path));
            } catch (final InvalidPathException ex) {
               Haxe4EPlugin.log().error(ex);
               UI.run(() -> new NotificationPopup(ex.getMessage()).open());
            }
         }
      }
      return paths;
   }

   protected DOMFile parseFile() throws RuntimeIOException, XMLException {
      return parseFile(asNonNull(location.getLocation()).toFile());
   }

   protected DOMFile parseFile(final File buildFile) throws RuntimeIOException, XMLException {
      try {
         return new DOMFile(buildFile);
      } catch (final IOException ex) {
         throw Exceptions.wrapAsRuntimeException(ex);
      }
   }
}
