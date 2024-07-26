/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.langserver;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageClient;
import org.haxe4e.Haxe4EPlugin;

/**
 * https://github.com/vshaxe/haxe-language-server/blob/master/shared/haxeLanguageServer/LanguageServerMethods.hx
 *
 * @author Sebastian Thomschke
 */
public interface HaxeLangServerClient extends LanguageClient {

   /**
    * only called if {@link HaxeLangServerLauncher#SEND_METHOD_RESULTS} is set to true
    */
   @SuppressWarnings("javadoc")
   @JsonNotification("haxe/didRunHaxeMethod")
   default void onHaxeDidRunMethod(final Object value) {
      Haxe4EPlugin.log().debug("onHaxeDidRunMethod: {0}", value);
   }

   /**
    * only called if {@link HaxeLangServerLauncher#SEND_METHOD_RESULTS} is set to true
    */
   @SuppressWarnings("javadoc")
   @JsonNotification("haxe/didChangeRequestQueue")
   default void onHaxeDidChangeRequestQueue(final Object value) {
      Haxe4EPlugin.log().debug("onHaxeDidChangeRequestQueue: {0}", value);
   }

   @JsonNotification("haxe/cacheBuildFailed")
   default void onHaxeCacheBuildFailed(final Object value) {
      Haxe4EPlugin.log().warn("onHaxeCacheBuildFailed: {0}", value);
   }
}
