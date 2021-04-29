/**
 * Copyright (C) 2021 Bill Havanki
 *
 * This file is part of Doppio.
 *
 * Doppio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.havanki.doppio;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/**
 * A loader for server properties that reads from a YAML file.
 */
@SuppressWarnings("unchecked")
public class ServerPropertiesYamlLoader {

  /**
   * Loads server properties from a YAML file.
   *
   * @param  reader reader for YAML file
   * @return        server properties
   * @throws IllegalStateException if the properties are invalid
   */
  public ServerProperties loadFromYaml(Reader reader) {
    LoadSettings loadSettings = LoadSettings.builder().build();
    Load load = new Load(loadSettings);
    Map<String, Object> m = (Map<String, Object>) load.loadFromReader(reader);

    ServerProperties.Builder b = ServerProperties.builder();
    b.root(getPath(m, "root", ServerProperties.DEFAULT_ROOT));
    b.host((String) m.get("host"));
    b.port(getInt(m, "port", ServerProperties.DEFAULT_PORT));
    b.controlPort(getInt(m, "controlPort",
                         ServerProperties.DEFAULT_CONTROL_PORT));
    b.shutdownTimeoutSec(getLong(m, "shutdownTimeoutSec",
                                 ServerProperties.DEFAULT_SHUTDOWN_TIMEOUT_SEC));
    b.numThreads(getInt(m, "numThreads",
                        ServerProperties.DEFAULT_NUM_THREADS));
    b.cgiDir(getPath(m, "cgiDir", ServerProperties.DEFAULT_CGI_DIR));
    b.maxLocalRedirects(getInt(m, "maxLocalRedirects",
                               ServerProperties.DEFAULT_MAX_LOCAL_REDIRECTS));
    b.forceCanonicalText(getBoolean(m, "forceCanonicalText",
                                    ServerProperties.DEFAULT_FORCE_CANONICAL_TEXT));
    b.textGeminiSuffixes(getStringList(m, "textGeminiSuffixes",
                                               ServerProperties.DEFAULT_TEXT_GEMINI_SUFFIXES));
    b.defaultContentType(getString(m, "defaultContentType",
                                   ServerProperties.DEFAULT_DEFAULT_CONTENT_TYPE));
    b.enableCharsetDetection(getBoolean(m, "enableCharsetDetection",
                                        ServerProperties.DEFAULT_ENABLE_CHARSET_DETECTION));
    b.defaultCharset(getString(m, "defaultCharset",
                               ServerProperties.DEFAULT_DEFAULT_CHARSET));
    b.favicon(getString(m, "favicon", ServerProperties.DEFAULT_FAVICON));
    b.feedPages(getStringList(m, "feedPages",
                              ServerProperties.DEFAULT_FEED_PAGES));
    b.logDir(getPath(m, "logDir", ServerProperties.DEFAULT_LOG_DIR));
    b.keystore(getPath(m, "keystore",
                       ServerProperties.DEFAULT_KEYSTORE));
    b.keystorePassword(getString(m, "keystorePassword",
                                 ServerProperties.DEFAULT_KEYSTORE_PASSWORD));
    b.setModSslCgiMetaVars(getBoolean(m, "setModSslCgiMetaVars",
                                      ServerProperties.DEFAULT_SET_MOD_SSL_CGI_META_VARS));

    try {
      b.secureDomains(buildSecureDomains(m));
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("Failed to build secure domain", e);
    }

    ServerProperties serverProps = b.build();
    serverProps.validate();
    return serverProps;
  }

  private String getString(Map<String, Object> m, String key,
                           String defaultValue) {
    if (!m.containsKey(key)) {
      return defaultValue;
    }
    return (String) m.get(key);
  }

  private List<String> getStringList(Map<String, Object> m, String key,
                                     List<String> defaultValue) {
    if (!m.containsKey(key)) {
      return defaultValue;
    }
    List<Object> value = (List<Object>) m.get(key);
    return value.stream()
        .map(String.class::cast)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  private Path getPath(Map<String, Object> m, String key, Path defaultValue) {
    if (!m.containsKey(key)) {
      return defaultValue;
    }
    return FileSystems.getDefault().getPath((String) m.get(key));
  }

  private int getInt(Map<String, Object> m, String key, int defaultValue) {
    if (!m.containsKey(key)) {
      return defaultValue;
    }
    return ((Integer) m.get(key)).intValue();
  }

  private long getLong(Map<String, Object> m, String key, long defaultValue) {
    if (!m.containsKey(key)) {
      return defaultValue;
    }
    Object value = m.get(key);
    if (value instanceof Integer) {
      return ((Integer) value).longValue();
    } else if (value instanceof Long) {
      return ((Long) value).longValue();
    } else {
      throw new IllegalStateException("Value for " + key + " out of range: " +
                                      value);
    }
  }

  private boolean getBoolean(Map<String, Object> m, String key, boolean defaultValue) {
    if (!m.containsKey(key)) {
      return defaultValue;
    }
    return ((Boolean) m.get(key)).booleanValue();
  }

  private List<SecureDomain> buildSecureDomains(Map<String, Object> m)
    throws GeneralSecurityException, IOException {
    List<SecureDomain> secureDomains = new ArrayList<>();
    if (!m.containsKey("secureDomains")) {
      return secureDomains;
    }
    Map<String, Object> sdm = (Map<String, Object>) m.get("secureDomains");
    for (String pathString : sdm.keySet()) {
      Path path = FileSystems.getDefault().getPath(pathString);
      Map<String, Object> secureDomainInfo =
          (Map<String, Object>) sdm.get(pathString);
      if (secureDomainInfo.containsKey("truststore")) {
        String truststoreString = (String) secureDomainInfo.get("truststore");
        Path truststore = FileSystems.getDefault().getPath(truststoreString);
        if (!secureDomainInfo.containsKey("truststorePassword")) {
          throw new IllegalStateException("Secure domain " + pathString +
                                          " specifies a truststore without " +
                                          "a password");
        }
        String truststorePassword =
          (String) secureDomainInfo.get("truststorePassword");
        secureDomains.add(new SecureDomain(path, truststore, truststorePassword));
      } else {
        secureDomains.add(new SecureDomain(path));
      }
    }
    return secureDomains;
  }
}
