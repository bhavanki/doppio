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

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * A secure area of the served resources, delineated by a path and an optional
 * means of authorizing access.
 */
public class SecureDomain {

  private final Path dir;
  private final X509TrustManager trustManager;

  /**
   * Creates a new secure domain with open authorization.
   *
   * @param  dir top directory of domain
   */
  public SecureDomain(Path dir) {
    if (dir == null) {
      throw new IllegalStateException("dir must not be null");
    }
    this.dir = dir;
    this.trustManager = new AllowAllTrustManager();
  }

  /**
   * Creates a new secure domain with truststore-based authorization. If the
   * truststore is null, authorization is open.
   *
   * @param  dir                      top directory of domain
   * @param  truststorePath           path to truststore
   * @param  truststorePassword       password for truststore
   * @throws GeneralSecurityException if there is a problem with the truststore
   * @throws IOException              if the truststore cannot be read
   */
  public SecureDomain(Path dir, Path truststorePath, String truststorePassword)
      throws GeneralSecurityException, IOException {
    if (dir == null) {
      throw new IllegalStateException("dir must not be null");
    }
    this.dir = dir;

    if (truststorePath == null) {
      this.trustManager = new AllowAllTrustManager();
    } else {
      KeyStore truststore =
          KeyStore.getInstance(truststorePath.toFile(),
                               truststorePassword.toCharArray());
      TrustManagerFactory tmf =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(truststore);
      X509TrustManager xtm = null;
      for (TrustManager tm : tmf.getTrustManagers()) {
        if (tm instanceof X509TrustManager) {
          xtm = (X509TrustManager) tm;
          break;
        }
      }
      if (xtm == null) {
        throw new IllegalStateException("No X509TrustManager available for " +
                                        "truststore at " + truststorePath);
      }
      trustManager = xtm;
    }
  }

  /**
   * Gets this secure domain's directory.
   *
   * @return directory of secure domain
   */
  public Path getDir() {
    return dir;
  }

  /**
   * Gets this secure domain's trust manager.
   *
   * @return trust manager of secure domain
   */
  public X509TrustManager getTrustManager() {
    return trustManager;
  }
}
