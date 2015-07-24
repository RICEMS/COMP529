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

import java.io.Serializable;

/**
 *
 * @author Andreas Kyrmegalos
 */
final class VaultPassword implements Serializable{
   
   private static final long serialVersionUID = 882076318126536249L;
   
   private char[] autoPass;
   private char[] userPass;

   char[] getAutoPass() {
      return autoPass;
   }

   void setAutoPass(char[] autoPass) {
      this.autoPass = autoPass;
   }

   char[] getUserPass() {
      return userPass;
   }

   void setUserPass(char[] userPass) {
      this.userPass = userPass;
   }
   
   void clearUserPass() {
      if (userPass!=null) {
         for (int i=0;i<userPass.length;i++) {
            userPass[i] = 0x00;
         }
         userPass = null;
      }
   }
   
   void clearPass() {
      if (userPass!=null) {
         for (int i=0;i<userPass.length;i++) {
            userPass[i] = 0x00;
         }
         userPass = null;
      }
      if (autoPass!=null) {
         for (int i=0;i<autoPass.length;i++) {
            autoPass[i] = 0x00;
         }
         autoPass = null;
      }
   }
   
   public String toString() {
      return "";
   }
}
