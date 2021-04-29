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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Server configuration properties.
 */
@SuppressWarnings("unchecked")
public class ServerProperties {

  static final Path DEFAULT_ROOT = Path.of("/var/gemini");
  static final int DEFAULT_PORT = 1965;
  static final int DEFAULT_CONTROL_PORT = 31965;
  static final long DEFAULT_SHUTDOWN_TIMEOUT_SEC = 5;
  static final int DEFAULT_NUM_THREADS = 4;
  static final Path DEFAULT_CGI_DIR = null;
  static final int DEFAULT_MAX_LOCAL_REDIRECTS = 10;
  static final boolean DEFAULT_FORCE_CANONICAL_TEXT = false;
  static final List<String> DEFAULT_TEXT_GEMINI_SUFFIXES =
    List.of(".gmi", ".gemini");
  static final String DEFAULT_DEFAULT_CONTENT_TYPE = "text/plain";
  static final boolean DEFAULT_ENABLE_CHARSET_DETECTION = false;
  static final String DEFAULT_DEFAULT_CHARSET = null;
  static final String DEFAULT_FAVICON = null;
  static final List<String> DEFAULT_FEED_PAGES = List.of();
  static final Path DEFAULT_LOG_DIR = null;
  static final Path DEFAULT_KEYSTORE = Path.of("/etc/doppio/keystore.jks");
  static final String DEFAULT_KEYSTORE_PASSWORD = "doppio";
  static final boolean DEFAULT_SET_MOD_SSL_CGI_META_VARS = false;

  private final Path root;
  private final String host;
  private final int port;
  private final int controlPort;
  private final long shutdownTimeoutSec;
  private final int numThreads;
  private final Path cgiDir;
  private final int maxLocalRedirects;
  private final boolean forceCanonicalText;
  private final List<String> textGeminiSuffixes;
  private final String defaultContentType;
  private final boolean enableCharsetDetection;
  private final String defaultCharset;
  private final String favicon;
  private final List<String> feedPages;
  private final Path logDir;
  private final List<SecureDomain> secureDomains;
  private final Path keystore;
  private final String keystorePassword;
  private final boolean setModSslCgiMetaVars;

  /**
   * Creates new server properties. Use of a builder is preferred to calling
   * this directly.
   */
  ServerProperties(
    Path root,
    String host,
    int port,
    int controlPort,
    long shutdownTimeoutSec,
    int numThreads,
    Path cgiDir,
    int maxLocalRedirects,
    boolean forceCanonicalText,
    List<String> textGeminiSuffixes,
    String defaultContentType,
    boolean enableCharsetDetection,
    String defaultCharset,
    String favicon,
    List<String> feedPages,
    Path logDir,
    List<SecureDomain> secureDomains,
    Path keystore,
    String keystorePassword,
    boolean setModSslCgiMetaVars
  ) {
    this.root = root;
    this.host = host;
    this.port = port;
    this.controlPort = controlPort;
    this.shutdownTimeoutSec = shutdownTimeoutSec;
    this.numThreads = numThreads;
    this.cgiDir = cgiDir;
    this.maxLocalRedirects = maxLocalRedirects;
    this.forceCanonicalText = forceCanonicalText;
    this.textGeminiSuffixes = textGeminiSuffixes;
    this.defaultContentType = defaultContentType;
    this.enableCharsetDetection = enableCharsetDetection;
    this.defaultCharset = defaultCharset;
    this.favicon = favicon;
    this.feedPages = feedPages;
    this.logDir = logDir;
    this.secureDomains = secureDomains;
    this.keystore = keystore;
    this.keystorePassword = keystorePassword;
    this.setModSslCgiMetaVars = setModSslCgiMetaVars;
  }

  void validate() {
    if (host == null) {
      throw new IllegalStateException("host may not be null");
    }
    if (port < 1 || port > 65535) {
      throw new IllegalStateException("port must be between 1 and 65535");
    }
    if (numThreads < 1) {
      throw new IllegalStateException("numThreads must be positive");
    }
    if (maxLocalRedirects < 0) {
      throw new IllegalStateException("maxLocalRedirects must be non-negative");
    }
  }

