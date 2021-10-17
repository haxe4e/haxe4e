/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.haxe4e.Haxe4EPlugin;

import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.ref.MutableRef;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuildFile {

   public final Path location;

   public HaxeBuildFile(final File location) {
      this.location = location.toPath();
   }

   public HaxeBuildFile(final Path location) {
      this.location = location;
   }

   public boolean exists() {
      return Files.exists(location);
   }

   public List<String> parseArgs() throws IOException {
      final var args = new ArrayList<String>();
      final var quotedWith = new MutableRef<Character>(null);
      final var arg = new StringBuilder();

      try (var lines = Files.lines(location)) {
         lines.forEach(line -> {
            for (final var ch : line.toCharArray()) {
               switch (ch) {
                  case '#':
                     if (quotedWith.get() == null) {
                        if (arg.length() > 0) {
                           args.add(arg.toString());
                           arg.setLength(0);
                        }
                        return;
                     }
                     arg.append('#');
                     break;

                  case '\'':
                     if (quotedWith.get() == null) {
                        quotedWith.set(ch);
                     } else if (quotedWith.get() == '\'') {
                        args.add(arg.toString());
                        arg.setLength(0);
                        quotedWith.set(null);
                     } else {
                        arg.append(ch);
                     }
                     break;

                  case '"':
                     if (quotedWith.get() == null) {
                        quotedWith.set(ch);
                     } else if (quotedWith.get() == '"') {
                        args.add(arg.toString());
                        arg.setLength(0);
                        quotedWith.set(null);
                     } else {
                        arg.append(ch);
                     }
                     break;

                  default:
                     if (Character.isWhitespace(ch) && quotedWith.get() == null) {
                        if (arg.length() > 0) {
                           args.add(arg.toString());
                           arg.setLength(0);
                        }
                     } else {
                        arg.append(ch);
                     }
               }
            }

            if (arg.length() > 0 && quotedWith.get() == null) {
               args.add(arg.toString());
               arg.setLength(0);
            }
         });
      }
      return args;
   }

   public Set<Haxelib> getDirectDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) throws IOException {
      Args.notNull("haxeSDK", haxeSDK);
      Args.notNull("monitor", monitor);

      final var deps = new TreeSet<Haxelib>();
      final var args = parseArgs();
      var nextArgIsLibrary = false;
      for (final var arg : args) {
         if (nextArgIsLibrary) {
            final var libName = Strings.substringBefore(arg, ":");
            final var libVer = Strings.substringAfter(arg, ":");
            try {
               deps.add(Haxelib.from(haxeSDK, libName, libVer, monitor));
            } catch (final IOException ex) {
               Haxe4EPlugin.log().error(ex);
               UI.run(() -> new NotificationPopup(ex.getMessage()).open());
            }
            nextArgIsLibrary = false;
         } else {
            switch (arg) {
               case "-L":
               case "-lib":
               case "--library":
                  nextArgIsLibrary = true;
                  break;
            }
         }
      }
      return deps;
   }

   public Set<Path> getSourcePaths() throws IOException {
      final var paths = new TreeSet<Path>();
      final var args = parseArgs();
      var nextArgIsSourcePath = false;
      for (final var arg : args) {
         if (nextArgIsSourcePath) {
            paths.add(Paths.get(arg));
            nextArgIsSourcePath = false;
         } else {
            switch (arg) {
               case "-p":
               case "-cp":
               case "--class-path":
                  nextArgIsSourcePath = true;
                  break;
            }
         }
      }
      return paths;
   }
}
