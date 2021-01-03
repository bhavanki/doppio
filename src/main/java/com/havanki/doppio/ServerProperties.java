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

import java.nio.file.FileSystems;
import java.nio.file.Path;
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
  private static final int DEFAULT_NUM_THREADS = 4;
  private static final Path DEFAULT_CGI_DIR = null;
  private static final int DEFAULT_MAX_LOCAL_REDIRECTS = 10;
  private static final boolean DEFAULT_FORCE_CANONICAL_TEXT = false;
  private static final Path DEFAULT_LOG_DIR = null;
  private static final List<Path> DEFAULT_SECURE_DIRS = List.of();
  private static final Path DEFAULT_KEYSTORE = Path.of("/etc/doppio/keystore.jks");
  private static final String DEFAULT_KEYSTORE_PASSWORD = "doppio";
  private static final Path DEFAULT_TRUSTSTORE = null;
  private static final String DEFAULT_TRUSTSTORE_PASSWORD = null;
  private static final boolean DEFAULT_SET_MOD_SSL_CGI_META_VARS = false;

  private final Path root;
  private final String host;
  private final int port;
  private final int numThreads;
  private final Path cgiDir;
  private final int maxLocalRedirects;
  private final boolean forceCanonicalText;
  private final Path logDir;
  private final List<Path> secureDirs;
  private final Path keystore;
  private final String keystorePassword;
  private final Path truststore;
  private final String truststorePassword;
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
    numThreads = getIntProperty(props, "numThreads", DEFAULT_NUM_THREADS);
    cgiDir = getPathProperty(props, "cgiDir", DEFAULT_CGI_DIR);
    maxLocalRedirects = getIntProperty(props, "maxLocalRedirects",
                                       DEFAULT_MAX_LOCAL_REDIRECTS);
    forceCanonicalText = getBooleanProperty(props, "forceCanonicalText",
                                            DEFAULT_FORCE_CANONICAL_TEXT);
    logDir = getPathProperty(props, "logDir", DEFAULT_LOG_DIR);
    secureDirs = getPathsProperty(props, "secureDirs", DEFAULT_SECURE_DIRS);
    keystore = getPathProperty(props, "keystore", DEFAULT_KEYSTORE);
    keystorePassword = props.getProperty("keystorePassword",
                                         DEFAULT_KEYSTORE_PASSWORD);
    truststore = getPathProperty(props, "truststore", DEFAULT_TRUSTSTORE);
    truststorePassword = props.getProperty("truststorePassword",
                                         DEFAULT_TRUSTSTORE_PASSWORD);
    setModSslCgiMetaVars = getBooleanProperty(props, "setModSslCgiMetaVars",
                                              DEFAULT_SET_MOD_SSL_CGI_META_VARS);

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

  private Path getPathProperty(Properties props, String key, Path defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return FileSystems.getDefault().getPath(props.getProperty(key));
  }

  private List<Path> getPathsProperty(Properties props, String key,
                                      List<Path> defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Arrays.stream(props.getProperty(key).split(":"))
        .filter(s -> !s.isEmpty())
        .map(p -> FileSystems.getDefault().getPath(p))
        .collect(Collectors.toList());
  }

  private int getIntProperty(Properties props, String key, int defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Integer.parseInt(props.getProperty(key));
  }

  private boolean getBooleanProperty(Properties props, String key,
                                     boolean defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Boolean.parseBoolean(props.getProperty(key));
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
   * Gets the secure directories for the server.
   *
   * @return secure directories
   */
  public List<Path> getSecureDirs() {
    return secureDirs;
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
   * Gets the truststore containing trusted certificates.
   *
   * @return truststore
   */
  public Path getTruststore() {
    return truststore;
  }

  /**
   * Gets the password for the truststore.
   *
   * @return truststore password
   */
  public String getTruststorePassword() {
    return truststorePassword;
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
