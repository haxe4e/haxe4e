/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.widget;

import java.util.function.Consumer;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.prefs.HaxeWorkspacePreference;
import org.haxe4e.util.ui.Buttons;
import org.haxe4e.util.ui.ComboWrapper;
import org.haxe4e.util.ui.GridDatas;

import net.sf.jstuff.core.ref.ObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class HaxeSDKSelectionGroup extends Composite {

   public final ObservableRef<HaxeSDK> selectedAltSDK = new ObservableRef<>();
   private HaxeSDK selectedAltSDK_internal;

   public HaxeSDKSelectionGroup(final Composite parent, final Object layoutData) {
      this(parent, SWT.NONE, layoutData);
   }

   public HaxeSDKSelectionGroup(final Composite parent, final int style, final Object layoutData) {
      super(parent, style);

      setLayoutData(layoutData);
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpHaxeSdk = new Group(this, SWT.NONE);
      grpHaxeSdk.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpHaxeSdk.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
      grpHaxeSdk.setText(Messages.Label_Haxe_SDK);

      final var radioDefaultSDK = new Button(grpHaxeSdk, SWT.RADIO);
      radioDefaultSDK.setText(Messages.Label_Default);
      radioDefaultSDK.setSelection(true);
      Buttons.onSelected(radioDefaultSDK, () -> selectedAltSDK.set(null));

      final var txtDefaultSdk = new Text(grpHaxeSdk, SWT.BORDER);
      txtDefaultSdk.setEditable(false);
      txtDefaultSdk.setLayoutData(GridDatas.fillHorizontalExcessive());

      final var radioAltSDK = new Button(grpHaxeSdk, SWT.RADIO);
      radioAltSDK.setText(Messages.Label_Alternative);
      Buttons.onSelected(radioAltSDK, () -> selectedAltSDK.set(selectedAltSDK_internal));

      final var cmbAltSDK_ = new Combo(grpHaxeSdk, SWT.READ_ONLY);
      cmbAltSDK_.setLayoutData(GridDataFactory.fillDefaults().create());
      final var cmbAltSDK = new ComboWrapper<HaxeSDK>(cmbAltSDK_) //
         .setLabelProvider(HaxeSDK::toShortString) //
         .onItemsChanged((widget, oldItems, newItems) -> {
            if (newItems.isEmpty()) {
               Buttons.selectRadio(radioDefaultSDK);
               radioAltSDK.setEnabled(false);
               widget.setEnabled(false);
            } else {
               radioAltSDK.setEnabled(true);
               widget.setEnabled(true);
            }
         }) //
         .onSelectionChanged(sdk -> {
            Buttons.selectRadio(sdk == null ? radioDefaultSDK : radioAltSDK);
            selectedAltSDK_internal = sdk;
            if (radioAltSDK.getSelection()) {
               selectedAltSDK.set(sdk);
            }
         });
      selectedAltSDK.subscribe((Consumer<HaxeSDK>) cmbAltSDK::setSelection);

      final var defaultSDK = HaxeWorkspacePreference.getDefaultHaxeSDK(false, true);
      final var sdks = HaxeWorkspacePreference.getHaxeSDKs();

      txtDefaultSdk.setText(defaultSDK == null ? "" : defaultSDK.toShortString());

      cmbAltSDK.setItems(sdks);
      if (!sdks.isEmpty()) {
         cmbAltSDK.setSelection(sdks.first(), true);
      }
   }
}
