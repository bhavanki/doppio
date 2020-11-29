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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.FileNameMap;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;

import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

  private static final String GEMINI_SCHEME = "gemini";
  private static final String CRLF = "\r\n";

  private final FileNameMap fileNameMap = URLConnection.getFileNameMap();

  private final ServerProperties serverProps;
  private final Socket socket;

  public RequestHandler(ServerProperties serverProps, Socket socket) {
    this.serverProps = serverProps;
    this.socket = socket;
  }

  @Override
  public void run() {
    try (InputStreamReader isr =
            new InputStreamReader(socket.getInputStream(),
                                  StandardCharsets.UTF_8);
         BufferedReader in = new BufferedReader(isr);
         OutputStream os = socket.getOutputStream();
         BufferedOutputStream out = new BufferedOutputStream(os)) {
      String request = in.readLine();
      URI uri;
      try {
        uri = new URI(request.trim());
        LOG.debug("Request URI: {}", uri);
      } catch (URISyntaxException e) {
        writeResponseHeader(out, StatusCodes.BAD_REQUEST,
                            "Invalid request URI");
        return;
      }
      if (!hasValidScheme(uri)) {
        writeResponseHeader(out, StatusCodes.PROXY_REQUEST_REFUSED,
                            "Only the gemini scheme is supported");
        return;
      }
      if (!hasMatchingHost(uri)) {
        writeResponseHeader(out, StatusCodes.PROXY_REQUEST_REFUSED,
                            "Invalid host");
        return;
      }

      String path = uri.getPath();
      LOG.debug("Path requested: {}", path);
      if (path.length() > 0 && path.charAt(0) == '/') {
        path = path.substring(1);
      }
      Path resourcePath = serverProps.getRoot().resolve(path);
      LOG.info("Resolved path: {}", resourcePath);

      File resourceFile = resourcePath.toFile();
      if (!resourceFile.exists()) {
        writeResponseHeader(out, StatusCodes.NOT_FOUND,
                            "Resource not found");
        return;
      }

      String fileName = resourcePath.getFileName().toString();
      String contentType = fileNameMap.getContentTypeFor(fileName);

      writeResponseHeader(out, StatusCodes.SUCCESS, contentType);
      writeFile(out, resourcePath);
    } catch (IOException e) {
      LOG.error("Failed to handle request", e);
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        LOG.debug("Failed to close socket", e);
      }
    }
  }

  private static final String RESPONSE_HEADER_FORMAT = "%d %s" + CRLF;

  private boolean hasValidScheme(URI uri) {
    String scheme = uri.getScheme();
    if (uri == null) {
      return true;
    }
    return (GEMINI_SCHEME.equals(scheme));
  }

  private boolean hasMatchingHost(URI uri) {
    return serverProps.getHost().equalsIgnoreCase(uri.getHost());
  }

  private void writeResponseHeader(BufferedOutputStream out, int statusCode,
                                   String meta)
    throws IOException {
    String header = String.format(RESPONSE_HEADER_FORMAT, statusCode, meta);
    out.write(header.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  private void writeFile(BufferedOutputStream out, Path resourcePath)
    throws IOException {
    Files.copy(resourcePath, out);
  }
}
