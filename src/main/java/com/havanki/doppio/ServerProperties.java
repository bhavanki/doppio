/**
 * Copyright (C) 2020 Bill Havanki
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
 * Server configuration properties.
 */
public class ServerProperties {

  private static final Path DEFAULT_ROOT = Path.of("/var/gemini");
  private static final int DEFAULT_PORT = 1965;
  private static final int DEFAULT_CONTROL_PORT = 31965;
  private static final long DEFAULT_SHUTDOWN_TIMEOUT_SEC = 5;
  private static final int DEFAULT_NUM_THREADS = 4;
  private static final Path DEFAULT_CGI_DIR = null;
  private static final int DEFAULT_MAX_LOCAL_REDIRECTS = 10;
  private static final List<String> DEFAULT_TEXT_GEMINI_SUFFIXES =
    List.of(".gmi", ".gemini");
  private static final String DEFAULT_DEFAULT_CONTENT_TYPE = "text/plain";
  private static final boolean DEFAULT_ENABLE_CHARSET_DETECTION = false;
  private static final String DEFAULT_DEFAULT_CHARSET = null;
  private static final String DEFAULT_FAVICON = null;
  private static final List<String> DEFAULT_FEED_PAGES = List.of();
  private static final boolean DEFAULT_FORCE_CANONICAL_TEXT = false;
  private static final Path DEFAULT_LOG_DIR = null;
  private static final Path DEFAULT_KEYSTORE = Path.of("/etc/doppio/keystore.jks");
  private static final String DEFAULT_KEYSTORE_PASSWORD = "doppio";
  private static final boolean DEFAULT_SET_MOD_SSL_CGI_META_VARS = false;

  private final Path root;
  private final String host;
  private final int port;
  private final int controlPort;
  private final long shutdownTimeoutSec;
  private final int numThreads;
  private final Path cgiDir;
  private final int maxLocalRedirects;
  private final boolean forceCanonicalText;
  private final List<String> textGeminiSuffixes;
  private final String defaultContentType;
  private final boolean enableCharsetDetection;
  private final String defaultCharset;
  private final String favicon;
  private final List<String> feedPages;
  private final Path logDir;
  private final List<SecureDomain> secureDomains;
  private final Path keystore;
  private final String keystorePassword;
  private final boolean setModSslCgiMetaVars;

