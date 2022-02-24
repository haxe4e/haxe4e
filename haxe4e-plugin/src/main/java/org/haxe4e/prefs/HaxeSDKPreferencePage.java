/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.prefs;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.model.NekoVM;
import org.haxe4e.util.ui.GridDatas;
import org.haxe4e.util.ui.StyledLabelProviderAdapter;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Fonts;
import de.sebthom.eclipse.commons.ui.Tables;
import net.sf.jstuff.core.collection.ObservableSet;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class HaxeSDKPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

   private CheckboxTableViewer haxeSDKTable;
   private final ObservableSet<HaxeSDK> haxeSDKs = new ObservableSet<>(new HashSet<>());
   private final MutableObservableRef<HaxeSDK> defaultHaxeSDK = MutableObservableRef.of(null);

   public HaxeSDKPreferencePage() {
      setDescription(Messages.Prefs_ManageSDKsDescription);
   }

   @Override
   public Control createContents(final Composite parent) {
      final var container = new Composite(parent, SWT.NULL);
      container.setLayout(new GridLayout(2, false));

      haxeSDKTable = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.FULL_SELECTION);
      haxeSDKTable.addCheckStateListener(event -> {
         if (event.getChecked()) {
            final var sdk = (HaxeSDK) event.getElement();
            defaultHaxeSDK.set(sdk);
         } else {
            defaultHaxeSDK.set(null);
         }
      });
      haxeSDKTable.setCheckStateProvider(new ICheckStateProvider() {
         @Override
         public boolean isChecked(final Object element) {
            return isDefaultHaxeSDK((HaxeSDK) element);
         }

         @Override
         public boolean isGrayed(final Object element) {
            return false;
         }
      });
      final var table = haxeSDKTable.getTable();
      table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4));
      table.setHeaderVisible(true);
      table.setLinesVisible(true);

      haxeSDKTable.setContentProvider((IStructuredContentProvider) input -> {
         @SuppressWarnings("unchecked")
         final var items = (List<HaxeSDK>) input;
         return items.toArray(new HaxeSDK[items.size()]);
      });

      haxeSDKTable.setContentProvider(new IStructuredContentProvider() {
         @Override
         public Object[] getElements(final Object input) {
            @SuppressWarnings("unchecked")
            final var items = (Collection<HaxeSDK>) input;
            return items.toArray(new HaxeSDK[items.size()]);
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
            refreshTable();
         }
      });

      final var colName = new TableViewerColumn(haxeSDKTable, SWT.NONE);
      colName.setLabelProvider(new DelegatingStyledCellLabelProvider(new StyledLabelProviderAdapter()) {
         @Override
         public StyledString getStyledText(final Object element) {
            return isDefaultHaxeSDK((HaxeSDK) element) //
               ? new StyledString(((HaxeSDK) element).getName(), Fonts.DEFAULT_FONT_BOLD_STYLER)
               : new StyledString(((HaxeSDK) element).getName());
         }
      });
      final var tblclmnName = colName.getColumn();
      tblclmnName.setWidth(100);
      tblclmnName.setText(Messages.Label_Name);

      final var colPath = new TableViewerColumn(haxeSDKTable, SWT.NONE);
      colPath.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(final Object element) {
            return ((HaxeSDK) element).getPath().toString();
         }
      });
      final var tblclmnPath = colPath.getColumn();
      tblclmnPath.setWidth(100);
      tblclmnPath.setText(Messages.Label_Path);

      final var btnAdd = new Button(container, SWT.NONE);
      btnAdd.setLayoutData(GridDatas.fillHorizontal());
      btnAdd.setText(Messages.Label_Add + "...");
      Buttons.onSelected(btnAdd, this::onButton_Add);

      final var btnEdit = new Button(container, SWT.NONE);
      btnEdit.setLayoutData(GridDatas.fillHorizontal());
      btnEdit.setText(Messages.Label_Edit + "...");
      btnEdit.setEnabled(false);
      Buttons.onSelected(btnEdit, this::onButton_Edit);

      final var btnRemove = new Button(container, SWT.NONE);
      btnRemove.setLayoutData(GridDatas.fillHorizontal());
      btnRemove.setText(Messages.Label_Remove);
      btnRemove.setEnabled(false);
      Buttons.onSelected(btnRemove, this::onButton_Remove);

      haxeSDKTable.addSelectionChangedListener(event -> {
         if (event.getSelection().isEmpty()) {
            btnEdit.setEnabled(false);
            btnRemove.setEnabled(false);
         } else {
            btnEdit.setEnabled(true);
            btnRemove.setEnabled(true);
         }
      });

      haxeSDKTable.setInput(haxeSDKs);
      Tables.autoResizeColumns(haxeSDKTable);

      haxeSDKs.subscribe(e -> refreshTable());
      defaultHaxeSDK.subscribe(this::refreshTable);

      return container;
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(HaxeWorkspacePreference.PREFS);
      haxeSDKs.addAll(HaxeWorkspacePreference.getHaxeSDKs());
      defaultHaxeSDK.set(HaxeWorkspacePreference.getDefaultHaxeSDK(false, false));
   }

   private boolean isDefaultHaxeSDK(final HaxeSDK sdk) {
      return Objects.equals(sdk, defaultHaxeSDK.get());
   }

   private void onButton_Add() {
      final var dialog = new HaxeSDKEditDialog(getShell());
      if (dialog.open() == Window.OK) {
         haxeSDKs.add(new HaxeSDK(dialog.haxeSDKName.get(), dialog.haxeSDKPath.get(), new NekoVM(dialog.nekoVMPath.get())));
      }
   }

   private void onButton_Edit() {
      final var sel = (StructuredSelection) haxeSDKTable.getSelection();
      if (sel.isEmpty())
         return;

      final var sdk = (HaxeSDK) sel.getFirstElement();
      final var dialog = new HaxeSDKEditDialog(getShell(), sdk);
      if (dialog.open() == Window.OK) {
         haxeSDKs.remove(sdk);
         haxeSDKs.add(new HaxeSDK(dialog.haxeSDKName.get(), dialog.haxeSDKPath.get(), new NekoVM(dialog.nekoVMPath.get())));
      }
   }

   private void onButton_Remove() {
      final var sel = (StructuredSelection) haxeSDKTable.getSelection();
      if (sel.isEmpty())
         return;
      final var sdk = (HaxeSDK) sel.getFirstElement();
      haxeSDKs.remove(sdk);
      haxeSDKTable.refresh();
   }

   @Override
   protected void performDefaults() {
      super.performDefaults();
   }

   @Override
   public boolean performOk() {
      HaxeWorkspacePreference.setHaxeSDKs(haxeSDKs);
      if (defaultHaxeSDK.get() != null) {
         HaxeWorkspacePreference.setDefaultHaxeSDK(defaultHaxeSDK.get().getName());
      }
      if (!HaxeWorkspacePreference.save()) {
         setValid(false);
         return false;
      }

      setValid(true);
      return true;
   }

   private void refreshTable() {
      haxeSDKTable.refresh();
      Tables.autoResizeColumns(haxeSDKTable);
   }
}
