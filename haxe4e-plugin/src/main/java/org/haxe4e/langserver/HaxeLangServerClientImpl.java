/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.langserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.util.TreeBuilder;

import net.sf.jstuff.core.concurrent.Threads;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction") // https://bugs.eclipse.org/bugs/show_bug.cgi?id=536215
public final class HaxeLangServerClientImpl extends LanguageClientImpl implements HaxeLangServerClient {

   private boolean isInitTriggered = false;

   private final Map<Either<String, Integer>, CountDownLatch> progressMonitors = new HashMap<>();

   @Override
   public CompletableFuture<Void> createProgress(final WorkDoneProgressCreateParams params) {
      Haxe4EPlugin.log().debug("createProgress: {0}", params);
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public void notifyProgress(final ProgressParams params) {
      synchronized (progressMonitors) {
         if (params.getValue().isLeft()) {
            final var notification = params.getValue().getLeft();
            if (notification instanceof WorkDoneProgressBegin) {
               final var progressBegin = (WorkDoneProgressBegin) notification;
               final var signal = new CountDownLatch(1);
               final var job = new Job(progressBegin.getTitle()) {
                  @Override
                  protected void canceling() {
                     getThread().interrupt();
                  }

                  @Override
                  protected IStatus run(final IProgressMonitor monitor) {
                     monitor.beginTask(progressBegin.getTitle(), IProgressMonitor.UNKNOWN);
                     try {
                        signal.await();
                     } catch (final InterruptedException ex) {
                        Threads.handleInterruptedException(ex);
                        monitor.setCanceled(true);
                     } finally {
                        monitor.done();
                     }
                     synchronized (progressMonitors) {
                        progressMonitors.remove(params.getToken());
                     }
                     return Status.OK_STATUS;
                  }
               };
               job.setPriority(Job.INTERACTIVE);
               job.schedule();
               progressMonitors.put(params.getToken(), signal);
            } else if (notification instanceof WorkDoneProgressEnd) {
               final var signal = progressMonitors.get(params.getToken());
               if (signal != null) {
                  signal.countDown();
               }
            }
         } else {
            Haxe4EPlugin.log().debug("notifyProgress: {0}", params);
         }
      }
   }

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
   public CompletableFuture<Void> registerCapability(final RegistrationParams params) {
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
