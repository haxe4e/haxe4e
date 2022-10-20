/*
 * Copyright 2021-2022 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.model;

import static net.sf.jstuff.core.validation.NullAnalysisHelper.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Sebastian Thomschke
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HaxelibJSON {

   private static final ObjectMapper JSON = new ObjectMapper();

   public static HaxelibJSON from(final Path path) throws IOException {
      return asNonNull(JSON.readValue(path.normalize().toAbsolutePath().toFile(), HaxelibJSON.class));
   }

   public String name = lazyNonNull();
   public @Nullable String version;
   public @Nullable String description;
   public @Nullable String license;
   public @Nullable URL url;
   public @Nullable String main;
   public @Nullable String classPath;
   public @Nullable Map<String, String> dependencies;
   public @Nullable LinkedHashSet<String> contributors;
   public @Nullable LinkedHashSet<String> tags;
   public @Nullable String releasenote;

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
   }
}
