/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Sebastian Thomschke
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HaxelibJSON {

   private static final ObjectMapper JSON = new ObjectMapper();

   public static HaxelibJSON from(final Path path) throws IOException {
      return JSON.readValue(path.toAbsolutePath().toFile(), HaxelibJSON.class);
   }

   public String name;
   public String version;
   public String description;
   public String license;
   public URL url;
   public String main;
   public String classPath;
   public Map<String, String> dependencies;
   public LinkedHashSet<String> contributors;
   public LinkedHashSet<String> tags;
   public String releasenote;

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
   }
}
