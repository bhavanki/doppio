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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * A loader for server properties that reads from Java properties.
 */
public class ServerPropertiesPropertiesLoader {

  /**
   * Loads server properties from Java properties.
   *
   * @param  props Java properties
   * @return       server properties
   * @throws IllegalStateException if the properties are invalid
   */
  public ServerProperties loadFromProperties(Properties props) {
    ServerProperties.Builder b = ServerProperties.builder();
    b.root(getPathProperty(props, "root", ServerProperties.DEFAULT_ROOT));
    b.host(props.getProperty("host"));
    b.port(getIntProperty(props, "port", ServerProperties.DEFAULT_PORT));
    b.controlPort(getIntProperty(props, "controlPort",
                                 ServerProperties.DEFAULT_CONTROL_PORT));
    b.shutdownTimeoutSec(getLongProperty(props, "shutdownTimeoutSec",
                                         ServerProperties.DEFAULT_SHUTDOWN_TIMEOUT_SEC));
    b.numThreads(getIntProperty(props, "numThreads",
                                ServerProperties.DEFAULT_NUM_THREADS));
    b.cgiDir(getPathProperty(props, "cgiDir", ServerProperties.DEFAULT_CGI_DIR));
    b.maxLocalRedirects(getIntProperty(props, "maxLocalRedirects",
                                       ServerProperties.DEFAULT_MAX_LOCAL_REDIRECTS));
    b.forceCanonicalText(getBooleanProperty(props, "forceCanonicalText",
                                            ServerProperties.DEFAULT_FORCE_CANONICAL_TEXT));
    b.textGeminiSuffixes(getStringListProperty(props, "textGeminiSuffixes",
                                               ServerProperties.DEFAULT_TEXT_GEMINI_SUFFIXES));
    b.defaultContentType(props.getProperty("defaultContentType",
                                           ServerProperties.DEFAULT_DEFAULT_CONTENT_TYPE));
    b.enableCharsetDetection(getBooleanProperty(props, "enableCharsetDetection",
                                                ServerProperties.DEFAULT_ENABLE_CHARSET_DETECTION));
    b.defaultCharset(props.getProperty("defaultCharset",
                                       ServerProperties.DEFAULT_DEFAULT_CHARSET));
    b.favicon(props.getProperty("favicon", ServerProperties.DEFAULT_FAVICON));
    // Counting emoji in Java just ain't reliable
    // https://lemire.me/blog/2018/06/15/emojis-java-and-strings/
    // if (favicon != null && favicon.codePointCount(0, favicon.length()) != 1) {
    //   throw new IllegalStateException("Favicon must be exactly one character, " +
    //                                   "found " + favicon.codePointCount(0, favicon.length()));
    // }
    b.feedPages(getStringListProperty(props, "feedPages",
                                      ServerProperties.DEFAULT_FEED_PAGES));
    b.logDir(getPathProperty(props, "logDir", ServerProperties.DEFAULT_LOG_DIR));
    b.keystore(getPathProperty(props, "keystore",
                               ServerProperties.DEFAULT_KEYSTORE));
    b.keystorePassword(props.getProperty("keystorePassword",
                                         ServerProperties.DEFAULT_KEYSTORE_PASSWORD));
    b.setModSslCgiMetaVars(getBooleanProperty(props, "setModSslCgiMetaVars",
                                              ServerProperties.DEFAULT_SET_MOD_SSL_CGI_META_VARS));

    try {
      b.secureDomains(buildSecureDomains(props));
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("Failed to build secure domain", e);
    }

    ServerProperties serverProps = b.build();
    serverProps.validate();
    return serverProps;
  }

  private List<String> getStringListProperty(Properties props, String key,
                                             List<String> defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Arrays.stream(props.getProperty(key).split(","))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  private Path getPathProperty(Properties props, String key, Path defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return FileSystems.getDefault().getPath(props.getProperty(key));
  }

  private int getIntProperty(Properties props, String key, int defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Integer.parseInt(props.getProperty(key));
  }

  private long getLongProperty(Properties props, String key, long defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Long.parseLong(props.getProperty(key));
  }

  private boolean getBooleanProperty(Properties props, String key,
                                     boolean defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Boolean.parseBoolean(props.getProperty(key));
  }

  private List<SecureDomain> buildSecureDomains(Properties props)
    throws GeneralSecurityException, IOException {
    List<SecureDomain> secureDomains = new ArrayList<>();
    for (String key : props.stringPropertyNames()) {
      if (!key.startsWith("secureDomain.")) {
        continue;
      }
      String[] domainValues = props.getProperty(key).split(":", 3);
      Path path = FileSystems.getDefault().getPath(domainValues[0]);
      if (domainValues.length > 1) {
        Path truststore = FileSystems.getDefault().getPath(domainValues[1]);
        if (domainValues.length < 3) {
          throw new IllegalStateException("Value for secure domain " + key +
                                          " specifies a truststore without " +
                                          "a password");
        }
        secureDomains.add(new SecureDomain(path, truststore, domainValues[2]));
      } else {
        secureDomains.add(new SecureDomain(path));
      }
    }
    return secureDomains;
  }

}
