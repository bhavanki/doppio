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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContentTypeResolverTest {

  private ContentTypeResolver resolver;

  @BeforeEach
  public void beforeEach() {
    resolver = new ContentTypeResolver(List.of(".gmi", ".gemini"));
  }

  @Test
  public void testGeminiDetection() {
    assertEquals("text/gemini", resolver.getContentTypeFor("file.gmi"));
    assertEquals("text/gemini", resolver.getContentTypeFor("file.gemini"));
  }

  @Test
  public void testNonGeminiDetection() {
    assertEquals("text/plain", resolver.getContentTypeFor("file.txt"));
  }

  @Test
  public void testDefaultDetection() {
    assertEquals("application/octet-stream",
    resolver.getContentTypeFor("file"));
  }
}