  /**
   * Creates a new set of server properties from Java properties.
   *
   * @param  props Java properties
   */
  public ServerProperties(Properties props) {
    root = getPathProperty(props, "root", DEFAULT_ROOT);
    host = props.getProperty("host");
    port = getIntProperty(props, "port", DEFAULT_PORT);
    controlPort = getIntProperty(props, "controlPort", DEFAULT_CONTROL_PORT);
    shutdownTimeoutSec = getLongProperty(props, "shutdownTimeoutSec",
                                         DEFAULT_SHUTDOWN_TIMEOUT_SEC);
    numThreads = getIntProperty(props, "numThreads", DEFAULT_NUM_THREADS);
    cgiDir = getPathProperty(props, "cgiDir", DEFAULT_CGI_DIR);
    maxLocalRedirects = getIntProperty(props, "maxLocalRedirects",
                                       DEFAULT_MAX_LOCAL_REDIRECTS);
    forceCanonicalText = getBooleanProperty(props, "forceCanonicalText",
                                            DEFAULT_FORCE_CANONICAL_TEXT);
    textGeminiSuffixes = getStringListProperty(props, "textGeminiSuffixes",
                                               DEFAULT_TEXT_GEMINI_SUFFIXES);
    defaultContentType = props.getProperty("defaultContentType",
                                           DEFAULT_DEFAULT_CONTENT_TYPE);
    enableCharsetDetection = getBooleanProperty(props, "enableCharsetDetection",
                                                DEFAULT_ENABLE_CHARSET_DETECTION);
    defaultCharset = props.getProperty("defaultCharset",
                                       DEFAULT_DEFAULT_CHARSET);
    favicon = props.getProperty("favicon", DEFAULT_FAVICON);
    // Counting emoji in Java just ain't reliable
    // https://lemire.me/blog/2018/06/15/emojis-java-and-strings/
    // if (favicon != null && favicon.codePointCount(0, favicon.length()) != 1) {
    //   throw new IllegalStateException("Favicon must be exactly one character, " +
    //                                   "found " + favicon.codePointCount(0, favicon.length()));
    // }
    feedPages = getStringListProperty(props, "feedPages", DEFAULT_FEED_PAGES);
    logDir = getPathProperty(props, "logDir", DEFAULT_LOG_DIR);
    keystore = getPathProperty(props, "keystore", DEFAULT_KEYSTORE);
    keystorePassword = props.getProperty("keystorePassword",
                                         DEFAULT_KEYSTORE_PASSWORD);
    setModSslCgiMetaVars = getBooleanProperty(props, "setModSslCgiMetaVars",
                                              DEFAULT_SET_MOD_SSL_CGI_META_VARS);

    try {
      secureDomains = buildSecureDomains(props);
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("Failed to build secure domain", e);
    }

    if (port < 1 || port > 65535) {
      throw new IllegalStateException("port must be between 1 and 65535");
    }
    if (numThreads < 1) {
      throw new IllegalStateException("numThreads must be positive");
    }
    if (maxLocalRedirects < 0) {
      throw new IllegalStateException("maxLocalRedirects must be non-negative");
    }
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

  /**
   * Gets the root (directory) for the server.
   *
   * @return root
   */
  public Path getRoot() {
    return root;
  }

  /**
   * Gets the host for the server.
   *
   * @return host
   */
  public String getHost() {
    return host;
  }

  /**
   * Gets the listening port for the server.
   *
   * @return port
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the control port for the server.
   *
   * @return control port
   */
  public int getControlPort() {
    return controlPort;
  }

  /**
   * Gets the shutdown timeout for the server, in seconds.
   *
   * @return shutdown timeout
   */
  public long getShutdownTimeoutSec() {
    return shutdownTimeoutSec;
  }

  /**
   * Gets the number of server threads handling requests.
   *
   * @return number of server threads
   */
  public int getNumThreads() {
    return numThreads;
  }

  /**
   * Gets the maximum number of CGI local redirects permitted by the server.
   *
   * @return maximum number of CGI local redirects
   */
  public int getMaxLocalRedirects() {
    return maxLocalRedirects;
  }

  /**
   * Gets whether text response bodies are forced to use canonical (DOS) line
   * endings.
   *
   * @return whether canonical text is returned in responses
   */
  public boolean isForceCanonicalText() {
    return forceCanonicalText;
  }

  /**
   * Get the suffixes for resources that have content type text/gemini.
   *
   * @return suffixes for text/gemini resources
   */
  public List<String> getTextGeminiSuffixes() {
    return textGeminiSuffixes;
  }

  /**
   * Gets the default content type for resources.
   *
   * @return resource default content type
   */
  public String getDefaultContentType() {
    return defaultContentType;
  }

  /**
   * Gets whether charset detection of text resources is enabled.
   *
   * @return whether charset detection of text resources is enabled
   */
  public boolean isEnableCharsetDetection() {
    return enableCharsetDetection;
  }

  /**
   * Gets the default charset for text resources.
   *
   * @return text resource default charset
   */
  public String getDefaultCharset() {
    return defaultCharset;
  }

  /**
   * Gets the site favicon.
   *
   * @return favicon
   */
  public String getFavicon() {
    return favicon;
  }

  /**
   * Gets the paths to the feed pages.
   *
   * @return feed pages
   */
  public List<String> getFeedPages() {
    return feedPages;
  }

  /**
   * Gets the CGI directory for the server.
   *
   * @return CGI directory
   */
  public Path getCgiDir() {
    return cgiDir;
  }

  /**
   * Gets the log directory for the server.
   *
   * @return log directory
   */
  public Path getLogDir() {
    return logDir;
  }

  /**
   * Gets the secure domains for the server.
   *
   * @return secure domains
   */
  public List<SecureDomain> getSecureDomains() {
    return secureDomains;
  }

  /**
   * Gets the keystore containing the server's private key.
   *
   * @return keystore
   */
  public Path getKeystore() {
    return keystore;
  }

  /**
   * Gets the password for the keystore.
   *
   * @return keystore password
   */
  public String getKeystorePassword() {
    return keystorePassword;
  }

  /**
   * Gets whether CGI meta-variables defined by Apache mod_ssl should be set
   * when executing a CGI script.
   *
   * @return whether to set mod_ssl CGI meta-variables
   */
  public boolean isSetModSslCgiMetaVars() {
    return setModSslCgiMetaVars;
  }
}
