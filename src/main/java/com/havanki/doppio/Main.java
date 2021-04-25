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

import java.io.FileReader;
import java.util.Properties;

/**
 * The main class for Doppio. The single argument is the path to the server
 * properties file.
 */
public class Main {

  /**
   * Entry point for the server.
   *
   * @param  args      command-line arguments: path to server properties file
   * @throws Exception if the server properties file could not be read, or if
   *                   the server fails to start
   */
  public static void main(String[] args) throws Exception {
    String serverPropsFile = args[0];
    ServerProperties serverProps;
    try (FileReader r = new FileReader(serverPropsFile)) {
      if (serverPropsFile.endsWith(".yaml")) {
        serverProps = new ServerProperties(r);
      } else {
        Properties props = new Properties();
        props.load(r);
        serverProps = new ServerProperties(props);
      }
    }
    Server server = new Server(serverProps);
    server.start();
  }
}
