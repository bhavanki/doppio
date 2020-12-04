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
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler for a single request. An instance of a handler is run for each
 * incoming request, in its own thread.
 */
public class RequestHandler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

  private static final String CRLF = "\r\n";

  private final ContentTypeResolver contentTypeResolver =
    new ContentTypeResolver();

  private final ServerProperties serverProps;
  private final AccessLogger accessLogger;
  private final Socket socket;
  private final RequestParser requestParser;

  /**
   * Creates a request handler.
   *
   * @param  serverProps  server properties
   * @param  accessLogger access logger
   * @param  socket       client socket
   */
  public RequestHandler(ServerProperties serverProps,
                        AccessLogger accessLogger,
                        Socket socket) {
    this.serverProps = serverProps;
    this.accessLogger = accessLogger;
    this.socket = socket;

    requestParser = new RequestParser(serverProps.getHost());
  }

  @Override
  public void run() {
    // Open input and output streams for the socket.
    try (InputStreamReader isr =
            new InputStreamReader(socket.getInputStream(),
                                  StandardCharsets.UTF_8);
         BufferedReader in = new BufferedReader(isr);
         OutputStream os = socket.getOutputStream();
         BufferedOutputStream out = new BufferedOutputStream(os)) {

      // Read the single-line Gemini request and parse it as a URI.
      String request = in.readLine().trim();
      URI uri;
      try {
        uri = requestParser.parse(request);
      } catch (RequestParser.RequestParserException e) {
        error(out, e.getStatusCode(), e.getMessage(), request);
        return;
      }

      // Pull the path out of the URI and find the matching path in the root
      // directory of the server.
      String path = uri.getPath();
      LOG.debug("Path requested: {}", path);
      if (path.length() > 0 && path.charAt(0) == '/') {
        path = path.substring(1);
      }
      Path resourcePath = serverProps.getRoot().resolve(path);
      LOG.debug("Resolved path: {}", resourcePath);

      File resourceFile = resourcePath.toFile();
      if (!resourceFile.exists()) {
        // If the path does not exist, fail with a NOT_FOUND.
        error(out, StatusCodes.NOT_FOUND, "Resource not found", request);
        return;
      } else if (resourceFile.isDirectory()) {
        // If the path is a directory, see if there is an index file to
        // serve from it.
        File resourceDir = resourceFile;
        resourceFile = null;
        for (String suffix : contentTypeResolver.getGeminiSuffixes()) {
          resourceFile = new File(resourceDir, "index" + suffix);
          if (resourceFile.exists()) {
            break;
          }
        }
        if (resourceFile == null) {
          error(out, StatusCodes.NOT_FOUND, "Index file not found", request);
          return;
        }
      }

      // Find the file's content type for the response header.
      String fileName = resourceFile.getName();
      String contentType = contentTypeResolver.getContentTypeFor(fileName);

      // Write out a SUCCESS response header and then the file contents as
      // the response body.
      writeResponseHeader(out, StatusCodes.SUCCESS, contentType);
      long bytesWritten = writeFile(out, resourceFile);
      accessLogger.log(socket, request, StatusCodes.SUCCESS, bytesWritten);
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

  private void error(BufferedOutputStream out, int statusCode, String meta,
                     String request)
    throws IOException {
    writeResponseHeader(out, statusCode, meta);
    accessLogger.logError(socket, request, statusCode);
  }

  private void writeResponseHeader(BufferedOutputStream out, int statusCode,
                                   String meta)
    throws IOException {
    String header = String.format(RESPONSE_HEADER_FORMAT, statusCode, meta);
    out.write(header.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  private long writeFile(BufferedOutputStream out, File resourceFile)
    throws IOException {
    return Files.copy(resourceFile.toPath(), out);
  }
}
