/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model.buildsystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.Haxelib;

import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.io.RuntimeIOException;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public class LixBuildFile extends HaxeBuildFile {

   public LixBuildFile(final IFile location) {
      super(BuildSystem.LIX, location);
   }

   private String getHaxeLibCachePath() {
      var cachePath = System.getenv("HAXE_LIBCACHE");
      if (Strings.isBlank(cachePath)) {
         cachePath = System.getenv("HAXESHIM_LIBCACHE");
      }
      if (Strings.isBlank(cachePath)) {
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
   public Set<Haxelib> getDirectDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) throws RuntimeIOException {
      Args.notNull("haxeSDK", haxeSDK);
      Args.notNull("monitor", monitor);

      final var haxeLibsFolder = location.getProject().getFolder("haxe_libraries");
      if (!haxeLibsFolder.exists())
         return Collections.emptySet();

      final var haxeLibCachePath = getHaxeLibCachePath();
      final var deps = new LinkedHashSet<Haxelib>();
      final var args = parseArgs();
      final var libs = getOptionValues(args, arg -> switch (arg) {
         case "-L", "-lib", "--library" -> true;
         default -> false;
      });
      for (final var lib : libs) {
         final var libName = Strings.substringBefore(lib, ":");
         try {
            final var libArgs = parseArgs(haxeLibsFolder.getFile(libName + ".hxml"));
            final var cp = Strings.replace(getOptionValues(libArgs, arg -> switch (arg) {
               case "-p", "-cp", "--class-path" -> true;
               default -> false;
            }).get(0), "${HAXE_LIBCACHE}", haxeLibCachePath);

            for (var folder = Paths.get(cp); folder != null && Files.exists(folder); folder = folder.getParent()) {
               if (Files.exists(folder.resolve("haxelib.json"))) {
                  deps.add(new Haxelib(folder, false));
                  break;
               }
            }

         } catch (final Exception ex) {
            Haxe4EPlugin.log().error(ex);
            UI.run(() -> new NotificationPopup(ex.getMessage()).open());
         }
      }
      return deps;
   }
}
