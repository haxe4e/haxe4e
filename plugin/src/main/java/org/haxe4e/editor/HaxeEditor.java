/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.ToggleBreakpointAction;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewerExtension4;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.lsp4e.operations.diagnostics.LSPDiagnosticsToMarkers;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.ui.internal.genericeditor.ExtensionBasedTextEditor;
import org.haxe4e.Haxe4EPlugin;

import net.sf.jstuff.core.reflection.Fields;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class HaxeEditor extends ExtensionBasedTextEditor {

   public static final String ID = HaxeEditor.class.getName();

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

      final var contentAssistant = getContentAssistant();
      if (contentAssistant != null) {
         contentAssistant.setAutoActivationDelay(500);
      }

      final var textWidget = getSourceViewer().getTextWidget();

      // workaround for https://github.com/haxe4e/haxe4e/issues/40
      // double clicking a variable also selects the type since ":" is seen as part of the word
      textWidget.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseDoubleClick(final MouseEvent ev) {
            final var selText = textWidget.getSelectionText();

            // if the current selection includes a colon we manually adjust the selection
            if (selText.indexOf(':') > -1) {
               final var selOffset = textWidget.getSelection().x;

               final var mouseOffset = textWidget.getOffsetAtPoint(new Point(ev.x, ev.y));
               // index of the char of the current selection where the mouse pointer clicked
               final var mouseSelectedChar = mouseOffset - selOffset;

               final var colonBeforeMouseCursor = selText.lastIndexOf(':', mouseSelectedChar);
               final var colonAfterMouseCursor = selText.indexOf(':', mouseSelectedChar);

               textWidget.setSelection( //
                  selOffset + colonBeforeMouseCursor + 1, //
                  selOffset + (colonAfterMouseCursor < 0 ? selText.length() : colonAfterMouseCursor) //
               );
            }
         }
      });
   }

   private @Nullable ContentAssistant getContentAssistant() {
      final var viewer = getSourceViewer();

      if (viewer instanceof final ISourceViewerExtension4 viewer4) {
         final var contentAssistantFacade = viewer4.getContentAssistantFacade();
         if (contentAssistantFacade != null //
               && Fields.read(contentAssistantFacade, "fContentAssistant") instanceof final ContentAssistant contentAssistant)
            return contentAssistant;
      }

      if (viewer instanceof SourceViewer //
            && Fields.read(viewer, "fContentAssistant") instanceof final ContentAssistant contentAssistant)
         return contentAssistant;

      return null;
   }

   private @Nullable IDocument getDocument() {
      return getDocumentProvider().getDocument(getEditorInput());
   }

   @Override
   protected void initializeKeyBindingScopes() {
      setKeyBindingScopes(new String[] {"org.haxe4e.editor.HaxeEditorContext"});
   }

   @Override
   protected void performSave(final boolean overwrite, final @Nullable IProgressMonitor progressMonitor) {

      // TODO workaround for https://github.com/vshaxe/vshaxe/issues/507
      final var res = getEditorInput().getAdapter(IResource.class);
      if (res == null)
         return;
      try {
         for (final var marker : res.findMarkers(LSPDiagnosticsToMarkers.LS_DIAGNOSTIC_MARKER_TYPE, false, IResource.DEPTH_ONE)) {
            if ("org.haxe4e.langserv".equals(marker.getAttribute(LSPDiagnosticsToMarkers.LANGUAGE_SERVER_ID))) {
               marker.delete();
            }
         }
      } catch (final CoreException ex) {
         Haxe4EPlugin.log().error(ex);
      }
      super.performSave(overwrite, progressMonitor);
   }

   private void toggleBreakpoint() {
      final var doc = getDocument();
      if (doc == null)
         return;

      final var parsedDoc = TMUIPlugin.getTMModelManager().connect(doc);
      final var rulerInfo = getAdapter(IVerticalRulerInfo.class);
      if (rulerInfo == null)
         return;

      final var lineIndex = rulerInfo.getLineOfLastMouseButtonActivity();
      final var tokens = parsedDoc.getLineTokens(lineIndex);
      if (tokens == null || tokens.isEmpty())
         return;

      // check if the current line is eligible for having a breakpoint
      var lineSupportsAddingBreakpoint = false;

      loop: for (final var token : tokens) {
         switch (token.type) {
            case "":
            case "block.hx.meta.class":
            case "block.hx.meta.class.method":
            case "hx.meta.class.block":
            case "hx.meta.class.block.method":
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
         if (resource == null)
            return;
         for (final var breakpoint : breakpoints) {
            try {
               if (breakpoint instanceof final ILineBreakpoint lineBreakpoint //
                     && resource.equals(breakpoint.getMarker().getResource()) //
                     && lineBreakpoint.getLineNumber() == lineIndex + 1) {
                  breakpoint.delete();
               }
            } catch (final CoreException ex) {
               Haxe4EPlugin.log().error(ex);
            }
         }
      }
   }
}