  /**
   * Gets the root (directory) for the server.
   *
   * @return root
   */
  public Path getRoot() {
    return root;
  }

  /**
   * Gets the host for the server.
   *
   * @return host
   */
  public String getHost() {
    return host;
  }

  /**
   * Gets the listening port for the server.
   *
   * @return port
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the control port for the server.
   *
   * @return control port
   */
  public int getControlPort() {
    return controlPort;
  }

  /**
   * Gets the shutdown timeout for the server, in seconds.
   *
   * @return shutdown timeout
   */
  public long getShutdownTimeoutSec() {
    return shutdownTimeoutSec;
  }

  /**
   * Gets the number of server threads handling requests.
   *
   * @return number of server threads
   */
  public int getNumThreads() {
    return numThreads;
  }

  /**
   * Gets the maximum number of CGI local redirects permitted by the server.
   *
   * @return maximum number of CGI local redirects
   */
  public int getMaxLocalRedirects() {
    return maxLocalRedirects;
  }

  /**
   * Gets whether text response bodies are forced to use canonical (DOS) line
   * endings.
   *
   * @return whether canonical text is returned in responses
   */
  public boolean isForceCanonicalText() {
    return forceCanonicalText;
  }

  /**
   * Get the suffixes for resources that have content type text/gemini.
   *
   * @return suffixes for text/gemini resources
   */
  public List<String> getTextGeminiSuffixes() {
    return textGeminiSuffixes;
  }

  /**
   * Gets the default content type for resources.
   *
   * @return resource default content type
   */
  public String getDefaultContentType() {
    return defaultContentType;
  }

  /**
   * Gets whether charset detection of text resources is enabled.
   *
   * @return whether charset detection of text resources is enabled
   */
  public boolean isEnableCharsetDetection() {
    return enableCharsetDetection;
  }

  /**
   * Gets the default charset for text resources.
   *
   * @return text resource default charset
   */
  public String getDefaultCharset() {
    return defaultCharset;
  }

  /**
   * Gets the site favicon.
   *
   * @return favicon
   */
  public String getFavicon() {
    return favicon;
  }

  /**
   * Gets the paths to the feed pages.
   *
   * @return feed pages
   */
  public List<String> getFeedPages() {
    return feedPages;
  }

  /**
   * Gets the CGI directory for the server.
   *
   * @return CGI directory
   */
  public Path getCgiDir() {
    return cgiDir;
  }

  /**
   * Gets the log directory for the server.
   *
   * @return log directory
   */
  public Path getLogDir() {
    return logDir;
  }

  /**
   * Gets the secure domains for the server.
   *
   * @return secure domains
   */
  public List<SecureDomain> getSecureDomains() {
    return secureDomains;
  }

  /**
   * Gets the keystore containing the server's private key.
   *
   * @return keystore
   */
  public Path getKeystore() {
    return keystore;
  }

  /**
   * Gets the password for the keystore.
   *
   * @return keystore password
   */
  public String getKeystorePassword() {
    return keystorePassword;
  }

  /**
   * Gets whether CGI meta-variables defined by Apache mod_ssl should be set
   * when executing a CGI script.
   *
   * @return whether to set mod_ssl CGI meta-variables
   */
  public boolean isSetModSslCgiMetaVars() {
    return setModSslCgiMetaVars;
  }

  /**
   * Gets a new builder for server properties.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for {@link ServerProperties}.
   */
  public static class Builder {

