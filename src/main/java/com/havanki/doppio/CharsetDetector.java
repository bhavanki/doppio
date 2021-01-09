/**
 * Copyright (C) 2021 Bill Havanki
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

import java.io.File;
import java.io.IOException;

import org.mozilla.universalchardet.UniversalDetector;

/**
 * A detector for the charset of text content.
 */
public class CharsetDetector {

  private final String defaultCharset;

  /**
   * Creates a new detector.
   *
   * @param  defaultCharset default charset to return when detection fails
   */
  public CharsetDetector(String defaultCharset) {
    this.defaultCharset = defaultCharset;
  }

  /**
   * Detects the charset for a file. This requires reading at least some
   * portion of the file. If the charset cannot be detected, this detector's
   * default charset is returned.
   *
   * @param  f           file
   * @return             detected charset for file
   * @throws IOException if reading the file failed
   */
  public String detect(File f) throws IOException {
    String charset = UniversalDetector.detectCharset(f);
    // Say that US-ASCII is UTF-8, since that is the guaranteed supported
    // charset for Gemini clients, and a resource encoded in US-ASCII is also
    // valid UTF-8. This way, short text/gemini resources in UTF-8 won't be
    // detected as US-ASCII, and potentially not rendered by clients.
    if ("us-ascii".equalsIgnoreCase(charset)) {
      charset = "UTF-8";
    }
    return charset != null ? charset : defaultCharset;
  }
}
