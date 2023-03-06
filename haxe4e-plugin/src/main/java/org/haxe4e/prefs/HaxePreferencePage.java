/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.prefs;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.haxe4e.localization.Messages;
import org.haxe4e.util.ui.GridDatas;

/**
 * @author Sebastian Thomschke
 */
public class HaxePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public HaxePreferencePage() {
      super(FieldEditorPreferencePage.GRID);
      setDescription(Messages.Prefs_GeneralDescription);
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(HaxeWorkspacePreference.STORE);
   }

   @Override
   protected void createFieldEditors() {
      final var parent = getFieldEditorParent();

      final var grpLangServer = new Group(parent, SWT.NONE);
      grpLangServer.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpLangServer.setText("Haxe Language Server - Troubleshooting");

      addField(new BooleanFieldEditor( //
         HaxeWorkspacePreference.PREFKEY_LANGSERV_TRACE_INITOPTS, //
         "Log Init Options", //
         grpLangServer //
      ));

      addField(new BooleanFieldEditor( //
         HaxeWorkspacePreference.PREFKEY_LANGSERV_TRACE_IO, //
         "Log client/server communication", //
         grpLangServer //
      ));

      addField(new BooleanFieldEditor( //
         HaxeWorkspacePreference.PREFKEY_LANGSERV_TRACE_METHOD_RESULTS, //
         "Log method results", //
         grpLangServer //
      ));

      // needs to be set after field editors were added
      grpLangServer.setLayout(GridLayoutFactory.swtDefaults().create());
   }
}
