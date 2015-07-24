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

package com.ericdaugherty.mail.server.services;

import com.ericdaugherty.mail.server.configuration.ConfigurationManager;

/**
 *
 * @author Andreas Kyrmegalos
 */
public interface AuthenticableSessionControl {
   
   public enum AuthMech {
      GSSAPI("GSSAPI"),
      SCRAM_SHA_512("SCRAM-SHA-512"),
      SCRAM_SHA_384("SCRAM-SHA-384"),
      SCRAM_SHA_256("SCRAM-SHA-256"),
      SCRAM_SHA_1("SCRAM-SHA-1"),
      DIGEST_MD5("DIGEST-MD5"),
      CRAM_SHA_512("CRAM-SHA-512"),
      CRAM_SHA_384("CRAM-SHA-384"),
      CRAM_SHA_256("CRAM-SHA-256"),
      CRAM_SHA_1("CRAM-SHA-1"),
      CRAM_MD5("CRAM-MD5"),
      PLAIN("PLAIN"),
      LOGIN("LOGIN");
      
      private final String name;
      
      private AuthMech(String name) {
         this.name = name;
      }
      
      public String getName() {
         return name;
      }
      
      public static AuthMech getAuthMech(String name) {
         
         name = name.replace('-', '_');
         try {
            return AuthMech.valueOf(name);
         } catch (IllegalArgumentException iae) {
            return null;
         }
      }
      
      public static String[] getActiveAuthMechs(ConfigurationManager configurationManager) {
         StringBuilder sb = new StringBuilder(30);
         AuthMech[] am = AuthMech.values();
         if (configurationManager.isGSSEnabled()) {
            sb.append(AuthMech.GSSAPI.getName()).append(",");
         }
         if (configurationManager.getSCRAMMembers() != null) {
            for (int i = 1; i < 5; i++) {
               if (configurationManager.getSCRAMMembers().contains(am[i].getName())) {
                  sb.append(am[i].getName()).append(",");
               }
            }
         }
         if (configurationManager.isDigestMD5Enabled()) {
            sb.append(AuthMech.DIGEST_MD5.getName()).append(",");
         }
         if (configurationManager.getCRAMMembers() != null) {
            for (int i = 6; i < 11; i++) {
               if (configurationManager.getCRAMMembers().contains(am[i].getName())) {
                  sb.append(am[i].getName()).append(",");
               }
            }
         }
         sb.append(AuthMech.PLAIN.getName()).append(",").append(AuthMech.LOGIN.getName());
         return sb.toString().split(",");
      }
   }
}
