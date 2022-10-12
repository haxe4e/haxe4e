/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.haxe4e.Haxe4EPlugin;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.SystemUtils;
import net.sf.jstuff.core.functional.Suppliers;
import net.sf.jstuff.core.io.Processes;
import net.sf.jstuff.core.validation.Args;

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
   private static final Supplier<@Nullable HaxeSDK> HAXESDK_FROM_PATH = Suppliers.memoize(() -> {
      final var haxepath = SystemUtils.getEnvironmentVariable(ENV_HAXEPATH, "");
      if (Strings.isBlank(haxepath))
         return null;

      var sdk = new HaxeSDK(Paths.get(haxepath), NekoVM.fromPath());
      if (sdk.isValid())
         return sdk;

      final var haxeCompiler = SystemUtils.findExecutable("haxe", true);
      if (haxeCompiler == null)
         return null;

      sdk = new HaxeSDK(asNonNullUnsafe(haxeCompiler.getParent()), NekoVM.fromPath());
      return sdk.isValid() ? sdk : null;
   }, haxeSDK -> haxeSDK == null ? 15_000 : 60_000);

   /**
    * Tries to locate the Haxe SDK via HAXEPATH and PATH environment variables
    *
    * @return null if not found
    */
   @Nullable
   public static HaxeSDK fromPath() {
      return HAXESDK_FROM_PATH.get();
   }

   private String name;
   private Path installRoot;

   @Nullable
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

      // only to satisfy annotation-based null-safety analysis:
      name = "";
      installRoot = Path.of("");
   }

   public HaxeSDK(final Path installRoot) {
      this.installRoot = installRoot.normalize().toAbsolutePath();
      name = "haxe-" + getVersion();
   }

   public HaxeSDK(final Path installRoot, @Nullable final NekoVM nekoVM) {
      this(installRoot);
      this.nekoVM = nekoVM;
   }

   public HaxeSDK(final String name, final Path installRoot) {
      Args.notBlank("name", name);

      this.name = name;
      this.installRoot = installRoot.normalize().toAbsolutePath();
   }

   public HaxeSDK(final String name, final Path installRoot, @Nullable final NekoVM nekoVM) {
      this(name, installRoot);
      this.nekoVM = nekoVM;
   }

   @Override
   public int compareTo(final HaxeSDK o) {
      return Strings.compare(name, o.name);
   }

   public void configureEnvVars(final Map<String, Object> env) {
      final var neko = getNekoVM();
      if (neko == null)
         throw new IllegalStateException("No Neko VM found.");

      env.merge("PATH", installRoot, //
         (oldValue, haxePath) -> haxePath + File.pathSeparator + oldValue //
      );
      env.merge("PATH", neko.getInstallRoot(), //
         (oldValue, nekoPath) -> nekoPath + File.pathSeparator + oldValue //
      );

      env.put(ENV_HAXEPATH, installRoot);
      env.put(ENV_HAXE_STD_PATH, getStandardLibDir());

      // if specified haxelib will install libs to <haxepath>/lib/...
      // if not specified haxelib will install libs to <user_home>/haxelib/...
      env.put(ENV_HAXELIB_PATH, getHaxelibsDir());
   }

   @Override
   public boolean equals(@Nullable final Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      final var other = (HaxeSDK) obj;
      return Objects.equals(name, other.name) //
         && Objects.equals(installRoot, other.installRoot) //
         && Objects.equals(nekoVM, other.nekoVM);
   }

   @JsonIgnore
   public Path getCompilerExecutable() {
      return installRoot.resolve(SystemUtils.IS_OS_WINDOWS ? "haxe.exe" : "haxe");
   }

   @JsonIgnore
   public Processes.Builder getCompilerProcessBuilder(final boolean cleanEnv) {
      return Processes.builder(getCompilerExecutable()) //
         .withEnvironment(env -> {
            if (cleanEnv) {
               env.clear();
            }
            configureEnvVars(env);
         });
   }

   @JsonIgnore
   public Path getHaxelibExecutable() {
      return installRoot.resolve(SystemUtils.IS_OS_WINDOWS ? "haxelib.exe" : "haxelib");
   }

   @JsonIgnore
   public Processes.Builder getHaxelibProcessBuilder(final List<Object> args) {
      return Processes.builder(getHaxelibExecutable()) //
         .withArgs(args) //
         .withEnvironment(this::configureEnvVars);
   }

   @JsonIgnore
   public Processes.Builder getHaxelibProcessBuilder(final @NonNull Object... args) {
      return Processes.builder(getHaxelibExecutable()) //
         .withArgs(args) //
         .withEnvironment(this::configureEnvVars);
   }

   @JsonIgnore
   public Path getHaxelibsDir() {
      final var pathFromEnv = System.getenv(ENV_HAXELIB_PATH);
      if (pathFromEnv != null) {
         final var p = Paths.get(pathFromEnv);
         if (Files.exists(p))
            return p.normalize().toAbsolutePath();
      }
      return installRoot.resolve("lib").normalize().toAbsolutePath();
   }

   public Path getInstallRoot() {
      return installRoot;
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

   @JsonIgnore
   public Path getStandardLibDir() {
      final var pathFromEnv = System.getenv(ENV_HAXE_STD_PATH);
      if (pathFromEnv != null) {
         final var p = Paths.get(pathFromEnv);
         if (Files.exists(p))
            return p.normalize().toAbsolutePath();
      }
      return installRoot.resolve("std").normalize().toAbsolutePath();
   }

   @Nullable
   @JsonIgnore
   public String getVersion() {
      final var processBuilder = Processes.builder(getCompilerExecutable()).withArg("--version");
      try (var reader = new BufferedReader(new InputStreamReader(processBuilder.start().getStdOut()))) {
         final var version = reader.readLine();
         return Strings.isBlank(version) ? null : version;
      } catch (final IOException ex) {
         Haxe4EPlugin.log().error(ex);
         return null;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, installRoot, nekoVM);
   }

   /**
    * If <code>installRoot</code> actually points to a valid location containing the Haxe compiler
    */
   @JsonIgnore
   public boolean isValid() {
      return isValidCached.get();
   }

   public String toShortString() {
      return name + " (" + installRoot + ")";
   }

   @Override
   public String toString() {
      return "HaxeSDK [name=" + name + ", installRoot=" + installRoot + "]";
   }
}
