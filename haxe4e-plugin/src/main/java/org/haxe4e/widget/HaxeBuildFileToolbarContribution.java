/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.widget;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.annotation.Nullable;
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
import org.haxe4e.prefs.HaxeProjectPreference;

import de.sebthom.eclipse.commons.ui.Editors;

/**
 * @author Ian Harrigan
 */
public class HaxeBuildFileToolbarContribution extends WorkbenchWindowControlContribution implements SelectionListener {

   public static @Nullable HaxeBuildFileToolbarContribution instance;
   private static @Nullable IProject currentProject;

   private CCombo buildFileDropDown = eventuallyNonNull();

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
      buildFileDropDown = new CCombo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY | SWT.NO_FOCUS | SWT.SINGLE);
      buildFileDropDown.setLayoutData(gridData);
      buildFileDropDown.addSelectionListener(this);

      // even read-only drop-down shows I-beam cursor - lets fix that
      final var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      final var cursor = new Cursor(shell.getDisplay(), SWT.CURSOR_ARROW);
      buildFileDropDown.setCursor(cursor);

      return container;
   }

   public void refresh(final @Nullable IProject project) {
      if (project == null || project == currentProject || buildFileDropDown.getListVisible())
         return;

      try {
         currentProject = project;

         final var prefs = HaxeProjectPreference.get(project);
         final var buildSystem = prefs.getBuildSystem();
         final var buildFiles = buildSystem.findFilesWithBuildFileExtension(project, true);

         buildFileDropDown.removeAll();

         if (!buildFiles.isEmpty()) {
            var currentBuildFile = prefs.getBuildFile();

            if (currentBuildFile == null) {
               currentBuildFile = buildSystem.toBuildFile(buildFiles.get(0));
               prefs.setBuildFilePath(currentBuildFile.getProjectRelativePath());
               prefs.save();
            }
            buildFileDropDown.setItems(buildFiles.stream().map(IFile::getProjectRelativePath).toArray(String[]::new));
            buildFileDropDown.setText(currentBuildFile.getProjectRelativePath());
         }

      } catch (final Exception ex) {
         Haxe4EPlugin.log().error(ex);
      }
   }

   @Override
   public void dispose() {
      buildFileDropDown.getCursor().dispose(); // to prevent "java.lang.Error: SWT Resource was not properly disposed"

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
      final var currentProject = HaxeBuildFileToolbarContribution.currentProject;
      if (currentProject == null)
         return;

      final var newBuildFilePath = buildFileDropDown.getItem(buildFileDropDown.getSelectionIndex());

      final var prefs = HaxeProjectPreference.get(currentProject);
      prefs.setBuildFilePath(newBuildFilePath);
      prefs.save();

      // dropdown steals focus, lets set it back to any active editor
      final var editor = Editors.getActiveEditor();
      if (editor != null) {
         editor.setFocus();
      }
   }
}
