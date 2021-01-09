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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mozilla.universalchardet.UniversalDetector;

public class CharsetDetectorTest {

  private UniversalDetector delegate;
  private CharsetDetector detector;
  private File testFile;

  @BeforeEach
  public void beforeEach() {
    delegate = mock(UniversalDetector.class);
    detector = new CharsetDetector("text/unknown");
    testFile = mock(File.class);
  }

  @Test
  public void testSuccessfulDetection() throws IOException {
    try (MockedStatic<UniversalDetector> delegate = mockStatic(UniversalDetector.class)) {
      delegate.when(() -> UniversalDetector.detectCharset(testFile))
        .thenReturn("utf-8");
      assertEquals("utf-8", detector.detect(testFile));
    }
  }

  @Test
  public void testUsAsciiPromotionDetection() throws IOException {
    try (MockedStatic<UniversalDetector> delegate = mockStatic(UniversalDetector.class)) {
      delegate.when(() -> UniversalDetector.detectCharset(testFile))
        .thenReturn("us-ascii");
      assertEquals("UTF-8", detector.detect(testFile));
    }
  }

  @Test
  public void testDefaultDetection() throws IOException {
    try (MockedStatic<UniversalDetector> delegate = mockStatic(UniversalDetector.class)) {
      delegate.when(() -> UniversalDetector.detectCharset(testFile))
        .thenReturn(null);
      assertEquals("text/unknown", detector.detect(testFile));
    }
  }
}
