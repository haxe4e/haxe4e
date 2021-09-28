/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.core.runtime.IProgressMonitor;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.collection.EvictingDeque;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class Haxelib implements Comparable<Haxelib> {

   public static Haxelib from(final HaxeSDK sdk, final String name, String version, final IProgressMonitor monitor) throws IOException {
      Args.notNull("sdk", sdk);
      Args.exists("sdk.getLibsDir()", sdk.getHaxelibsDir());

      Path libLocation = null;

      /*
       * determine expected location on file system
       */
      monitor.setTaskName("Locating haxelib " + name + "...");
      if (Strings.isNotEmpty(version)) {
         libLocation = sdk.getHaxelibsDir().resolve(name).resolve(Strings.replace(version, ".", ","));
      } else {
         final var devFile = sdk.getHaxelibsDir().resolve(name).resolve(".dev");
         if (Files.exists(devFile)) {
            try (var stream = Files.lines(devFile)) {
               final var path = stream.findFirst().orElse(null);
               if (path != null) {
                  final var p = Paths.get(path);
                  if (Files.exists(p)) {
                     libLocation = p;
                  }
               }
            }
         }

         if (libLocation == null) {
            final var currentFile = sdk.getHaxelibsDir().resolve(name).resolve(".current");
            if (Files.exists(currentFile)) {
               try (var stream = Files.lines(currentFile)) {
                  version = stream.findFirst().orElse("");
                  if (Strings.isNotEmpty(version)) {
                     libLocation = sdk.getHaxelibsDir().resolve(name).resolve(Strings.replace(version, ".", ","));
                  }
               }
            }
         }
      }

      if (libLocation != null && Files.exists(libLocation))
         return new Haxelib(libLocation);

      monitor.setTaskName("Installing haxelib " + name + ":" + version);
      final var out = new EvictingDeque<String>(4);
      final var processBuilder = sdk.getHaxelibProcessBuilder("install", name, version) //
         .withWorkingDirectory(sdk.getPath()) //
         .withRedirectErrorToOutput() //
         .withRedirectOutput(line -> {
            out.add(line);
            monitor.setTaskName(line);
         });

      try {
         switch (processBuilder //
            .start() //
            .waitForExit(2, TimeUnit.MINUTES) //
            .terminate(10, TimeUnit.SECONDS) //
            .getState()) {
            case SUCCEEDED:
               break;
            default:
               throw new IOException("Failed to install haxelib " + name + ":" + version + ":\n" + Strings.join(out));
         }
      } catch (final InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new IOException(ex);
      }
      return from(sdk, name, version, monitor);
   }

   public final Path location;
   public final HaxelibJSON meta;

   private Haxelib(final Path location) throws IOException {
      Args.notNull("location", location);

      this.location = location;
      meta = HaxelibJSON.from(location.resolve("haxelib.json"));
   }

   @Override
   public int compareTo(final Haxelib obj) {
      return location.compareTo(obj.location);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final var other = (Haxelib) obj;
      return location.equals(other.location);
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
