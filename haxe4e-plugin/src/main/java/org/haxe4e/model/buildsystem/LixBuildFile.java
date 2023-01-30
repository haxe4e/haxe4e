/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model.buildsystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.Haxelib;
import org.haxe4e.model.HaxelibJSON;

import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.io.RuntimeIOException;

/**
 * @author Sebastian Thomschke
 */
public class LixBuildFile extends HaxeBuildFile {

   public LixBuildFile(final IFile location) {
      super(BuildSystem.LIX, location);
   }

   private String getHaxeLibCachePath() {
      var cachePath = System.getenv("HAXE_LIBCACHE");
      if (cachePath == null || cachePath.isBlank()) {
         cachePath = System.getenv("HAXESHIM_LIBCACHE");
      }
      if (cachePath == null || cachePath.isBlank()) {
         var haxeShimRoot = System.getenv("HAXE_ROOT");
         if (Strings.isBlank(haxeShimRoot)) {
            haxeShimRoot = System.getenv("HAXESHIM_ROOT");
         }
         if (Strings.isBlank(haxeShimRoot)) {
            if (SystemUtils.IS_OS_WINDOWS) {
               haxeShimRoot = System.getenv("APPDATA") + File.separatorChar + "haxe";
            } else {
               haxeShimRoot = System.getenv("HOME") + File.separatorChar + "haxe";
            }
         }
         cachePath = haxeShimRoot + File.separatorChar + "haxe_libraries";
      }
      return cachePath;
   }

   @Override
   public Collection<Haxelib> getDirectDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) throws RuntimeIOException {
      final var haxeLibsLockFolder = getProject().getFolder("haxe_libraries");
      if (!haxeLibsLockFolder.exists())
         return Collections.emptySet();

      final var deps = new HashMap<String, Haxelib>();
      final var content = getBuildFileContent();
      final var libs = getOptionValues(content.args, arg -> switch (arg) {
         case "-L", "-lib", "--library" -> true;
         default -> false;
      });

      final var haxeLibCachePath = getHaxeLibCachePath();

      for (final var lib : libs) {
         final var libName = Strings.substringBefore(lib, ":");
         try {
            final var libArgs = parseArgs(haxeLibsLockFolder.getFile(libName + ".hxml"));
            final var libCP = Strings.replace(getOptionValues(libArgs, arg -> switch (arg) {
               case "-p", "-cp", "--class-path" -> true;
               default -> false;
            }).get(0), "${HAXE_LIBCACHE}", haxeLibCachePath);

            Path folder = null;
            for (folder = Paths.get(libCP); folder != null && Files.exists(folder); folder = folder.getParent()) {
               if (Files.exists(folder.resolve(HaxelibJSON.FILENAME))) {
                  final var dep = new Haxelib(folder, false);
                  deps.put(dep.meta.name, dep);
                  break;
               }
            }
         } catch (final Exception ex) {
            Haxe4EPlugin.log().error(ex);
            UI.run(() -> new NotificationPopup(ex.getMessage()).open());
         }
      }

      for (final var includedBuildFile : content.includedBuildFiles) {
         includedBuildFile.getDirectDependencies(haxeSDK, monitor).forEach(i -> deps.putIfAbsent(i.meta.name, i));
      }
      return deps.values();
   }
}
