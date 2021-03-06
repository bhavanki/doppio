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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RequestParserTest {

  private static final String HOST = "gemini.example.com";
  private static final int PORT = 1965;

  private RequestParser requestParser;

  @BeforeEach
  public void beforeEach() {
    requestParser = new RequestParser(HOST, PORT);
  }

  @Test
  public void testSuccess() throws Exception {
    String request = "gemini://gemini.example.com/foo";

    URI uri = requestParser.parse(request);

    assertEquals(new URI(request), uri);
  }

  @Test
  public void testSuccessWithExplicitPort() throws Exception {
    String request = "gemini://gemini.example.com:1965/foo";

    URI uri = requestParser.parse(request);

    assertEquals(new URI(request), uri);
  }

  @Test
  public void testInvalidURI() throws Exception {
    String request = "garbage://";

    RequestParser.RequestParserException e =
        assertThrows(RequestParser.RequestParserException.class,
                     () -> requestParser.parse(request));

    assertEquals("Invalid request URI", e.getMessage());
    assertEquals(StatusCodes.BAD_REQUEST, e.getStatusCode());
  }

  @Test
  public void testNotGeminiScheme() throws Exception {
    String request = "http://gemini.example.com/foo";

    RequestParser.RequestParserException e =
        assertThrows(RequestParser.RequestParserException.class,
                     () -> requestParser.parse(request));

    assertEquals("Only the gemini scheme is supported", e.getMessage());
    assertEquals(StatusCodes.PROXY_REQUEST_REFUSED, e.getStatusCode());
  }

  @Test
  public void testUserInfo() {
    String request = "gemini://user@gemini.example.com/foo";

    RequestParser.RequestParserException e =
        assertThrows(RequestParser.RequestParserException.class,
                     () -> requestParser.parse(request));

    assertEquals("User-info component not permitted", e.getMessage());
    assertEquals(StatusCodes.BAD_REQUEST, e.getStatusCode());
  }

  @Test
  public void testFragment() {
    String request = "gemini://gemini.example.com/foo#bar";

    RequestParser.RequestParserException e =
        assertThrows(RequestParser.RequestParserException.class,
                     () -> requestParser.parse(request));

    assertEquals("Fragment component not permitted", e.getMessage());
    assertEquals(StatusCodes.BAD_REQUEST, e.getStatusCode());
  }

  @Test
  public void testWrongHost() throws Exception {
    String request = "gemini://www.example.com/foo";

    RequestParser.RequestParserException e =
        assertThrows(RequestParser.RequestParserException.class,
                     () -> requestParser.parse(request));

    assertEquals("Invalid host", e.getMessage());
    assertEquals(StatusCodes.PROXY_REQUEST_REFUSED, e.getStatusCode());
  }

  @Test
  public void testWrongPort() throws Exception {
    String request = "gemini://gemini.example.com:8080/foo";

    RequestParser.RequestParserException e =
        assertThrows(RequestParser.RequestParserException.class,
                     () -> requestParser.parse(request));

    assertEquals("Invalid port", e.getMessage());
    assertEquals(StatusCodes.PROXY_REQUEST_REFUSED, e.getStatusCode());
  }
}
