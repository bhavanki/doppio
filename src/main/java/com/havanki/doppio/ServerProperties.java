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

public class ServerProperties {

  private static final int DEFAULT_PORT = 1965;
  private static final int DEFAULT_NUM_THREADS = 4;

  private final Path root;
  private final String host;
  private final int port;
  private final int numThreads;

  public ServerProperties(Properties props) {
    root = FileSystems.getDefault().getPath(props.getProperty("root"));
    host = props.getProperty("host");
    port = getIntProperty(props, "port", DEFAULT_PORT);
    numThreads = getIntProperty(props, "numThreads", DEFAULT_NUM_THREADS);
  }

  private final int getIntProperty(Properties props, String key,
                                   int defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Integer.parseInt(props.getProperty(key));
  }

  public Path getRoot() {
    return root;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public int getNumThreads() {
    return numThreads;
  }
}
