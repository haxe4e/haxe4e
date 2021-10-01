/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.tests.project;

import static org.assertj.core.api.Assertions.*;

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.direct.project.Project;
import org.eclipse.reddeer.eclipse.condition.ProjectExists;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.cleanworkspace.CleanWorkspaceRequirement;
import org.eclipse.reddeer.swt.condition.ShellIsActive;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ShellMenu;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.haxe4e.localization.Messages;
import org.haxe4e.project.HaxeProjectNature;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sebastian Thomschke
 */
@RunWith(RedDeerSuite.class)
@SuppressWarnings("unused")
@Ignore
public class NewHaxeProjectTest {

   private CleanWorkspaceRequirement cwr = new CleanWorkspaceRequirement();

   @Before
   public void setup() {
      cwr.fulfill();
   }

   @After
   public void teardown() {
      cwr.fulfill();
   }

   @Test
   public void testMenuEntriesAvailable() {
      assertThat(new ShellMenu().hasItem("File", "New", Messages.Label_Haxe_Project)).isTrue();
      assertThat(new ShellMenu().hasItem("File", "New", Messages.Label_Haxe_File)).isTrue();
   }

   @Test
   public void testNewHaxeProject() {
      // launch File -> New -> Haxe Project
      new ShellMenu().getItem("File", "New", Messages.Label_Haxe_Project).select();

      // dismiss Haxe not found error dialog
      new WaitUntil(new ShellIsActive(Messages.Prefs_NoSDKRegistered_Title));
      new OkButton().click();

      // dismiss preference dialog
      new WaitUntil(new ShellIsActive("Preferences (Filtered)"));
      new CancelButton().click();

      // fill out the wizard
      new DefaultShell(Messages.NewHaxeProject);
      new LabeledText("Project name:").setText("haxe-project");
      new FinishButton().click();
      new WaitUntil(new ProjectExists("haxe-project"));

      // check presence of project
      assertThat(Project.getProjectNatureIds("haxe-project")).contains(HaxeProjectNature.NATURE_ID);
   }
}
