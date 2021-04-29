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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServerPropertiesTest {

  static final String ROOT = ServerProperties.DEFAULT_ROOT + "/testroot";
  static final String HOST = "gemini.example.com";
  static final int PORT = ServerProperties.DEFAULT_PORT + 100;
  static final int CONTROL_PORT = ServerProperties.DEFAULT_CONTROL_PORT + 100;
  static final long SHUTDOWN_TIMEOUT_SEC =
    ServerProperties.DEFAULT_SHUTDOWN_TIMEOUT_SEC + 10L;
  static final int NUM_THREADS = ServerProperties.DEFAULT_NUM_THREADS + 2;
  static final String CGI_DIR = ServerProperties.DEFAULT_CGI_DIR + "/testcgi";
  static final int MAX_LOCAL_REDIRECTS =
    ServerProperties.DEFAULT_MAX_LOCAL_REDIRECTS + 1;
  static final boolean FORCE_CANONICAL_TEXT =
    !ServerProperties.DEFAULT_FORCE_CANONICAL_TEXT;
  static final List<String> TEXT_GEMINI_SUFFIXES =
    append(ServerProperties.DEFAULT_TEXT_GEMINI_SUFFIXES, ".g");
  static final String DEFAULT_CONTENT_TYPE = "text/html";
  static final boolean ENABLE_CHARSET_DETECTION =
    !ServerProperties.DEFAULT_ENABLE_CHARSET_DETECTION;
  static final String DEFAULT_CHARSET = "utf-8";
  static final String FAVICON = "🗑";
  static final List<String> FEED_PAGES = List.of("testgemlog.gmi");
  static final String LOG_DIR = "/var/log/doppio";
  static final List<SecureDomain> SECURE_DOMAINS =
    List.of(new SecureDomain(Path.of("/path1")));
  static final String KEYSTORE =
    ServerProperties.DEFAULT_KEYSTORE.toString() + ".test";
  static final String KEYSTORE_PASSWORD =
    ServerProperties.DEFAULT_KEYSTORE_PASSWORD + "123456";
  static final boolean SET_MOD_SSL_CGI_META_VARS =
    !ServerProperties.DEFAULT_SET_MOD_SSL_CGI_META_VARS;

  protected void assertMinimal(ServerProperties sp) {
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

  protected void assertMaximal(ServerProperties sp) {
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

    assertEquals(1, sp.getSecureDomains().size());
    assertEquals("/path1", sp.getSecureDomains().get(0).getDir().toString());
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
