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
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public class TemporaryCertificateGenerator {

  private final PrivateKey key;
  private final X509Certificate cert;

  public TemporaryCertificateGenerator(String host, long validityInSec)
      throws GeneralSecurityException, IOException {
    CertAndKeyGen certGen = new CertAndKeyGen("RSA", "SHA256WithRSA");
    certGen.generate(4096);

    key = certGen.getPrivateKey();
    cert = certGen.getSelfCertificate(new X500Name("CN=" + host), validityInSec);
  }

  public PrivateKey getPrivateKey() {
    return key;
  }

  public X509Certificate getCertificate() {
    return cert;
  }
}
