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

  public static final int SUCCESS = 20;
  public static final int REDIRECT_TEMPORARY = 30;
  public static final int TEMPORARY_FAILURE = 40;
  public static final int CGI_ERROR = 42;
  public static final int NOT_FOUND = 51;
  public static final int PERMANENT_FAILURE = 50;
  public static final int PROXY_REQUEST_REFUSED = 53;
  public static final int BAD_REQUEST = 59;
}
