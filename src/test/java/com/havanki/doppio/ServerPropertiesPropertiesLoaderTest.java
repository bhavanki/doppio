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

import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServerPropertiesPropertiesLoaderTest extends ServerPropertiesTest {

  private Properties props;
  private ServerPropertiesPropertiesLoader loader;
  private ServerProperties sp;

  @BeforeEach
  public void beforeEach() {
    props = new Properties();
    loader = new ServerPropertiesPropertiesLoader();
  }

  @Test
  public void testMinimal() {
    props.setProperty("host", HOST);

    sp = loader.loadFromProperties(props);

    assertMinimal(sp);
  }

  @Test
  public void testMaximal() {
    props.setProperty("root", ROOT);
    props.setProperty("host", HOST);
    props.setProperty("port", Integer.toString(PORT));
    props.setProperty("controlPort", Integer.toString(CONTROL_PORT));
    props.setProperty("shutdownTimeoutSec",
                      Long.toString(SHUTDOWN_TIMEOUT_SEC));
    props.setProperty("numThreads", Integer.toString(NUM_THREADS));
    props.setProperty("cgiDir", CGI_DIR);
    props.setProperty("maxLocalRedirects",
                      Integer.toString(MAX_LOCAL_REDIRECTS));
    props.setProperty("forceCanonicalText",
                      Boolean.toString(FORCE_CANONICAL_TEXT));
    props.setProperty("textGeminiSuffixes",
                      TEXT_GEMINI_SUFFIXES.stream()
                      .collect(Collectors.joining(",")));
    props.setProperty("defaultContentType", DEFAULT_CONTENT_TYPE);
    props.setProperty("enableCharsetDetection",
                      Boolean.toString(ENABLE_CHARSET_DETECTION));
    props.setProperty("defaultCharset", DEFAULT_CHARSET);
    props.setProperty("favicon", FAVICON);
    props.setProperty("feedPages",
                      FEED_PAGES.stream().collect(Collectors.joining(",")));
    props.setProperty("logDir", LOG_DIR);
    props.setProperty("secureDomain.1", "/path1");
    props.setProperty("keystore", KEYSTORE);
    props.setProperty("keystorePassword", KEYSTORE_PASSWORD);
    props.setProperty("setModSslCgiMetaVars",
                      Boolean.toString(SET_MOD_SSL_CGI_META_VARS));

    sp = loader.loadFromProperties(props);

    assertMaximal(sp);
  }

  // Validity tests aren't particular to loading from properties, but it's
  // more convenient to create the test data via properties.

  @Test
  public void testInvalidPort() {
    props.setProperty("host", HOST);
    props.setProperty("port", "123456");

    IllegalStateException e =
        assertThrows(IllegalStateException.class,
                     () -> loader.loadFromProperties(props));

    assertEquals("port must be between 1 and 65535", e.getMessage());
  }

  @Test
  public void testInvalidNumThreads() {
    props.setProperty("host", HOST);
    props.setProperty("numThreads", "0");

    IllegalStateException e =
        assertThrows(IllegalStateException.class,
                     () -> loader.loadFromProperties(props));

    assertEquals("numThreads must be positive", e.getMessage());
  }

  @Test
  public void testInvalidMaxLocalRedirects() {
    props.setProperty("host", HOST);
    props.setProperty("maxLocalRedirects", "-1");

    IllegalStateException e =
        assertThrows(IllegalStateException.class,
                     () -> loader.loadFromProperties(props));

    assertEquals("maxLocalRedirects must be non-negative", e.getMessage());
  }
}
