/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.Nullable;
import org.haxe4e.util.LOG;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.functional.Suppliers;
import net.sf.jstuff.core.io.Processes;
import net.sf.jstuff.core.validation.Args;
import net.sf.jstuff.core.validation.Assert;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeSDK implements Comparable<HaxeSDK> {

   /**
    * Points to a folder containing the Haxe compiler
    */
   public static final String ENV_HAXEPATH = "HAXEPATH";

   /**
    * Points to a folder containing downloaded haxelibs
    */
   public static final String ENV_HAXELIB_PATH = "HAXELIB_PATH";

   /**
    * Points to a folder containing the Haxe standard library
    */
   public static final String ENV_HAXE_STD_PATH = "HAXE_STD_PATH";

   @JsonIgnore
   private static final Supplier<HaxeSDK> HAXESDK_FROM_PATH = Suppliers.memoize(() -> {
      final var haxepath = System.getenv(ENV_HAXEPATH);
      if (Strings.isBlank(haxepath))
         return null;

      var sdk = new HaxeSDK(Paths.get(haxepath), NekoVM.fromPath());
      if (sdk.isValid())
         return sdk;

      final var haxeCompiler = SystemUtils.findExecutable("haxe", true);
      if (haxeCompiler == null)
         return null;

      sdk = new HaxeSDK(haxeCompiler.getParent(), NekoVM.fromPath());
      return sdk.isValid() ? sdk : null;
   }, haxeSDK -> haxeSDK == null ? 15_000 : 60_000);

   /**
    * Tries to locate the Haxe SDK via HAXEPATH and PATH environment variables
    *
    * @return null if not found
    */
   public static HaxeSDK fromPath() {
      return HAXESDK_FROM_PATH.get();
   }

   private String name;
   private Path path;
   private NekoVM nekoVM;

   @JsonIgnore
   private final Supplier<Boolean> isValidCached = Suppliers.memoize(() -> {
      final var compiler = getCompilerExecutable();
      if (!Files.isExecutable(compiler))
         return false;

      final var processBuilder = Processes.builder(compiler);
      try (var reader = new BufferedReader(new InputStreamReader(processBuilder.start().getStdOut()))) {
         if (reader.readLine().contains("Haxe Compiler"))
            return true;
      } catch (final IOException ex) {
         // ignore
      }
      return false;
   }, valid -> valid ? 60_000 : 15_000);

   @SuppressWarnings("unused")
   private HaxeSDK() {
      // for Jackson
   }

   public HaxeSDK(final Path path) {
      Args.notNull("path", path);

      this.path = path.toAbsolutePath();
      name = "haxe-" + getVersion();
   }

   public HaxeSDK(final Path path, final NekoVM nekoVM) {
      Args.notNull("path", path);
      Args.notNull("nekoVM", nekoVM);

      this.path = path.toAbsolutePath();
      name = "haxe-" + getVersion();
      this.nekoVM = nekoVM;
   }

   public HaxeSDK(final String name, final Path path) {
      Args.notBlank("name", name);
      Args.notNull("path", path);

      this.name = name;
      this.path = path.toAbsolutePath();
   }

   public HaxeSDK(final String name, final Path path, final NekoVM nekoVM) {
      Args.notBlank("name", name);
      Args.notNull("path", path);
      Args.notNull("nekoVM", nekoVM);

      this.name = name;
      this.path = path.toAbsolutePath();
      this.nekoVM = nekoVM;
   }

   @Override
   public int compareTo(final HaxeSDK o) {
      return Strings.compare(name, o.name);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final var other = (HaxeSDK) obj;
      return Objects.equals(name, other.name) //
         && Objects.equals(path, other.path) //
         && Objects.equals(nekoVM, other.nekoVM);
   }

   @JsonIgnore
   public Path getCompilerExecutable() {
      return path.resolve(SystemUtils.IS_OS_WINDOWS ? "haxe.exe" : "haxe");
   }

   @JsonIgnore
   public Path getHaxelibExecutable() {
      return path.resolve(SystemUtils.IS_OS_WINDOWS ? "haxelib.exe" : "haxelib");
   }

   @JsonIgnore
   public Processes.Builder getHaxelibProcessBuilder(final String... args) {
      final var neko = getNekoVM();
      Assert.notNull(neko, "No Neko VM found.");

      return Processes.builder(getHaxelibExecutable()) //
         .withArgs(args) //
         .withEnvironment(env -> {
            env.merge("PATH", neko.getPath().toString(), //
               (oldValue, nekoPath) -> nekoPath + File.pathSeparator + oldValue //
            );

            env.put(ENV_HAXEPATH, path);
            env.put(ENV_HAXE_STD_PATH, getStandardLibDir());

            // if specified haxelib will install libs to <haxepath>/lib/...
            // if not specified haxelib will install libs to <user_home>/haxelib/...
            env.put(ENV_HAXELIB_PATH, getHaxelibsDir());
         });
   }

   @JsonIgnore
   public Path getHaxelibsDir() {
      final var pathFromEnv = System.getenv(ENV_HAXELIB_PATH);
      if (pathFromEnv != null) {
         final var p = Paths.get(pathFromEnv);
         if (Files.exists(p))
            return p;
      }
      return path.resolve("lib");
   }

   public String getName() {
      return name;
   }

   /**
    * Property can be removed once haxelib does not rely on NekoVM anymore.
    *
    * @return null if no NekoVM is configured or found on PATH.
    */
   @Nullable
   public NekoVM getNekoVM() {
      return nekoVM == null ? NekoVM.fromPath() : nekoVM;
   }

   public Path getPath() {
      return path;
   }

   @JsonIgnore
   public Path getStandardLibDir() {
      final var pathFromEnv = System.getenv(ENV_HAXE_STD_PATH);
      if (pathFromEnv != null) {
         final var p = Paths.get(pathFromEnv);
         if (Files.exists(p))
            return p;
      }
      return path.resolve("std");
   }

   @JsonIgnore
   public String getVersion() {
      if (!isValid())
         return null;

      final var processBuilder = Processes.builder(getCompilerExecutable()).withArg("--version");
      try (var reader = new BufferedReader(new InputStreamReader(processBuilder.start().getStdOut()))) {
         final var version = reader.readLine();
         return Strings.isBlank(version) ? null : version;
      } catch (final IOException ex) {
         LOG.error(ex);
         return null;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, path, nekoVM);
   }

   /**
    * If <code>path</code> actually points to a valid location containing the Haxe compiler
    */
   @JsonIgnore
   public boolean isValid() {
      return isValidCached.get();
   }

   public String toShortString() {
      return name + " (" + path + ")";
   }

   @Override
   public String toString() {
      return "HaxeSDK [name=" + name + ", path=" + path + "]";
   }
}
