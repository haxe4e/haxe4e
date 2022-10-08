/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.widget;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.buildsystem.BuildFile;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.ui.GridDatas;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.ref.MutableObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class HaxeBuildFileSelectionGroup extends Composite {

   private @Nullable HaxeProjectPreference projectPrefs;
   private Button btnBrowseForBuildFile;
   public final MutableObservableRef<@Nullable BuildFile> selectedBuildFile = MutableObservableRef.of(null);

   public HaxeBuildFileSelectionGroup(final Composite parent, final int style, final Object layoutData) {
      super(parent, style);

      setLayoutData(layoutData);
      setLayout(GridLayoutFactory.fillDefaults().create());

      final var grpBuildFile = new Group(this, SWT.NONE);
      grpBuildFile.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpBuildFile.setText("Build File");
      grpBuildFile.setLayout(new GridLayout(2, false));

      final var txtSelectedBuildFile = new Text(grpBuildFile, SWT.BORDER);
      txtSelectedBuildFile.setEditable(false);
      txtSelectedBuildFile.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtSelectedBuildFile, selectedBuildFile, this::getBuildFile, bf -> bf == null ? "" : bf.getProjectRelativePath());

      btnBrowseForBuildFile = new Button(grpBuildFile, SWT.NONE);
      btnBrowseForBuildFile.setText(Messages.Label_Browse);
      btnBrowseForBuildFile.setEnabled(false);
      Buttons.onSelected(btnBrowseForBuildFile, this::onBrowseForBuildFile);
   }

   public HaxeBuildFileSelectionGroup(final Composite parent, final Object layoutData) {
      this(parent, SWT.NONE, layoutData);
   }

   private @Nullable BuildFile getBuildFile(final String path) {
      final var projectPrefs = this.projectPrefs;
      if (projectPrefs == null || Strings.isBlank(path))
         return null;
      final var buildSystem = projectPrefs.getBuildSystem();
      return buildSystem.toBuildFile(projectPrefs.getProject().getFile(path));
   }

   private AbstractElementListSelectionDialog createSelectBuildFileDialog() {
      final var dlg = new ElementListSelectionDialog(getShell(), new LabelProvider() {
         @Override
         public @Nullable Image getImage(final @Nullable Object element) {
            return Haxe4EPlugin.get().getSharedImage(Constants.IMAGE_HAXE_BUILD_FILE);
         }

         @Override
         public String getText(final @Nullable Object element) {
            if (element == null)
               return "";
            return ((IFile) element).getProjectRelativePath().toPortableString();
         }
      });
      final var buildSystem = asNonNull(projectPrefs).getBuildSystem();
      dlg.setTitle("Select a build file (*." + buildSystem.getBuildFileExtension() + ")");
      dlg.setMessage("Enter a string to search for a file:");
      dlg.setEmptyListMessage("The project has no build files.");
      dlg.setEmptySelectionMessage("No matches found.");
      try {
         final var buildFiles = buildSystem.findFilesWithBuildFileExtension(asNonNull(projectPrefs).getProject(), true);
         if (!buildFiles.isEmpty()) {
            dlg.setElements(buildFiles.toArray(IFile[]::new));
            dlg.setInitialSelections(selectedBuildFile.get());
         }
      } catch (final CoreException ex) {
         Haxe4EPlugin.log().error(ex);
      }
      return dlg;
   }

   private void onBrowseForBuildFile() {
      final var dialog = createSelectBuildFileDialog();

      if (dialog.open() == Window.OK) {
         final var buildFile = (IFile) dialog.getFirstResult();

         if (buildFile != null) {
            final var buildSystem = asNonNull(projectPrefs).getBuildSystem();
            selectedBuildFile.set(buildSystem.toBuildFile(buildFile));
         }
      }
   }

   public void setProject(final @Nullable IProject project) {
      if (project == null) {
         projectPrefs = null;
         btnBrowseForBuildFile.setEnabled(false);
      } else {
         projectPrefs = HaxeProjectPreference.get(project);
         selectedBuildFile.set(projectPrefs.getBuildFile());
         btnBrowseForBuildFile.setEnabled(true);
      }
   }
}
