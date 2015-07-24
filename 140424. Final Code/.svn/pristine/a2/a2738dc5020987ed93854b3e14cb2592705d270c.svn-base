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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import javax.security.sasl.SaslException;

//Local imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.AuthenticationException;
import com.ericdaugherty.mail.server.errors.MalformedBase64ContentException;
import com.ericdaugherty.mail.server.services.pop3.Pop3Processor;

//Other imports
import org.apache.commons.codec.binary.Base64;

/**
 * Verify client authentication using SASL SCRAM-SHA-*.
 *
 * @author Andreas Kyrmegalos
 */
public class SCRAMServerMode extends PlainServerMode {
   
   private final String authMech;
   
   private String authenticationID;
   
   private String serverNonce;
   
   private byte[] salt;
   
   private byte[] challengeBytes;
   
   private String clientFirstMessageBare, serverFirstMessage, clientFinalMessageWithoutProof;
   
   public SCRAMServerMode(boolean smtp, String authMech) {
      super(smtp);
      this.authMech = authMech;
   }
   
   private List<char[]> getResponseList(byte[] responseBytes) {
      
      List<char[]> responseList = new ArrayList<char[]>();
      
      ByteBuffer bb = ByteBuffer.wrap(responseBytes);
      CharBuffer cb = utf8Charset.decode(bb);
      char[] response = new char[cb.remaining()];
      cb.get(response);
      
      char[] entry;
      char delimiter = ',';
      int previousPos = -1;
      for (int pos=0;pos<response.length;pos++) {
         if (response[pos]==delimiter) {
            entry = new char[pos-previousPos-1];
            System.arraycopy(response, pos+1, entry, 0, pos-previousPos-1);
            responseList.add(entry);
            previousPos = pos;
         }
         else if(pos==response.length-1) {
            entry = new char[pos-previousPos];
            System.arraycopy(response, pos+1, entry, 0, pos-previousPos);
            responseList.add(entry);
         }
      }
      return responseList;
   }
   
