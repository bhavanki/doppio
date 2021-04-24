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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AtomizerTest {

  private Instant now;
  private Atomizer atomizer;

  @BeforeEach
  public void beforeEach() {
    now = Instant.now();
    Clock testClock = Clock.fixed(now, ZoneOffset.UTC);
    atomizer = new Atomizer(testClock);
  }

  private static final String DOC_URL = "gemini://gemini.example.com/gemlog/";

  private static final String NORMAL_FEED_GEMINI =
    "# J. Random Geminaut's gemlog\n\n" +
    "Welcome to my Gemlog, where you can read every Friday about my adventures in urban gardening and abstract algebra!\n\n" +
    "## My posts\n\n" +
    "=> bokashi.gmi  2020-11-20 - Early Bokashi composting experiments\n" +
    "=> finite-simple-groups.gmi 2020-11-13 - Trying to get to grips with finite simple groups...\n" +
    "=> balcony.gmi  2020-11-06 - I started a balcony garden!\n\n" +
    "## Other gemlogs I enjoy\n\n" +
    "=> gemini://example.com/foo/  Abelard Lindsay's gemlog\n" +
    "=> gemini://example.net/bar/  Vladimir Harkonnen's gemlog\n" +
    "=> gemini://example.org/baz/  Case Pollard's gemlog\n\n" +
    "=> ../  Back to my homepage\n\n" +
    "Thanks for stopping by!";

  // It's bokashi, not bokashmi
  private static final String NORMAL_FEED_ATOM =
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
    "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n\n" +
    "<title>J. Random Geminaut's gemlog</title>\n" +
    "<link href=\"" + DOC_URL + "\"/>\n" +
    "<updated>2020-11-20T12:00:00Z</updated>\n" +
    "<id>" + DOC_URL + "</id>\n\n" +
    "  <entry>\n" +
    "    <title>Early Bokashi composting experiments</title>\n" +
    "    <link rel=\"alternate\" href=\"gemini://gemini.example.com/gemlog/bokashi.gmi\"/>\n" +
    "    <id>gemini://gemini.example.com/gemlog/bokashi.gmi</id>\n" +
    "    <updated>2020-11-20T12:00:00Z</updated>\n" +
    "  </entry>\n\n" +
    "  <entry>\n" +
    "    <title>Trying to get to grips with finite simple groups...</title>\n" +
    "    <link rel=\"alternate\" href=\"gemini://gemini.example.com/gemlog/finite-simple-groups.gmi\"/>\n" +
    "    <id>gemini://gemini.example.com/gemlog/finite-simple-groups.gmi</id>\n" +
    "    <updated>2020-11-13T12:00:00Z</updated>\n" +
    "  </entry>\n\n" +
    "  <entry>\n" +
    "    <title>I started a balcony garden!</title>\n" +
    "    <link rel=\"alternate\" href=\"gemini://gemini.example.com/gemlog/balcony.gmi\"/>\n" +
    "    <id>gemini://gemini.example.com/gemlog/balcony.gmi</id>\n" +
    "    <updated>2020-11-06T12:00:00Z</updated>\n" +
    "  </entry>\n\n" +
    "</feed>";

  @Test
  public void testNormal() throws Exception {
    assertEquals(NORMAL_FEED_ATOM, atomizer.atomize(DOC_URL, NORMAL_FEED_GEMINI));
  }

  private static final String SUBTITLE_FEED_GEMINI =
    "# J. Random Geminaut's gemlog\n\n" +
    "## A non-normative example\n\n" +
    "Welcome to my Gemlog, where you can read every Friday about my adventures in urban gardening and abstract algebra!\n\n" +
    "## My posts\n\n" +
    "=> bokashi.gmi  2020-11-20 - Early Bokashi composting experiments";

  private static final String SUBTITLE_FEED_ATOM =
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
    "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n\n" +
    "<title>J. Random Geminaut's gemlog</title>\n" +
    "<subtitle>A non-normative example</subtitle>\n" +
    "<link href=\"" + DOC_URL + "\"/>\n" +
    "<updated>2020-11-20T12:00:00Z</updated>\n" +
    "<id>" + DOC_URL + "</id>\n\n" +
    "  <entry>\n" +
    "    <title>Early Bokashi composting experiments</title>\n" +
    "    <link rel=\"alternate\" href=\"gemini://gemini.example.com/gemlog/bokashi.gmi\"/>\n" +
    "    <id>gemini://gemini.example.com/gemlog/bokashi.gmi</id>\n" +
    "    <updated>2020-11-20T12:00:00Z</updated>\n" +
    "  </entry>\n\n" +
    "</feed>";

  @Test
  public void testSubtitle() throws Exception {
    assertEquals(SUBTITLE_FEED_ATOM, atomizer.atomize(DOC_URL, SUBTITLE_FEED_GEMINI));
  }

  private static final String EMPTY_FEED_GEMINI =
    "# J. Random Geminaut's gemlog\n\n" +
    "Welcome to my Gemlog, where you can read every Friday about my adventures in urban gardening and abstract algebra!\n\n" +
    "## My posts\n\n" +
    "Nothing to see here\n\n" +
    "## Other gemlogs I enjoy\n\n" +
    "=> gemini://example.com/foo/  Abelard Lindsay's gemlog\n" +
    "=> gemini://example.net/bar/  Vladimir Harkonnen's gemlog\n" +
    "=> gemini://example.org/baz/  Case Pollard's gemlog\n\n" +
    "=> ../  Back to my homepage\n\n" +
    "Thanks for stopping by!";

  private static final String EMPTY_FEED_ATOM =
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
    "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n\n" +
    "<title>J. Random Geminaut's gemlog</title>\n" +
    "<link href=\"" + DOC_URL + "\"/>\n" +
    "<updated>{now}</updated>\n" +
    "<id>" + DOC_URL + "</id>\n\n" +
    "</feed>";

  @Test
  public void testEmpty() throws Exception {
    String expectedFeed = EMPTY_FEED_ATOM
      .replace("{now}", DateTimeFormatter.ISO_INSTANT.format(now));
    assertEquals(expectedFeed, atomizer.atomize(DOC_URL, EMPTY_FEED_GEMINI));
  }

}
