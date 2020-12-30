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

/**
 * Metadata gathered from the headers of a proper CGI response.
 */
public class CgiResponseMetadata {
  private String contentType;
  private Integer statusCode;
  private String reasonPhrase;
  private URI location;

  /**
   * Sets the content type.
   *
   * @param contentType content type
   */
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Gets the content type.
   *
   * @return content type
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Sets the status code.
   *
   * @param statusCode status code
   */
  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * Gets the status code.
   *
   * @return status code
   */
  public Integer getStatusCode() {
    return statusCode;
  }

  /**
   * Sets the reason phrase.
   *
   * @param reasonPhrase reason phrase
   */
  public void setReasonPhrase(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

  /**
   * Gets the reason phrase.
   *
   * @return reason phrase
   */
  public String getReasonPhrase() {
    return reasonPhrase;
  }

  /**
   * Sets the location.
   *
   * @param location location
   */
  public void setLocation(URI location) {
    this.location = location;
  }

  /**
   * Gets the location.
   *
   * @return location
   */
  public URI getLocation() {
    return location;
  }
}
