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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Gemini server.
 */
public class Server {

  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private final ServerProperties serverProps;
  private final ExecutorService executorService;

  /**
   * Creates a new server.
   *
   * @param  serverProps server properties
   */
  public Server(ServerProperties serverProps) {
    this.serverProps = serverProps;
    executorService =
      Executors.newFixedThreadPool(serverProps.getNumThreads());
  }

  private ServerSocket controlSocket;
  private ServerSocket serverSocket;
  private AccessLogger accessLogger;

  /**
   * Starts the server in the calling thread. This method exits when the server
   * is shutting down.
   *
   * @throws IOException if the server fails to start
   * @throws GeneralSecurityException if there is a problem with establishing
   *                                  TLS connectivity
   */
  public void start() throws IOException, GeneralSecurityException {
    int controlPort = serverProps.getControlPort();
    if (controlPort >= 0) {
      controlSocket = ServerSocketFactory.getDefault()
        .createServerSocket(controlPort, 0, InetAddress.getLoopbackAddress());
    } else {
      LOG.debug("Control socket disabled");
      controlSocket = null;
    }

    SSLContext sslContext = buildSSLContext();
    serverSocket = sslContext.getServerSocketFactory()
      .createServerSocket(serverProps.getPort());

    accessLogger = new AccessLogger(serverProps.getLogDir());

    // Set some custom SSL parameters:
    // - require TLS 1.3 or 1.2
    // - require SNI with an exact match for the server's host
    SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
    sslParameters.setProtocols(new String[] { "TLSv1.3", "TLSv1.2" });
    String hostRegex = serverProps.getHost().replace(".", "\\.");
    SNIMatcher sniMatcher = SNIHostName.createSNIMatcher(hostRegex);
    sslParameters.setSNIMatchers(Collections.singletonList(sniMatcher));
    sslParameters.setWantClientAuth(true);
    ((SSLServerSocket) serverSocket).setSSLParameters(sslParameters);

    if (controlSocket != null) {
      new Thread(new ControlRunnable(controlSocket, this), "control").start();
    }

    LOG.info("Doppio {} started", Version.VERSION);
    LOG.info("Server listening on port {}", serverProps.getPort());
    LOG.info("Control listening on port {}", serverProps.getControlPort());
    try {
      // Accept connections and hand them off to request handlers until there
      // is a SocketException, which should indicate that the server is
      // shutting down.
      while (true) {
        LOG.debug("Accepting connection");
        SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
        executorService.submit(new RequestHandler(serverProps, accessLogger,
                                                  clientSocket));
      }
    } catch (SocketException e) {
      LOG.info("Server socket closed, shutting down");
      LOG.debug("Accept exception", e);
    } finally {
      executorService.shutdown();
      try {
        executorService.awaitTermination(serverProps.getShutdownTimeoutSec(),
                                         TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        LOG.debug("Interrupted while waiting for executor termination", e);
      }
      executorService.shutdownNow();

      try {
        accessLogger.close();
      } catch (IOException e) {
        LOG.warn("Failed to close access log", e);
      }
    }
  }

  private SSLContext buildSSLContext()
    throws IOException, GeneralSecurityException {
    SSLContext sslContext = SSLContext.getInstance("TLS");

    KeyStore keystore =
        KeyStore.getInstance(serverProps.getKeystore().toFile(),
                             serverProps.getKeystorePassword().toCharArray());
    KeyManagerFactory kmf =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keystore, serverProps.getKeystorePassword().toCharArray());

    // Secure domains each control client authentication.
    TrustManager[] trustManagers = new TrustManager[] {
      new AllowAllTrustManager()
    };

    sslContext.init(kmf.getKeyManagers(), trustManagers, null);
    return sslContext;
  }

  /**
   * Shuts down the server.
   */
  public void shutdown() {
    // You can't interrupt a server socket blocked on accept; you have
    // to just close it.
    try {
      serverSocket.close();
    } catch (IOException e) {
      LOG.debug("Failed to close server socket", e);
    }
  }
}
