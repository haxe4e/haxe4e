/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e;

/**
 * @author Sebastian Thomschke
 */
public interface Constants {

   String IMAGE_ICON = "src/main/resources/images/logo/haxe_icon.png";
   String IMAGE_NAVIGATOR_HAXE_PROJECT = "src/main/resources/images/navigator/haxe_project.png";
   String IMAGE_HAXE_BUILD_FILE = "platform:/plugin/org.eclipse.ui.externaltools/icons/full/obj16/builder.png";
   String IMAGE_HAXE_DEPENDENCIES = "platform:/plugin/org.eclipse.ui.externaltools/icons/full/obj16/classpath.png";
   String IMAGE_HAXE_SOURCE_FOLDER = "src/main/resources/images/navigator/packagefolder_obj.png";
   String IMAGE_HAXE_SOURCE_PACKAGE = "src/main/resources/images/navigator/package_obj.png";
   String IMAGE_OUTLINE_SYMBOL_ENUM_MEMBER = "src/main/resources/images/outline/enum_member.png";
   String IMAGE_OUTLINE_SYMBOL_TYPEDEF = "src/main/resources/images/outline/typedef.png";

   String LAUNCH_ATTR_PROJECT = "launch.haxe.project";
   String LAUNCH_ATTR_HAXE_SDK = "launch.haxe.sdk";
   String LAUNCH_ATTR_HAXE_BUILD_FILE = "launch.haxe.haxe_build_file";

   String HAXE_BUILD_FILE_EXTENSION = "hxml";
   String DEFAULT_HAXE_BUILD_FILE = "build.hxml";

   /**
    * id of <launchConfigurationType/> as specified in plugin.xml
    */
   String LAUNCH_HAXE_CONFIGURATION_ID = "org.haxe4e.launch.haxe";

   /**
    * id of <launchGroup/> as specified in plugin.xml
    */
   String LAUNCH_HAXE_GROUP = "org.haxe4e.launch.haxe.group";
}
