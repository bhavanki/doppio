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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServerPropertiesTest {

  private static final String ROOT =
    ServerProperties.DEFAULT_ROOT + "/testroot";
  private static final String HOST = "gemini.example.com";
  private static final int PORT = ServerProperties.DEFAULT_PORT + 100;
  private static final int CONTROL_PORT =
    ServerProperties.DEFAULT_CONTROL_PORT + 100;
  private static final long SHUTDOWN_TIMEOUT_SEC =
    ServerProperties.DEFAULT_SHUTDOWN_TIMEOUT_SEC + 10L;
  private static final int NUM_THREADS =
    ServerProperties.DEFAULT_NUM_THREADS + 2;
  private static final String CGI_DIR =
    ServerProperties.DEFAULT_CGI_DIR + "/testcgi";
  private static final int MAX_LOCAL_REDIRECTS =
    ServerProperties.DEFAULT_MAX_LOCAL_REDIRECTS + 1;
  private static final boolean FORCE_CANONICAL_TEXT =
    !ServerProperties.DEFAULT_FORCE_CANONICAL_TEXT;
  private static final List<String> TEXT_GEMINI_SUFFIXES =
    append(ServerProperties.DEFAULT_TEXT_GEMINI_SUFFIXES, ".g");
  private static final String DEFAULT_CONTENT_TYPE = "text/html";
  private static final boolean ENABLE_CHARSET_DETECTION =
    !ServerProperties.DEFAULT_ENABLE_CHARSET_DETECTION;
  private static final String DEFAULT_CHARSET = "utf-8";
  private static final String FAVICON = "🗑";
  private static final List<String> FEED_PAGES = List.of("testgemlog.gmi");
  private static final String LOG_DIR = "/var/log/doppio";
  private static final String KEYSTORE =
    ServerProperties.DEFAULT_KEYSTORE.toString() + ".test";
  private static final String KEYSTORE_PASSWORD =
    ServerProperties.DEFAULT_KEYSTORE_PASSWORD + "123456";
  private static final boolean SET_MOD_SSL_CGI_META_VARS =
    !ServerProperties.DEFAULT_SET_MOD_SSL_CGI_META_VARS;

  private Properties props;
  private ServerProperties sp;

  @BeforeEach
  public void beforeEach() {
    props = new Properties();
  }

  @Test
  public void testMinimal() {
    props.setProperty("host", HOST);

    sp = new ServerProperties(props);

    assertEquals(HOST, sp.getHost());

    assertEquals(ServerProperties.DEFAULT_ROOT, sp.getRoot());
    assertEquals(ServerProperties.DEFAULT_PORT, sp.getPort());
    assertEquals(ServerProperties.DEFAULT_CONTROL_PORT, sp.getControlPort());
    assertEquals(ServerProperties.DEFAULT_SHUTDOWN_TIMEOUT_SEC,
                 sp.getShutdownTimeoutSec());
    assertEquals(ServerProperties.DEFAULT_NUM_THREADS, sp.getNumThreads());
    assertEquals(ServerProperties.DEFAULT_CGI_DIR, sp.getCgiDir());
    assertEquals(ServerProperties.DEFAULT_MAX_LOCAL_REDIRECTS,
                 sp.getMaxLocalRedirects());
    assertEquals(ServerProperties.DEFAULT_FORCE_CANONICAL_TEXT,
                 sp.isForceCanonicalText());
    assertEquals(ServerProperties.DEFAULT_TEXT_GEMINI_SUFFIXES,
                 sp.getTextGeminiSuffixes());
    assertEquals(ServerProperties.DEFAULT_DEFAULT_CONTENT_TYPE,
                 sp.getDefaultContentType());
    assertEquals(ServerProperties.DEFAULT_ENABLE_CHARSET_DETECTION,
                 sp.isEnableCharsetDetection());
    assertEquals(ServerProperties.DEFAULT_DEFAULT_CHARSET,
                 sp.getDefaultCharset());
    assertEquals(ServerProperties.DEFAULT_FAVICON,
                 sp.getFavicon());
    assertEquals(ServerProperties.DEFAULT_FEED_PAGES, sp.getFeedPages());
    assertEquals(ServerProperties.DEFAULT_LOG_DIR, sp.getLogDir());
    assertEquals(List.of(), sp.getSecureDomains());
    assertEquals(ServerProperties.DEFAULT_KEYSTORE, sp.getKeystore());
    assertEquals(ServerProperties.DEFAULT_KEYSTORE_PASSWORD,
                 sp.getKeystorePassword());
    assertEquals(ServerProperties.DEFAULT_SET_MOD_SSL_CGI_META_VARS,
                 sp.isSetModSslCgiMetaVars());
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
    props.setProperty("keystore", KEYSTORE);
    props.setProperty("keystorePassword", KEYSTORE_PASSWORD);
    props.setProperty("setModSslCgiMetaVars",
                      Boolean.toString(SET_MOD_SSL_CGI_META_VARS));

    sp = new ServerProperties(props);

    assertEquals(Path.of(ROOT), sp.getRoot());
    assertEquals(HOST, sp.getHost());
    assertEquals(PORT, sp.getPort());
    assertEquals(CONTROL_PORT, sp.getControlPort());
    assertEquals(SHUTDOWN_TIMEOUT_SEC, sp.getShutdownTimeoutSec());
    assertEquals(NUM_THREADS, sp.getNumThreads());
    assertEquals(Path.of(CGI_DIR), sp.getCgiDir());
    assertEquals(MAX_LOCAL_REDIRECTS, sp.getMaxLocalRedirects());
    assertEquals(FORCE_CANONICAL_TEXT, sp.isForceCanonicalText());
    assertEquals(TEXT_GEMINI_SUFFIXES, sp.getTextGeminiSuffixes());
    assertEquals(DEFAULT_CONTENT_TYPE, sp.getDefaultContentType());
    assertEquals(ENABLE_CHARSET_DETECTION, sp.isEnableCharsetDetection());
    assertEquals(DEFAULT_CHARSET, sp.getDefaultCharset());
    assertEquals(FAVICON, sp.getFavicon());
    assertEquals(FEED_PAGES, sp.getFeedPages());
    assertEquals(Path.of(LOG_DIR), sp.getLogDir());
    assertEquals(Path.of(KEYSTORE), sp.getKeystore());
    assertEquals(KEYSTORE_PASSWORD, sp.getKeystorePassword());
    assertEquals(SET_MOD_SSL_CGI_META_VARS, sp.isSetModSslCgiMetaVars());
  }

  @Test
  public void testInvalidPort() {
    props.setProperty("host", HOST);
    props.setProperty("port", "123456");

    IllegalStateException e =
        assertThrows(IllegalStateException.class,
                     () -> new ServerProperties(props));

    assertEquals("port must be between 1 and 65535", e.getMessage());
  }

  @Test
  public void testInvalidNumThreads() {
    props.setProperty("host", HOST);
    props.setProperty("numThreads", "0");

    IllegalStateException e =
        assertThrows(IllegalStateException.class,
                     () -> new ServerProperties(props));

    assertEquals("numThreads must be positive", e.getMessage());
  }

  @Test
  public void testInvalidMaxLocalRedirects() {
    props.setProperty("host", HOST);
    props.setProperty("maxLocalRedirects", "-1");

    IllegalStateException e =
        assertThrows(IllegalStateException.class,
                     () -> new ServerProperties(props));

    assertEquals("maxLocalRedirects must be non-negative", e.getMessage());
  }

  @SafeVarargs
  private static <T> List<T> append(List<T> l, T... items) {
    List<T> la = new ArrayList<>(l);
    for (T item : items) {
      la.add(item);
    }
    return la;
  }
}
