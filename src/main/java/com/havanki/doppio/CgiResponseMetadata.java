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

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContentType() {
    return contentType;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public void setReasonPhrase(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

  public void setLocation(URI location) {
    this.location = location;
  }

  public URI getLocation() {
    return location;
  }
}
