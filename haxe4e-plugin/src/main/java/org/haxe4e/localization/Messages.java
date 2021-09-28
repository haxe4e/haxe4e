/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.localization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.osgi.util.NLS;
import org.haxe4e.util.LOG;

import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.io.IOUtils;
import net.sf.jstuff.core.reflection.Fields;

/**
 * @author Sebastian Thomschke
 */
public final class Messages extends NLS {

   private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages";

   // Keys with default values directly assigned in this class are only used by Java classes.
   // Keys without default values are loaded from messages.properties, because they are also referenced in plugin.xml

   // CHECKSTYLE:IGNORE .* FOR NEXT 100 LINES

   public static String Label_Haxe_Configuration = "Haxe Configuration";
   public static String Label_Haxe_SDK = "Haxe SDK";
   public static String Label_Haxe_File;
   public static String Label_Haxe_Project;
   public static String Label_Haxe_Terminal = "Haxe Terminal";
   public static String Label_Haxe_Build_File;
   public static String Label_Name = "Name";
   public static String Label_Path = "Path";
   public static String Label_Project = "Project";
   public static String Label_Add = "Add";
   public static String Label_Browse = "Browse...";
   public static String Label_Edit = "Edit";
   public static String Label_Remove = "Remove";
   public static String Label_Default = "Default";
   public static String Label_Alternative = "Alternative";

   public static String Error_ValueMustBeSpecified = "{0} must be specified.";

   public static String SDKPathInvalid = "Haxe SDK Path invalid.";
   public static String SDKPathInvalid_Descr = "'{0}' does not point to a valid Haxe SDK";
   public static String NekoPathInvalid = "Neko VM Path invalid";
   public static String NekoPathInvalid_Descr = "'{0}' does not point to a valid Neko VM";

   public static String Prefs_ManageSDKsDescription = "Manage installed Haxe SDKs. By default the checked SDK will be used for newly created Haxe projects.";
   public static String Prefs_NoSDKRegistered_Title = "Haxe SDK missing";
   public static String Prefs_NoSDKRegistered_Body = "No Haxe SDK configured";
   public static String Prefs_SDKPath = "Haxe SDK path";
   public static String Prefs_SDKPathInvalid_Message = "No valid Haxe SDK seems to be installed on your system.\n\nPlease ensure that the Haxe SDK is installed and is on your PATH.";
   public static String Prefs_SavingPreferencesFailed = "Saving preferences failed.";

   public static String NewHaxeFile = "New Haxe File";
   public static String NewHaxeFile_Descr;
   public static String NewHaxeFile_Creating = "Creating Haxe file '{0}'...";
   public static String NewHaxeFile_OpeningInEditor = "Opening Haxe file in editor...";
   public static String NewHaxeFile_DirectoryDoesNotExist = "Directory '{0}' does not exist.";

   public static String NewHaxeProject = "New Haxe Project";
   public static String NewHaxeProject_Descr;
   public static String NewHaxeProject_SDKNotFound_Message = "No valid Haxe SDK found! Please specify a valid SDK on the Haxe preference page.";
   public static String NewHaxeProject_ErrorTitle = "Project Creation Problem";
   public static String NewHaxeProject_UnexpectedError = "Unexpected error: {0}";
   public static String NewHaxeProject_CaseVariantExistsError = "The file system is not case sensitive. An existing file or directory conflicts with '{0}'.";

   public static String Launch_NoProjectSelected = "No project selected";
   public static String Launch_NoProjectSelected_Descr = "Please select a project to launch";
   public static String Launch_SDKPath_Descr = "The path where the Haxe SDK is installed";
   public static String Launch_HaxeBuildFile_Descr = "Haxe build file";
   public static String Launch_RunningFile = "Running {0}";
   public static String Launch_CouldNotRunHaxe = "Could not run Haxe";
   public static String Launch_InitializingLaunchConfigTabFailed = "Initializing LaunchConfigTab failed";
   public static String Launch_CreatingLaunchConfigFailed = "Creating new launch configuration failed";

   static {
      //NLS.initializeMessages(BUNDLE_NAME, Messages.class); // does not support field default values
      initMessages();
   }

   private static void initMessages() {
      /*
       * calculate names of message properties files based on current locale
       */
      final List<String> messagePropFiles = new ArrayList<>(4);
      {
         final var root = BUNDLE_NAME.replace('.', '/');
         var locale = Locale.getDefault().toString();
         while (true) {
            messagePropFiles.add(root + '_' + locale + ".properties");
            if (locale.lastIndexOf('_') == -1) {
               break;
            }
            locale = Strings.substringBeforeLast(locale, "_");
         }
         messagePropFiles.add(root + ".properties");
         Collections.reverse(messagePropFiles);
      }

      /*
       * load values from all .properties files
       */
      final var messageProps = new Properties();
      for (final String variant : messagePropFiles) {
         try (var input = Messages.class.getClassLoader().getResourceAsStream(variant)) {
            if (input != null) {
               messageProps.load(input);
            }
         } catch (final IOException ex) {
            LOG.error(ex, "Error loading {0}", variant);
         }
      }

      /*
       * load plugin.xml as String to later check for references to message properties
       */
      var pluginXml = "";
      try (var is = Messages.class.getClassLoader().getResourceAsStream("/plugin.xml")) {
         pluginXml = IOUtils.toString(is);
      } catch (final IOException ex) {
         LOG.error(ex);
      }

      /*
       * apply message properties to fields
       */
      for (final var entry : messageProps.entrySet()) {
         final var fieldName = entry.getKey().toString();
         final var field = Fields.find(Messages.class, fieldName);
         if (field == null) {
            if (!pluginXml.contains("\"%" + fieldName + "\"")) {
               LOG.warn("Unused message property: " + fieldName);
            }
            continue;
         }

         try {
            Fields.write(null, field, entry.getValue());
         } catch (final Exception ex) {
            LOG.error(ex);
         }
      }

      /*
       * ensure all fields are set
       */
      for (final var field : Messages.class.getFields()) {
         if (!Fields.isStatic(field)) {
            continue;
         }
         if (Fields.read(null, field) == null) {
            LOG.error("Uninizialied message property: " + field.getName());
         }
      }
   }

   private Messages() {
   }
}
