/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.ToggleBreakpointAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm4e.ui.internal.model.TMModelManager;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.ui.editors.text.TextEditor;
import org.haxe4e.util.LOG;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class HaxeEditor extends TextEditor {

   private volatile Position caretPosition;

   private final CaretListener caretListener = event -> {
      try {
         final var document = getDocument();
         caretPosition = LSPEclipseUtils.toPosition(event.caretOffset, document);

         // TODO https://github.com/vshaxe/vshaxe/issues/502
         // highlightMatchingOccurrences(caretPosition);
      } catch (final Exception ex) {
         LOG.error(ex);
      }
   };

   public HaxeEditor() {
      // https://github.com/eclipse/tm4e/wiki/UI#with-texteditor
      setSourceViewerConfiguration(new SourceViewerConfiguration() {
         @Override
         public IPresentationReconciler getPresentationReconciler(final ISourceViewer viewer) {
            return new TMPresentationReconciler();
         }
      });
   }

   @Override
   protected IVerticalRulerColumn createAnnotationRulerColumn(final CompositeRuler ruler) {
      return new AnnotationRulerColumn(VERTICAL_RULER_WIDTH, getAnnotationAccess()) {
         @Override
         protected void mouseDoubleClicked(final int rulerLine) {
            toggleBreakpoint();
         }
      };
   }

   @Override
   public void createPartControl(final Composite parent) {
      super.createPartControl(parent);
      getSourceViewer().getTextWidget().addCaretListener(caretListener);
   }

   @Override
   public void dispose() {
      try {
         getSourceViewer().getTextWidget().removeCaretListener(caretListener);
      } catch (final NullPointerException ex) {
         // ignore
      }
      super.dispose();
   }

   private IDocument getDocument() {
      return getDocumentProvider().getDocument(getEditorInput());
   }

   @SuppressWarnings("unused")
   private void highlightMatchingOccurrences() {
      final var infos = LanguageServiceAccessor.getLSPDocumentInfosFor(getDocument(), capabilities -> {
         final var docHighlight = capabilities.getDocumentHighlightProvider();
         return docHighlight != null && (docHighlight.getLeft() == Boolean.TRUE || docHighlight.isRight());
      });

      infos.forEach(info -> {
         final var identifier = new TextDocumentIdentifier(info.getFileUri().toString());
         final var params = new DocumentHighlightParams(identifier, caretPosition);
         info.getInitializedLanguageClient().whenComplete((client, ex) -> client.getTextDocumentService().documentHighlight(params));
      });
   }

   private void toggleBreakpoint() {
      final var doc = getDocument();
      final var parsedDoc = TMModelManager.getInstance().connect(doc);
      final var rulerInfo = getAdapter(IVerticalRulerInfo.class);
      final var lineNumber = rulerInfo.getLineOfLastMouseButtonActivity();

      final var tokens = parsedDoc.getLineTokens(lineNumber);
      if (tokens.isEmpty())
         return;

      // check if the current line is eligible for having a breakpoint
      var lineSupportsAddingBreakpoint = false;
      loop: for (final var token : tokens) {
         switch (token.type) {
            case "block.hx.meta.class":
            case "block.hx.meta.class.method":
            case "meta.class.hx.block.method":
               continue;
            default:
               if (token.type.contains("comment") || token.type.contains("punctuation")) {
                  continue;
               }
               lineSupportsAddingBreakpoint = true;
               break loop;
         }
      }

      if (lineSupportsAddingBreakpoint) {
         final var action = new ToggleBreakpointAction(this, doc, rulerInfo);
         action.update();
         action.run();
      } else {
         // if the current line is eligible for having a breakpoint then remove any potentially existing breakpoints
         final var breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(DSPPlugin.ID_DSP_DEBUG_MODEL);
         if (breakpoints.length == 0)
            return;

         final var resource = getEditorInput().getAdapter(IResource.class);
         for (final IBreakpoint breakpoint : breakpoints) {
            try {
               if (breakpoint instanceof ILineBreakpoint //
                  && resource.equals(breakpoint.getMarker().getResource()) //
                  && ((ILineBreakpoint) breakpoint).getLineNumber() == lineNumber + 1) {
                  breakpoint.delete();
               }
            } catch (final CoreException ex) {
               LOG.error(ex);
            }
         }
      }
   }
}
