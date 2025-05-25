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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class TemporaryCertificateGenerator {

  private static SecureRandom random;
  private static Provider bouncy;

  private final PrivateKey key;
  private final X509Certificate cert;

  public TemporaryCertificateGenerator(String host, long validityInSec)
      throws GeneralSecurityException, IOException {
    synchronized (TemporaryCertificateGenerator.class) {
      if (random == null) {
        random = new SecureRandom();
      }
      if (bouncy == null) {
        bouncy = new BouncyCastleProvider();
        Security.addProvider(bouncy);
      }
    }

    // Generate the key pair.
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(4096, random);
    KeyPair pair = keyGen.generateKeyPair();
    key = pair.getPrivate();

    // Construct a self-signed certificate.
    // Adapted from https://stackoverflow.com/questions/43960761
    Instant now = Instant.now();
    Date startDate = new Date(now.toEpochMilli());
    Instant then = now.plus(validityInSec, ChronoUnit.SECONDS);
    Date endDate = new Date(then.toEpochMilli());

    X500Name dnName = new X500Name("CN=" + host);
    BigInteger certSerialNumber = BigInteger.valueOf(now.toEpochMilli());

    SubjectPublicKeyInfo subjectPublicKeyInfo =
      SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded());
    X509v3CertificateBuilder certificateBuilder =
      new X509v3CertificateBuilder(dnName, certSerialNumber, startDate, endDate,
                                   dnName, subjectPublicKeyInfo);
    try {
      ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
        .setProvider(bouncy).build(pair.getPrivate());
      X509CertificateHolder certificateHolder =
        certificateBuilder.build(contentSigner);
      cert = new JcaX509CertificateConverter().getCertificate(certificateHolder);
    } catch (OperatorCreationException e) {
      throw new GeneralSecurityException(e);
    }
  }

  public PrivateKey getPrivateKey() {
    return key;
  }

  public X509Certificate getCertificate() {
    return cert;
  }
}
