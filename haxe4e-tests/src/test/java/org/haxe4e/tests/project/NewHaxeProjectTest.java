/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.tests.project;

import static org.assertj.core.api.Assertions.*;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
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
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Sebastian Thomschke
 */
@RunWith(RedDeerSuite.class)
@SuppressWarnings("unused")
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
      try { // CHECKSTYLE:IGNORE RequireFailForTryCatchInJunitCheck
         new WaitUntil(new ShellIsActive(Messages.Prefs_NoSDKRegistered_Title), TimePeriod.MEDIUM);
         new OkButton().click();
      } catch (final WaitTimeoutExpiredException ex) {
         // ignore
      }

      // dismiss preference dialog
      try { // CHECKSTYLE:IGNORE RequireFailForTryCatchInJunitCheck
         new WaitUntil(new ShellIsActive("Preferences (Filtered)"), TimePeriod.MEDIUM);
         new CancelButton().click();
      } catch (final WaitTimeoutExpiredException ex) {
         // ignore
      }

      // fill out the wizard
      new DefaultShell(Messages.NewHaxeProject);
      new LabeledText("Project name:").setText("haxe-project");
      new FinishButton().click();
      new WaitUntil(new ProjectExists("haxe-project"));

      // check presence of project
      assertThat(Project.getProjectNatureIds("haxe-project")).contains(HaxeProjectNature.NATURE_ID);
   }
}
