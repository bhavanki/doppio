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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLogger implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(AccessLogger.class);

  private final FileWriter accessLogWriter;
  private boolean closed;

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
    "%s - %s %s \"%s\" %d %d\r\n";
  private static final DateTimeFormatter ACCESS_LOG_DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern("(dd/MMM/yyyy:HH:mm:ss Z)")
      .withZone(ZoneId.systemDefault());

  public void logError(Socket socket, String request, int statusCode) {
    log(socket, request, statusCode, 0);
  }

  public synchronized void log(Socket socket, String request, int statusCode,
                               long responseBodySize) {
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
    // remote username not yet implemented
    String remoteUsername = "-";
    String timestamp = ACCESS_LOG_DATE_TIME_FORMATTER.format(Instant.now());

    String line = String.format(ACCESS_LOG_FORMAT, remoteAddress,
                                remoteUsername, timestamp, request,
                                statusCode, responseBodySize);

    try {
      accessLogWriter.write(line);
      accessLogWriter.flush();
    } catch (IOException e) {
      LOG.warn("Failed to write to access log", e);
    }
  }
}