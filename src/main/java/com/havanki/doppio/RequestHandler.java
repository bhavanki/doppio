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
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler for a single request. An instance of a handler is run for each
 * incoming request, in its own thread.
 */
public class RequestHandler implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

  private static final int MAX_REQUEST_BYTES = 1026; // 1024 + CRLF
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

    requestParser = new RequestParser(serverProps.getHost(), serverProps.getPort());
  }

  @Override
  public void run() {
    String request = null;
    int statusCode = StatusCodes.PERMANENT_FAILURE;
    long responseBodySize = 0;

    // Check for a valid session / successful handshake.
    SSLSession session = socket.getSession();
    if (!session.isValid()) {
      LOG.debug("Session is invalid, rejecting");
      try {
        socket.close();
      } catch (IOException e) {
        LOG.debug("Failed to close socket", e);
      }
      return;
    }

    // Retrieve the peer certificate, if any.
    X509Certificate peerCertificate;
    try {
      peerCertificate = (X509Certificate)
          ((session.getPeerCertificates())[0]);
    } catch (SSLPeerUnverifiedException e) {
      peerCertificate = null;
    }

    // Open input and output streams for the socket.
    try (BoundedInputStream bis =
          new BoundedInputStream(socket.getInputStream(), MAX_REQUEST_BYTES);
         InputStreamReader isr =
          new InputStreamReader(bis,
                                StandardCharsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT));
         BufferedReader in = new BufferedReader(isr);
         OutputStream os = socket.getOutputStream();
         BufferedOutputStream out = new BufferedOutputStream(os)) {

      // Read the single-line Gemini request.
      request = in.readLine();
      if (request == null) {
        throw new IOException("Read null line from request");
      }
      request = request.trim();

      // Parse the request as a URI.
      URI uri;
      try {
        uri = requestParser.parse(request);
      } catch (RequestParser.RequestParserException e) {
        statusCode = e.getStatusCode();
        writeResponseHeader(out, statusCode, e.getMessage());
        return;
      }

      // Loop handling requests until there is no longer a local redirect, or
      // the maximum number of local redirects has been exceeded.
      File resourceFile = null;
      int numLocalRedirects = 0;
      while (numLocalRedirects <= serverProps.getMaxLocalRedirects()) {

        // Normalize the URI to avoid any .. shenanigans.
        uri = uri.normalize();

        // Pull the path out of the URI and find the matching path in the root
        // directory of the server.
        String path = uri.getPath();
        LOG.debug("Path requested: {}", path);
        if (path.startsWith("/..") || path.startsWith("..")) {
          statusCode = StatusCodes.BAD_REQUEST;
          writeResponseHeader(out, statusCode, "Illegal path in URI");
          return;
        }
        if (path.length() > 0 && path.charAt(0) == '/') {
          path = path.substring(1);
        }
        Path resourcePath = serverProps.getRoot().resolve(path);
        LOG.debug("Resolved path: {}", resourcePath);

        // If the resource is in a secure directory, require authentication.
        // Do this before checking if the resource exists so as not to leak
        // info.
        boolean isSecure = false;
        for (Path secureDir : serverProps.getSecureDirs()) {
          if (Path.of(path).startsWith(secureDir)) {
            isSecure = true;
            if (peerCertificate == null) {
              statusCode = StatusCodes.CLIENT_CERTIFICATE_REQUIRED;
              writeResponseHeader(out, statusCode, "Authentication required");
              return;
            }
            break;
          }
        }

        // If the resource is in a secure directory, validate the peer
        // certificate.
        if (isSecure) {
          try {
            peerCertificate.checkValidity();
          } catch (CertificateExpiredException e) {
            statusCode = StatusCodes.CERTIFICATE_NOT_VALID;
            writeResponseHeader(out, statusCode, "Certificate has expired");
            return;
          } catch (CertificateNotYetValidException e) {
            statusCode = StatusCodes.CERTIFICATE_NOT_VALID;
            writeResponseHeader(out, statusCode, "Certificate is not yet valid");
            return;
          }
        }

        // Determine if the resource is a CGI script.
        boolean isCgi = serverProps.getCgiDir() != null &&
          Path.of(path).startsWith(serverProps.getCgiDir());
        LOG.debug("CGI? {}", isCgi);

        // Locate the resource, finding the path to it and any extra path
        // information.
        Optional<Path[]> splitResourcePath =
          splitResourcePath(resourcePath, isCgi);
        if (splitResourcePath.isEmpty()) {
          // If the resource does not exist, fail with a NOT_FOUND.
          statusCode = StatusCodes.NOT_FOUND;
          writeResponseHeader(out, statusCode, "Resource not found");
          return;
        }
        resourceFile = splitResourcePath.get()[0].toFile();

        // Non-CGI resources cannot be redirects, so break out of the redirect
        // loop now for them.
        if (!isCgi) {
          break;
        }

        // Accessing a directory isn't valid for CGI.
        if (resourceFile.isDirectory()) {
          statusCode = StatusCodes.BAD_REQUEST;
          writeResponseHeader(out, statusCode,
                              "Cannot access directory over CGI");
          return;
        }

        // Start a process to run the CGI script.
        ProcessBuilder pb;
        try {
          pb = new CgiProcessBuilderFactory()
            .createCgiProcessBuilder(resourceFile, splitResourcePath.get(),
                                     uri, socket, peerCertificate, serverProps);
        } catch (IOException e) {
          statusCode = StatusCodes.TEMPORARY_FAILURE;
          writeResponseHeader(out, statusCode,
                              "Failed to resolve CGI resource path");
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

            // If the location URI is non-absolute (not starting with a
            // scheme), then treat it as a local redirect.
            if (isRedirect && !responseMetadata.getLocation().isAbsolute()) {
              LOG.debug("Local redirect: {}", responseMetadata.getLocation());
              uri = responseMetadata.getLocation();
              numLocalRedirects++;
              continue; // the while loop for local redirects
            }

            // Determine the response status code. If not explicitly provided,
            // default to 30 for a redirect and 20 otherwise.
            Integer statusCodeInt = responseMetadata.getStatusCode();
            if (statusCodeInt == null) {
                statusCode = isRedirect ?
                  StatusCodes.REDIRECT_TEMPORARY : StatusCodes.SUCCESS;
            } else {
              statusCode = statusCodeInt.intValue();
            }

            // Determine the meta string for the response. For a redirect,
            // this is the URI to redirect to. Otherwise, it's the content
            // type of the response body.
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
              if (serverProps.isForceCanonicalText() && meta.startsWith("text/")) {
                try (OutputStream bodyOut = new LineEndingConvertingOutputStream(out)) {
                  responseBodySize = processStdout.transferTo(bodyOut);
                }
              } else {
                responseBodySize = processStdout.transferTo(out);
              }
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
        } // end processing CGI output

        return; // NOPMD

      } // end while redirecting

      // If the number of local redirects has been exceeded, fail now.
      if (numLocalRedirects > serverProps.getMaxLocalRedirects()) {
        statusCode = StatusCodes.CGI_ERROR;
        writeResponseHeader(out, statusCode,
                            "Exceeded maximum number of local redirects");
        return;
      }

      // This should not happen, since maxLocalRedirects cannot be negative.
      if (resourceFile == null) {
        statusCode = StatusCodes.PERMANENT_FAILURE;
        writeResponseHeader(out, statusCode,
                            "Internal error, check the server log");
        LOG.error("resourceFile is null after local redirect loop");
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
      if (serverProps.isForceCanonicalText() && contentType.startsWith("text/")) {
        OutputStream bodyOut = new LineEndingConvertingOutputStream(out);
        responseBodySize = writeFile(bodyOut, resourceFile);
        bodyOut.flush(); // do not close, let try-with-resources handle it
      } else {
        responseBodySize = writeFile(out, resourceFile);
      }
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

  private Optional<Path[]> splitResourcePath(final Path resourcePath, boolean isCgi) {
    if (!isCgi) {
      // The whole path is the resource, if it's present.
      if (resourcePath.toFile().exists()) {
        return Optional.of(new Path[] { resourcePath, null });
      }
      return Optional.empty();
    }

    // Find the portion of the path that points to something in the CGI
    // directory. Everything after that is extra path information to pass to
    // the script.
    Path cgiDir = serverProps.getRoot().resolve(serverProps.getCgiDir());
    Path scriptPath = resourcePath; // start with the entire path
    while (!scriptPath.equals(cgiDir)) {
      if (scriptPath.toFile().exists()) {
        // Note that this doesn't care if the path is actually a directory.
        // That case is rejected later anyway.
        return Optional.of(new Path[] {
          scriptPath,
          scriptPath.relativize(resourcePath)
        });
      }
      scriptPath = scriptPath.getParent(); // try one directory up
    }

    // Nothing in the CGI directory matched. Note that this could also mean
    // that the CGI directory itself should be the "script" path, and
    // everything else is extra path information, but that isn't valid anyway.
    return Optional.empty();
  }

  private static final String RESPONSE_HEADER_FORMAT = "%d %s" + CRLF;

  private void writeResponseHeader(BufferedOutputStream out, int statusCode,
                                   String meta)
    throws IOException {
    String header = String.format(RESPONSE_HEADER_FORMAT, statusCode, meta);
    out.write(header.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  private long writeFile(OutputStream out, File resourceFile)
    throws IOException {
    return Files.copy(resourceFile.toPath(), out);
  }
}
