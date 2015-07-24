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

package com.ericdaugherty.mail.server.services.smtp.client.auth.impl;

//Java import
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

//Encoding imports
import org.apache.commons.codec.binary.Base64;


//Local import
import com.ericdaugherty.mail.server.errors.AuthenticationException;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class ClientPLAINSMTPAuthenticator extends AbstractClientSMTPAuthenticator{
   
   private String response;
   
   public ClientPLAINSMTPAuthenticator(SaslClient saslClient) {
      super(saslClient);
   }
   
   @Override
   public String generateResponse(String replyCode, String challenge) throws AuthenticationException {
      
      if (completed) throw new AuthenticationException("Authentication already completed.");
      if (failed) throw new AuthenticationException("Authentication already tried and failed.");
      
      switch(stage) {
         case INIT: {
            
            try {
               byte[] responseData = saslClient.evaluateChallenge(null);
               response = new String(Base64.encodeBase64(responseData), usAsciiCharset);
               stage = STAGE.FIRST;
               return "AUTH PLAIN "+response;
            }
            catch (SaslException se) {
               failed = true;
               throw new AuthenticationException();
            }
         }
         case FIRST: {
            if (replyCode.length() != 0) {
                char firstdigit = replyCode.charAt(0);
                if (firstdigit == THREE) {
                   stage = STAGE.SECOND;
                   return response;
                }
                else if (firstdigit == TWO) {
                   completed = true;
                   return null;
                }
            }
            throw new AuthenticationException();
         }
         case SECOND: {
            if (replyCode.length() != 0) {
                char firstdigit = replyCode.charAt(0);
                if (firstdigit == TWO) {
                   completed = true;
                   return null;
                }
            }
            throw new AuthenticationException();
         }
         default: {
            throw new AssertionError("Undefined Authentication Stage");
         }
      }
   }
}
