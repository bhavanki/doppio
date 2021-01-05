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
import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for {@code ProcessBuilder} objects that can run CGI scripts.
 */
public class CgiProcessBuilderFactory {

  private static final Logger LOG =
    LoggerFactory.getLogger(CgiProcessBuilderFactory.class);

  private static final String AUTH_TYPE = "Certificate";   // whatever
  private static final String GATEWAY_INTERFACE = "CGI/1.1";
  private static final String REQUEST_METHOD = "";         // must be set
  private static final String SERVER_PROTOCOL = "GEMINI";  // probably right
  private static final String SERVER_SOFTWARE_PREFIX = "Doppio";

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME;

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
                                                URI uri, SSLSocket socket, X509Certificate peerCert,
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
    pbenv.put("GEMINI_URL", uri.toString());     // note that this is normalized
    pbenv.put("GEMINI_URL_PATH", uri.getPath()); // note that this is decoded
    if (uri.getQuery() != null) {
      pbenv.put("QUERY_STRING", uri.getQuery());
    }

    InetSocketAddress remoteSocketAddress =
      (InetSocketAddress) socket.getRemoteSocketAddress();
    if (remoteSocketAddress != null) {
      pbenv.put("REMOTE_ADDR", remoteSocketAddress.getAddress().getHostAddress());
      pbenv.put("REMOTE_HOST", remoteSocketAddress.getHostString());
    }

    // Basic TLS variables
    pbenv.put("TLS_CIPHER", socket.getSession().getCipherSuite());
    pbenv.put("TLS_VERSION", socket.getSession().getProtocol());
    pbenv.put("TLS_SESSION_ID", byteArrayToHexString(socket.getSession().getId()));

    // Apache mod_ssl variables
    if (serverProps.isSetModSslCgiMetaVars()) {
      pbenv.put("SSL_CIPHER", socket.getSession().getCipherSuite());
      pbenv.put("SSL_PROTOCOL", socket.getSession().getProtocol());
      pbenv.put("SSL_SESSION_ID", byteArrayToHexString(socket.getSession().getId()));
    }

    if (peerCert != null) {
      pbenv.put("AUTH_TYPE", AUTH_TYPE);
      pbenv.put("REMOTE_USER", peerCert.getSubjectX500Principal().getName());
      String fingerprint = fingerprint(peerCert);
      if (fingerprint != null) {
        pbenv.put("TLS_CLIENT_HASH", fingerprint(peerCert));
      }
      pbenv.put("TLS_CLIENT_ISSUER", peerCert.getIssuerX500Principal().getName());
      OffsetDateTime notBefore = peerCert.getNotBefore().toInstant().atOffset(ZoneOffset.UTC);
      OffsetDateTime notAfter = peerCert.getNotAfter().toInstant().atOffset(ZoneOffset.UTC);
      String remain = Long.toString(OffsetDateTime.now(ZoneOffset.UTC)
                                    .until(notAfter, ChronoUnit.DAYS));
      pbenv.put("TLS_CLIENT_NOT_BEFORE", TIMESTAMP_FORMATTER.format(notBefore));
      pbenv.put("TLS_CLIENT_NOT_AFTER", TIMESTAMP_FORMATTER.format(notAfter));
      pbenv.put("TLS_CLIENT_REMAIN", remain);
      pbenv.put("TLS_CLIENT_SERIAL", peerCert.getSerialNumber().toString());
      pbenv.put("TLS_CLIENT_SUBJECT", peerCert.getSubjectX500Principal().getName());
      pbenv.put("TLS_CLIENT_VERSION", Integer.toString(peerCert.getVersion()));

      // More Apache mod_ssl variables
      if (serverProps.isSetModSslCgiMetaVars()) {
        pbenv.put("SSL_CLIENT_I_DN", peerCert.getIssuerX500Principal().getName());
        pbenv.put("SSL_CLIENT_M_SERIAL", peerCert.getSerialNumber().toString());
        pbenv.put("SSL_CLIENT_M_VERSION", Integer.toString(peerCert.getVersion()));
        pbenv.put("SSL_CLIENT_S_DN", peerCert.getSubjectX500Principal().getName());
        pbenv.put("SSL_CLIENT_V_START", TIMESTAMP_FORMATTER.format(notBefore));
        pbenv.put("SSL_CLIENT_V_END", TIMESTAMP_FORMATTER.format(notAfter));
        pbenv.put("SSL_CLIENT_V_REMAIN", remain);
      }
    }

    pbenv.put("REQUEST_METHOD", REQUEST_METHOD);
    pbenv.put("SCRIPT_NAME", "/" + serverProps.getRoot().relativize(splitPaths[0]).toString());
    pbenv.put("SERVER_NAME", serverProps.getHost());
    pbenv.put("SERVER_PORT", Integer.toString(serverProps.getPort()));
    pbenv.put("SERVER_PROTOCOL", SERVER_PROTOCOL);
    pbenv.put("SERVER_SOFTWARE",
              String.format("%s %s", SERVER_SOFTWARE_PREFIX, Version.VERSION));

    return pb;
  }

  static String byteArrayToHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  static String fingerprint(X509Certificate cert) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      return byteArrayToHexString(sha256.digest(cert.getEncoded()));
    } catch (NoSuchAlgorithmException e) {
      // Shouldn't happen but ...
      LOG.warn("This JDK does not support SHA-256, cannot fingerprint cert", e);
      return null;
    } catch (CertificateEncodingException e) {
      LOG.warn("Failed to encode cert, cannot fingerprint", e);
      return null;
    }
  }
}
