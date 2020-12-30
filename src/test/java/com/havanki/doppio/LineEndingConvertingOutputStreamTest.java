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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LineEndingConvertingOutputStreamTest {

  private ByteArrayOutputStream baos;
  private LineEndingConvertingOutputStream lecos;
  private String in;
  private String expected;
  private String out;

  @BeforeEach
  public void beforeEach() {
    baos = new ByteArrayOutputStream();
    lecos = new LineEndingConvertingOutputStream(baos);
  }

  @Test
  public void testUnixToDos() throws Exception {
    in = "two shots\nof\nespresso";
    expected = "two shots\r\nof\r\nespresso";

    out = writeSingleByte(in);

    assertEquals(expected, out);

    out = writeMultiByte(in);

    assertEquals(expected, out);
  }

  @Test
  public void testUnixToDosEdgeCases() throws Exception {
    in = "\ntwo shots\nof\n\nespresso\n";
    expected = "\r\ntwo shots\r\nof\r\n\r\nespresso\r\n";

    out = writeSingleByte(in);

    assertEquals(expected, out);

    out = writeMultiByte(in);

    assertEquals(expected, out);
  }

  @Test
  public void testMacToDos() throws Exception {
    in = "two shots\rof\respresso";
    expected = "two shots\r\nof\r\nespresso";

    out = writeSingleByte(in);

    assertEquals(expected, out);

    out = writeMultiByte(in);

    assertEquals(expected, out);
  }

  @Test
  public void testMacToDosEdgeCases() throws Exception {
    in = "\rtwo shots\rof\r\respresso\r";
    expected = "\r\ntwo shots\r\nof\r\n\r\nespresso\r\n";

    out = writeSingleByte(in);

    assertEquals(expected, out);

    out = writeMultiByte(in);

    assertEquals(expected, out);
  }

  @Test
  public void testDosToDos() throws Exception {
    in = "two shots\r\nof\r\nespresso";
    expected = "two shots\r\nof\r\nespresso";

    out = writeSingleByte(in);

    assertEquals(expected, out);

    out = writeMultiByte(in);

    assertEquals(expected, out);
  }

  @Test
  public void testDosToDosEdgeCases() throws Exception {
    in = "\r\ntwo shots\r\nof\r\n\r\nespresso\r\n";
    expected = "\r\ntwo shots\r\nof\r\n\r\nespresso\r\n";

    out = writeSingleByte(in);

    assertEquals(expected, out);

    out = writeMultiByte(in);

    assertEquals(expected, out);
  }

  @Test
  public void testMixedToDos() throws Exception {
    in = "two shots\nof\respresso";
    expected = "two shots\r\nof\r\nespresso";

    out = writeSingleByte(in);

    assertEquals(expected, out);

    out = writeMultiByte(in);

    assertEquals(expected, out);
  }

  @Test
  public void testMixedToDosEdgeCases() throws Exception {
    in = "\rtwo shots\r\nof\n\respresso\r";
    expected = "\r\ntwo shots\r\nof\r\n\r\nespresso\r\n";

    out = writeSingleByte(in);

    assertEquals(expected, out);

    out = writeMultiByte(in);

    assertEquals(expected, out);
  }

  private String writeSingleByte(String in) throws Exception {
    byte[] inBytes = in.getBytes(StandardCharsets.UTF_8);
    try {
      lecos.reset();
      baos.reset();
      for (int i = 0; i < inBytes.length; i++) {
        lecos.write(inBytes[i]);
      }
    } finally {
      lecos.close();
    }
    return baos.toString(StandardCharsets.UTF_8);
  }

  private String writeMultiByte(String in) throws Exception {
    byte[] inBytes = in.getBytes(StandardCharsets.UTF_8);
    try {
      lecos.reset();
      baos.reset();
      lecos.write(inBytes);
    } finally {
      lecos.close();
    }
    return baos.toString(StandardCharsets.UTF_8);
  }
}
