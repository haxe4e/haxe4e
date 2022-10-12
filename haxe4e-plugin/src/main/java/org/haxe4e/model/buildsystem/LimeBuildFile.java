/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model.buildsystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
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
   public Set<Path> getSourcePaths() throws RuntimeIOException, XMLException {
      final var paths = new LinkedHashSet<Path>();
      final var domFile = parseFile();
      for (final var srcNode : domFile.findNodes("/project/source")) {
         final var path = ((Element) srcNode).getAttribute("path");
         if (path != null && !path.isBlank()) {
            try {
               paths.add(Paths.get(path));
            } catch (final InvalidPathException ex) {
               Haxe4EPlugin.log().error(ex);
               UI.run(() -> new NotificationPopup(ex.getMessage()).open());
            }
         }
      }
      return paths;
   }

   protected DOMFile parseFile() throws RuntimeIOException, XMLException {
      return parseFile(location.getLocation().toFile());
   }

   protected DOMFile parseFile(final File buildFile) throws RuntimeIOException, XMLException {
      try {
         return new DOMFile(buildFile);
      } catch (final IOException ex) {
         throw Exceptions.wrapAsRuntimeException(ex);
      }
   }
}
