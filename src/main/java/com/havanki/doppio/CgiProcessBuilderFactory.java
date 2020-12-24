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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * A factory for {@code ProcessBuilder} objects that can run CGI scripts.
 */
public class CgiProcessBuilderFactory {

  private static final String AUTH_TYPE = "Certificate";   // whatever
  private static final String GATEWAY_INTERFACE = "CGI/1.1";
  private static final String SERVER_PROTOCOL = "GEMINI";  // probably right
  private static final String SERVER_SOFTWARE_PREFIX = "Doppio";

  /**
   * Creates a {@code ProcessBuilder} for a CGI script. This includes setting
   * expected environment variables.
   *
   * @param  resourceFile script file
   * @param  splitPaths   full path to script and extra path information
   * @param  uri          original request URI
   * @param  socket       client socket
   * @param  peerCert     principal identifying peer, if any
   * @param  serverProps  server properties
   * @return              process builder
   * @throws IOException  if the canonical path for the script file cannot be
   *                      determined
   */
  public ProcessBuilder createCgiProcessBuilder(File resourceFile, Path[] splitPaths,
                                                URI uri, Socket socket, X509Certificate peerCert,
                                                ServerProperties serverProps)
    throws IOException {
    // Run the resource file as the command. Combine standard output and
    // standard error so they are fed back together in the server response.
    String command = resourceFile.getCanonicalPath();
    ProcessBuilder pb = new ProcessBuilder()
      .command(command)
      .directory(resourceFile.getParentFile())
      .redirectErrorStream(true);

    // Set CGI environment variables.
    Map<String, String> pbenv = pb.environment();
    pbenv.put("GATEWAY_INTERFACE", GATEWAY_INTERFACE);
    String extraPath = splitPaths[1].toString();
    if (!extraPath.isEmpty()) {
      pbenv.put("PATH_INFO", "/" + extraPath);
      pbenv.put("PATH_TRANSLATED",
                serverProps.getRoot().resolve(extraPath).toString());
    }
    if (uri.getQuery() != null) {
      pbenv.put("QUERY_STRING", uri.getQuery());
    }

    InetSocketAddress remoteSocketAddress =
      (InetSocketAddress) socket.getRemoteSocketAddress();
    if (remoteSocketAddress != null) {
      pbenv.put("REMOTE_ADDR", remoteSocketAddress.getAddress().getHostAddress());
      pbenv.put("REMOTE_HOST", remoteSocketAddress.getHostString());
    }

    if (peerCert != null) {
      pbenv.put("AUTH_TYPE", AUTH_TYPE);
      pbenv.put("REMOTE_USER", peerCert.getSubjectX500Principal().getName());
      // Apache mod_ssl variables
      pbenv.put("SSL_CLIENT_I_DN", peerCert.getIssuerX500Principal().getName());
      pbenv.put("SSL_CLIENT_S_DN", peerCert.getSubjectX500Principal().getName());
    }

    // REQUEST_METHOD is not applicable to Gemini
    pbenv.put("SCRIPT_NAME", "/" + serverProps.getRoot().relativize(splitPaths[0]).toString());
    pbenv.put("SERVER_NAME", serverProps.getHost());
    pbenv.put("SERVER_PORT", Integer.toString(serverProps.getPort()));
    pbenv.put("SERVER_PROTOCOL", SERVER_PROTOCOL);
    pbenv.put("SERVER_SOFTWARE",
              String.format("%s %s", SERVER_SOFTWARE_PREFIX, Version.VERSION));

    return pb;
  }
}
