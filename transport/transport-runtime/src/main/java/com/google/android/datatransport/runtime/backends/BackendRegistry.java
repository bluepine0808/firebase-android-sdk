// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.datatransport.runtime.backends;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Container for registered {@link TransportBackend}s. */
@Singleton
public class BackendRegistry {

  private static final String TAG = "BackendRegistry";
  private static final String BACKEND_KEY_PREFIX = "backend:";

  private final Context applicationContext;
  private final CreationContextFactory creationContext;
  private final Map<String, TransportBackend> backends = new HashMap<>();
  private Map<String, String> backendProviders = null;

  @Inject
  public BackendRegistry(Context applicationContext, CreationContextFactory creationContext) {
    this.applicationContext = applicationContext;
    this.creationContext = creationContext;
  }

  @Nullable
  public synchronized TransportBackend get(String name) {
    if (backends.containsKey(name)) {
      return backends.get(name);
    }
    String backendProviderName = getBackendProviders().get(name);
    if (backendProviderName == null) {
      return null;
    }

    try {
      BackendFactory provider =
          (BackendFactory)
              Class.forName(backendProviderName).getDeclaredConstructor().newInstance();
      TransportBackend newBackend = provider.create(creationContext.create(name));
      backends.put(name, newBackend);
      return newBackend;
    } catch (ClassNotFoundException e) {
      Log.w(TAG, String.format("Class %s is not found.", backendProviderName), e);
    } catch (IllegalAccessException e) {
      Log.w(TAG, String.format("Could not instantiate %s.", backendProviderName), e);
    } catch (InstantiationException e) {
      Log.w(TAG, String.format("Could not instantiate %s.", backendProviderName), e);
    } catch (NoSuchMethodException e) {
      Log.w(TAG, String.format("Could not instantiate %s", backendProviderName), e);
    } catch (InvocationTargetException e) {
      Log.w(TAG, String.format("Could not instantiate %s", backendProviderName), e);
    }

    return null;
  }

  private Map<String, String> getBackendProviders() {
    if (backendProviders == null) {
      backendProviders = discover(applicationContext);
    }
    return backendProviders;
  }

  private Map<String, String> discover(Context ctx) {
    Bundle metadata = getMetadata(ctx);

    if (metadata == null) {
      Log.w(TAG, "Could not retrieve metadata, returning empty list of transport backends.");
      return Collections.emptyMap();
    }

    Map<String, String> backendNames = new HashMap<>();
    for (String key : metadata.keySet()) {
      Object rawValue = metadata.get(key);
      if (rawValue instanceof String && key.startsWith(BACKEND_KEY_PREFIX)) {
        for (String name : ((String) rawValue).split(",")) {
          name = name.trim();
          if (name.isEmpty()) {
            continue;
          }
          backendNames.put(name, key.substring(BACKEND_KEY_PREFIX.length()));
        }
      }
    }
    return backendNames;
  }

  private static Bundle getMetadata(Context context) {
    try {
      PackageManager manager = context.getPackageManager();
      if (manager == null) {
        Log.w(TAG, "Context has no PackageManager.");
        return null;
      }
      ServiceInfo info =
          manager.getServiceInfo(
              new ComponentName(context, TransportBackendDiscovery.class),
              PackageManager.GET_META_DATA);
      if (info == null) {
        Log.w(TAG, "TransportBackendDiscovery has no service info.");
        return null;
      }
      return info.metaData;
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, "Application info not found.");
      return null;
    }
  }
}