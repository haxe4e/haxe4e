/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.widget;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.project.HaxeProject;

import de.sebthom.eclipse.commons.ui.Editors;

/**
 * @author Ian Harrigan
 */
public class HaxeBuildFileToolbarContribution extends WorkbenchWindowControlContribution implements SelectionListener {
   public static HaxeBuildFileToolbarContribution instance;

   public static IProject currentProject;
   private CCombo hxmlList;

   public HaxeBuildFileToolbarContribution() {
      instance = this;
   }

   @Override
   public boolean isDynamic() {
      return true;
   }

   @Override
   protected Control createControl(final Composite parent) {
      final var container = new Composite(parent, SWT.NONE);
      final var gridLayout = new GridLayout(1, false);
      gridLayout.marginLeft = 7;
      gridLayout.marginTop = 0;
      gridLayout.marginHeight = 0;
      gridLayout.marginWidth = 0;
      container.setLayout(gridLayout);
      final var gridData = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
      gridData.widthHint = 120;
      hxmlList = new CCombo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY | SWT.NO_FOCUS | SWT.SINGLE);
      hxmlList.setLayoutData(gridData);
      hxmlList.addSelectionListener(this);

      // even read-only dropdown shows I-beam cursor - lets fix that
      final var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      final var cursor = new Cursor(shell.getDisplay(), SWT.CURSOR_ARROW);
      hxmlList.setCursor(cursor);

      return container;
   }

   public void refresh(final IProject project) {
      if (project == null)
         return;

      if (project == currentProject)
         return;

      if (hxmlList.getListVisible())
         return;

      try {
         currentProject = project;

         final var haxeProject = new HaxeProject(project);
         var currentBuildFile = haxeProject.getPrefs().getHaxeBuildFile();
         final var buildFiles = haxeProject.getBuildFiles();
         if ((currentBuildFile == null || currentBuildFile.trim().length() == 0) && buildFiles.size() > 0) {
            currentBuildFile = buildFiles.get(0);
            haxeProject.setBuildFile(currentBuildFile);
         }
         hxmlList.removeAll();
         hxmlList.setItems(buildFiles.toArray(new String[0]));
         hxmlList.setText(currentBuildFile);
      } catch (final Exception ex) {
         Haxe4EPlugin.log().error(ex);
      }
   }

   @Override
   public void dispose() {
      hxmlList.getCursor().dispose(); // to prevent "java.lang.Error: SWT Resource was not properly disposed"

      super.dispose();
   }

   @Override
   public void widgetSelected(final SelectionEvent event) {
      handleSelectionChanged();
   }

   @Override
   public void widgetDefaultSelected(final SelectionEvent e) {
      handleSelectionChanged();
   }

   private void handleSelectionChanged() {
      if (currentProject != null) {
         final var newHxmlFile = hxmlList.getItem(hxmlList.getSelectionIndex());
         final var haxeProject = new HaxeProject(currentProject);

         haxeProject.setBuildFile(newHxmlFile);

         // dropdown steals focus, lets set it back to any active editor
         final var editor = Editors.getActiveEditor();
         if (editor != null) {
            editor.setFocus();
         }
      }
   }
}
