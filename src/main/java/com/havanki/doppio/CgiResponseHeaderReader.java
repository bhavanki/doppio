package com.havanki.doppio;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class that reads header lines in the response (output) of proper
 * CGI scripts.
 */
public class CgiResponseHeaderReader {

  private static final Logger LOG = LoggerFactory.getLogger(CgiResponseHeaderReader.class);

  private static final String CONTENT_TYPE_PREFIX = "Content-Type:";
  private static final int CONTENT_TYPE_PREFIX_LEN = CONTENT_TYPE_PREFIX.length();

  private static final String STATUS_PREFIX = "Status:";
  private static final int STATUS_PREFIX_LEN = STATUS_PREFIX.length();

  /**
   * Consumes CGI response headers from the given input stream, which is
   * expected to be the output from a CGI script.
   *
   * @param  processStdout CGI output as an input stream
   * @return               response metadata
   * @throws IOException   if the headers are malformed
   */
  public CgiResponseMetadata consumeHeaders(InputStream processStdout)
    throws IOException {
    CgiResponseMetadata responseMetadata = new CgiResponseMetadata();

    String responseHeader;
    while (true) {
      responseHeader = readLine(processStdout);
      if (responseHeader == null || responseHeader.isEmpty()) {
        break;
      }
      LOG.debug("Response header: {}", responseHeader);

      if (responseHeader.startsWith(CONTENT_TYPE_PREFIX)) {
        // Content-Type: <content-type>
        responseMetadata.setContentType(responseHeader
          .substring(CONTENT_TYPE_PREFIX_LEN).trim());

      } else if (responseHeader.startsWith("Status:")) {
        // Status: <status-code>[ <reason-phrase>]
        String statusValue = responseHeader.substring(STATUS_PREFIX_LEN).trim();
        if (statusValue.isEmpty()) {
          throw new IOException("Status response header has empty value");
        }
        String statusCodeString;
        int spidx = statusValue.indexOf(" ");
        if (spidx != -1) {
          statusCodeString = statusValue.substring(0, spidx);
        } else {
          statusCodeString = statusValue;
        }
        try {
          responseMetadata.setStatusCode(Integer.parseInt(statusCodeString));
        } catch (NumberFormatException e) {
          throw new IOException("Status response header has invalid status code " +
                                statusCodeString);
        }
        if (spidx != -1) {
          responseMetadata.setReasonPhrase(statusValue.substring(spidx + 1));
        }
      } else {
        LOG.warn("Unsupported CGI response header: {}", responseHeader);
      }
    }

    // Content-Type is required, so fail if it isn't present.
    if (responseMetadata.getContentType() == null) {
      throw new IOException("Content-Type response header not provided");
    }
    return responseMetadata;
  }

  /**
   * Reads a line of UTF-8 text from an input stream. This is here because after
   * CGI response headers are read, the rest of the stream is fed to the server
   * response, and if a normal Java buffered input stream or reader is used,
   * it is liable to take out extra bytes from the stream that don't belong to
   * CGI response headers, thus cutting off the beginning of the body content.
   *
   * Only "\n" is detected as the end of a line, and not "\r\n". However, RFC
   * 3875 specifies "\n" as the line ending for CGI response headers, so this
   * is actually OK.
   *
   * @param  in          input stream
   * @return             line of text
   * @throws IOException if reading from the stream fails
   */
  private String readLine(InputStream in) throws IOException {
    byte[] lineBytes = new byte[80];
    int len = 0;
    int b = 0;
    while (true) {
      b = in.read();
      if (b == -1) {
        break;
      }
      if (b == '\n') {
        break;
      }
      if (lineBytes.length < len + 1) {
        byte[] newLineBytes = new byte[2 * lineBytes.length];
        System.arraycopy(lineBytes, 0, newLineBytes, 0, len);
        lineBytes = newLineBytes;
      }
      lineBytes[len] = (byte) b;
      len++;
    }

    if (len == 0) {
      return b == -1 ? null : "";
    }
    return new String(lineBytes, StandardCharsets.UTF_8);
  }
}