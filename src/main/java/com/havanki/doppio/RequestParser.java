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

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parsing for Gemini requests.
 */
public class RequestParser {

  private static final Logger LOG = LoggerFactory.getLogger(RequestParser.class);

  private static final String GEMINI_SCHEME = "gemini";

  private final String host;

  /**
   * Creates a new parser.
   * @param  host the server host
   */
  public RequestParser(String host) {
    this.host = host;
  }

  /**
   * Parses a Gemini request into a URI. Parsing fails if: the request is not a
   * valid URI; the URI is not for the "gemini" scheme; or the request is for a
   * host not served by this server.
   *
   * @param  request                request to parse
   * @return                        parsed URI
   * @throws RequestParserException if the request cannot be parsed
   */
  public URI parse(String request) throws RequestParserException {
    URI uri;
    try {
      uri = new URI(request);
      LOG.debug("Request URI: {}", uri);
    } catch (URISyntaxException e) {
      throw new RequestParserException("Invalid request URI",
                                       StatusCodes.BAD_REQUEST);
    }

    // Validate that the URI has the "gemini" scheme.
    String scheme = uri.getScheme();
    if (scheme == null) {
      throw new RequestParserException("The gemini scheme is required",
                                       StatusCodes.BAD_REQUEST);
    }
    if (!hasValidScheme(scheme)) {
      throw new RequestParserException("Only the gemini scheme is supported",
                                       StatusCodes.PROXY_REQUEST_REFUSED);
    }

    // Ensure the URI refers to a host served by this server.
    if (!hasMatchingHost(uri)) {
      throw new RequestParserException("Invalid host",
                                       StatusCodes.PROXY_REQUEST_REFUSED);
    }

    return uri;
  }

  private boolean hasValidScheme(String scheme) {
    return GEMINI_SCHEME.equals(scheme);
  }

  private boolean hasMatchingHost(URI uri) {
    return host.equalsIgnoreCase(uri.getHost());
  }

  /**
   * An exception thrown by {@link RequestParser#parse(String)}.
   */
  public static class RequestParserException extends Exception {

    /**
     * The response status code.
     */
    private final int statusCode;

    private RequestParserException(String msg, int statusCode) {
      super(msg);
      this.statusCode = statusCode;
    }

    /**
     * Gets a Gemini response status code appropriate for the parse failure.
     *
     * @return status code
     */
    public int getStatusCode() {
      return statusCode;
    }
  }
}
