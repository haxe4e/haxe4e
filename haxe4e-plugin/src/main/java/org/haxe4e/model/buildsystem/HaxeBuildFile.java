/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model.buildsystem;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.Haxelib;

import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.io.RuntimeIOException;
import net.sf.jstuff.core.ref.MutableRef;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuildFile extends BuildFile {

   public static List<String> parseArgs(final IFile buildFile) throws RuntimeIOException {
      return parseArgs(asNonNull(buildFile.getLocation()).toFile().toPath());
   }

   public static List<String> parseArgs(final Path buildFile) throws RuntimeIOException {
      if (!Files.exists(buildFile))
         return Collections.emptyList();

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
         throw new RuntimeIOException(ex);
      }
      return args;
   }

   HaxeBuildFile(final BuildSystem bs, final IFile location) {
      super(bs, location);
   }

   public HaxeBuildFile(final IFile location) {
      super(BuildSystem.HAXE, location);
   }

   @Override
   public Set<Haxelib> getDirectDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) throws RuntimeIOException {
      final var deps = new LinkedHashSet<Haxelib>();
      final var args = parseArgs();
      final var libs = getOptionValues(args, arg -> switch (arg) {
         case "-L", "-lib", "--library" -> true;
         default -> false;
      });
      for (final var lib : libs) {
         final var libName = Strings.substringBefore(lib, ":");
         final var libVer = Strings.substringAfter(lib, ":");
         try {
            deps.add(Haxelib.from(haxeSDK, libName, libVer, monitor));
         } catch (final Exception ex) {
            Haxe4EPlugin.log().error(ex);
            UI.run(() -> new NotificationPopup(ex.getMessage()).open());
         }
      }
      return deps;
   }

   protected List<String> getOptionValues(final List<String> args, final Predicate<String> optionNameTester) {
      final var values = new ArrayList<String>(Math.min(args.size() / 2, 8));
      var nextArgIsCollectable = false;
      for (final var arg : args) {
         if (nextArgIsCollectable) {
            values.add(arg);
            nextArgIsCollectable = false;
         } else if (optionNameTester.test(arg)) {
            nextArgIsCollectable = true;
         }
      }
      return values;
   }

   @Override
   public Set<IPath> getSourcePaths() throws RuntimeIOException {
      final var args = parseArgs();
      return getOptionValues(args, arg -> switch (arg) {
         case "-p", "-cp", "--class-path" -> true;
         default -> false;
      }).stream().map(org.eclipse.core.runtime.Path::fromOSString).collect(Collectors.toCollection(LinkedHashSet::new));
   }

   public List<String> parseArgs() throws RuntimeIOException {
      return parseArgs(location);
   }
}
