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

/**
 * Gemini response status codes.
 */
public final class StatusCodes {

  private StatusCodes() {
  }

  /**
   * SUCCESS: The request was handled successfully.
   */
  public static final int SUCCESS = 20;
  /**
   * REDIRECT_TEMPORARY: The server redirects the client to a new resource
   * location.
   */
  public static final int REDIRECT_TEMPORARY = 30;
  /**
   * TEMPORARY_FAILURE: The request has failed, but an identical request may
   * succeed later.
   */
  public static final int TEMPORARY_FAILURE = 40;
  /**
   * CGI_ERROR: A CGI process died unexpectedly or timed out.
   */
  public static final int CGI_ERROR = 42;
  /**
   * PERMANENT_FAILURE: The request has failed, and an identical request will
   * reliably fail later, for the same reason.
   */
  public static final int PERMANENT_FAILURE = 50;
  /**
   * NOT_FOUND: The requested resource could not be found, but may be available
   * in the future.
   */
  public static final int NOT_FOUND = 51;
  /**
   * PROXY_REQUEST_REFUSED: The request was for a resource at a foreign domain
   * and the server does not accept proxy requests.
   */
  public static final int PROXY_REQUEST_REFUSED = 53;
  /**
   * BAD_REQUEST: The request was malformed / could not be parsed.
   */
  public static final int BAD_REQUEST = 59;
  /**
   * CLIENT_CERTIFICATE_REQUIRED: Access to the requested resource requires a
   * client certificate.
   */
  public static final int CLIENT_CERTIFICATE_REQUIRED = 60;
  /**
   * CERTIFICATE_NOT_VALID: The supplied client certificate is invalid,
   * regardless of the requested resource.
   */
  public static final int CERTIFICATE_NOT_VALID = 62;
}
