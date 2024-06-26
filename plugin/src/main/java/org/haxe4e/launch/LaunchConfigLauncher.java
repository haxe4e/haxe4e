/*
 * SPDX-FileCopyrightText: © The Haxe4E authors
 * SPDX-FileContributor: Sebastian Thomschke
 * SPDX-License-Identifier: EPL-2.0
 * SPDX-ArtifactOfProjectHomePage: https://github.com/haxe4e/haxe4e
 */
package org.haxe4e.launch;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.asNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.lsp4e.debug.DSPPlugin;
import org.eclipse.lsp4e.debug.launcher.DSPLaunchDelegate.DSPLaunchDelegateLaunchBuilder;
import org.eclipse.wildwebdeveloper.embedder.node.NodeJSManager;
import org.haxe4e.Constants;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.buildsystem.BuildFile;
import org.haxe4e.model.buildsystem.BuildSystem;
import org.haxe4e.prefs.HaxeProjectPreference;
import org.haxe4e.util.TreeBuilder;

import de.sebthom.eclipse.commons.resources.Projects;
import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;

/**
 * This class is registered via the plugin.xml
 *
 * @author Sebastian Thomschke
 */
@SuppressWarnings("restriction")
public class LaunchConfigLauncher extends LaunchConfigurationDelegate {

   @Override
   public void launch(final ILaunchConfiguration config, final String mode, final ILaunch launch, final @Nullable IProgressMonitor monitor)
         throws CoreException {

      final var projectName = config.getAttribute(Constants.LAUNCH_ATTR_PROJECT, "");
      final @Nullable IProject project = Strings.isBlank(projectName) ? null : Projects.getProject(projectName);
      if (project == null) {
         Dialogs.showError(Messages.Launch_NoProjectSelected, Messages.Launch_NoProjectSelected_Descr);
         return;
      }
      final var projectLoc = asNonNull(project.getLocation());

      final var prefs = HaxeProjectPreference.get(project);
      final var haxeSDK = prefs.getEffectiveHaxeSDK();
      if (haxeSDK == null || !haxeSDK.isValid()) {
         Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
         return;
      }

      final var buildSystem = prefs.getBuildSystem();
      if (buildSystem != BuildSystem.HAXE && buildSystem != BuildSystem.LIX) {
         Dialogs.showError("Unsupported Build System", "Running code via " + buildSystem + " is not yet supported.");
         return;
      }

      final var hxmlFileRelativePath = config.getAttribute(Constants.LAUNCH_ATTR_HAXE_BUILD_FILE, "");
      final BuildFile hxmlFile;
      if (Strings.isBlank(hxmlFileRelativePath)) {
         hxmlFile = BuildSystem.HAXE.findDefaultBuildFile(project);
         if (hxmlFile == null) {
            Dialogs.showError("Default build file not found.", "Default build file " + buildSystem.getDefaultBuildFileNames().first()
                  + " not found.");
            return;
         }
      } else {
         final var hxmlFilePath = Paths.get(projectLoc.toPortableString(), hxmlFileRelativePath);
         if (!Files.exists(hxmlFilePath)) {
            Dialogs.showError("Build file does not exist", "The configured build file '" + hxmlFileRelativePath + "' does not exist.");
            return;
         }
         hxmlFile = buildSystem.toBuildFile(project.getFile(hxmlFileRelativePath));
      }

      final var hxmlFilePath = asNonNull(hxmlFile.location.getLocation()).toFile().toPath().normalize().toAbsolutePath();

      final var workdir = Paths.get(config.getAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, projectLoc.toOSString()));
      final var envVars = config.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Collections.emptyMap());
      final var appendEnvVars = config.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);

      switch (mode) {

         case ILaunchManager.DEBUG_MODE:
            final var debuggerEnvVars = new HashMap<String, Object>();
            debuggerEnvVars.put("PATH", asNonNull(System.getenv("PATH")));
            debuggerEnvVars.putAll(envVars);
            haxeSDK.configureEnvVars(debuggerEnvVars);

            // to prevent StackOverflowError in lsp4j
            debuggerEnvVars.replaceAll((k, v) -> v = Objects.toString(v));

            // workaround for nodejs issue: https://github.com/nodejs/node/issues/20605 in conjunction with
            // https://github.com/vshaxe/eval-debugger/blob/fd1c7844e9b13fce6f2f85c7b19d8948cd230cce/src/Main.hx#L110-L114
            // resulting in "Error: 'haxelib' is not recognized as an internal or external command"
            debuggerEnvVars.put("Path", asNonNull(debuggerEnvVars.get("PATH")));

            // LaunchRequestArguments https://github.com/vshaxe/vscode-debugadapter-extern/blob/master/src/vscode/debugProtocol/DebugProtocol.hx#L764
            // EvalLaunchRequestArguments https://github.com/vshaxe/eval-debugger/blob/master/src/Main.hx#L14
            final var evalDebuggerOpts = new TreeBuilder<String>() //
               .put("cwd", workdir.toString()) //
               .put("args", Arrays.asList(hxmlFilePath.toString())) //
               .put("haxeExecutable", new TreeBuilder<String>() //
                  .put("executable", haxeSDK.getCompilerExecutable().toString()) //
                  .put("env", debuggerEnvVars)) //
               .put("noDebug", false) //
               .put("showGeneratedVariables", true) //
               .put("stopOnEntry", false) //
               .put("trace", true) //
               .getMap();

            try {
               final var evalDebuggerJS = Haxe4EPlugin.resources().extract("langsrv/haxe-eval-debugger.min.js");
               final var builder = new DSPLaunchDelegateLaunchBuilder(config, ILaunchManager.DEBUG_MODE, launch, monitor);
               builder.setLaunchDebugAdapter( //
                  NodeJSManager.getNodeJsLocation().getAbsolutePath(), //
                  List.of(evalDebuggerJS.getAbsolutePath()));
               builder.setMonitorDebugAdapter(config.getAttribute(DSPPlugin.ATTR_DSP_MONITOR_DEBUG_ADAPTER, true));
               builder.setDspParameters(evalDebuggerOpts);
               new LaunchDebugConfig().launch(builder);
            } catch (final IOException | CoreException ex) {
               Dialogs.showStatus("Failed to start debug session", Haxe4EPlugin.status().createError(ex), true);
            }
            return;

         case ILaunchManager.RUN_MODE:
            try {
               final var proc = haxeSDK.getCompilerProcessBuilder(!appendEnvVars) //
                  .withArg(hxmlFilePath) //
                  .withEnvironment(env -> env.putAll(envVars)) //
                  .withWorkingDirectory(workdir) //
                  .onExit(process -> {
                     try {
                        RefreshUtil.refreshResources(config, monitor);
                     } catch (final CoreException e) {
                        Haxe4EPlugin.log().error(e);
                     }
                  }) //
                  .start();
               launch.addProcess(DebugPlugin.newProcess(launch, proc.getProcess(), Messages.Label_Haxe_Terminal));
            } catch (final IOException ex) {
               Dialogs.showStatus(Messages.Launch_CouldNotRunHaxe, Haxe4EPlugin.status().createError(ex), true);
            }
            return;

         default:
            UI.run(() -> MessageDialog.openError(null, "Unsupported launch mode", "Launch mode [" + mode + "] is not supported."));
      }
   }
}
