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

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.List;

/**
 * A resolver for file content types.
 */
public class ContentTypeResolver {

  /**
   * The default content type to return when all else fails.
   */
  public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

  private static final FileNameMap FILE_NAME_MAP =
    URLConnection.getFileNameMap();

  private final List<String> textGeminiSuffixes;

  public ContentTypeResolver(List<String> textGeminiSuffixes) {
    this.textGeminiSuffixes = textGeminiSuffixes;
  }

  /**
   * Gets the content type for a file, based on just its filename. This relies
   * mostly on Java's built-in MIME type detection, but adds support for
   * text/gemini.<p>
   *
   * If none of the usual detection techniques work, then this method returns
   * {@link #DEFAULT_CONTENT_TYPE}.
   *
   * @param  fileName name of file with content
   * @return          content type
   */
  public String getContentTypeFor(String fileName) {
    if (textGeminiSuffixes.stream().anyMatch(s -> fileName.endsWith(s))) {
      return "text/gemini";
    }
    String contentType = FILE_NAME_MAP.getContentTypeFor(fileName);
    return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
  }
}
