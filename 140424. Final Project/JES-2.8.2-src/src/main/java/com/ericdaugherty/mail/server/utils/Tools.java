/******************************************************************************
 * This program is a 100% Java Email Server.
 ******************************************************************************
 * Copyright (c) 2001-2014, Eric Daugherty (http://www.ericdaugherty.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither the name of the copyright holder nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ******************************************************************************
 * For current versions and more information, please visit:
 * http://javaemailserver.sf.net/
 *
 * or contact the author at:
 * andreaskyrmegalos@hotmail.com
 *
 ******************************************************************************
 * This program is based on the CSRMail project written by Calvin Smith.
 * http://crsemail.sourceforge.net/
 ******************************************************************************
 *
 * $Rev: 334 $
 * $Date: 2014-01-04 02:56:07 +0100 (Sat, 04 Jan 2014) $
 *
 ******************************************************************************/

package com.ericdaugherty.mail.server.utils;

//Java Imports
import java.io.*;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.*;
import java.security.interfaces.RSAPrivateCrtKey;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class Tools {

   private static void createTruststoreWithCACertificate(String certificatePathname, String alias, String truststorePathname)
         throws Exception {
      BufferedInputStream bis = null;
      FileOutputStream fos = null;
      try {
         CertificateFactory cf = CertificateFactory.getInstance("X.509");
         bis = new BufferedInputStream(new FileInputStream(certificatePathname));
         fos = new FileOutputStream(truststorePathname);
         Certificate serverCert = (X509Certificate)cf.generateCertificate(bis);
         KeyStore ks = KeyStore.getInstance("jks");
         ks.load(null, null);
         ks.setCertificateEntry(alias, serverCert);
         ks.store(fos, "password".toCharArray());

      } finally {
         IOUtils.close(bis);
         IOUtils.close(fos);
      }
   }
   
   public static void setCertificateInTruststore(String certificatePathname, String alias, String truststorePathname)
         throws Exception {
      BufferedInputStream bis = null;
      FileInputStream fis = null;
      FileOutputStream fos = null;
      try {
         CertificateFactory cf = CertificateFactory.getInstance("X.509");
         bis = new BufferedInputStream(new FileInputStream(new File(certificatePathname)));
         fos = new FileOutputStream(truststorePathname);
         fis = new FileInputStream(truststorePathname);
         Certificate serverCert = (X509Certificate)cf.generateCertificate(bis);
         KeyStore ks = KeyStore.getInstance("jks");
         ks.load(fis, null);
         ks.setCertificateEntry(alias, serverCert);
         ks.store(fos, "password".toCharArray());

      }
      finally {
         IOUtils.close(bis);
         IOUtils.close(fis);
         IOUtils.close(fos);
      }
   }

   private static void createKeystoreWithPrivateKey(String pkcs12Alias, char[] pkcs12Password, String pkcs12Pathname, String alias, char[] password, String keystorePathname )
         throws Exception {

      FileInputStream fis = null;
      FileOutputStream fos = null;
      try {
         KeyStore tempks = KeyStore.getInstance("pkcs12", "SunJSSE");
         File pkcs12 = new File(pkcs12Pathname);
         tempks.load(fis = new FileInputStream(pkcs12), pkcs12Password);
         Key key = tempks.getKey(pkcs12Alias, pkcs12Password);
         if (key == null) {
            throw new RuntimeException("Got null key from keystore!");
         }
         RSAPrivateCrtKey privKey = (RSAPrivateCrtKey) key;
         Certificate[] clientCerts = tempks.getCertificateChain(pkcs12Alias);
         if (clientCerts == null) {
            throw new RuntimeException("Got null cert chain from keystore!");
         }
         KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry(privKey, clientCerts);
         KeyStore.ProtectionParameter kspp = new KeyStore.PasswordProtection(password);
         
         KeyStore ks = KeyStore.getInstance("jceks");
         ks.load(null, password);
         ks.setEntry(alias, pke, kspp);
         ks.store(fos = new FileOutputStream(keystorePathname), password);

      }
      finally {
         IOUtils.close(fis);
         IOUtils.close(fos);
      }
   }
   
   public static void setPrivateKeyInKeystore(String pkcs12Alias, char[] pkcs12Password, String pkcs12Pathname, String alias, char[] password, String keystorePathname )
         throws Exception {

      FileInputStream fis = null;
      KeyStore tempks = KeyStore.getInstance("pkcs12", "SunJSSE");
      File pkcs12 = new File(pkcs12Pathname);
      try {
         tempks.load(fis = new FileInputStream(pkcs12), pkcs12Password);
      } finally {
         IOUtils.close(fis);
      }
      Key key = tempks.getKey(pkcs12Alias, pkcs12Password);
      if (key == null) {
         throw new RuntimeException("Got null key from keystore!");
      }
      RSAPrivateCrtKey privKey = (RSAPrivateCrtKey) key;
      Certificate[] clientCerts = tempks.getCertificateChain(pkcs12Alias);
      if (clientCerts == null) {
         throw new RuntimeException("Got null cert chain from keystore!");
      }
      KeyStore.PrivateKeyEntry pke = new KeyStore.PrivateKeyEntry(privKey, clientCerts);
      KeyStore.ProtectionParameter kspp = new KeyStore.PasswordProtection(password);

      KeyStore ks = KeyStore.getInstance("jceks");
      try {
         ks.load(fis = new FileInputStream(keystorePathname), password);
      } finally {
         IOUtils.close(fis);
      }
      ks.setEntry(alias, pke, kspp);
      
      FileOutputStream fos = null;
      try {
         ks.store(fos = new FileOutputStream(keystorePathname), password);
      } finally {
         IOUtils.close(fos);
      }
   }

   public static void main(String[] input) throws Exception{

      if (input[0].toLowerCase().equals("tru")) createTruststoreWithCACertificate(input[1], input[2], input[3]);
      if (input[0].toLowerCase().equals("truSet")) setCertificateInTruststore(input[1], input[2], input[3]);
      else if (input[0].toLowerCase().equals("key")) createKeystoreWithPrivateKey(input[1], input[2].toCharArray(), input[3], input[4], input[5].toCharArray(), input[6]);
      else if (input[0].toLowerCase().equals("keySet")) setPrivateKeyInKeystore(input[1], input[2].toCharArray(), input[3], input[4], input[5].toCharArray(), input[6]);
   }
}
