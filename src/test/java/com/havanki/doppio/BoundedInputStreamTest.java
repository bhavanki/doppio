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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BoundedInputStreamTest {

  private byte[] source;
  private ByteArrayInputStream bais;
  private BoundedInputStream in;
  private int b;

  @AfterEach
  public void afterEach() throws Exception {
    in.close();
  }

  @Test
  public void testSingleByteReadBelowBound() throws Exception {
    source = "hellodude".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    int numBytesRead = readAll();

    assertEquals(9, numBytesRead);
  }

  @Test
  public void testSingleByteReadAtBound() throws Exception {
    source = "hellothere".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    int numBytesRead = readAll();

    assertEquals(10, numBytesRead);
  }

  @Test
  public void testSingleByteReadAboveBound() throws Exception {
    source = "hellofriend".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    int numBytesRead = readAll();

    assertEquals(10, numBytesRead);
  }

  @Test
  public void testSingleByteReadZero() throws Exception {
    source = new byte[0];
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    int numBytesRead = readAll();

    assertEquals(0, numBytesRead);
  }

  private int readAll() throws Exception {
    int numBytesRead = 0;
    while (true) {
      b = in.read();
      if (b == -1) {
        break;
      }
      numBytesRead++;
    }
    return numBytesRead;
  }

  @Test
  public void testArrayReadExactIntervals() throws Exception {
    source = "hellodude".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 9); // avoid writing past end of dest

    byte[] dest = new byte[9];
    int len = 3;
    int numBytesRead = readArrayAll(dest, len);

    assertEquals(9, numBytesRead);
    assertArrayEquals(dest, source);
  }

  @Test
  public void testArrayReadInexactIntervals() throws Exception {
    source = "hellothere".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10); // avoid writing past end of dest

    byte[] dest = new byte[10];
    int len = 3;
    int numBytesRead = readArrayAll(dest, len);

    assertEquals(10, numBytesRead);
    assertArrayEquals(dest, source);
  }

  @Test
  public void testArrayReadZero() throws Exception {
    source = new byte[0];
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 0); // avoid writing past end of dest

    byte[] dest = new byte[0];
    int len = 3;
    int numBytesRead = readArrayAll(dest, len);

    assertEquals(0, numBytesRead);
    assertArrayEquals(dest, source);
  }

  private int readArrayAll(byte[] dest, int len) throws Exception {
    int off = 0;
    int numBytesRead = 0;
    while (true) {
      int readLen = in.read(dest, off, len);
      if (readLen == -1) {
        break;
      }
      numBytesRead += readLen;
      off += len;
    }
    return numBytesRead;
  }

  @Test
  public void testSkipMiddle() throws Exception {
    source = "hellothere".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    int b = in.read();
    assertEquals((int) 'h', b);

    assertEquals(3L, in.skip(3L));

    b = in.read();
    assertEquals((int) 'o', b);
  }

  @Test
  public void testSkipBeginning() throws Exception {
    source = "hellothere".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    assertEquals(5L, in.skip(5L));

    int b = in.read();
    assertEquals((int) 't', b);
  }

  @Test
  public void testSkipEndExact() throws Exception {
    source = "hellothere".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    byte[] bs = new byte[5];
    in.read(bs, 0, 5);
    assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bs);

    assertEquals(5L, in.skip(5L));
    int b = in.read();
    assertEquals(-1, b);

    assertEquals(0L, in.skip(1L));
  }

  @Test
  public void testSkipEndOvershoot() throws Exception {
    source = "hellothere".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    byte[] bs = new byte[5];
    in.read(bs, 0, 5);
    assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bs);

    assertEquals(5L, in.skip(7L));
    int b = in.read();
    assertEquals(-1, b);

    assertEquals(0L, in.skip(1L));
  }

  @Test
  public void testAvailableBelowBound() throws Exception {
    source = "hellothere".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    byte[] bs = new byte[5];
    in.read(bs, 0, 5);

    int actualAvailable = bais.available();
    assumeTrue(actualAvailable > 0);

    assertEquals(actualAvailable, in.available());
  }

  @Test
  public void testAvailableAtBound() throws Exception {
    source = "hellothere".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    byte[] bs = new byte[10];
    in.read(bs, 0, 10);

    int actualAvailable = bais.available();
    assertEquals(0, actualAvailable);

    assertEquals(actualAvailable, in.available());
  }

  @Test
  public void testAvailableAboveBound() throws Exception {
    source = "hellofriend".getBytes(StandardCharsets.UTF_8);
    bais = new ByteArrayInputStream(source);
    in = new BoundedInputStream(bais, 10);

    byte[] bs = new byte[10];
    in.read(bs, 0, 10);

    int actualAvailable = bais.available();
    assumeTrue(actualAvailable > 0);

    assertEquals(0, in.available());
  }
}
