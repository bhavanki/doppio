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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

import javax.net.ssl.SSLPeerUnverifiedException;
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
  private final SSLSocket socket;
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
                        SSLSocket socket) {
    this.serverProps = serverProps;
    this.accessLogger = accessLogger;
    this.socket = socket;

    requestParser = new RequestParser(serverProps.getHost());
  }

  @Override
  public void run() {
    String request = null;
    int statusCode = StatusCodes.PERMANENT_FAILURE;
    long responseBodySize = 0;

    // Retrieve the peer principal, if any.
    Principal peerPrincipal;
    try {
      peerPrincipal = socket.getSession().getPeerPrincipal();
    } catch (SSLPeerUnverifiedException e) {
      peerPrincipal = null;
    }

    // Open input and output streams for the socket.
    try (InputStreamReader isr =
            new InputStreamReader(socket.getInputStream(),
                                  StandardCharsets.UTF_8);
         BufferedReader in = new BufferedReader(isr);
         OutputStream os = socket.getOutputStream();
         BufferedOutputStream out = new BufferedOutputStream(os)) {

      // Read the single-line Gemini request and parse it as a URI.
      request = in.readLine().trim();
      URI uri;
      try {
        uri = requestParser.parse(request);
      } catch (RequestParser.RequestParserException e) {
        statusCode = e.getStatusCode();
        writeResponseHeader(out, statusCode, e.getMessage());
        return;
      }

      // Pull the path out of the URI and find the matching path in the root
      // directory of the server.
      String path = uri.getPath();
      LOG.debug("Path requested: {}", path);
      if (path.length() > 0 && path.charAt(0) == '/') {
        path = path.substring(1);
      }
      boolean isCgi = serverProps.getCgiDir() != null &&
        Path.of(path).startsWith(serverProps.getCgiDir());
      Path resourcePath = serverProps.getRoot().resolve(path);
      LOG.debug("Resolved path: {}", resourcePath);
      LOG.debug("CGI? {}", isCgi);

      File resourceFile = resourcePath.toFile();
      if (!resourceFile.exists()) {
        // If the path does not exist, fail with a NOT_FOUND.
        statusCode = StatusCodes.NOT_FOUND;
        writeResponseHeader(out, statusCode, "Resource not found");
        return;
      }

      // Handle a CGI script invocation.
      if (isCgi) {
        // Accessing a directory isn't valid for CGI.
        if (resourceFile.isDirectory()) {
          statusCode = StatusCodes.BAD_REQUEST;
          writeResponseHeader(out, statusCode, "Cannot access directory over CGI");
          return;
        }

        // Start a process to run the CGI script.
        ProcessBuilder pb;
        try {
          pb = new CgiProcessBuilderFactory()
            .createCgiProcessBuilder(resourceFile, uri, socket, peerPrincipal,
                                     serverProps);
        } catch (IOException e) {
          statusCode = StatusCodes.TEMPORARY_FAILURE;
          writeResponseHeader(out, statusCode, "Failed to resolve CGI resource path");
          return;
        }
        LOG.debug("Executing CGI {}", pb.command());
        Process p = pb.start();

        // Process the script output.
        try {
          try (InputStream processStdout = p.getInputStream()) {

            // Consume the response headers. If the script fails before it
            // starts generating output, then expected headers will not be
            // found and the server will return a CGI error.
            CgiResponseMetadata responseMetadata;
            try {
              responseMetadata = new CgiResponseHeaderReader()
                .consumeHeaders(processStdout);
            } catch (IOException e) {
              LOG.error("CGI script returned invalid response headers", e);
              statusCode = StatusCodes.CGI_ERROR;
              writeResponseHeader(out, statusCode,
                                  "CGI script returned invalid response headers");
              return;
            }

            // Check if the response indicates a redirect.
            boolean isRedirect = responseMetadata.getLocation() != null;

            // Determine the response status code. If not explicitly provided,
            // default to 30 for a redirect and 20 otherwise.
            Integer statusCodeInt = responseMetadata.getStatusCode();
            if (statusCodeInt == null) {
                statusCode = isRedirect ?
                  StatusCodes.REDIRECT_TEMPORARY : StatusCodes.SUCCESS;
            } else {
              statusCode = statusCodeInt.intValue();
            }

            // Determine the meta string for the response. For a redirect, this
            // is the URI to redirect to. Otherwise, it's the content type of
            // the response body.
            String meta;
            if (isRedirect) {
              meta = responseMetadata.getLocation().toString();
            } else {
              meta = responseMetadata.getContentType();
            }

            // Write out a response header.
            writeResponseHeader(out, statusCode, meta);

            // Pipe the body content out when the response is not a redirect.
            if (!isRedirect) {
              responseBodySize = processStdout.transferTo(out);
            }
          }
        } finally {
          // Wait for the script process to exit. If the script fails while it
          // is generating output, transfer of the response body just stops.
          try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
              LOG.warn("CGI exited with code {}", exitCode);
            }
          } catch (InterruptedException e) {
            LOG.info("Interrupted while waiting for CGI to complete");
          }
        }
        return;
      }

      // At this point, the resource is treated as static.
      if (resourceFile.isDirectory()) {
        // If the path is a directory, see if there is an index file to
        // serve from it.
        File resourceDir = resourceFile;
        resourceFile = null;
        for (String suffix : contentTypeResolver.getGeminiSuffixes()) {
          File indexFile = new File(resourceDir, "index" + suffix);
          if (indexFile.exists()) {
            resourceFile = indexFile;
            break;
          }
        }
        if (resourceFile == null) {
          statusCode = StatusCodes.NOT_FOUND;
          writeResponseHeader(out, statusCode, "Index file not found");
          return;
        }
      }

      // Find the file's content type for the response header.
      String fileName = resourceFile.getName();
      String contentType = contentTypeResolver.getContentTypeFor(fileName);
      LOG.debug("Detected content type: {}", contentType);

      // Write out a SUCCESS response header and then the file contents as
      // the response body.
      statusCode = StatusCodes.SUCCESS;
      writeResponseHeader(out, statusCode, contentType);
      responseBodySize = writeFile(out, resourceFile);
    } catch (IOException e) {
      LOG.error("Failed to handle request", e);
      statusCode = StatusCodes.TEMPORARY_FAILURE;
    } catch (RuntimeException e) {
      LOG.error("Unexpected exception", e);
      statusCode = StatusCodes.PERMANENT_FAILURE;
      throw e;
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        LOG.debug("Failed to close socket", e);
      }

      // Write to the access log.
      if (request == null) {
        request = "?";
      }
      accessLogger.log(socket, request, statusCode, responseBodySize);
    }
  }

  private static final String RESPONSE_HEADER_FORMAT = "%d %s" + CRLF;

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
