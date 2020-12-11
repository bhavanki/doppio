package com.havanki.doppio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CgiResponseHeaderReaderTest {

  private CgiResponseHeaderReader reader;
  private InputStream in;
  private CgiResponseMetadata metadata;

  @BeforeEach
  public void beforeEach() {
    reader = new CgiResponseHeaderReader();
  }

  @Test
  public void testContentTypeOnly() throws Exception {
    in = newInputStream("Content-Type: text/plain\n" +
                        "\n" +
                        "Hello");

    CgiResponseMetadata metadata = reader.consumeHeaders(in);

    assertEquals("text/plain", metadata.getContentType());
    assertEquals(StatusCodes.SUCCESS, metadata.getStatusCode());
    assertNull(metadata.getReasonPhrase());

    verifyRemaining(in, "Hello");
  }

  @Test
  public void testNoContentType() throws Exception {
    in = newInputStream("Status: 21\n" +
                        "\n" +
                        "Hello");

    assertThrows(IOException.class, () -> reader.consumeHeaders(in),
                 "Content-Type response header not provided");
  }


  @Test
  public void testStatusNoReasonPhrase() throws Exception {
    in = newInputStream("Content-Type: text/plain\n" +
                        "Status: 21\n" +
                        "\n" +
                        "Hello");

    CgiResponseMetadata metadata = reader.consumeHeaders(in);

    assertEquals("text/plain", metadata.getContentType());
    assertEquals(21, metadata.getStatusCode());
    assertNull(metadata.getReasonPhrase());

    verifyRemaining(in, "Hello");
  }

  @Test
  public void testStatusReasonPhrase() throws Exception {
    in = newInputStream("Content-Type: text/plain\n" +
                        "Status: 21 Because I said so\n" +
                        "\n" +
                        "Hello");

    CgiResponseMetadata metadata = reader.consumeHeaders(in);

    assertEquals("text/plain", metadata.getContentType());
    assertEquals(21, metadata.getStatusCode());
    assertEquals("Because I said so", metadata.getReasonPhrase());

    verifyRemaining(in, "Hello");
  }

  @Test
  public void testStatusEmptyReasonPhrase() throws Exception {
    in = newInputStream("Content-Type: text/plain\n" +
                        "Status: 21 \n" +
                        "\n" +
                        "Hello");

    CgiResponseMetadata metadata = reader.consumeHeaders(in);

    assertEquals("text/plain", metadata.getContentType());
    assertEquals(21, metadata.getStatusCode());
    assertNull(metadata.getReasonPhrase()); // the header is trimmed

    verifyRemaining(in, "Hello");
  }

  @Test
  public void testStatusLongReasonPhrase() throws Exception {
    in = newInputStream("Content-Type: text/plain\n" +
                        "Status: 21 This is a very long phrase which will " +
                        "exercise the readLine implementation in the reader " +
                        "class because using Java's buffering causes " +
                        "characters to get eaten afterwards\n" +
                        "\n" +
                        "Hello");

    CgiResponseMetadata metadata = reader.consumeHeaders(in);

    assertEquals("text/plain", metadata.getContentType());
    assertEquals(21, metadata.getStatusCode());
    assertEquals("This is a very long phrase which will " +
                 "exercise the readLine implementation in the reader " +
                 "class because using Java's buffering causes " +
                 "characters to get eaten afterwards", metadata.getReasonPhrase());

    verifyRemaining(in, "Hello");
  }

  @Test
  public void testNoStatus() throws Exception {
    in = newInputStream("Content-Type: text/plain\n" +
                        "Status:\n" +
                        "\n" +
                        "Hello");
    assertThrows(IOException.class, () -> reader.consumeHeaders(in),
                 "Status response header has empty value");
  }

  @Test
  public void testBadStatusCode() throws Exception {
    in = newInputStream("Content-Type: text/plain\n" +
                        "Status: potato\n" +
                        "\n" +
                        "Hello");
    assertThrows(IOException.class, () -> reader.consumeHeaders(in),
                 "Status response header has invalid status code potato");
  }

  @Test
  public void testSkipUnknownHeader() throws Exception {
    in = newInputStream("Content-Type: text/plain\n" +
                        "Color: vermillion\n" +
                        "\n" +
                        "Hello");

    CgiResponseMetadata metadata = reader.consumeHeaders(in);

    assertEquals("text/plain", metadata.getContentType());
    assertEquals(StatusCodes.SUCCESS, metadata.getStatusCode());
    assertNull(metadata.getReasonPhrase());

    verifyRemaining(in, "Hello");
  }


  private static InputStream newInputStream(String content) {
    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
    return new ByteArrayInputStream(contentBytes);
  }

  private void verifyRemaining(InputStream in, String expectedContent)
    throws Exception {
    try (InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
         BufferedReader br = new BufferedReader(isr)) {
      StringBuilder content = new StringBuilder();
      String line;
      boolean addNl = false;
      while ((line = br.readLine()) != null) {
        if (addNl) {
          content.append("\n");
        }
        content.append(line);
      }

      assertEquals(expectedContent, content.toString());
    }
  }

}
