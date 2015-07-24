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
 * $Rev$
 * $Date$
 *
 ******************************************************************************/

package com.ericdaugherty.mail.server.configuration;

//Java imports
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;

/**
 *
 * @author Andreas Kyrmegalos
 */
final class JSSEConfigurator {

   static String[] getVersionConstrainedEnabledProtocols() {
      
      int javaVersion = Integer.parseInt(System.getProperty("java.version").substring(2,3));
      if (javaVersion==6) {
         return new String[] {"TLSv1","SSLv3"};
      }
      //Version 7 or greater is implied here
      else {
         return new String[] {"TLSv1.2","TLSv1.1","TLSv1","SSLv3"};
      }
   }
   
   private final static String[] ENABLED_CIPHERS = new String[]{
                                               "TLS_RSA_WITH_AES_256_CBC_SHA",
                                               "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
                                               "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
                                               "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                                               "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                                               "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                                               "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
                                               "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                                               "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                                               "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                                               "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                                               "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                                               "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                                               "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                                               "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
   
   private static final String[] LIMITED_ENABLED_CIPHERS = new String[]{
                                               "TLS_RSA_WITH_AES_128_CBC_SHA",
                                               "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
                                               "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
                                               "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                                               "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                                               "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                                               "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                                               "SSL_RSA_WITH_DES_CBC_SHA",
                                               "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                                               "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                                               "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
   
   private static String[] getCiphers(String[] ciphers) {
      
      String[] enabledCiphers;
      int javaVersion = Integer.parseInt(System.getProperty("java.version").substring(2,3));
      int updateVersion = System.getProperty("java.version").indexOf('_');
      if (updateVersion!=-1) {
         updateVersion = Integer.parseInt(System.getProperty("java.version").substring(updateVersion+1));
      }
      if ((javaVersion==6&&updateVersion>=22)||javaVersion>=7) {
         enabledCiphers = ciphers;
      }
      //The version doesn't incorporate the TLS renegotiation phase 2 fix
      else {
         enabledCiphers = new String[ciphers.length-1];
         System.arraycopy(ciphers, 0, enabledCiphers, 0, enabledCiphers.length);
      }

      SSLContext ctx = null;
      try {
         ctx = SSLContext.getInstance("TLS");
         ctx.init(null, null, null);
         String [] dcs = ctx.getServerSocketFactory().getDefaultCipherSuites();
         List<String> temp = new ArrayList<String>(enabledCiphers.length);
         for (String cipher:enabledCiphers) {
            for (String dCipher:dcs) {
               if (cipher.equals(dCipher)) {
                  temp.add(dCipher);
                  break;
               }
            }
         }
         enabledCiphers = temp.toArray(new String[temp.size()]);
      }
      catch (GeneralSecurityException gse) {
         List<String> temp = new ArrayList<String>(enabledCiphers.length);
         for (String cipher:enabledCiphers) {
            if (cipher.indexOf("ECDH")==-1) {
               temp.add(cipher);
            }
         }
         enabledCiphers = temp.toArray(new String[temp.size()]);
      }
      
      return enabledCiphers;
   }
   
   static String[] getVersionConstrainedEnabledCiphers() {
      return getCiphers(ENABLED_CIPHERS);
   }
   
   static String[] getVersionConstrainedLimitedEnabledCiphers() {
      return getCiphers(LIMITED_ENABLED_CIPHERS);
   }
}
