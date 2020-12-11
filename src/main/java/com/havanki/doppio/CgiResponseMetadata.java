package com.havanki.doppio;

/**
 * Metadata gathered from the headers of a proper CGI response.
 */
public class CgiResponseMetadata {
  private String contentType;
  private int statusCode = StatusCodes.SUCCESS;
  private String reasonPhrase;

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContentType() {
    return contentType;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setReasonPhrase(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }
}
