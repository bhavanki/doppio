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

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A manager for the server access log. The log follows the Apache httpd common
 * log format.
 */
public class AccessLogger implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(AccessLogger.class);

  private final FileWriter accessLogWriter;
  private boolean closed;

  /**
   * Creates a new logger. If the log directory is null, this logger does not
   * log anything.
   *
   * @param  logDir      directory where access log is written
   * @throws IOException if the access log cannot be opened
   */
  public AccessLogger(Path logDir) throws IOException {
    if (logDir == null) {
      accessLogWriter = null;
    } else {
      File accessLogFile = logDir.resolve("access.log").toFile();
      accessLogWriter = new FileWriter(accessLogFile,
        StandardCharsets.UTF_8, accessLogFile.exists());
    }
    closed = false;
  }

  /**
   * Closes the access log.
   *
   * @throws IOException if the log cannot be closed
   */
  public synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    if (accessLogWriter != null) {
      accessLogWriter.close();
    }
    closed = true;
  }

  private static final String ACCESS_LOG_FORMAT =
    "%s - %s [%s] \"%s\" %d %s\r\n";
  static final DateTimeFormatter ACCESS_LOG_DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
      .withZone(ZoneId.systemDefault());

  /**
   * Logs a successful access.
   *
   * @param socket           request socket
   * @param remoteUsername   remote username, if authenticated
   * @param request          request text
   * @param statusCode       response status code
   * @param responseBodySize the size of the response body, in bytes
   */
  public void log(Socket socket, String remoteUsername, String request,
                  int statusCode, long responseBodySize) {
    log(socket, remoteUsername, request, statusCode, responseBodySize,
        Instant.now());
  }

  synchronized void log(Socket socket, String remoteUsername, String request,
                        int statusCode, long responseBodySize, Instant timestamp) {
    if (closed) {
      throw new IllegalStateException("Logger is closed");
    }
    if (accessLogWriter == null) {
      return;
    }

    String remoteAddress;
    InetSocketAddress remoteSocketAddress =
      (InetSocketAddress) socket.getRemoteSocketAddress();
    if (remoteSocketAddress != null) {
      remoteAddress = remoteSocketAddress.getHostString();
    } else {
      remoteAddress = "-";
    }
    if (remoteUsername == null) {
      remoteUsername = "-";
    } else {
      remoteUsername = URLEncoder.encode(remoteUsername, StandardCharsets.UTF_8);
    }
    String timestampStr = ACCESS_LOG_DATE_TIME_FORMATTER.format(timestamp);
    String responseBodySizeStr = responseBodySize > 0L ?
      Long.toString(responseBodySize) : "-";

    String line = String.format(ACCESS_LOG_FORMAT, remoteAddress,
                                remoteUsername, timestampStr, request,
                                statusCode, responseBodySizeStr);

    try {
      accessLogWriter.write(line);
      accessLogWriter.flush();
    } catch (IOException e) {
      LOG.warn("Failed to write to access log", e);
    }
  }
}
