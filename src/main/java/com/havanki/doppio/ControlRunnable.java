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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A runnable that manages control socket connections. Connection processing
 * is single-threaded. Each client is expected to send a single line command.
 */
public class ControlRunnable implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(ControlRunnable.class);

  /**
   * Command to shutdown the server.
   */
  public static final String COMMAND_SHUTDOWN = "shutdown";

  private final ServerSocket controlSocket;
  private final ServerSocket serverSocket;

  /**
   * Creates a new runnable.
   *
   * @param  controlSocket socket for control messages
   * @param  serverSocket  server socket
   */
  public ControlRunnable(ServerSocket controlSocket, ServerSocket serverSocket) {
    this.controlSocket = controlSocket;
    this.serverSocket = serverSocket;
  }

  @Override
  public void run() {
    boolean shutdown = false;
    while (!shutdown) {
      LOG.debug("Accepting control connection");
      Socket clientSocket;
      try {
        clientSocket = controlSocket.accept();
      } catch (IOException e) {
        // For now, just bail on the control socket completely
        LOG.error("Failed to accept from control socket, disabling", e);
        return;
      }
      try (InputStream in = clientSocket.getInputStream();
           InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
           BufferedReader br = new BufferedReader(isr)) {
        String command = br.readLine();
        switch (command) {
          case COMMAND_SHUTDOWN:
            LOG.info("Received shutdown command");
            shutdown = true;
            // You can't interrupt a server socket blocked on accept; you have
            // to just close it.
            try {
              serverSocket.close();
            } catch (IOException e) {
              LOG.debug("Failed to close server socket", e);
            }
            break;
          default:
            LOG.error("Unknown control command {}", command);
        }
      } catch (IOException e) {
        LOG.error("Failed to read command from control socket", e);
      } finally {
        try {
          clientSocket.close();
        } catch (IOException e) {
          LOG.debug("Failed to close client socket", e);
        }
      }
    }
  }
}
