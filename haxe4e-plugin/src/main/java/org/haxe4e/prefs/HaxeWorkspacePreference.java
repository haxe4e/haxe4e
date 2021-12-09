/*
 * Copyright 2021 by the Haxe4E authors.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.haxe4e.prefs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.haxe4e.Haxe4EPlugin;
import org.haxe4e.localization.Messages;
import org.haxe4e.model.HaxeSDK;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import de.sebthom.eclipse.commons.ui.Dialogs;
import de.sebthom.eclipse.commons.ui.UI;
import net.sf.jstuff.core.Strings;
import net.sf.jstuff.core.io.RuntimeIOException;

/**
 * @author Sebastian Thomschke
 */
public final class HaxeWorkspacePreference {

   private static final ObjectMapper JSON = new ObjectMapper();

   private static final String PROPERTY_DEFAULT_HAXE_SDK = "haxe.default_sdk";
   private static final String PROPERTY_HAXE_SDKS = "haxe.sdks";
   private static final String PROPERTY_WARNED_NO_SDK_REGISTERED = "haxe.warned_no_sdk_registered";

   static {
      // this disables usage of com.fasterxml.jackson.databind.ext.NioPathDeserializer
      // which results in freezes because on first usage all drive letters are iterated
      // which will hang for mapped but currently not reachable network drives
      final var m = new SimpleModule("CustomNioPathSerialization");
      m.addSerializer(Path.class, new ToStringSerializer());
      m.addDeserializer(Path.class, new FromStringDeserializer<Path>(Path.class) {
         private static final long serialVersionUID = 1L;

         @Override
         protected Path _deserialize(final String value, final DeserializationContext ctxt) throws IOException {
            return Paths.get(value);
         }
      });
      JSON.registerModule(m);
   }

   static final IPersistentPreferenceStore PREFS = new ScopedPreferenceStore(InstanceScope.INSTANCE, Haxe4EPlugin.PLUGIN_ID);

   // CHECKSTYLE:IGNORE .* FOR NEXT 3 LINES
   private static final SortedSet<HaxeSDK> haxeSDKs = new TreeSet<>();
   private static boolean isHaxeSDKsInitialized = false;

   private static void ensureHaxeSDKsInitialized() {
      synchronized (haxeSDKs) {
         if (!isHaxeSDKsInitialized) {
            isHaxeSDKsInitialized = true;

            final var haxeSDKsSerialized = PREFS.getString(PROPERTY_HAXE_SDKS);
            if (Strings.isNotBlank(haxeSDKsSerialized)) {
               try {
                  haxeSDKs.addAll(JSON.readValue(haxeSDKsSerialized, new TypeReference<List<HaxeSDK>>() {}));
               } catch (final Exception ex) {
                  Haxe4EPlugin.log().error(ex);
               }
            }

            if (haxeSDKs.isEmpty()) {
               final var defaultSDK = HaxeSDK.fromPath();
               if (defaultSDK != null) {
                  Haxe4EPlugin.log().info("Registering system {0} with {1}", defaultSDK, defaultSDK.getNekoVM());
                  haxeSDKs.add(defaultSDK);
                  setDefaultHaxeSDK(defaultSDK.getName());
                  save();
               }
            }
         }

         if (haxeSDKs.isEmpty() && !PREFS.getBoolean(PROPERTY_WARNED_NO_SDK_REGISTERED)) {
            for (final var ste : new Throwable().getStackTrace()) {
               final var prefPage = HaxeSDKPreferencePage.class.getName();
               // don't show warning on empty workspace if we directly go to Haxe Prefs
               if (ste.getClassName().equals(prefPage))
                  return;
            }

            PREFS.setValue(PROPERTY_WARNED_NO_SDK_REGISTERED, true);
            save();

            UI.run(() -> {
               Dialogs.showError(Messages.Prefs_NoSDKRegistered_Title, Messages.Prefs_NoSDKRegistered_Body);
               final var dialog = PreferencesUtil.createPreferenceDialogOn( //
                  UI.getShell(), //
                  HaxeSDKPreferencePage.class.getName(), //
                  new String[] {HaxeSDKPreferencePage.class.getName()}, //
                  null //
               );
               dialog.open();
            });
         }
      }
   }

   /**
    * @return null if not found
    */
   public static HaxeSDK getDefaultHaxeSDK(final boolean verify, final boolean searchPATH) {
      final var defaultSDK = getHaxeSDK(PREFS.getString(PROPERTY_DEFAULT_HAXE_SDK));

      if (defaultSDK != null) {
         if (verify) {
            if (defaultSDK.isValid())
               return defaultSDK;
         } else
            return defaultSDK;
      }

      synchronized (haxeSDKs) {
         if (!haxeSDKs.isEmpty()) {
            if (verify) {
               for (final var h : haxeSDKs) {
                  if (h.isValid())
                     return h;
               }
            } else
               return haxeSDKs.first();
         }
      }

      if (!searchPATH)
         return null;

      return HaxeSDK.fromPath();
   }

   /**
    * @return null if not found
    */
   public static HaxeSDK getHaxeSDK(final String name) {
      if (Strings.isEmpty(name))
         return null;

      ensureHaxeSDKsInitialized();

      synchronized (haxeSDKs) {
         for (final var sdk : haxeSDKs)
            if (Objects.equals(name, sdk.getName()))
               return sdk;
      }
      return null;
   }

   public static SortedSet<HaxeSDK> getHaxeSDKs() {
      ensureHaxeSDKsInitialized();

      synchronized (haxeSDKs) {
         return new TreeSet<>(haxeSDKs);
      }
   }

   public static boolean save() {
      try {
         PREFS.save();
         return true;
      } catch (final IOException ex) {
         Dialogs.showStatus(Messages.Prefs_SavingPreferencesFailed, Haxe4EPlugin.status().createError(ex), true);
         return false;
      }
   }

   public static void setDefaultHaxeSDK(final String name) {
      PREFS.setValue(PROPERTY_DEFAULT_HAXE_SDK, name);
   }

   public static void setHaxeSDKs(final Set<HaxeSDK> newSDKs) {
      ensureHaxeSDKsInitialized();

      synchronized (haxeSDKs) {
         haxeSDKs.clear();
         if (newSDKs != null) {
            haxeSDKs.addAll(newSDKs);
         }
         try {
            PREFS.setValue(PROPERTY_HAXE_SDKS, JSON.writeValueAsString(haxeSDKs));
         } catch (final JsonProcessingException ex) {
            throw new RuntimeIOException(ex);
         }
      }
   }

   private HaxeWorkspacePreference() {
   }
}
