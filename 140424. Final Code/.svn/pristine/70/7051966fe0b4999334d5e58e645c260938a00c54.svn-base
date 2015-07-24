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

package com.ericdaugherty.mail.server.configuration;

import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.derby.authentication.UserAuthenticator;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class DbAuthentication implements UserAuthenticator {

   /** Logger */
   private static final Log log = LogFactory.getLog(DbAuthentication.class);
   
   private final char[] username, password;
   private final char[] guiUsername, guiPassword;

   public DbAuthentication() {
      JESVaultControl jesVaultControl = JESVaultControl.getInstance();
      char[] username = jesVaultControl.getPassword(ConfigurationParameterConstants.BACKEND_DB_USERNAME);
      char[] guiUsername = jesVaultControl.getPassword(ConfigurationParameterConstants.GUI_DB_USERNAME);

      int pos;
      StringBuilder sb;

      sb = new StringBuilder();
      sb.append(username);
      if (sb.length()>0) {
         this.username = sb.substring(0, sb.indexOf("\u0000")).toCharArray();
         pos = sb.indexOf("\u0000");
         this.password = new char[sb.length()-pos-1];
         sb.getChars(pos+1, sb.length(), password, 0);
      }
      else {
         this.username = null;
         this.password = null;
      }
      if (sb.length()>0) {
         sb.delete(0, sb.length());
      }

      sb = new StringBuilder();
      sb.append(guiUsername);
      if (sb.length()>0) {
         this.guiUsername = sb.substring(0, sb.indexOf("\u0000")).toCharArray();
         pos = sb.indexOf("\u0000");
         this.guiPassword = new char[sb.length()-pos-1];
         sb.getChars(pos+1, sb.length(), guiPassword, 0);
      }
      else {
         this.guiUsername = null;
         this.guiPassword = null;
      }
      if (sb.length()>0) {
         sb.delete(0, sb.length());
      }
   }

   @Override
   public boolean authenticateUser(String username, String password,
           String databaseName, Properties info) throws SQLException {

      if (databaseName != null && !databaseName.equals("JES")) {

         log.warn(databaseName + " is not a valid database name.");
         return false;
      }
      if (username == null) {
         return false;
      }
      boolean match;
      match = true;
      if (username.length() != this.username.length) {
         match = false;
      }
      if (match) {
         for (int i = 0; i < username.length(); i++) {
            if (username.charAt(i) != this.username[i]) {
               match = false;
               break;
            }
         }
      }
      if (match) {
         match = true;
         if (password.length() != this.password.length) {
            match = false;
         }
         if (match) {
            for (int i = 0; i < password.length(); i++) {
               if (password.charAt(i) != this.password[i]) {
                  match = false;
                  break;
               }
            }
         }
         if (match) {

            log.info("successfully logged on to the db.");
            return true;
         }
         log.warn("The supplied credentials are invalid.");
         return false;
      }

      match = true;
      if (username.length() != this.guiUsername.length) {
         match = false;
      }
      if (match) {
         for (int i = 0; i < username.length(); i++) {
            if (username.charAt(i) != this.guiUsername[i]) {
               match = false;
               break;
            }
         }
      }
      if (match) {
         match = true;
         if (password.length() != this.guiPassword.length) {
            match = false;
         }
         if (match) {
            for (int i = 0; i < password.length(); i++) {
               if (password.charAt(i) != this.guiPassword[i]) {
                  match = false;
                  break;
               }
            }
         }
         if (match) {

            log.info("successfully logged on to the db (gui).");
            return true;
         }
         log.warn("The supplied credentials are invalid.");
         return false;
      }

      log.warn("The supplied credentials are invalid.");
      return false;
   }
}
