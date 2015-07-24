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

//Java imports
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.security.*;
import java.util.Arrays;

//Logging imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import static com.ericdaugherty.mail.server.configuration.Utils.*;
import com.ericdaugherty.mail.server.info.Realm;
import com.ericdaugherty.mail.server.utils.ByteUtils;
import com.xlat4cast.jes.crypto.digest.JESMessageDigest;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 *
 * @author Andreas Kyrmegalos
 */
public final class PasswordFactory {

   /** Logger Category for this class. */
   private static final Log log = LogFactory.getLog(PasswordFactory.class);
   private static PasswordFactory instance;
   

   public static synchronized void instantiate(BackEndType backEndType) {

      if (instance == null) {
         instance = new PasswordFactory(backEndType);
      }
   }

   public static void shutdown() {

      instance = null;
   }

   public static PasswordFactory getInstance() {
      return instance;
   }
   
   private final Charset iso88591Charset = ConfigurationManager.getIso88591Charset();
   private final Charset utf8Charset = ConfigurationManager.getUtf8Charset();
   
   private final PasswordHasherGetter peg;

   private PasswordFactory(BackEndType backEndType) {

      if (backEndType == BackEndType.FILE) {
         peg = new FilePasswordHasherGetter();
      } else {
         peg = new DbPasswordHasherGetter();
      }
   }

   public final PasswordHasher getPasswordHasher() {

      return peg.getHasher();
   }
   
   private final class DbPasswordHasherGetter extends PasswordHasherGetter {

      @Override
      public final PasswordHasher getHasher() {

         return new DbPasswordHasher();
      }
   }

   private final class FilePasswordHasherGetter extends PasswordHasherGetter {

      @Override
      public final PasswordHasher getHasher() {

         return new FilePasswordHasher();
      }
   }

   private abstract class PasswordHasherGetter {

      public abstract PasswordHasher getHasher();
   }

   private final class DbPasswordHasher extends AbstractPasswordHasher {

      private byte[] salt;
      
      @Override
      public final void setSalt(String salt) {

         this.salt = stringToByte(salt, 16);
      }

      @Override
      public final String getSalt() {

         return new String(byteToCharArray(salt, 48));
      }

      @Override
      public final char[] hashPassword(final char[] password) {

         try {
            
            CharBuffer cb = CharBuffer.wrap(password);
            ByteBuffer bb;
            synchronized(charset) {
               bb = charset.encode(cb);
            }
            byte[] encoded = new byte[bb.remaining()];
            bb.get(encoded);

            if (salt == null) {

               salt = new byte[16];

               SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
               sr.nextBytes(salt);
            }

            MessageDigest md = JESMessageDigest.getInstance("SHA");

            md.update(salt);
            md.update(encoded);

            byte[] hash = md.digest();
            for (int i = 0; i < (1999); i++) {
               hash = md.digest(hash);
            }
            
            char[] fromBytes = byteToCharArray(hash, 60);
            char[] output = new char[fromBytes.length+5];
            System.arraycopy(SHA_C, 0, output, 0, 5);
            System.arraycopy(fromBytes, 0, output, 5, fromBytes.length);

            return output;
         } catch (NoSuchAlgorithmException exception) {
            log.error("", exception);
            return null;
         }
      }

      @Override
      public final boolean passwordMatches(char[] storedPassword, char[] password) {

         if(storedPassword == null) {
            return false;
         }
         int offset = storedPassword.length < 5 ? 0 : new String(storedPassword,0,5).equals(SHA_S) ? 5 : 0;
         if (offset == 5) {
            if (salt == null)
               return false;
            return Arrays.equals(storedPassword, hashPassword(password));
         }
         else {
            return Arrays.equals(storedPassword, password);
         }
      }
   }

   private final class FilePasswordHasher extends AbstractPasswordHasher {

      @Override
      public final void setSalt(String salt) {
      }

      @Override
      public final String getSalt() {
         return null;
      }

      @Override
      public final char[] hashPassword(char[] password) {

         try {
            MessageDigest md = JESMessageDigest.getInstance("SHA");
            
            CharBuffer cb = CharBuffer.wrap(password);
            ByteBuffer bb;
            synchronized(charset) {
               bb = charset.encode(cb);
            }
            byte[] encoded = new byte[bb.remaining()];
            bb.get(encoded);

            //Create the encrypted Byte[]
            md.update(encoded);
            byte[] hash = md.digest();
            
            char[] fromBytes = byteToCharArray(hash, 60);
            char[] output = new char[fromBytes.length+5];
            System.arraycopy(SHA_C, 0, output, 0, 5);
            System.arraycopy(fromBytes, 0, output, 5, fromBytes.length);

            return output;
         } catch (NoSuchAlgorithmException nsae) {
            log.error("Error getting password hash - " + nsae.getMessage());
            return null;
         }
      }

