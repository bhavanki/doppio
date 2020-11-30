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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private final ServerProperties serverProps;
  private final ExecutorService executorService;

  public Server(ServerProperties serverProps) {
    this.serverProps = serverProps;
    executorService =
      Executors.newFixedThreadPool(serverProps.getNumThreads());
  }

  private ServerSocket serverSocket;
  private AccessLogger accessLogger;

  public void start() throws Exception {
    serverSocket = SSLServerSocketFactory.getDefault()
      .createServerSocket(serverProps.getPort());
    accessLogger = new AccessLogger(serverProps.getLogDir());

    SSLParameters sslParameters =
        SSLContext.getDefault().getDefaultSSLParameters();
    sslParameters.setProtocols(new String[] { "TLSv1.3", "TLSv1.2" });
    String hostRegex = serverProps.getHost().replace(".", "\\.");
    SNIMatcher sniMatcher = SNIHostName.createSNIMatcher(hostRegex);
    sslParameters.setSNIMatchers(Collections.singletonList(sniMatcher));
    ((SSLServerSocket) serverSocket).setSSLParameters(sslParameters);

    try {
      while (true) {
        LOG.debug("Accepting connection");
        Socket clientSocket = serverSocket.accept();
        executorService.submit(new RequestHandler(serverProps, accessLogger,
                                                  clientSocket));
      }
    } catch (SocketException e) {
      LOG.error("Exception while accepting new connection", e);
    } finally {
      try {
        accessLogger.close();
      } catch (IOException e) {
        LOG.warn("Failed to close access log", e);
      }
    }
  }

  public void shutdown() {
    executorService.shutdown();
  }
}
