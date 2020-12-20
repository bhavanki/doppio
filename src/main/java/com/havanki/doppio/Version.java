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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {

  public static final String VERSION;

  static {
    try (InputStream in = Version.class.getResourceAsStream("/version.properties")) {
      Properties versionProps = new Properties();
      versionProps.load(in);
      VERSION = versionProps.getProperty("version");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load version.properties", e);
    }
  }
}
