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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AccessLoggerTest {

  private static final InetSocketAddress ADDRESS =
    new InetSocketAddress("gemini-client.example.com", 51965);

  private Socket socket;
  private AccessLogger accessLogger;
  private Instant timestamp;

  @TempDir
  Path logDir;

  @BeforeEach
  public void beforeEach() throws Exception {
    socket = mock(Socket.class);
    when(socket.getRemoteSocketAddress()).thenReturn(ADDRESS);

    accessLogger = new AccessLogger(logDir);
    timestamp = Instant.now();
  }

  @Test
  public void testLog() throws Exception {
    String remoteUsername = "bob";
    String request = "request";
    int statusCode = 20;
    long responseBodySize = 100L;

    accessLogger.log(socket, remoteUsername, request, statusCode,
                     responseBodySize, timestamp);
    accessLogger.close();

    String line = getLogLine();
    String timestampStr = AccessLogger.ACCESS_LOG_DATE_TIME_FORMATTER
      .format(timestamp);
    assertEquals("gemini-client.example.com - bob [" + timestampStr +
                 "] \"request\" 20 100", line);
  }

  @Test
  public void testLogNoContent() throws Exception {
    String remoteUsername = "bob";
    String request = "request";
    int statusCode = 51;
    long responseBodySize = 0L;

    accessLogger.log(socket, remoteUsername, request, statusCode,
                     responseBodySize, timestamp);
    accessLogger.close();

    String line = getLogLine();
    assertTrue(line.endsWith(" \"request\" 51 -"));
  }

  private String getLogLine() throws Exception {
    try (FileReader fr = new FileReader(logDir.resolve("access.log").toFile(),
                                        StandardCharsets.UTF_8);
         BufferedReader br = new BufferedReader(fr)) {
      String line = br.readLine();
      assertNotNull(line);
      return line;
    }
  }

}
