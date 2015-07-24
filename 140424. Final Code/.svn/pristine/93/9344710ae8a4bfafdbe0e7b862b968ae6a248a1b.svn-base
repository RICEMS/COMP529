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

package com.ericdaugherty.mail.server.configuration.cbc;

//Java imports
import java.util.List;

//Local imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;

/**
 *
 * @author Andreas Kyrmegalos
 */
public final class RetrieveDbPassword extends CBCResponseExecutor {
   
   private char[] password;
   
   public RetrieveDbPassword(List<String> lines) {
      super(lines);
   }

   @Override
   public void processLines() throws CBCResponseException {
      
      if (lines.size() != 1)
         throw new CBCResponseException("\"RetrieveDbPassword\" payload must consist of exactly one line.");
      String line = lines.get(0);
      if (line.startsWith(DB_PASSWORD)) {
         line = line.substring(DB_PASSWORD.length()).trim();
         if (line.isEmpty())
            throw new CBCResponseException("Error retrieving the password, CASE1");
         password = ConfigurationManager.getInstance().getGUIDbPassword();
         if (password == null)
            throw new CBCResponseException("Error retrieving the password, CASE2");
         StringBuilder sb = new StringBuilder();
         sb.append(password);
         int nullCharPos = sb.indexOf("\u0000");
         String username = sb.substring(0, nullCharPos);
         if (!line.equals(username)
               || nullCharPos == sb.length() - 1
               || sb.length() == username.length()) {
            clearPassword();
            throw new CBCResponseException("Error retrieving the password, CASE3");
         }
         password = new char[sb.length()-nullCharPos-1];
         sb.getChars(nullCharPos+1, sb.length(), password, 0);
      } else {
         throw new CBCResponseException("Error retrieving the password, CASE4");
      }
   }
   
   private void clearPassword() {
      if (password == null)
         return;
      for (int i = 0;i < password.length;i++) {
         password[i] = 0xff;
      }
   }

   @Override
   public String getResponse() {
      if (password == null)
         return null;
      String guiDbPassword = new String(password);
      try {
         return guiDbPassword;
      } finally {
         clearPassword();
      }
   }
}