    private Path root = DEFAULT_ROOT;
    private String host;
    private int port = DEFAULT_PORT;
    private int controlPort = DEFAULT_CONTROL_PORT;
    private long shutdownTimeoutSec = DEFAULT_SHUTDOWN_TIMEOUT_SEC;
    private int numThreads = DEFAULT_NUM_THREADS;
    private Path cgiDir = DEFAULT_CGI_DIR;
    private int maxLocalRedirects = DEFAULT_MAX_LOCAL_REDIRECTS;
    private boolean forceCanonicalText = DEFAULT_FORCE_CANONICAL_TEXT;
    private List<String> textGeminiSuffixes = DEFAULT_TEXT_GEMINI_SUFFIXES;
    private String defaultContentType = DEFAULT_DEFAULT_CONTENT_TYPE;
    private boolean enableCharsetDetection = DEFAULT_ENABLE_CHARSET_DETECTION;
    private String defaultCharset = DEFAULT_DEFAULT_CHARSET;
    private String favicon = DEFAULT_FAVICON;
    private List<String> feedPages = DEFAULT_FEED_PAGES;
    private Path logDir = DEFAULT_LOG_DIR;
    private List<SecureDomain> secureDomains = new ArrayList<>();
    private Path keystore = DEFAULT_KEYSTORE;
    private String keystorePassword = DEFAULT_KEYSTORE_PASSWORD;
    private boolean setModSslCgiMetaVars = DEFAULT_SET_MOD_SSL_CGI_META_VARS;

    public Builder root(Path root) {
      this.root = root;
      return this;
    }
    public Builder host(String host) {
      this.host = host;
      return this;
    }
    public Builder port(int port) {
      this.port = port;
      return this;
    }
    public Builder controlPort(int controlPort) {
      this.controlPort = controlPort;
      return this;
    }
    public Builder shutdownTimeoutSec(long shutdownTimeoutSec) {
      this.shutdownTimeoutSec = shutdownTimeoutSec;
      return this;
    }
    public Builder numThreads(int numThreads) {
      this.numThreads = numThreads;
      return this;
    }
    public Builder cgiDir(Path cgiDir) {
      this.cgiDir = cgiDir;
      return this;
    }
    public Builder maxLocalRedirects(int maxLocalRedirects) {
      this.maxLocalRedirects = maxLocalRedirects;
      return this;
    }
    public Builder forceCanonicalText(boolean forceCanonicalText) {
      this.forceCanonicalText = forceCanonicalText;
      return this;
    }
    public Builder textGeminiSuffixes(List<String> textGeminiSuffixes) {
      this.textGeminiSuffixes = textGeminiSuffixes;
      return this;
    }
    public Builder defaultContentType(String defaultContentType) {
      this.defaultContentType = defaultContentType;
      return this;
    }
    public Builder enableCharsetDetection(boolean enableCharsetDetection) {
      this.enableCharsetDetection = enableCharsetDetection;
      return this;
    }
    public Builder defaultCharset(String defaultCharset) {
      this.defaultCharset = defaultCharset;
      return this;
    }
    public Builder favicon(String favicon) {
      this.favicon = favicon;
      return this;
    }
    public Builder feedPages(List<String> feedPages) {
      this.feedPages = feedPages;
      return this;
    }
    public Builder logDir(Path logDir) {
      this.logDir = logDir;
      return this;
    }
    public Builder secureDomains(List<SecureDomain> secureDomains) {
      this.secureDomains = secureDomains;
      return this;
    }
    public Builder keystore(Path keystore) {
      this.keystore = keystore;
      return this;
    }
    public Builder keystorePassword(String keystorePassword) {
      this.keystorePassword = keystorePassword;
      return this;
    }
    public Builder setModSslCgiMetaVars(boolean setModSslCgiMetaVars) {
      this.setModSslCgiMetaVars = setModSslCgiMetaVars;
      return this;
    }

    public ServerProperties build() {
      return new ServerProperties(
        root,
        host,
        port,
        controlPort,
        shutdownTimeoutSec,
        numThreads,
        cgiDir,
        maxLocalRedirects,
        forceCanonicalText,
        textGeminiSuffixes,
        defaultContentType,
        enableCharsetDetection,
        defaultCharset,
        favicon,
        feedPages,
        logDir,
        secureDomains,
        keystore,
        keystorePassword,
        setModSslCgiMetaVars
      );
    }
  }
}
