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

package com.ericdaugherty.mail.server.info;

//Log imports
import java.util.Arrays;
import java.util.Locale;
import java.nio.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.spec.SecretKeySpec;

//Log imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.auth.SCRAMServerMode;
import com.ericdaugherty.mail.server.auth.Saslprep;
import com.ericdaugherty.mail.server.auth.Saslprep.StringType;
import com.ericdaugherty.mail.server.auth.SaslprepException;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.utils.ByteUtils;
import com.xlat4cast.jes.crypto.PBKDF2;
import com.xlat4cast.jes.crypto.digest.JESMessageDigest;
import com.xlat4cast.jes.crypto.mac.HMACParameterSpec;
import com.xlat4cast.jes.crypto.mac.JESMac;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 *
 * @author Andreas Kyrmegalos
 */
public abstract class AbstractUser implements User {

   /** Logger */
   protected static final Log log = LogFactory.getLog(AbstractUser.class);

   private final static transient Charset charset = Charset.forName("UTF-8");

   protected EmailAddress emailAddress;
   
   private final String uniqueUsername;
   
   private int hashCode;
   
   protected char[] password;
   private EmailAddress[] forwardAddresses;
   private byte[] storedKey;
   private byte[] serverKey;
   private byte[] serverSignature;

   public AbstractUser(EmailAddress address) {
      emailAddress = address;
      
      uniqueUsername = emailAddress.getUsername().toLowerCase(ConfigurationManager.LOCALE)+'@'+address.getDomain().getUniqueName();
      
      hashCode = emailAddress.getUsername().toLowerCase(ConfigurationManager.LOCALE).hashCode();
      hashCode = 17*hashCode + emailAddress.getDomain().hashCode();
   }

   @Override
   public boolean isPasswordValid(char[] plainTextPassword, String authenticationMechanism, Object authenticationData) {
      
      if (log.isDebugEnabled()) log.debug("Authenticating User: " + getUserAdress());
      
      int offset = password.length<5?0:new String(password, 0, 5).equals(ENC_S)?5:0;
            
      if (authenticationMechanism.startsWith("CRAM")) {
         try {
            JESMac jesMac = JESMac.getInstance("Hmac"+authenticationMechanism.substring(5));
            byte[] challengeBytes = (byte[])authenticationData;
            
            CharBuffer cb = CharBuffer.wrap(password.clone(), offset, password.length-offset);
            ByteBuffer bb;
            synchronized(charset) {
               bb = charset.encode(cb);
            }
            byte[] key = new byte[bb.remaining()];
            bb.get(key);
         
            jesMac.init(new SecretKeySpec(key,"Hmac"+authenticationMechanism.substring(5)), new HMACParameterSpec());
            jesMac.update(challengeBytes,0,challengeBytes.length);
            return Arrays.equals(jesMac.doFinal(),ByteUtils.toByteArray(plainTextPassword));
         }
         catch (GeneralSecurityException ex) {
            log.error(ex);
            return false;
         }
      }
      else if (authenticationMechanism.startsWith("SCRAM")){
         
         try {
            JESMac jesMac = JESMac.getInstance("Hmac"+authenticationMechanism.substring(6));
            SCRAMServerMode.AuthenticationData authenticationDataSCRAM = (SCRAMServerMode.AuthenticationData)authenticationData;
            MessageDigest md = JESMessageDigest.getInstance(authenticationMechanism.substring(6));
            byte[] clientKey;
            if (storedKey == null) {

               char[] password = new char[this.password.length-offset];
               System.arraycopy(this.password, offset, password, 0, password.length);
               
               password = Saslprep.prepareString(password, true, StringType.STORED_STRING, false);
               
               CharBuffer cb = CharBuffer.wrap(password, 0, password.length);
               ByteBuffer bb;
               synchronized(charset) {
                  bb = charset.encode(cb);
               }
               byte[] key = new byte[bb.remaining()];
               bb.get(key);

               jesMac.init(new SecretKeySpec(key,"Hmac"+authenticationMechanism.substring(6)), new HMACParameterSpec());
               byte[] saltedPassword = new byte[jesMac.getMacLength()];
               PBKDF2 pbkdf2 = new PBKDF2();
               pbkdf2.deriveKey(jesMac, authenticationDataSCRAM.getSalt(), authenticationDataSCRAM.getIteration(), saltedPassword);

               jesMac.init(new SecretKeySpec(saltedPassword,"Hmac"+authenticationMechanism.substring(6)), new HMACParameterSpec());
               clientKey = "Client Key".getBytes();
               jesMac.update(clientKey, 0, clientKey.length);
               clientKey = jesMac.doFinal();

               storedKey = md.digest(clientKey);
            
               jesMac.init(new SecretKeySpec(saltedPassword,"Hmac"+authenticationMechanism.substring(6)), new HMACParameterSpec());
               serverKey = "Server Key".getBytes();
               jesMac.update(serverKey, 0, serverKey.length);
               serverKey = jesMac.doFinal();
            }
            
            jesMac.init(new SecretKeySpec(storedKey,"Hmac"+authenticationMechanism.substring(6)), new HMACParameterSpec());
            byte[] authMessageBytes = authenticationDataSCRAM.getAuthMessage().getBytes(ConfigurationManager.getUtf8Charset());
            
            jesMac.update(authMessageBytes, 0, authMessageBytes.length);
            clientKey = jesMac.doFinal();
            
            CharBuffer cb = CharBuffer.wrap(plainTextPassword.clone(), 0, plainTextPassword.length);
            ByteBuffer bb;
            synchronized(charset) {
               bb = charset.encode(cb);
            }
            byte[] clientProof = new byte[bb.remaining()];
            bb.get(clientProof);
            
            if (clientKey.length!=clientProof.length) {
               return false;
            }
            for (int i=0;i<clientKey.length;i++) {
               clientKey[i] ^= clientProof[i];
            }
            
            jesMac.init(new SecretKeySpec(serverKey,"Hmac"+authenticationMechanism.substring(6)), new HMACParameterSpec());
            jesMac.update(authMessageBytes, 0, authMessageBytes.length);
            serverSignature = jesMac.doFinal();
            
            return Arrays.equals(md.digest(clientKey), storedKey);
         }
         catch (SaslprepException ex) {
            log.error(ex);
            return false;
         }
         catch (GeneralSecurityException ex) {
            log.error(ex);
            return false;
         }
      }
      else {
         return false;
      }
   }
   
