/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;
import org.haxe4e.project.HaxeProjectNature;
import org.haxe4e.project.HaxeProjectSelectionDialog;
import org.haxe4e.util.ui.GridDatas;
import org.haxe4e.widget.HaxeBuildFileSelectionGroup;
import org.haxe4e.widget.HaxeSDKSelectionGroup;

import de.sebthom.eclipse.commons.ui.Buttons;
import de.sebthom.eclipse.commons.ui.Projects;
import de.sebthom.eclipse.commons.ui.Texts;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.ref.ObservableRef;

/**
 * @author Sebastian Thomschke
 */
public class LaunchConfigForm extends Composite {

   public final ObservableRef<IProject> selectedProject = new ObservableRef<>();
   public final ObservableRef<String> buildFile;
   public final ObservableRef<HaxeSDK> selectedAltSDK;

   public LaunchConfigForm(final Composite parent, final int style) {
      super(parent, style);
      setLayout(new GridLayout(1, false));

      final var grpProject = new Group(this, SWT.NONE);
      grpProject.setLayout(new GridLayout(2, false));
      grpProject.setLayoutData(GridDatas.fillHorizontalExcessive());
      grpProject.setText(Messages.Label_Project);

      final var txtSelectedProject = new Text(grpProject, SWT.BORDER);
      txtSelectedProject.setEditable(false);
      txtSelectedProject.setLayoutData(GridDatas.fillHorizontalExcessive());
      Texts.bind(txtSelectedProject, selectedProject, //
         projectName -> Strings.isEmpty(projectName) //
            ? null //
            : Projects.getProject(projectName, HaxeProjectNature.NATURE_ID), //
         project -> project == null ? "" : project.getName() //
      );

      final var btnBrowseProject = new Button(grpProject, SWT.NONE);
      btnBrowseProject.setText(Messages.Label_Browse);
      Buttons.onSelected(btnBrowseProject, this::onSelectProject);

      final var grpBuildFile = new HaxeBuildFileSelectionGroup(this, GridDatas.fillHorizontalExcessive());
      buildFile = grpBuildFile.buildFile;

      selectedProject.subscribe(grpBuildFile.project::set);

      final var grpHaxeSDKSelection = new HaxeSDKSelectionGroup(this, GridDatas.fillHorizontalExcessive());
      selectedAltSDK = grpHaxeSDKSelection.selectedAltSDK;
   }

   private void onSelectProject() {
      var project = selectedProject.get(); //
      project = new HaxeProjectSelectionDialog(getShell()) //
         .setSelectedProject(project) //
         .show();

      if (project != null) {
         selectedProject.set(project);
      }
   }
}