      @Override
      public final boolean passwordMatches(char[] storedPassword, char[] password) {

         if(storedPassword == null) {
            return false;
         }
         int offset = new String(storedPassword,0,5).equals(ENC_S)?5:0;
         if (offset == 5) {
            char[] pass = new char[storedPassword.length-offset];
            System.arraycopy(storedPassword, 5, pass, 0, storedPassword.length-offset);
            return Arrays.equals(pass, password);
         }
         return Arrays.equals(storedPassword, hashPassword(password));
      }
   }

   private abstract class AbstractPasswordHasher implements PasswordHasher {

      @Override
      public abstract char[] hashPassword(char[] password);

      @Override
      public final char[] hashRealmPassword(String username, Realm realm, char[] password) throws GeneralSecurityException {
         
         CharsetEncoder encoder = Charset.forName(ISO_8859_1).newEncoder();
         CharBuffer cb = CharBuffer.wrap(password);
         ByteBuffer bb;
         //If all three are encoded in ISO-8859-1 use that, otherwise UTF-8
         byte[] usernameBytes = null, realmBytes = null, passwordBytes = null, colonBytes = ":".getBytes();
         boolean utf8 = false;
         try {
            usernameBytes = username.getBytes(iso88591Charset);
            realmBytes = (realm.isNullRealm()?"":realm.getUniqueName()).getBytes(iso88591Charset);
            bb = encoder.encode(cb);
            byte[] encoded = new byte[bb.remaining()];
            bb.get(encoded);
            passwordBytes = encoded;
         }
         catch (CharacterCodingException cee) {
            utf8 = true;
         }
         if (utf8) {
            usernameBytes = username.getBytes(utf8Charset);
               realmBytes = (realm.isNullRealm() ? "" : realm.getUniqueName()).getBytes(utf8Charset);
            synchronized(PasswordFactory.charset) {
               bb = PasswordFactory.charset.encode(cb);
            }
            byte[] encoded = new byte[bb.remaining()];
            bb.get(encoded);
            passwordBytes = encoded;
         }
         
         MessageDigest md = JESMessageDigest.getInstance("MD5");
         
         md.update(usernameBytes);
         md.update(colonBytes);
         md.update(realmBytes);
         md.update(colonBytes);
         md.update(passwordBytes);
         byte[] hash = md.digest();
         
         return ByteUtils.toHex(hash);
      }

      @Override
      public abstract boolean passwordMatches(char[] storedPassword, char[] password);

      //Convert the String into a byte array
      protected final byte[] stringToByte(String string, int size) {

         byte[] bytes = new byte[size];

         for (int i = 0; i < string.length(); i += 3) {

            bytes[i / 3] = Byte.parseByte("" + (Integer.parseInt(string.substring(i, i + 3)) - 128));
         }

         return bytes;

      }

      //Convert the byte array into a String
      protected final char[] byteToCharArray(byte[] hash, int size) {

         StringBuilder hashSb = new StringBuilder(size);
         String byteString;
         int byteLength;

         for (int index = 0; index < hash.length; index++) {

            byteString = String.valueOf(hash[index] + 128);

            //Pad string to 3.  Otherwise hash may not be unique.
            byteLength = byteString.length();
            switch (byteLength) {
               case 1:
                  byteString = "00" + byteString;
                  break;
               case 2:
                  byteString = "0" + byteString;
                  break;
               default:break;
            }
            hashSb.append(byteString);
         }

         char[] output = new char[hashSb.length()];
         hashSb.getChars(0, hashSb.length(), output, 0);
         return output;
      }

      //Convert the 10 based string representation of a binary string into a 16 based string
//      private String toHex(String string, int len) {
//         
//         StringBuilder sb = new StringBuilder((len/3)*2);
//         
//         for (int i = 0; i < len; i += 3) {
//            sb.append(toHex(string.substring(i, i+3)));
//         }
//         return sb.toString();
//      }

//      private String toHex(String s) {
//         int i = ((Integer.parseInt(s) - 128) << 24) >>> 24;
//
//         if (i < (byte) 16) {
//            return "0" + Integer.toString(i, 16);
//         } else {
//            return Integer.toString(i, 16);
//         }
//      }
   }
   
   public static void clear(@CheckForNull char[] password) {
      
      if (password == null)
         return;
      for (int i = 0, length = password.length;i < length;i++)
         password[i] = 0xff;
   }
   
   private static final String ISO_8859_1 = "ISO-8859-1";
   private static final String UTF_8 = "UTF-8";
   private static final Charset charset = Charset.forName(UTF_8);
}
