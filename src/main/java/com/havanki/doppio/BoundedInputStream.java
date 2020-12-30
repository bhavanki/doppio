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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that limits the number of bytes that may be read.
 */
public class BoundedInputStream extends FilterInputStream {

  private final int maxBytes;
  private int byteCount;

  /**
   * Creates a new input stream that reads from the specified underlying input
   * stream.
   *
   * @param  in       underlying input stream
   * @param  maxBytes maximum number of bytes that may be read
   * @throws IllegalArgumentException if maxBytes is negative
   */
  public BoundedInputStream(InputStream in, int maxBytes) {
    super(in);

    if (maxBytes < 0) {
      throw new IllegalArgumentException("maxBytes must be non-negative");
    }
    this.maxBytes = maxBytes;

    byteCount = 0;
  }

  @Override
  public int available() throws IOException {
    int numBytesRemaining = getNumBytesRemaining();
    if (numBytesRemaining <= 0) {
      return 0;
    }

    return Math.min(numBytesRemaining, super.available());
  }

  @Override
  public int read() throws IOException {
    if (byteCount >= maxBytes) {
      return -1;
    }
    int b = super.read();
    if (b != -1) {
      byteCount++;
    }
    return b;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int numBytesRemaining = getNumBytesRemaining();
    if (numBytesRemaining <= 0) {
      return -1;
    }
    int numBytesToRead = Math.min(numBytesRemaining, len);
    int numBytesRead = super.read(b, off, numBytesToRead);
    byteCount += numBytesRead;
    return numBytesRead;
  }

  @Override
  public long skip(long n) throws IOException {
    int numBytesRemaining = getNumBytesRemaining();
    if (numBytesRemaining <= 0) {
      return 0L;
    }

    long numBytesToSkip = Math.min((long) numBytesRemaining, n);
    long numBytesSkipped = super.skip(numBytesToSkip);
    if (numBytesSkipped > 0L) {
      byteCount += (int) numBytesSkipped;
    }
    return numBytesSkipped;
  }

  private int getNumBytesRemaining() {
    return maxBytes - byteCount;
  }
}
