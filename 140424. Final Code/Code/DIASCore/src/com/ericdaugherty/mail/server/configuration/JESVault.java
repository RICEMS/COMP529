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
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class JESVault implements Serializable {

   private static final long serialVersionUID = 698575485940234820L;
   Map<String, VaultPassword> vaultEntries;

   JESVault(Map<String, VaultPassword> vaultEntries) {
      this.vaultEntries = vaultEntries;
   }

   /**
    * Retrieve the password for the specified id
    * If a user supplied password exists, this one is returned
    * Otherwise, a possibly null auto-generated one is returned
    * 
    * @return char[] the password
    */
   char[] getPassword(String id) {

      VaultPassword vp = null;
      Iterator<String> iter = vaultEntries.keySet().iterator();
      String key;
      while (iter.hasNext()) {
         key = iter.next();
         if (key.startsWith(id)) {
            vp = vaultEntries.get(id);
         }
      }
      if (vp==null) return null;
      char[] password = vp.getUserPass() != null ? vp.getUserPass() : vp.getAutoPass();
      if (password!=null) {
         password = password.clone();
      }
      return password;
   }

   /* 
    * This method used to put a kerberos principal or keystore (user defined) password
    */
   void addIdentityPassword(String identity, char[] pwd) {

      VaultPassword vp;

      // A keystore entry gets a automated key, so an entry should always exist
      if (identity.equals("keystore")) {

         vp = vaultEntries.get(identity);
         if (vp == null) {
            vp = new VaultPassword();
         }
         vp.setUserPass(pwd.clone());
      } else {
         vp = new VaultPassword();
      }

      vp.setUserPass(pwd.clone());
      vaultEntries.put(identity, vp);
   }

}
