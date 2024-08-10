/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.prefs;

import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.haxe4e.localization.Messages;

import de.sebthom.eclipse.commons.prefs.fieldeditor.GroupFieldEditor;

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

      addField(new GroupFieldEditor("Haxe Language Server - Troubleshooting", parent, group -> List.of( //
         new BooleanFieldEditor(HaxeWorkspacePreference.PREFKEY_LSP_TRACE_INITOPTS, "Log Init Options", group), //
         new BooleanFieldEditor(HaxeWorkspacePreference.PREFKEY_LSP_TRACE_IO, "Log Language Server Protocol communication", group), //
         new BooleanFieldEditor(HaxeWorkspacePreference.PREFKEY_LSP_TRACE_METHOD_RESULTS,
            "Log Language Server Protocol communication (method results)", group), //
         new BooleanFieldEditor(HaxeWorkspacePreference.PREFKEY_LSP_TRACE_IO_VERBOSE,
            "Log Language Server Protocol communication (verbose)", group) //
      )));

      addField(new GroupFieldEditor("Haxe Debug Adapter - Troubleshooting", parent, group -> List.of( //
         new BooleanFieldEditor(HaxeWorkspacePreference.PREFKEY_DAP_TRACE_IO, "Log Debug Adatper Protocol communication", group), //
         new BooleanFieldEditor(HaxeWorkspacePreference.PREFKEY_DAP_TRACE_IO_VERBOSE, "Log Debug Adatper Protocol communication (verbose)",
            group) //
      )));
   }
}
