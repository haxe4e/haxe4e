/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.launch;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate.DSPLaunchDelegateLaunchBuilder;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;
import org.haxe4e.Constants;
import org.haxe4e.localization.Messages;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.BundleResourceUtils;
import org.haxe4e.util.StatusUtils;
import org.haxe4e.util.TreeBuilder;
import org.haxe4e.util.ui.Dialogs;
import org.haxe4e.util.ui.UI;

import net.sf.jstuff.core.Strings;

/**
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class LaunchConfig extends LaunchConfigurationDelegate {

   @Override
   public void launch(final ILaunchConfiguration config, final String mode, final ILaunch launch, final IProgressMonitor monitor)
      throws CoreException {

      final var projectName = config.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "");
      final var project = Strings.isBlank(projectName) ? null : ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      if (project == null || !project.exists()) {
         Dialogs.showError(Messages.Launch_NoProjectSelected, Messages.Launch_NoProjectSelected_Descr);
         return;
      }

      final var prefs = new HaxeProjectPreference(project);
      final var haxeSDK = prefs.getEffectiveHaxeSDK();
      if (!haxeSDK.isValid()) {
         Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
         return;
      }

      var hxmlFile = config.getAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, Constants.DEFAULT_HAXE_BUILD_FILE);
      if ("".equals(hxmlFile)) {
         hxmlFile = Constants.DEFAULT_HAXE_BUILD_FILE;
      }
      final var hxmlFilePath = Paths.get(project.getLocation().toPortableString(), hxmlFile);

      final var cwd = Paths.get(config.getAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, project.getLocation().toOSString()));
      final var envVars = config.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Collections.emptyMap());
      final var appendEnvVars = config.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);

      switch (mode) {
         case ILaunchManager.DEBUG_MODE:

            // LaunchRequestArguments https://github.com/vshaxe/vscode-debugadapter-extern/blob/master/src/vscode/debugProtocol/DebugProtocol.hx#L764
            // EvalLaunchRequestArguments https://github.com/vshaxe/eval-debugger/blob/master/src/Main.hx#L14
            final var initOptions = new TreeBuilder<String>() //
               .put("cwd", cwd.toString()) //
               .put("args", Arrays.asList(hxmlFilePath.toString())) //
               .put("haxeExecutable", new TreeBuilder<String>() //
                  .put("executable", haxeSDK.getCompilerExecutable().toString()) //
                  .put("env", envVars) //
               ) //
               .put("stopOnEntry", false) //
               .getMap();

            try {
               final var evalDebuggerJS = BundleResourceUtils.extractBundleResource("langsrv/haxe-eval-debugger.js");
               final List<String> debugCmdArgs = Collections.singletonList(evalDebuggerJS.getAbsolutePath());

               final var builder = new DSPLaunchDelegateLaunchBuilder(config, ILaunchManager.DEBUG_MODE, launch, monitor);
               builder.setLaunchDebugAdapter(NodeJSManager.getNodeJsLocation().getAbsolutePath(), debugCmdArgs);
               builder.setMonitorDebugAdapter(config.getAttribute(DSPPlugin.ATTR_DSP_MONITOR_DEBUG_ADAPTER, true));
               builder.setDspParameters(initOptions);
               new DSPLaunchDelegate() {}.launch(builder);
            } catch (final IOException ex) {
               Dialogs.showStatus("Failed to start debug session", StatusUtils.createError(ex), true);
            }
            return;
         case ILaunchManager.RUN_MODE:
            HaxeRunner.launchHxmlFile( //
               launch, //
               haxeSDK.getCompilerExecutable(), //
               hxmlFilePath, //
               cwd, //
               envVars, //
               appendEnvVars, //
               action -> { //
                  try {
                     project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                  } catch (final CoreException e) {
                     org.haxe4e.util.LOG.error(e);
                  }
               });
            return;
         default:
            UI.run(() -> MessageDialog.openError(null, "Unsupported launch mode", "Launch mode [" + mode + "] is not supported."));
      }
   }

}
