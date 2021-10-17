/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.widget;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.project.HaxeProject;
import org.haxe4e.util.ui.GridDatas;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.ref.ObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuildFileSelectionGroup extends Composite {

   public final ObservableRef<IProject> project = new ObservableRef<>();
   public final ObservableRef<String> buildFile = new ObservableRef<>();

   public HaxeBuildFileSelectionGroup(final Composite parent, final Object layoutData) {
      this(parent, SWT.NONE, layoutData);
   }

   public HaxeBuildFileSelectionGroup(final Composite parent, final int style, final Object layoutData) {
      super(parent, style);

      setLayoutData(layoutData);
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpBuildFile = new Group(this, SWT.NONE);
      grpBuildFile.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpBuildFile.setText("Build file (*.hxml)");
      grpBuildFile.setLayout(new GridLayout(2, false));

      final var txtSelectedBuildFile = new Text(grpBuildFile, SWT.BORDER);
      txtSelectedBuildFile.setEditable(false);
      txtSelectedBuildFile.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtSelectedBuildFile, buildFile);

      final var btnBrowseBuildFile = new Button(grpBuildFile, SWT.NONE);
      btnBrowseBuildFile.setText(Messages.Label_Browse);
      btnBrowseBuildFile.setEnabled(false);
      Buttons.onSelected(btnBrowseBuildFile, this::onBuildFileButton);

      project.subscribe(p -> {
         btnBrowseBuildFile.setEnabled(p != null);
         if (p != null) {
            final var prefs = new HaxeProjectPreference(p);
            buildFile.set(prefs.getHaxeBuildFile());
         }
      });
   }

   private AbstractElementListSelectionDialog createSelectBuildFileDialog(final Shell shell, final IProject project,
      final String preselectedBuildFile) {

      final var dialog = new ElementListSelectionDialog(shell, new LabelProvider() {
         @Override
         public Image getImage(final Object element) {
            return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_BUILD_FILE);
         }
      });

      dialog.setTitle("Select a Haxe build file");
      dialog.setMessage("Enter a string to search for a file:");
      dialog.setEmptyListMessage("The project has no build files.");

      try {
         final var haxeProject = new HaxeProject(project);
         final var buildFiles = haxeProject.getBuildFiles();
         dialog.setElements(buildFiles.toArray(new String[buildFiles.size()]));
         dialog.setInitialSelections(preselectedBuildFile);
      } catch (final CoreException e) {
         Haxe4EPlugin.log().error(e);
      }

      return dialog;
   }

   private void onBuildFileButton() {
      final var dialog = createSelectBuildFileDialog(getShell(), project.get(), buildFile.get());

      if (dialog.open() == Window.OK) {
         final var buildFilePath = dialog.getFirstResult().toString();

         if (buildFilePath != null) {
            buildFile.set(buildFilePath);
         }
      }
   }
}
