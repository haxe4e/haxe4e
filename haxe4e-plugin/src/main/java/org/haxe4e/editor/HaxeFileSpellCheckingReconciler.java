/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.tm4e.core.model.IModelTokensChangedListener;
import org.eclipse.tm4e.core.model.ModelTokensChangedEvent;
import org.eclipse.tm4e.core.model.Range;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.internal.model.TMDocumentModel;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import org.haxe4e.Haxe4EPlugin;

/**
 * {@link PresentationReconciler} that performs incremental spell checking of comments
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public final class HaxeFileSpellCheckingReconciler extends TMPresentationReconciler implements IModelTokensChangedListener,
   ITextInputListener {

   private static final boolean TRACE_SPELLCHECK_REGIONS = Platform.getDebugBoolean("org.haxe4e/trace/spellcheck/regions");

   private static final SpellingService SPELLING_SERVICE = EditorsUI.getSpellingService();

   private ITextViewer viewer;

   private Job spellcheckJob;

   private List<Region> collectRegionsToSpellcheck(final TMDocumentModel docModel, final List<Range> changedRanges) {
      if (TRACE_SPELLCHECK_REGIONS) {
         System.out.println("----------collectRegionsToSpellcheck----------");
      }
      final var doc = docModel.getDocument();
      final var regionsToSpellcheck = new ArrayList<Region>();
      for (final var range : changedRanges) {
         try {
            var blockCommentStart = -1;
            for (var lineIndex = range.fromLineNumber - 1; lineIndex < range.toLineNumber; lineIndex++) {
               for (final var token : docModel.getLineTokens(lineIndex)) {

                  if (TRACE_SPELLCHECK_REGIONS) {
                     System.out.println(lineIndex + ":" + doc.getLineOffset(lineIndex) + " " + token.startIndex + " " + token.type);
                  }
                  switch (token.type) {
                     // single line comment
                     case "hx.meta.class.block.comment.line.double-slash.method":
                     case "hx.meta.class.block.method.comment.line.double-slash":
                        regionsToSpellcheck.add(new Region(doc.getLineOffset(lineIndex) + token.startIndex, doc.getLineLength(lineIndex)
                           - token.startIndex));
                        break;

                     // beginning or end marker of a block comment
                     case "hx.punctuation.definition.block.comment":
                     case "hx.punctuation.meta.class.definition.block.comment":
                     case "hx.punctuation.meta.class.definition.block.comment.method":
                        if (blockCommentStart > -1) {
                           regionsToSpellcheck.add(new Region(blockCommentStart, doc.getLineOffset(lineIndex) + token.startIndex
                              - blockCommentStart));
                           blockCommentStart = -1;
                        }
                        break;

                     // content of block comment
                     case "hx.block.comment":
                     case "hx.meta.class.block.comment":
                     case "hx.meta.class.block.comment.method":
                        if (blockCommentStart == -1) {
                           blockCommentStart = doc.getLineOffset(lineIndex) + token.startIndex;
                        }
                        break;
                  }

               }
               if (blockCommentStart > -1) {
                  regionsToSpellcheck.add(new Region(blockCommentStart, doc.getLineLength(lineIndex)));
               }
            }
         } catch (final BadLocationException ex) {
            Haxe4EPlugin.log().error(ex);
         }
      }
      return regionsToSpellcheck;
   }

   @Override
   public void inputDocumentAboutToBeChanged(final IDocument oldInput, final IDocument newInput) {
   }

   @Override
   public void inputDocumentChanged(final IDocument oldInput, final IDocument newInput) {
      if (viewer == null)
         return;

      final var document = viewer.getDocument();
      if (document == null)
         return;

      final var model = TMUIPlugin.getTMModelManager().connect(document);
      model.addModelTokensChangedListener(this);
   }

   @Override
   public void install(final ITextViewer viewer) {
      super.install(viewer);
      this.viewer = viewer;
      viewer.addTextInputListener(this);
   }

   @Override
   public void modelTokensChanged(final ModelTokensChangedEvent event) {
      if (!(event.model instanceof TMDocumentModel))
         return;

      final var docModel = (TMDocumentModel) event.model;
      final var doc = docModel.getDocument();

      final var textFileBuffer = ITextFileBufferManager.DEFAULT.getTextFileBuffer(doc);
      if (textFileBuffer == null)
         return;

      if (spellcheckJob != null) {
         spellcheckJob.cancel();
      }

      final var loc = textFileBuffer.getLocation();
      spellcheckJob = new Job("Spellchecking" + (loc == null ? "" : " [" + loc + "]") + "...") {
         @Override
         protected IStatus run(final IProgressMonitor monitor) {
            final var regionsToSpellcheck = collectRegionsToSpellcheck(docModel, event.ranges);
            final var annotationModel = textFileBuffer.getAnnotationModel();

            spellcheck(doc, regionsToSpellcheck, annotationModel, monitor);
            return Status.OK_STATUS;
         }
      };
      spellcheckJob.setPriority(Job.DECORATE);
      spellcheckJob.schedule();
   }

   private void spellcheck(final IDocument doc, final List<Region> regionsToCheck, final IAnnotationModel annotationModel,
      final IProgressMonitor monitor) {
      SPELLING_SERVICE.check( //
         doc, //
         regionsToCheck.toArray(new Region[regionsToCheck.size()]), //
         new SpellingContext(), //
         new ISpellingProblemCollector() {
            private Map<SpellingAnnotation, Position> newSpellingErrors = new HashMap<>();

            @Override
            public void accept(final SpellingProblem problem) {
               newSpellingErrors.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
            }

            @Override
            public void beginCollecting() {
            }

            @Override
            public void endCollecting() {
               final var outdatedAnnotations = new HashSet<Annotation>();
               annotationModel.getAnnotationIterator().forEachRemaining(anno -> {
                  if (SpellingAnnotation.TYPE.equals(anno.getType())) {
                     final var pos = annotationModel.getPosition(anno);
                     final var annoStart = pos.getOffset();
                     final var annoEnd = pos.getOffset() + pos.length;
                     for (final var region : regionsToCheck) {
                        if (annoStart >= region.getOffset() && annoEnd <= region.getOffset() + region.getLength()) {
                           outdatedAnnotations.add(anno);
                           break;
                        }
                     }
                  }
               });

               if (annotationModel instanceof IAnnotationModelExtension) {
                  ((IAnnotationModelExtension) annotationModel).replaceAnnotations( //
                     outdatedAnnotations.toArray(new SpellingAnnotation[outdatedAnnotations.size()]), //
                     newSpellingErrors //
                  );
               } else {
                  outdatedAnnotations.forEach(annotationModel::removeAnnotation);
                  newSpellingErrors.forEach(annotationModel::addAnnotation);
               }
            }
         }, monitor);
   }

   @Override
   public void uninstall() {
      super.uninstall();
      viewer.removeTextInputListener(this);
      viewer = null;
   }
}
