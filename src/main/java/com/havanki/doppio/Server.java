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

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private static final int DEFAULT_PORT = 1965;
  private static final int DEFAULT_NUM_THREADS = 4;

  private final Path root;
  private final int port;
  private final ExecutorService executorService;

  public Server(Properties props) {
    root = FileSystems.getDefault().getPath(props.getProperty("root"));
    port = getIntProperty(props, "port", DEFAULT_PORT);
    int numThreads = getIntProperty(props, "numThreads", DEFAULT_NUM_THREADS);
    executorService = Executors.newFixedThreadPool(numThreads);
  }

  private final int getIntProperty(Properties props, String key,
                                   int defaultValue) {
    if (!props.containsKey(key)) {
      return defaultValue;
    }
    return Integer.parseInt(props.getProperty(key));
  }

  private ServerSocket serverSocket;

  public void start() throws Exception {
    serverSocket = SSLServerSocketFactory.getDefault()
      .createServerSocket(port);

    SSLParameters sslParameters =
        SSLContext.getDefault().getDefaultSSLParameters();
    sslParameters.setProtocols(new String[] { "TLSv1.3", "TLSv1.2" });
    ((SSLServerSocket) serverSocket).setSSLParameters(sslParameters);

    try {
      while (true) {
        LOG.debug("Accepting connection");
        Socket clientSocket = serverSocket.accept();
        executorService.submit(new RequestHandler(clientSocket));
      }
    } catch (SocketException e) {
      LOG.error("Exception while accepting new connection", e);
    }
  }

  public void shutdown() {
    executorService.shutdown();
  }
}
