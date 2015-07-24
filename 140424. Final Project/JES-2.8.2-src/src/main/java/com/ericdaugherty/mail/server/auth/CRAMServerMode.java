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

package com.ericdaugherty.mail.server.auth;

//Java imports
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Random;
import javax.security.sasl.SaslException;

//Local imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.AuthenticationException;
import com.ericdaugherty.mail.server.services.pop3.Pop3Processor;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class CRAMServerMode extends PlainServerMode {

   private String fqdn;
   private byte[] challengeBytes;
   private String authMech;

   public CRAMServerMode(boolean smtp, String fqdn, String authMech) {
      super(smtp);
      this.fqdn = fqdn;
      this.authMech = authMech;
   }

   @Override
   public byte[] evaluateResponse(byte[] responseBytes) throws SaslException {

      if (completed)
         throw new SaslException("Authentication already completed.");
      if (failed)
         throw new SaslException("Authentication already tried and failed.");
      
      //The server sends the initial challenge (phase I)
      if (challengeBytes == null) {

         Random random = new Random();

         StringBuilder sb = new StringBuilder();
         int randomInt;
         sb.append('<');
         randomInt = random.nextInt();
         if (randomInt<0) {
            randomInt *= -1;
         }
         sb.append(randomInt);
         randomInt = random.nextInt();
         if (randomInt<0) {
            randomInt *= -1;
         }
         sb.append(randomInt);
         sb.append('.');
         sb.append(System.currentTimeMillis());
         sb.append('@');
         sb.append(fqdn);
         sb.append('>');
         String challengeStr = sb.toString();
         challengeBytes = challengeStr.getBytes(ConfigurationManager.getUsAsciiCharset());
         return challengeBytes.clone();

      } //The server concludes the authentication process (phase II)
      else {
         
         try {
            
            ByteBuffer bb = ByteBuffer.wrap(responseBytes);
            CharBuffer cb = utf8Charset.decode(bb);
            char[] response = new char[cb.remaining()];
            cb.get(response);
         
            int lastIndexOfSpace = response.length-1;
            char WSP = ' ';
            for (;lastIndexOfSpace>=0;lastIndexOfSpace--) {
               if (response[lastIndexOfSpace]==WSP) break;
            }
            char[] password = new char[response.length-lastIndexOfSpace-1];
            System.arraycopy(response, lastIndexOfSpace+1, password, 0, password.length);
            String authenticationID = new String(response, 0, lastIndexOfSpace);
            
            if (authenticationID.indexOf(' ')!=-1) {
               throw new SaslException("The response's format doesn't comply to the CRAM specification");
            }
            try {
               authenticationID = getValidAuthenticationID(authenticationID);
            }
            catch (SaslException se) {
               if (pop3&&se.getCause().getMessage().equals(DOMAIN_REQUIRED)) {
                  se.initCause(new AuthenticationException(Pop3Processor.MESSAGE_NEED_USER_DOMAIN));
               }
               throw se;
            }
            return finalizeAuthentication.finalize(authenticationID, null, password);
         }
         catch (SaslException se) {
            failed = true;
            throw se;
         }
      }
   }
   
   @Override
   protected boolean isPasswordValid(char[] password) {
      return user.isPasswordValid(password, authMech, challengeBytes);
   }

   @Override
   public String getMechanismName() {
      return authMech;
   }

   @Override
   public void dispose() throws SaslException {
      super.dispose();
      fqdn = null;
      challengeBytes = null;
      authMech = null;
   }
}
