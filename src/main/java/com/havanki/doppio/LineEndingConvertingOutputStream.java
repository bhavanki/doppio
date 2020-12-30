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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A filtering output stream that converts Mac and Unix line endings to DOS
 * line endings("\r\n"). Existing DOS line endings are preserved.<p>
 *
 * This implementation works by holding on to carriage return characters ('\r')
 * as they stream through. A consequence is that, when this stream is flushed,
 * a held carriage return is not flushed, since the next character written may
 * require it to be dropped. However, upon close, a held carriage return results
 * in emission of a trailing DOS newline.
 */
public class LineEndingConvertingOutputStream extends FilterOutputStream {

  private static final int NONE = -1;
  private static final int CR = '\r';
  private static final int NL = '\n';

  private int heldByte;

  /**
   * Creates a new output stream built on the specified underlying output stream.
   *
   * @param  out underlying output stream
   */
  public LineEndingConvertingOutputStream(OutputStream out) {
    super(out);

    heldByte = NONE;
  }

  @Override
  public void write(int b) throws IOException {
    switch (b) {
      case CR:
        if (heldByte == CR) {
          writeCRLF(); // Mac (previous) line conversion
        }
        heldByte = CR;
        break;
      case NL:
        writeCRLF(); // DOS preservation or Unix conversion
        heldByte = NONE;
        break;
      default:
        if (heldByte == CR) {
          writeCRLF(); // Mac (previous) line conversion
          heldByte = NONE;
        }
        super.write(b);
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    // TBD make more efficient
    for (int i = 0; i < len; i++) {
      write(b[off + i]);
    }
  }

  /**
   * Resets this stream, so that it may be reused with new output. (This drops
   * any held carriage return.)
   */
  public void reset() {
    heldByte = NONE;
  }

  @Override
  public void close() throws IOException {
    if (heldByte == CR) {
      writeCRLF();
    }
    super.close();
  }

  private void writeCRLF() throws IOException {
    super.write(CR);
    super.write(NL);
  }
}
