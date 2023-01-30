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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.Haxelib;
import org.haxe4e.model.HaxelibJSON;

import de.sebthom.eclipse.commons.resources.Resources;
import de.sebthom.eclipse.commons.ui.UI;
import de.sebthom.eclipse.commons.ui.widgets.NotificationPopup;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.functional.Suppliers;
import net.sf.jstuff.core.io.RuntimeIOException;
import net.sf.jstuff.core.ref.MutableRef;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuildFile extends BuildFile {

   protected static List<String> getOptionValues(final List<String> args, final Predicate<String> optionNameTester) {
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

   public static List<String> parseArgs(final IFile buildFile) throws RuntimeIOException {
      return parseArgs(asNonNull(buildFile.getLocation()).toFile().toPath());
   }

   public static List<String> parseArgs(final Path buildFile) throws RuntimeIOException {
      if (!Files.exists(buildFile))
         return Collections.emptyList();

      final var args = new ArrayList<String>();
      try (var lines = Files.lines(buildFile)) {
         final MutableRef<@Nullable Character> quotedWith = MutableRef.of(null);
         final var arg = new StringBuilder();
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

   protected static final class BuildFileContent {
      final List<String> args;
      final List<HaxeBuildFile> includedBuildFiles = new ArrayList<>(4);

      BuildFileContent(final List<String> args) {
         this.args = args;
      }
   }

   private final Supplier<BuildFileContent> content = Suppliers.memoize(() -> {
      final var content = new BuildFileContent(parseArgs(location));
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

   HaxeBuildFile(final BuildSystem bs, final IFile location) {
      super(bs, location);

   }

   public HaxeBuildFile(final IFile location) {
      this(BuildSystem.HAXE, location);
   }

   protected BuildFileContent getBuildFileContent() {
      return content.get();
   }

   @Override
   public Collection<Haxelib> getDirectDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) throws RuntimeIOException {
      final var deps = new HashMap<String, Haxelib>();
      final var content = getBuildFileContent();
      final var libs = getOptionValues(content.args, arg -> switch (arg) {
         case "-L", "-lib", "--library" -> true;
         default -> false;
      });

      for (final String lib : libs) {
         final var libElems = Strings.split(lib, ":", 2);
         try {
            final var dep = Haxelib.from(haxeSDK, libElems[0], libElems.length > 1 ? libElems[1] : null, monitor);
            deps.put(dep.meta.name, dep);
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

   private final Supplier<Set<IPath>> getSourcePaths = Suppliers.memoize(() -> {
      final var args = parseArgs();
      final var sourcePaths = getOptionValues(args, arg -> switch (arg) {
         case "-p", "-cp", "--class-path" -> true;
         default -> false;
      }).stream().map(org.eclipse.core.runtime.Path::fromOSString).collect(Collectors.toCollection(LinkedHashSet::new));

      if (sourcePaths.isEmpty()) {
         final var jsonFile = getProject().getFile(HaxelibJSON.FILENAME);
         if (jsonFile.exists()) {
            try {
               final var json = HaxelibJSON.from(asNonNull(jsonFile.getLocation()).toFile().toPath());
               final var cp = json.classPath;
               if (cp != null) {
                  final var cpFolder = getProject().getFolder(cp);
                  if (cpFolder.exists())
                     return Set.of(org.eclipse.core.runtime.Path.fromOSString(cp));
               }
            } catch (final IOException ex) {
               Haxe4EPlugin.log().error(ex);
               UI.run(() -> new NotificationPopup(ex.getMessage()).open());
            }
         }
      }
      return sourcePaths;
   }, (args, ageMS) -> System.currentTimeMillis() - Resources.lastModified(location) > ageMS);

   @Override
   public Set<IPath> getSourcePaths() throws RuntimeIOException {
      return getSourcePaths.get();
   }

   public List<String> parseArgs() throws RuntimeIOException {
      return getBuildFileContent().args;
   }
}
