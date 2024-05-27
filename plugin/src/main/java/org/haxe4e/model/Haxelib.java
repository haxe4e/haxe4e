/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.model;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.haxe4e.Haxe4EPlugin;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.collection.EvictingDeque;
import net.sf.jstuff.core.collection.tuple.Tuple2;
import net.sf.jstuff.core.io.Processes.ProcessState;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class Haxelib implements Comparable<Haxelib> {

   private static final String ANY_VER = "<any>";

   public static Haxelib from(final HaxeSDK sdk, final String name, @Nullable String version, final IProgressMonitor monitor)
         throws IOException {
      Args.isDirectoryReadable("sdk.getLibsDir()", sdk.getHaxelibsDir());

      if (version == null || version.isBlank()) {
         version = ANY_VER;
      }

      /*
       * determine expected location on file system
       */
      monitor.setTaskName("Locating haxelib " + name + ":" + version + "...");
      if (ANY_VER.equals(version)) { // search for any version
         final var devFile = sdk.getHaxelibsDir().resolve(name).resolve(".dev");
         if (Files.exists(devFile)) {
            try (var stream = Files.lines(devFile)) {
               final var path = asNullable(stream.findFirst().orElse(null));
               if (path != null) {
                  final var libLocation = Paths.get(path);
                  if (Files.exists(libLocation))
                     return new Haxelib(libLocation, true);
               }
            }
         }

         final var currentFile = sdk.getHaxelibsDir().resolve(name).resolve(".current");
         if (Files.exists(currentFile)) {
            try (var stream = Files.lines(currentFile)) {
               version = asNullable(stream.findFirst().orElse(null));
               if (version != null && !version.isBlank()) {
                  final var libLocation = sdk.getHaxelibsDir().resolve(name).resolve(Strings.replace(version, ".", ","));
                  if (Files.exists(libLocation))
                     return new Haxelib(libLocation, false);
               }
            }
         }

      } else { // search for specific version
         final var libLocation = sdk.getHaxelibsDir().resolve(name).resolve(Strings.replace(version, ".", ","));
         if (Files.exists(libLocation))
            return new Haxelib(libLocation, false);
      }

      if (version == null || version.isBlank()) {
         version = ANY_VER;
      }

      monitor.setTaskName("Installing haxelib " + name + ":" + version);
      final var out = new EvictingDeque<String>(4);
      final var haxelibProcessBuilder = sdk.getHaxelibProcessBuilder( //
         ANY_VER.equals(version) //
               ? List.of("install", name)
               : List.of("install", name, version)) //
         .withWorkingDirectory(sdk.getInstallRoot()) //
         .withRedirectErrorToOutput() //
         .withRedirectOutput(line -> {
            out.add(line);
            monitor.setTaskName(line);
         });

      try {
         final var result = haxelibProcessBuilder //
            .start() //
            .waitForExit(2, TimeUnit.MINUTES) //
            .terminate(10, TimeUnit.SECONDS) //
            .getState();
         if (result != ProcessState.SUCCEEDED)
            throw new IOException("Failed to install haxelib " + name + ":" + version + ":\n" + Strings.join(out));
      } catch (final InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new IOException(ex);
      }
      return from(sdk, name, version, monitor);
   }

   public final boolean isDevVersion;
   public final Path location;
   public final HaxelibJSON meta;

   public Haxelib(final Path directory, final boolean isDevVersion) throws IOException {
      this.isDevVersion = isDevVersion;
      location = directory;
      meta = HaxelibJSON.from(location.resolve(HaxelibJSON.FILENAME));
   }

   @Override
   public int compareTo(final Haxelib obj) {
      return location.compareTo(obj.location);
   }

   @Override
   public boolean equals(@Nullable final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final var other = (Haxelib) obj;
      return location.equals(other.location);
   }

   protected void collectDependencies(final int level, final Map<String, Tuple2<Integer, Haxelib>> collected, final HaxeSDK haxeSDK,
         final IProgressMonitor monitor) {
      for (final var dep : getDirectDependencies(haxeSDK, monitor)) {
         final var alreadyCollected = collected.get(dep.meta.name);
         if (alreadyCollected == null || alreadyCollected.get1() > level) {
            collected.put(dep.meta.name, Tuple2.create(level, dep));

            // transitive dependencies
            dep.collectDependencies(level + 1, collected, haxeSDK, monitor);
         }
      }
   }

   public Collection<Haxelib> getDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) {
      final var deps = new HashMap<String, Tuple2<Integer, Haxelib>>();
      collectDependencies(0, deps, haxeSDK, monitor);
      deps.remove(meta.name); // prevent circular dependencies via nested transitive dependencies
      return deps.values().stream().map(Tuple2::get2).toList();
   }

   public Collection<Haxelib> getDirectDependencies(final HaxeSDK haxeSDK, final IProgressMonitor monitor) {
      final var deps = new HashMap<String, Haxelib>();
      final var depsJSON = meta.dependencies;
      if (depsJSON != null) {
         for (final var dep : depsJSON.entrySet()) {
            if (!deps.containsKey(dep.getKey())) {
               try {
                  deps.put(dep.getKey(), Haxelib.from(haxeSDK, dep.getKey(), dep.getValue(), monitor));
               } catch (final IOException ex) {
                  Haxe4EPlugin.log().error(ex);
               }
            }
         }
      }
      return deps.values();
   }

   @Override
   public int hashCode() {
      return location.hashCode();
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
   }
}
