/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.langserver;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.prefs.HaxeWorkspacePreference;
import org.haxe4e.util.TreeBuilder;
import org.haxe4e.util.io.LinePrefixingTeeInputStream;
import org.haxe4e.util.io.LinePrefixingTeeOutputStream;

import de.sebthom.eclipse.commons.resources.Projects;
import net.sf.jstuff.core.Strings;

/**
 * Runs the node.js based embedded haxe-language-server.
 *
 * See https://github.com/vshaxe/haxe-language-server
 *
 * @author Sebastian Thomschke
 */
public final class HaxeLangServerLauncher extends ProcessStreamConnectionProvider {

   public HaxeLangServerLauncher() throws IOException {
      final var languageServerJS = Haxe4EPlugin.resources().extract("langsrv/haxe-language-server.min.js");
      setWorkingDirectory(SystemUtils.getUserDir().getAbsolutePath());
      setCommands(Arrays.asList( //
         NodeJSManager.getNodeJsLocation().getAbsolutePath(), //
         languageServerJS.getAbsolutePath() //
      ));
   }

   @Override
   public @Nullable InputStream getErrorStream() {
      final var stream = super.getErrorStream();
      if (!HaxeWorkspacePreference.isLangServTraceIO())
         return stream;

      if (stream == null)
         return null;
      return new LinePrefixingTeeInputStream(stream, System.out, "SRVERR >> ");
   }

   @Override
   public Map<String, Object> getInitializationOptions(final @Nullable URI projectRootUri) {
      final var project = Projects.findProjectOfResource(projectRootUri);
      final @Nullable HaxeSDK haxeSDK;
      final var displayServerArgs = new ArrayList<String>();

      if (project == null) {
         haxeSDK = HaxeWorkspacePreference.getDefaultHaxeSDK(true, true);
      } else {
         final var projectPrefs = HaxeProjectPreference.get(project);
         haxeSDK = projectPrefs.getEffectiveHaxeSDK();
         final var buildFile = projectPrefs.getBuildFile();
         if (buildFile != null) {
            switch (projectPrefs.getBuildSystem()) {
               case HAXE:
                  displayServerArgs.add(asNonNull(buildFile.location.getLocation()).toOSString());
                  break;
               case LIME:
                  for (final var source : buildFile.getSourcePaths()) {
                     displayServerArgs.add("--class-path");
                     displayServerArgs.add(source.toString());
                  }
                  if (haxeSDK != null) {
                     for (final var haxelib : buildFile.getDependencies(haxeSDK, new NullProgressMonitor())) {
                        displayServerArgs.add("--library");
                        if (Strings.isBlank(haxelib.meta.version)) {
                           displayServerArgs.add(haxelib.meta.name);
                        } else {
                           displayServerArgs.add(haxelib.meta.name + ":" + haxelib.meta.version);
                        }
                     }
                  }
                  break;
               case LIX:
                  for (final var source : buildFile.getSourcePaths()) {
                     displayServerArgs.add("--class-path");
                     displayServerArgs.add(source.toString());
                  }
                  if (haxeSDK != null) {
                     for (final var haxelib : buildFile.getDependencies(haxeSDK, new NullProgressMonitor())) {
                        displayServerArgs.add("--class-path");
                        displayServerArgs.add(haxelib.location.resolve(Strings.defaultIfNull(haxelib.meta.classPath, ".")).toString());
                     }
                  }
                  break;
               default:
                  throw new UnsupportedOperationException("Unsupported build-system: " + this);
            }
         }
      }
      final var nekoVM = haxeSDK == null ? null : haxeSDK.getNekoVM();

      // InitOptions https://github.com/vshaxe/haxe-language-server/blob/master/src/haxeLanguageServer/Configuration.hx#L87
      final var opts = new TreeBuilder<String>() //
         // https://github.com/vshaxe/haxe-language-server/blob/master/shared/haxeLanguageServer/DisplayServerConfig.hx
         .put("displayServerConfig", new TreeBuilder<String>() //
            .put("path", haxeSDK == null ? null : haxeSDK.getCompilerExecutable().toString()) //
            .compute("env", leaf -> {
               if (haxeSDK == null)
                  return;

               leaf.put("PATH", //
                  haxeSDK.getHaxelibExecutable().getParent() // add haxelib to path which is executed by Haxe Display Server
                     + File.pathSeparator //
                     + (nekoVM == null ? null : nekoVM.getInstallRoot()) // add neko to path which is required by haxelib
               );

               // required for haxelib process spawned Haxe Display Server to analyze dependencies
               leaf.put(HaxeSDK.ENV_HAXELIB_PATH, haxeSDK.getHaxelibsDir().toString());
            })
            // ConfigurePrintParams https://github.com/HaxeFoundation/haxe/blob/development/std/haxe/display/Server.hx#L49
            .put("print", "completion", false) // if true, logs completion response to console
            .put("print", "reusing", false) // no idea what this does
         ) //
         .put("displayArguments", displayServerArgs) //
         // HaxelibConfig https://github.com/vshaxe/haxe-language-server/blob/master/src/haxeLanguageServer/Configuration.hx#L9
         .put("haxelibConfig", "executable", haxeSDK == null ? null : haxeSDK.getHaxelibExecutable().toString()) //
         .put("sendMethodResults", HaxeWorkspacePreference.isLangServTraceMethodResults()) //
         .getMap();

      if (HaxeWorkspacePreference.isLangServTraceInitOptions()) {
         Haxe4EPlugin.log().info(opts);
      }
      return opts;
   }

   @Override
   public @Nullable InputStream getInputStream() {
      final var stream = super.getInputStream();

      if (!HaxeWorkspacePreference.isLangServTraceIO())
         return stream;

      return stream == null ? null : new LinePrefixingTeeInputStream(stream, System.out, "SERVER >> ");
   }

   @Override
   public @Nullable OutputStream getOutputStream() {
      final var stream = super.getOutputStream();

      if (!HaxeWorkspacePreference.isLangServTraceIO())
         return stream;

      return stream == null ? null : new LinePrefixingTeeOutputStream(stream, System.out, "CLIENT >> ");
   }

   @Override
   public String getTrace(final @Nullable URI rootUri) {
      // return "verbose"; // has no effect, maybe not implemented in Haxe language server
      return "off";
   }
}
