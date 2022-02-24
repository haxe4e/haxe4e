/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model.buildsystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.Haxelib;

import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.exception.Exceptions;
import net.sf.jstuff.core.io.RuntimeIOException;
import net.sf.jstuff.core.ref.MutableRef;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuildFile extends BuildFile {

   public HaxeBuildFile(final IFile location) {
      super(BuildSystem.HAXE, location);
   }

   @Override
   public Set<Haxelib> getDirectDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) throws RuntimeIOException {
      Args.notNull("haxeSDK", haxeSDK);
      Args.notNull("monitor", monitor);

      final var deps = new LinkedHashSet<Haxelib>();
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

   @Override
   public Set<Path> getSourcePaths() throws RuntimeIOException {
      final var paths = new LinkedHashSet<Path>();
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

   public List<String> parseArgs() throws RuntimeIOException {
      return parseArgs(location.getLocation().toFile().toPath());
   }

   protected List<String> parseArgs(final Path buildFile) throws RuntimeIOException {
      final var args = new ArrayList<String>();
      final MutableRef<@Nullable Character> quotedWith = MutableRef.of(null);
      final var arg = new StringBuilder();

      try (var lines = Files.lines(buildFile)) {
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
      } catch (final IOException ex) {
         throw Exceptions.wrapAsRuntimeException(ex);
      }
      return args;
   }
}