   @Override
   public byte[] evaluateResponse(byte[] responseBytes) throws SaslException {
      
      if (completed) throw new SaslException("Authentication already completed.");
      if (failed) throw new SaslException("Authentication already tried and failed.");
      
      try {
         List<char[]> response = getResponseList(responseBytes);
               
         if (challengeBytes==null) {

            clientFirstMessageBare = new String(responseBytes, utf8Charset);
            clientFirstMessageBare = clientFirstMessageBare.substring(clientFirstMessageBare.indexOf(',', clientFirstMessageBare.indexOf(',')+1)+1);

            String gs2CbindFlag = new String(response.get(0));
            if (!gs2CbindFlag.startsWith("n")) {
               if (gs2CbindFlag.startsWith("y")||gs2CbindFlag.startsWith("p=")) {
                  throw new SaslException("channel-binding-not-supported");
               }
               else {
                  throw new SaslException("Non specified channel-binding flag");
               }
            }

            //No need to saslprep, the smtp local-part only allows printable ASCII characters
            String authorizationID = new String(response.get(1));
            if (authorizationID.isEmpty()) {
               authorizationID = null;
            }
            else {
               if (!authorizationID.startsWith("a=")) {
                  throw new SaslException("other-error");
               }
               authorizationID = authorizationID.substring(2);
               //Check for improper equality sign use
               String lowercase = authorizationID.toLowerCase(ConfigurationManager.LOCALE);
               char c;
               for (int i=0;i<lowercase.length();i++) {
                  c = lowercase.charAt(i);
                  if (c!='=') continue;
                  if (i+2>=lowercase.length()||!(lowercase.substring(i,i+3).equals("=2c")||lowercase.substring(i,i+3).equals("=3d"))) {
                     throw new SaslException("invalid-username-encoding");
                  }
               }

               StringBuilder sb = new StringBuilder(authorizationID.length());
               for (int i=0;i<authorizationID.length();i++) {
                  c = authorizationID.charAt(i);
                  if (c!='=') {
                     sb.append(c);
                     continue;
                  }
                  if (lowercase.substring(i,i+3).equals("=2c")) {
                     sb.append(',');
                     i+=2;
                  }
                  if (lowercase.substring(i,i+3).equals("=3d")) {
                     sb.append('=');
                     i+=2;
                  }
               }
               this.authorizationID = getValidAuthenticationID(sb.toString());
            }

            if (new String(response.get(2)).startsWith("m")) {
               throw new SaslException("extensions-not-supported");
            }

            //No need to saslprep, the smtp local-part only allows printable ASCII characters
            String authenticationID = new String(response.get(2));
            if (!authenticationID.startsWith("n=")) {
               throw new SaslException("other-error");
            }
            authenticationID = authenticationID.substring(2);
            //Check for improper equality sign use
            String lowercase = authenticationID.toLowerCase(ConfigurationManager.LOCALE);
            char c;
            for (int i=0;i<lowercase.length();i++) {
               c = lowercase.charAt(i);
               if (c!='=') continue;
               if (i+2>=lowercase.length()||!(lowercase.substring(i,i+3).equals("=2c")||lowercase.substring(i,i+3).equals("=3d"))) {
                  throw new SaslException("invalid-username-encoding");
               }
            }
            
            StringBuilder sb = new StringBuilder(authenticationID.length());
            for (int i=0;i<authenticationID.length();i++) {
               c = authenticationID.charAt(i);
               if (c!='=') {
                  sb.append(c);
                  continue;
               }
               if (lowercase.substring(i,i+3).equals("=2c")) {
                  sb.append(',');
                  i+=2;
               }
               if (lowercase.substring(i,i+3).equals("=3d")) {
                  sb.append('=');
                  i+=2;
               }
            }
            try {
               this.authenticationID = getValidAuthenticationID(sb.toString());
            }
            catch (SaslException se) {
               if (pop3&&se.getCause().getMessage().equals(DOMAIN_REQUIRED)) {
                  se.initCause(new AuthenticationException(Pop3Processor.MESSAGE_NEED_USER_DOMAIN));
               }
               throw se;
            }

            String nonce = new String(response.get(3));
            if (!nonce.startsWith("r=")) {
               throw new SaslException("other-error");
            }
            nonce = nonce.substring(2);

            //Ignore the rest of the response

            //Generate the server nonce to append to the client nonce
            SecureRandom sr;
            try {
               sr = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException ex) {
               //SHA1PRNG is not going to go away any time soon
               sr = new SecureRandom();
            }
            int snonceCount = 8 + sr.nextInt(8);
            sb = new StringBuilder(nonce.length()+snonceCount);
            sb.append(nonce);
            for (int i=0;i<snonceCount;i++) {
               while((c=(char)(0x21+sr.nextInt(0x5e)))==0x2c){}
               sb.append(c);
            }
            serverNonce = sb.toString();

            salt = new byte[16];
            sr.nextBytes(salt);

            sb = new StringBuilder();
            sb.append("r=");
            sb.append(serverNonce);
            sb.append(',');
            sb.append("s=");
            sb.append(new String(Base64.encodeBase64(salt),usAsciiCharset));
            sb.append(",i=4096");
            serverFirstMessage = sb.toString();

            challengeBytes = serverFirstMessage.getBytes(ConfigurationManager.getUsAsciiCharset());
            return challengeBytes.clone();
         }
         else {

            clientFinalMessageWithoutProof = new String(responseBytes, utf8Charset);
            clientFinalMessageWithoutProof = clientFinalMessageWithoutProof.substring(0, clientFinalMessageWithoutProof.lastIndexOf(','));

            String channelBinding = new String(response.get(0));
            if (!channelBinding.equals("c=biws")) {
               throw new SaslException("channel-bindings-dont-match");
            }

            String nonce = new String(response.get(1));
            if (!nonce.startsWith("r=")) {
               throw new SaslException("other-error");
            }
            nonce = nonce.substring(2);

            if (!nonce.equals(serverNonce)) {
               throw new SaslException("other-error");
            }

            char[] proof = response.get(response.size()-1);
            if (proof[0]!='p'||proof[1]!='=') {
               throw new SaslException("other-error");
            }
            char[] temp = new char[proof.length-2];
            System.arraycopy(proof, 2, temp, 0, proof.length-2);
            proof = temp;
            
            CharBuffer cb = CharBuffer.wrap(proof);
            ByteBuffer bb = utf8Charset.encode(cb);
            byte[] encoded = new byte[bb.remaining()];
            bb.get(encoded);
            
            if (!Base64.isArrayByteBase64(encoded)) {
               throw new SaslException("Can not decode Base64 Content",new MalformedBase64ContentException());
            }
            
            bb = ByteBuffer.wrap(Base64.decodeBase64(encoded));
            cb = utf8Charset.decode(bb);
            proof = new char[cb.remaining()];
            cb.get(proof);

            finalizeAuthentication.finalize(authenticationID, authorizationID, proof);
            byte[] serverSignature = Base64.encodeBase64(user.getServerSignature());
            byte[] v = "v=".getBytes(ConfigurationManager.getUtf8Charset());
            byte[] challenge = new byte[v.length+serverSignature.length];
            System.arraycopy(v, 0, challenge, 0, v.length);
            System.arraycopy(serverSignature, 0, challenge, v.length, serverSignature.length);

            return challenge;
         }
      }
      catch (SaslException se) {
         failed = true;
         throw se;
      }
   }
   
   @Override
   protected boolean isPasswordValid(char[] password) {
      return user.isPasswordValid(password, authMech, new AuthenticationData(salt, 4096,
               clientFirstMessageBare+","+serverFirstMessage+","+clientFinalMessageWithoutProof));
   }

   @Override
   public String getMechanismName() {
      return authMech;
   }

   @Override
   public void dispose() throws SaslException {
      super.dispose();
      authenticationID = null;
      serverNonce = null;
      challengeBytes = null;
      clientFirstMessageBare = null;
      serverFirstMessage = null;
      clientFinalMessageWithoutProof = null;
   }
   
   public static class AuthenticationData {
      
      private final byte[] salt;
      private final int iteration;
      private final String authMessage;
      
      private AuthenticationData(byte[] salt, int iteration, String authMessage) {
         this.salt = salt.clone();
         this.iteration = iteration;
         this.authMessage = authMessage;
      }

      public byte[] getSalt() {
         return salt.clone();
      }

      public int getIteration() {
         return iteration;
      }

      public String getAuthMessage() {
         return authMessage;
      }
   }
}
