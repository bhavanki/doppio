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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility to convert a Gemini index page into an Atom feed. The index page
 * must be formatted according to the "Subscribing to Gemini pages"
 * specification:
 *
 * gemini://gemini.circumlunar.space/docs/companion/subscription.gmi
 */
public class Atomizer {

  private static final Logger LOG = LoggerFactory.getLogger(Atomizer.class);

  private static final String FEED_TEMPLATE =
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
    "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n\n" +
    "<title>{title}</title>\n" +
    "{subtitle}" +
    "<link href=\"{link}\"/>\n" +
    "<updated>{updated}</updated>\n" +
    "<id>{id}</id>\n\n" +
    "{entries}" +
    "</feed>";

  private static final String ENTRY_TEMPLATE =
    "  <entry>\n" +
    "    <title>{title}</title>\n" +
    "    <link rel=\"alternate\" href=\"{link}\"/>\n" +
    "    <id>{id}</id>\n" +
    "    <updated>{updated}</updated>\n" +
    "  </entry>\n\n";

  private static final String DEFAULT_TITLE_FORMAT = "Feed: %s";

  private static final Pattern H1_PATTERN = Pattern.compile("#([^#]+)");
  private static final Pattern H2_PATTERN = Pattern.compile("##([^#]+)");
  private static final Pattern ENTRY_PATTERN =
    Pattern.compile("=>\\s*(\\S+)\\s+(\\d{4}-\\d{2}-\\d{2})(.+)");

  private final Clock clock;

  /**
   * Creates a new atomizer.
   */
  public Atomizer() {
    this(Clock.systemUTC());
  }

  /**
   * Creates a new atomizer that uses the given clock to generate default
   * updated times for empty feeds. Useful for testing.
   */
  Atomizer(Clock clock) {
    this.clock = clock;
  }

  /**
   * Converts a Gemini index page into an Atom feed.
   *
   * @param  feedDirUriString feed directory URI (also where the index page is)
   * @param  docContent       index page content
   * @return                  Atom feed content
   * @throws IOException      if the page content cannot be read
   */
  public String atomize(String feedDirUriString, String docContent) {

    if (!feedDirUriString.endsWith("/")) {
      feedDirUriString += "/";
    }

    String title = null;
    String subtitle = null;
    Instant updated = null;

    boolean subtitleAvailable = true;
    List<String> entryList = new ArrayList<>();

    try (StringReader sr = new StringReader(docContent);
         BufferedReader br = new BufferedReader(sr)) {
      String line;
      while ((line = br.readLine()) != null) {

        // Ignore empty lines.
        if (line.isBlank()) {
          continue;
        }

        // Use the first level 1 header as the feed title.
        if (title == null) {
          Matcher h1Matcher = H1_PATTERN.matcher(line);
          if (h1Matcher.matches()) {
            title = h1Matcher.group(1).trim();
            continue;
          }
        }

        // After the title, use the first level 2 header as the feed subtitle.
        // Any non-header (and non-blank) lines before that candidate header
        // means there is no subtitle.
        if (title != null && subtitleAvailable) {
          if (!line.startsWith("#")) {
            subtitleAvailable = false;
          } else {
            Matcher h2Matcher = H2_PATTERN.matcher(line);
            if (h2Matcher.matches()) {
              subtitle = h2Matcher.group(1).trim();
              subtitleAvailable = false;
              continue;
            }
          }
        }

        // For efficiency, skip any non-link lines here on out.
        if (!line.startsWith("=>")) {
          continue;
        }

        // Pick out any link lines that work as feed entries.
        Matcher entryMatcher = ENTRY_PATTERN.matcher(line);
        if (entryMatcher.matches()) {
          String entryUrl = feedDirUriString + entryMatcher.group(1);
          String iso8601Date = entryMatcher.group(2);
          String iso8601Timestamp = iso8601Date + "T12:00:00Z";
          String entryTitle = cleanupEntryTitle(entryMatcher.group(3));
          String entryContent = ENTRY_TEMPLATE
            .replace("{title}", entryTitle)
            .replace("{link}", entryUrl)
            .replace("{id}", entryUrl)
            .replace("{updated}", iso8601Timestamp);
          entryList.add(entryContent);

          // If this entry is the most recent so far, remember its timestamp.
          try {
            Instant entryInstant =
              DateTimeFormatter.ISO_INSTANT.parse(iso8601Timestamp, Instant::from);
            if (updated == null || entryInstant.isAfter(updated)) {
              updated = entryInstant;
            }
          } catch (DateTimeParseException e) {
            // It's possible to have entries with invalid timestamps (e.g.,
            // 2021-01-99), so just ignore those for this calculation.
            LOG.warn("Failed to parse update time {} for entry {}", iso8601Timestamp, entryUrl);
          }
        }
      }
    } catch (IOException e) {
      // This shouldn't happen because this is reading from a string.
      throw new IllegalStateException("Received I/O error while reading page content", e);
    }

    if (title == null) {
      title = DEFAULT_TITLE_FORMAT.format(feedDirUriString);
    }
    if (subtitle != null) {
      subtitle = String.format("<subtitle>%s</subtitle>\n", subtitle);
    } else {
      subtitle = "";
    }
    if (updated == null) {
      updated = Instant.now(clock);
    }

    String feedContent = FEED_TEMPLATE
      .replace("{title}", title)
      .replace("{subtitle}", subtitle)
      .replace("{link}", feedDirUriString)
      .replace("{id}", feedDirUriString)
      .replace("{updated}", DateTimeFormatter.ISO_INSTANT.format(updated))
      .replace("{entries}", entryList.stream().collect(Collectors.joining()));
    return feedContent;
  }

  /**
   * Cleans up the title text for an entry.
   *
   * @param  title originally read title
   * @return       cleaned-up title
   */
  private static String cleanupEntryTitle(String title) {
    return title
      .trim()  // remove whitespace on either end
      .replaceFirst("[\\s\\p{Punct}]*", "");  // remove ws + punctuation from the front
  }
}
