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

import java.io.StringReader;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.env.EnvConfig;

public class ServerPropertiesYamlLoaderTest extends ServerPropertiesTest {

  private ServerPropertiesYamlLoader loader;
  private ServerProperties sp;

  @BeforeEach
  public void beforeEach() {
    loader = new ServerPropertiesYamlLoader();
  }

  private static final String MINIMAL_YAML = "host: " + HOST;

  @Test
  public void testMinimalYaml() throws Exception {
    try (StringReader sr = new StringReader(MINIMAL_YAML)) {
      sp = loader.loadFromYaml(sr);
    }

    assertMinimal(sp);
  }

  private static final String MAXIMAL_YAML =
    "root: " + ROOT +
    "\nhost: " + HOST +
    "\nport: " + Integer.toString(PORT) +
    "\ncontrolPort: " + Integer.toString(CONTROL_PORT) +
    "\nshutdownTimeoutSec: " + Long.toString(SHUTDOWN_TIMEOUT_SEC) +
    "\nnumThreads: " + Integer.toString(NUM_THREADS) +
    "\ncgiDir: " + CGI_DIR +
    "\nmaxLocalRedirects: " + Integer.toString(MAX_LOCAL_REDIRECTS) +
    "\nforceCanonicalText: " + Boolean.toString(FORCE_CANONICAL_TEXT) +
    "\ntextGeminiSuffixes:" +
    TEXT_GEMINI_SUFFIXES.stream()
        .map(s -> "\n- " + s)
        .collect(Collectors.joining()) +
    "\ndefaultContentType: " + DEFAULT_CONTENT_TYPE +
    "\nenableCharsetDetection: " + Boolean.toString(ENABLE_CHARSET_DETECTION) +
    "\ndefaultCharset: " + DEFAULT_CHARSET +
    "\nfavicon: " + FAVICON +
    "\nfeedPages:" +
    FEED_PAGES.stream()
        .map(s -> "\n- " + s)
        .collect(Collectors.joining()) +
    "\nlogDir: " + LOG_DIR +
    "\nsecureDomains:" +
    "\n  /path1: {}" +
    "\nkeystore: " + KEYSTORE +
    "\nkeystorePassword: " + KEYSTORE_PASSWORD +
    "\nsetModSslCgiMetaVars: " + Boolean.toString(SET_MOD_SSL_CGI_META_VARS);

  @Test
  public void testMaximalYaml() throws Exception {
    try (StringReader sr = new StringReader(MAXIMAL_YAML)) {
      sp = loader.loadFromYaml(sr);
    }

    assertMaximal(sp);
  }

  private static final String ENV_MINIMAL_YAML = "host: ${TEST_HOSTNAME}";

  @Test
  public void testEnvironmentSubstitution() {
    EnvConfig envConfig = new EnvConfig() {
      @Override
      public Optional<String> getValueFor(String name, String separator,
                                          String value, String environment) {
        if ("TEST_HOSTNAME".equals(name)) {
          return Optional.of("env.gemini.example.com");
        }
        return Optional.empty();
      }
    };

    try (StringReader sr = new StringReader(ENV_MINIMAL_YAML)) {
      sp = loader.loadFromYaml(sr, envConfig);
    }

    assertEquals("env.gemini.example.com", sp.getHost());
  }
}
