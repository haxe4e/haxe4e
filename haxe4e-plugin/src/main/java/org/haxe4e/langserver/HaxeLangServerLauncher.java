/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.langserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.prefs.HaxeWorkspacePreference;
import org.haxe4e.util.TreeBuilder;
import org.haxe4e.util.io.LinePrefixingTeeInputStream;
import org.haxe4e.util.io.LinePrefixingTeeOutputStream;

/**
 * Runs the node.js based embedded haxe-language-server.
 *
 * See https://github.com/vshaxe/haxe-language-server
 *
 * @author Sebastian Thomschke
 */
public final class HaxeLangServerLauncher extends ProcessStreamConnectionProvider {

   private static final boolean TRACE_IO = Platform.getDebugBoolean("org.haxe4e/trace/langserv/io");
   private static final boolean TRACE_INIT_OPTIONS = Platform.getDebugBoolean("org.haxe4e/trace/langserv/init_options");

   /**
    * https://github.com/search?q=org%3Avshaxe+sendMethodResults&type=code
    */
   private static final boolean TRACE_METHOD_RESULTS = Platform.getDebugBoolean("org.haxe4e/trace/langserv/method_results");

   public HaxeLangServerLauncher() throws IOException {
      final var languageServerJS = Haxe4EPlugin.resources().extract("langsrv/haxe-language-server.min.js");
      setWorkingDirectory(SystemUtils.getUserDir().getAbsolutePath());
      setCommands(Arrays.asList( //
         NodeJSManager.getNodeJsLocation().getAbsolutePath(), //
         languageServerJS.getAbsolutePath() //
      ));
   }

   @Override
   public InputStream getErrorStream() {
      final var stream = super.getErrorStream();
      if (!TRACE_IO)
         return stream;

      if (stream == null)
         return null;
      return new LinePrefixingTeeInputStream(stream, System.out, "SRVERR >> ");
   }

   @Override
   public Object getInitializationOptions(final URI projectRootUri) {

      IProject project = null;
      for (final IContainer container : ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(projectRootUri)) {
         if (container instanceof IProject) {
            project = (IProject) container;
            break;
         }
      }

      final HaxeSDK haxeSDK;
      final List<String> displayServerArgs;

      if (project == null) {
         displayServerArgs = Collections.emptyList();
         haxeSDK = HaxeWorkspacePreference.getDefaultHaxeSDK(true, true);
      } else {
         final var projectPrefs = HaxeProjectPreference.get(project);
         haxeSDK = projectPrefs.getEffectiveHaxeSDK();
         final var buildFile = projectPrefs.getEffectiveHaxeBuildFile();
         displayServerArgs = buildFile == null ? Collections.emptyList()
            : Arrays.asList(buildFile.getRawLocation().makeAbsolute().toOSString());
      }
      final var nekoVM = haxeSDK == null ? null : haxeSDK.getNekoVM();

      // InitOptions https://github.com/vshaxe/haxe-language-server/blob/master/src/haxeLanguageServer/Configuration.hx#L87
      final Map<String, ?> opts = new TreeBuilder<String>() //
         // https://github.com/vshaxe/haxe-language-server/blob/master/shared/haxeLanguageServer/DisplayServerConfig.hx
         .put("displayServerConfig", new TreeBuilder<String>() //
            .put("path", haxeSDK == null ? null : haxeSDK.getCompilerExecutable().toString()) //
            .compute("env", leaf -> {
               if (haxeSDK == null)
                  return;

               leaf.put("PATH", //
                  haxeSDK.getHaxelibExecutable().getParent() // add haxelib to path which is executed by Haxe Display Server
                     + File.pathSeparator //
                     + (nekoVM == null ? null : nekoVM.getPath()) // add neko to path which is required by haxelib
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
         .put("sendMethodResults", TRACE_METHOD_RESULTS) //
         .getMap();

      if (TRACE_INIT_OPTIONS) {
         Haxe4EPlugin.log().info(opts);
      }
      return opts;
   }

   @Override
   public InputStream getInputStream() {
      final var stream = super.getInputStream();

      if (!TRACE_IO)
         return stream;

      return stream == null ? null : new LinePrefixingTeeInputStream(stream, System.out, "SERVER >> ");
   }

   @Override
   public OutputStream getOutputStream() {
      final var stream = super.getOutputStream();

      if (!TRACE_IO)
         return stream;

      return stream == null ? null : new LinePrefixingTeeOutputStream(stream, System.out, "CLIENT >> ");
   }

   @Override
   public String getTrace(final URI rootUri) {
      // return "verbose"; // has no effect, maybe not implemented in Haxe language server
      return "off";
   }
}