   @Override
   public byte[] getServerSignature() {
      return serverSignature.clone();
   }

   @Override
   public String getUsername() {
      return emailAddress.getUsername();
   }

   @Override
   public Domain getDomain() {
      return emailAddress.getDomain();
   }

   @Override
   public String getUniqueName() {
      return uniqueUsername;
   }

   @Override
   public String getUserAdress() {
      return emailAddress.getAddress();
   }

   @Override
   public EmailAddress getEmailAddress() {
      return emailAddress;
   }

   @Override
   public void setPassword(char[] password) {
      this.password = password.clone();
   }

   @Override
   public EmailAddress[] getForwardAddresses() {
      return forwardAddresses.clone();
   }

   @Override
   public void setForwardAddresses(EmailAddress[] forwardAddresses) {
      this.forwardAddresses = forwardAddresses.clone();
   }

   /**
    * Returns an array of Strings that represent email addresses to deliver
    * email to this user.  If the forwardAddresses is not null or empty,
    * this will return the forwardAddresses array.  Otherwise, this will return
    * the user's email address.
    *
    * @return array of strings that represent email addresses.
    */
   @Override
   public EmailAddress[] getDeliveryAddresses() {

      if (forwardAddresses != null && forwardAddresses.length > 0) {
         return forwardAddresses.clone();
      } else {
         return new EmailAddress[]{EmailAddress.getEmailAddress(getUsername(), getDomain())};
      }
   }

   /**
    * 
    * @return the hashCode
    */
   @Override
   public final int hashCode() {
      return hashCode;
   }

   /**
    * Two polymorphic User Objects are treated as representing
    * the same User if the case-insensitive {@link String}
    * depiction of the username and the case-insensitive {@link Domain}
    * depiction of the domain are equal for both objects.
    * 
    * @param object the object to consider whether it is meaningfully equivalent to this instance
    * @return true if this instance of Domain is equivalent to the supplied object parameter
    */
   @Override
   public final boolean equals(Object object) {

      if (object==this) return true;
      if (object==null) return false;
      if (!(object instanceof AbstractUser)) return false;
      AbstractUser that = (AbstractUser)object;
      if ((this.emailAddress.getUsername()==null&&that.emailAddress.getUsername()!=null)||
            (!this.emailAddress.getUsername().equalsIgnoreCase(that.emailAddress.getUsername()))) return false;
      if ((this.emailAddress.getDomain()==null&&that.emailAddress.getDomain()!=null)||
            (!this.emailAddress.getDomain().equals(that.emailAddress.getDomain()))) return false;
      return true;
   }
   
   @Override
   public String toString() {
      return "[user: "+emailAddress+"]";
   }
    
   static final String ENC_S = "{ENC}";
    
   static final char[] ENC_C = ENC_S.toCharArray();
    
   static final String SHA_S = "{SHA}";
    
   static final char[] SHA_C = SHA_S.toCharArray();
}
