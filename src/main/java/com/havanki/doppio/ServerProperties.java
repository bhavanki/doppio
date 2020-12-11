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
import java.util.Properties;

/**
 * Server configuration properties.
 */
public class ServerProperties {

  private static final Path DEFAULT_ROOT = Path.of("/var/gemini");
  private static final int DEFAULT_PORT = 1965;
  private static final int DEFAULT_NUM_THREADS = 4;
  private static final Path DEFAULT_CGI_DIR = null;
  private static final Path DEFAULT_LOG_DIR = null;

  private final Path root;
  private final String host;
  private final int port;
  private final int numThreads;
  private final Path cgiDir;
  private final Path logDir;

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
    logDir = getPathProperty(props, "logDir", DEFAULT_LOG_DIR);
  }

  private final Path getPathProperty(Properties props, String key,
                                     Path defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return FileSystems.getDefault().getPath(props.getProperty(key));
  }

  private final int getIntProperty(Properties props, String key,
                                   int defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Integer.parseInt(props.getProperty(key));
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
}
