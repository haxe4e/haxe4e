/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.haxe4e.Haxe4EPlugin;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.concurrent.Threads;
import net.sf.jstuff.core.functional.Suppliers;
import net.sf.jstuff.core.io.Processes;
import net.sf.jstuff.core.validation.Args;

/**
 * @author Sebastian Thomschke
 */
public final class NekoVM implements Comparable<NekoVM> {

   private static final String ENV_NEKOPATH = "NEKOPATH";
   private static final String ENV_NEKO_INSTPATH = "NEKO_INSTPATH";

   @JsonIgnore
   private static final Supplier<NekoVM> NEKO_FROM_PATH = Suppliers.memoize(() -> {
      final var nekoPath = System.getenv(ENV_NEKOPATH);
      if (Strings.isNotBlank(nekoPath)) {
         final var sdk = new NekoVM(Paths.get(nekoPath));
         if (sdk.isValid())
            return sdk;
      }

      final var nekoInstPath = System.getenv(ENV_NEKO_INSTPATH);
      if (Strings.isNotBlank(nekoInstPath)) {
         final var sdk = new NekoVM(Paths.get(nekoInstPath));
         if (sdk.isValid())
            return sdk;
      }

      final var nekoExe = SystemUtils.findExecutable("neko", true);
      if (nekoExe != null) {
         final var sdk = new NekoVM(nekoExe.getParent());
         if (sdk.isValid())
            return sdk;
      }
      return null;
   }, nekoVM -> nekoVM == null ? 15_000 : 60_000);

   /**
    * Tries to locate the Neko VM via NEKOPATH and PATH environment variables
    */
   public static NekoVM fromPath() {
      return NEKO_FROM_PATH.get();
   }

   private String name;
   private Path path;

   @JsonIgnore
   private final Supplier<Boolean> isValidCached = Suppliers.memoize(() -> {
      final var exe = getExecutable();
      if (!Files.isExecutable(exe))
         return false;

      final var out = new StringBuilder();
      try {
         Processes.builder(exe) //
            .withRedirectOutput(out) //
            .start() //
            .waitForExit(10, TimeUnit.SECONDS) //
            .terminate(5, TimeUnit.SECONDS);
      } catch (final IOException ex) {
         // ignore
      } catch (final InterruptedException ex) {
         Threads.handleInterruptedException(ex);
      }
      return Strings.contains(out, "NekoVM");
   }, valid -> valid ? 60_000 : 15_000);

   @SuppressWarnings("unused")
   private NekoVM() {
      // for Jackson
   }

   public NekoVM(final Path path) {
      Args.notNull("path", path);

      this.path = path.toAbsolutePath();
      name = "neko-" + getVersion();
   }

   public NekoVM(final String name, final Path path) {
      Args.notBlank("name", name);
      Args.notNull("path", path);

      this.name = name;
      this.path = path.toAbsolutePath();
   }

   @Override
   public int compareTo(final NekoVM o) {
      return Strings.compare(name, o.name);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final var other = (NekoVM) obj;
      return Objects.equals(name, other.name) //
         && Objects.equals(path, other.path);
   }

   @JsonIgnore
   public Path getExecutable() {
      return path.resolve(SystemUtils.IS_OS_WINDOWS ? "neko.exe" : "neko");
   }

   public String getName() {
      return name;
   }

   public Path getPath() {
      return path;
   }

   @JsonIgnore
   public String getVersion() {
      if (!isValid())
         return null;

      final var processBuilder = Processes.builder(getExecutable());
      try (var reader = new BufferedReader(new InputStreamReader(processBuilder.start().getStdOut()))) {
         final var version = Strings.substringBetween(reader.readLine(), "NekoVM ", " (c)");
         return Strings.isBlank(version) ? null : version;
      } catch (final IOException ex) {
         Haxe4EPlugin.log().error(ex);
         return null;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, path);
   }

   /**
    * If <code>path</code> actually points to a valid location containing the Haxe compiler
    */
   @JsonIgnore
   public boolean isValid() {
      final var valid = isValidCached.get();
      return valid;
   }

   public String toShortString() {
      return name + " (" + path + ")";
   }

   @Override
   public String toString() {
      return "NekoVM [name=" + name + ", path=" + path + "]";
   }
}
