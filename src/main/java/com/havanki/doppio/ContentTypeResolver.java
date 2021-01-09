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

  private static final FileNameMap FILE_NAME_MAP =
    URLConnection.getFileNameMap();

  private final List<String> textGeminiSuffixes;
  private final String defaultContentType;

  /**
   * Creates a new resolver.
   *
   * @param  textGeminiSuffixes list of file suffixes for text/gemini resources
   * @param  defaultContentType default content type, for when detection fails
   */
  public ContentTypeResolver(List<String> textGeminiSuffixes,
                             String defaultContentType) {
    this.textGeminiSuffixes = textGeminiSuffixes;
    this.defaultContentType = defaultContentType;
  }

  /**
   * Gets the content type for a file, based on just its filename. This relies
   * mostly on Java's built-in MIME type detection, but adds support for
   * text/gemini.<p>
   *
   * If none of the usual detection techniques work, then this method returns
   * a default content type.
   *
   * @param  fileName name of file with content
   * @return          content type
   */
  public String getContentTypeFor(String fileName) {
    if (textGeminiSuffixes.stream().anyMatch(s -> fileName.endsWith(s))) {
      return "text/gemini";
    }
    String contentType = FILE_NAME_MAP.getContentTypeFor(fileName);
    return contentType != null ? contentType : defaultContentType;
  }
}
