/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.langserver;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.util.TreeBuilder;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction") // https://bugs.eclipse.org/bugs/show_bug.cgi?id=536215
public final class HaxeLangServerClientImpl extends LanguageClientImpl implements HaxeLangServerClient {

   private boolean isInitTriggered = false;

   @Override
   public void onHaxeCacheBuildFailed(final Object value) {
      Haxe4EPlugin.log().warn("onHaxeCacheBuildFailed: {0}", value);
   }

   @Override
   public void onHaxeDidChangeRequestQueue(final Object value) {
      Haxe4EPlugin.log().debug("onHaxeDidChangeRequestQueue: {0}", value);
   }

   @Override
   public void onHaxeDidRunMethod(final Object value) {
      Haxe4EPlugin.log().debug("onHaxeDidRunMethod: {0}", value);
   }

   @Override
   public @NonNullByDefault({}) CompletableFuture<Void> registerCapability(final RegistrationParams params) {
      if (!isInitTriggered) {
         // workaround for https://github.com/vshaxe/vshaxe/issues/501
         final var event = new DidChangeConfigurationParams(new TreeBuilder<String>() //
            .put("haxe", new TreeBuilder<String>() //
               .getMap() //
            ).getMap());

         getLanguageServer().getWorkspaceService().didChangeConfiguration(event);
         isInitTriggered = true;
      }

      return super.registerCapability(params);
   }
}
