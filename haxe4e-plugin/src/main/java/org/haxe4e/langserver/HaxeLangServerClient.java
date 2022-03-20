/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.langserver;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * @author Sebastian Thomschke
 */
public interface HaxeLangServerClient extends LanguageClient {

   /**
    * only called if {@link HaxeLangServerLauncher#SEND_METHOD_RESULTS} is set to true
    */
   @SuppressWarnings("javadoc")
   @JsonNotification("haxe/didRunHaxeMethod")
   void onHaxeDidRunMethod(Object value);

   /**
    * only called if {@link HaxeLangServerLauncher#SEND_METHOD_RESULTS} is set to true
    */
   @SuppressWarnings("javadoc")
   @JsonNotification("haxe/didChangeRequestQueue")
   void onHaxeDidChangeRequestQueue(Object value);

   @JsonNotification("haxe/cacheBuildFailed")
   void onHaxeCacheBuildFailed(Object value);
}
