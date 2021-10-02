/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.perspective;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * @author Sebastian Thomschke
 */
public class HaxePerspective implements IPerspectiveFactory {

   @Override
   public void createInitialLayout(final IPageLayout layout) {
      defineLayout(layout);
      defineMenuActions(layout);
   }

   public void defineLayout(final IPageLayout layout) {
      final var editorArea = layout.getEditorArea();

      final var left = layout.createFolder("left", IPageLayout.LEFT, (float) 0.20, editorArea);
      left.addView(IPageLayout.ID_PROJECT_EXPLORER);

      final var bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.80, editorArea);
      bottom.addView("org.eclipse.ui.console.ConsoleView");
      bottom.addView(IPageLayout.ID_BOOKMARKS);
      bottom.addView(IPageLayout.ID_TASK_LIST);
      bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
      bottom.addView(IPageLayout.ID_PROGRESS_VIEW);
      bottom.addView("org.eclipse.team.ui.GenericHistoryView");
      bottom.addView("org.eclipse.egit.ui.StagingView");

      final var right = layout.createFolder("right", IPageLayout.RIGHT, (float) 0.80, editorArea);
      right.addView(IPageLayout.ID_OUTLINE);
      right.addView(IPageLayout.ID_PROP_SHEET);
   }

   public void defineMenuActions(final IPageLayout layout) {
      // Add entries to "Window > Show View > ..."
      layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
      layout.addShowViewShortcut("org.eclipse.ui.console.ConsoleView");
      layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS);
      layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
      layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
      layout.addShowViewShortcut(IPageLayout.ID_PROGRESS_VIEW);
      layout.addShowViewShortcut("org.eclipse.team.ui.GenericHistoryView");
      layout.addShowViewShortcut("org.eclipse.egit.ui.StagingView");
      layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
      layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
      layout.addShowViewShortcut("org.eclipse.pde.runtime.LogView");
   }
}
