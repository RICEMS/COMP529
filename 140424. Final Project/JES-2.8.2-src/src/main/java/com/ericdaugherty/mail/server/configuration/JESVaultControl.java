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
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.callback.*;

//Log imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.Mail;
import static com.ericdaugherty.mail.server.configuration.Utils.*;
import static com.ericdaugherty.mail.server.configuration.ConfigurationParameterConstants.*;
import com.ericdaugherty.mail.server.crypto.scrypt.Scrypt;
import com.ericdaugherty.mail.server.errors.VaultException;
import com.ericdaugherty.mail.server.info.User;
import com.ericdaugherty.mail.server.utils.*;
import com.xlat4cast.jes.crypto.digest.JESMessageDigest;

//Other imports
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Andreas Kyrmegalos
 */
class JESVaultControl {

   /** Logger */
   private static final Log log = LogFactory.getLog(JESVaultControl.class);
   private static JESVaultControl instance;
   private final Charset utf8Charset = ConfigurationManager.getUtf8Charset();
   private final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();
   private final ConfigurationManager cm = ConfigurationManager.getInstance();
   private final boolean saveMaster;
   private final String secDir;
   private final boolean limitedCryptography;
   private final Control control = new ScryptControl();
   private final UserPassControl upc;
   private JESVault jesVault;
   
   static synchronized void initialize() throws GeneralSecurityException{
      if (instance == null) {
         //Never called from more than one Threads. No worries.
         instance = new JESVaultControl();
      }
   }

   static JESVaultControl getInstance() {
      return instance;
   }

   public static void shutdown() {

      instance = null;
   }

   private JESVaultControl() throws GeneralSecurityException{

      this.saveMaster = cm.isPersistMaster();
      this.secDir = cm.getSecurityDirectory();
      this.limitedCryptography = cm.isLimitedCryptography();
      this.upc = cm.getBackEndType() == BackEndType.FILE ? new FileUserPassControl() : new DbUserPassControl();
   }

   char[] getPassword(String id) {

      String lowerCaseId = id.toLowerCase();
      if (lowerCaseId.equals(BACKEND_DB_USERNAME)
            || lowerCaseId.equals(GUI_DB_USERNAME)) {
         lowerCaseId = id.substring(0, id.lastIndexOf('.'));
         Iterator<String> iter = jesVault.vaultEntries.keySet().iterator();
         String key;
         while (iter.hasNext()) {
            key = iter.next();
            if (key.toLowerCase().startsWith(lowerCaseId)) {
               StringBuilder sb = new StringBuilder(key.substring(lowerCaseId.length() + 1));
               sb.append('\u0000');
               sb.append(jesVault.getPassword(key));
               char[] password = new char[sb.length()];
               sb.getChars(0, sb.length(), password, 0);
               return password;
            }
         }
         return null;
      }
      return jesVault.getPassword(id);
   }

   public void setUserPassword(User user, char[] persistedPassword) {
      upc.setUserPassword(user, persistedPassword);
   }

   public char[] encryptUserPassword(char[] password) {
      return upc.encryptUserPassword(password);
   }

   /* 
    * This method is used to put a kerberos principal or keystore (user defined) password
    */
   void addIdentityPassword(String identity, char[] pwd) {
      jesVault.addIdentityPassword(identity, pwd);
   }

   void addPrincipals(List<String> principals) {

      Iterator<String> keys = jesVault.vaultEntries.keySet().iterator();
      String key;
      while (keys.hasNext()) {
         key = keys.next();
         if (key.equals("keystore") || isDBStoreKey(key) || isDBAccessKey(key)) {
            continue;
         }
         if (!principals.contains(key)) {
            jesVault.vaultEntries.get(key).clearPass();
            keys.remove();
         }
      }
   }

   boolean containsKey(String key) {
      return jesVault.vaultEntries.containsKey(key);
   }

   void loadPasswords() {
      control.loadPasswords();
   }

   void savePasswords() {
      control.savePasswords();
   }

   private abstract class UserPassControl {

      private final Cipher cipher;

      private UserPassControl() throws GeneralSecurityException {
         cipher = Cipher.getInstance("AES/CTR/NoPadding");
      }

      public abstract void setUserPassword(User user, char[] persistedPassword);

      public char[] encryptUserPassword(char[] password) {

         try {
      
            CharBuffer cb = CharBuffer.wrap(password);
            ByteBuffer bb;
            synchronized(control) {
               bb = utf8Charset.encode(cb);
            }
            byte[] encoded = new byte[bb.remaining()];
            bb.get(encoded);

            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            int keyId = sr.nextInt(64);
            SecretKeySpec key = new SecretKeySpec(ByteUtils.toByteArray(jesVault.vaultEntries.get("dbStore" + keyId).getAutoPass()), "AES");
            byte[] ivBytes = new byte[cm.isLimitedCryptography() ? 16 : 32];
            sr.nextBytes(ivBytes);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            byte[] hash;
            
            MessageDigest md = JESMessageDigest.getInstance("SHA-256");
            md.update((byte) ivBytes.length);
            md.update((byte) keyId);
            md.update(ivBytes);

            synchronized (upc) {
               cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
               encoded = cipher.doFinal(encoded);
            }

            hash = md.digest(encoded);

            byte[] output = new byte[2 + ivBytes.length + encoded.length + 32];
            output[0] = (byte) ivBytes.length;
            output[1] = (byte) keyId;
            System.arraycopy(ivBytes, 0, output, 2, ivBytes.length);
            System.arraycopy(encoded, 0, output, 2 + ivBytes.length, encoded.length);
            System.arraycopy(hash, 0, output, 2 + ivBytes.length + encoded.length, hash.length);
            
            bb = ByteBuffer.wrap(Base64.encodeBase64(output));
            synchronized(control) {
               cb = utf8Charset.decode(bb);
            }
            char[] encPass = new char[cb.remaining()];
            cb.get(encPass);
            
            return encPass;
         } catch (GeneralSecurityException ex) {
            log.error("An error was generated while encrypting a user password.", ex);
            return null;
         }
      }

      protected final char[] getPasswordEntry(char[] persistedPassword) {

         try {
            
            CharBuffer cb = CharBuffer.wrap(persistedPassword);
            ByteBuffer bb;
            synchronized(control) {
               bb = utf8Charset.encode(cb);
            }
            byte[] input = new byte[bb.remaining()];
            bb.get(input);
            
            byte[] decoded = Base64.decodeBase64(input);
            int ivLength = decoded[0];

            SecretKeySpec key = new SecretKeySpec(ByteUtils.toByteArray(jesVault.vaultEntries.get("dbStore" + decoded[1]).getAutoPass()), "AES");
            byte[] ivBytes = new byte[ivLength];
            System.arraycopy(decoded, 2, ivBytes, 0, ivLength);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            byte[] hash = new byte[32];
            System.arraycopy(decoded, decoded.length - 32, hash, 0, 32);

            MessageDigest md = JESMessageDigest.getInstance("SHA-256");
            md.update(decoded, 0, 2 + ivLength);
            md.update(decoded, 2 + ivLength, decoded.length - 2 - ivLength - 32);

            if (!Arrays.equals(md.digest(), hash)) {
               return null;
            }

            synchronized (upc) {
               cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
               decoded = cipher.doFinal(decoded, 2 + ivLength, decoded.length - 2 - ivLength - 32);
            }
            
            bb = ByteBuffer.wrap(decoded);
            synchronized(control) {
               cb = utf8Charset.decode(bb);
            }
            char[] decPass = new char[cb.remaining()];
            cb.get(decPass);
            
            return decPass;
         } catch (GeneralSecurityException ex) {
            log.error("An error was generated while decrypting a user password.", ex);
            return null;
         }

      }
   }

   private final class FileUserPassControl extends UserPassControl {
      
      private FileUserPassControl() throws GeneralSecurityException{}

      @Override
      public final void setUserPassword(User user, char[] persistedPassword) {
         
         int offset = persistedPassword.length<5?0:new String(persistedPassword, 0, 5).equals(ENC_S)?5:0;

         if (offset == 5) {
            
            char[] pass = new char[persistedPassword.length-5];
            System.arraycopy(persistedPassword, 5, pass, 0, persistedPassword.length-5);
            
            pass = getPasswordEntry(pass);
            if (pass == null) {
               user.setPassword(null);
            } else {
               
               char[] password = new char[pass.length+5];
               System.arraycopy(ENC_C, 0, password, 0, 5);
               System.arraycopy(pass, 0, password, 5, pass.length);
               user.setPassword(password);
            }
         } else {
            user.setPassword(persistedPassword);
         }
      }
   }

   private final class DbUserPassControl extends UserPassControl {

      private DbUserPassControl() throws GeneralSecurityException{}

      @Override
      public final void setUserPassword(User user, char[] persistedPassword) {

         user.setPassword(getPasswordEntry(persistedPassword));
      }
   }

   private abstract class Control {

      abstract void loadPasswords();

      abstract void savePasswords();
   }

   private void passwordpopup(final VaultPassword vp) {

      final Object lock = new Object();
      
      PasswordPopup ppu = new PasswordPopup(vp, "Please enter master password:", lock, PasswordStrengthChecker.defaultLimits);

      synchronized (lock) {
         java.awt.EventQueue.invokeLater(new PasswordPopup.PasswordPopupRunnable(ppu));
         while(ppu.isWorking() && lock()) {
            try {
               lock.wait(5*1000L);
            } catch (InterruptedException ex) {
               log.error("There was an error while retrieving the master password");
               throw new RuntimeException("There was an error while retrieving the master password", ex);
            }
         }
      }
   }
   
   private boolean lock() {
      Mail mail = Mail.getInstance();
      if (mail == null)
         return false;
      return !mail.isShuttingDown();
   }

   private final class ScryptControl extends Control {

      private byte[] master, newMaster;

      @Override
      void loadPasswords() {

         Map<String, char[]> pwds = new HashMap<String, char[]>();
         Map<String, char[]> newPwds = new HashMap<String, char[]>();
         
         File sealerFile = new File(secDir, "pwds.sea");
         File pwdFile = new File(secDir, "password");
         if (pwdFile.exists()) {
            Properties properties = null;
            DelimitedInputStream dis = null;
            try {
               dis = new DelimitedInputStream(new FileInputStream(pwdFile), 2048, true);
               JESProperties jesProperties = new JESProperties(dis);
               jesProperties.load();
               properties = jesProperties.getProperties();
            } catch (IOException e) {
               log.error(e.getMessage());
               throw new RuntimeException(e.getMessage(), e);
            } finally {
               IOUtils.close(dis);
            }
            Enumeration enumeration = properties.keys();
            String key, lowerCaseKey, pass;
            Charset utf8Charset = ConfigurationManager.getUtf8Charset();
            while (enumeration.hasMoreElements()) {
               key = ((String) enumeration.nextElement());
               lowerCaseKey = key.toLowerCase();
               pass = properties.getProperty(key);
               if (pass == null || pass.length() == 0) {
                  if (lowerCaseKey.startsWith("new.")) {

                     key = key.substring(4);
                     if (!Boolean.valueOf(PasswordStrengthChecker.checkStrength(key).get("success"))) {
                        log.error("The supplied password is weak. Please check the docs for details");
                        throw new RuntimeException("The supplied password is weak. Please check the docs for details");
                     }
                     newMaster = key.getBytes(utf8Charset);
                  } else {

                     if (!Boolean.valueOf(PasswordStrengthChecker.checkStrength(key).get("success"))) {
                        log.error("The supplied password is weak. Please check the docs for details");
                        throw new RuntimeException("The supplied password is weak. Please check the docs for details");
                     }
                     master = key.getBytes(utf8Charset);
                  }
               } else if (lowerCaseKey.startsWith("new.")) {
                  lowerCaseKey = lowerCaseKey.substring(4);
                  if (lowerCaseKey.startsWith(BACKEND_DB_USERNAME.substring(0, BACKEND_DB_USERNAME.lastIndexOf('.')))) {
                     key = key.substring(4);
                     key = BACKEND_DB_USERNAME.substring(0, BACKEND_DB_USERNAME.lastIndexOf('.')) + key.substring(key.lastIndexOf('.'));
                     newPwds.put(key, pass.toCharArray());
                  } else if (lowerCaseKey.startsWith(GUI_DB_USERNAME.substring(0, GUI_DB_USERNAME.lastIndexOf('.')))) {
                     key = key.substring(4);
                     key = GUI_DB_USERNAME.substring(0, GUI_DB_USERNAME.lastIndexOf('.')) + key.substring(key.lastIndexOf('.'));
                     newPwds.put(key, pass.toCharArray());
                  } else {
                     newPwds.put(key.substring(4), pass.toCharArray());
                  }
               } else {
                  if (lowerCaseKey.startsWith(BACKEND_DB_USERNAME.substring(0, BACKEND_DB_USERNAME.lastIndexOf('.')))) {
                     key = BACKEND_DB_USERNAME.substring(0, BACKEND_DB_USERNAME.lastIndexOf('.')) + key.substring(key.lastIndexOf('.'));
                  } else if (lowerCaseKey.startsWith(GUI_DB_USERNAME.substring(0, GUI_DB_USERNAME.lastIndexOf('.')))) {
                     key = GUI_DB_USERNAME.substring(0, GUI_DB_USERNAME.lastIndexOf('.')) + key.substring(key.lastIndexOf('.'));
                  }
                  pwds.put(key, pass.toCharArray());
               }
            }
            if (newMaster != null && master != null && Arrays.equals(master, newMaster)) {
               log.error("The new master password must differ from the old one");
               throw new RuntimeException("The new master password must differ from the old one");
            }

            boolean fileGone;
            fileGone = pwdFile.delete();
            if (!fileGone) {
               if (log.isDebugEnabled()) {
                  log.debug("File " + pwdFile + " was not deleted.");
               }
            }
         }

         File legacy = new File(secDir, "pwds.dat");
         //The legacy vault exists. An upgrade is in order
         if (legacy.exists()) {
            java.security.Key secretKey = null;
            BufferedReader br = null;
            try {
               br = new BufferedReader(new InputStreamReader(new FileInputStream(sealerFile), "US-ASCII"));
               String line = br.readLine();
               if (line == null)
                  throw new RuntimeException("Expected entry in pwds.dat not found.");
               secretKey = new SecretKeySpec(Base64.decodeBase64(line.getBytes(ConfigurationManager.getUsAsciiCharset())), "AES");

            } catch (IOException e) {
               log.error(e.getMessage(), e);
               throw new RuntimeException(e.getMessage(), e);
            } finally {
               IOUtils.close(br);
            }

            if (secretKey != null) {
            
               Map<String, VaultPassword> vaultEntries = new HashMap<String, VaultPassword>();
               Iterator<Map.Entry<String,char[]>> iter = pwds.entrySet().iterator();
               Map.Entry<String,char[]> pwdsEntry;
               VaultPassword vp;
               while (iter.hasNext()) {
                  pwdsEntry = iter.next();
                  vp = new VaultPassword();
                  vp.setUserPass(pwdsEntry.getValue());
                  vaultEntries.put(pwdsEntry.getKey(), vp);
               }

               ObjectInputStream ois = null;
               try {
                  ois = new ObjectInputStream(new FileInputStream(legacy));
                  SealedObject so = (SealedObject) ois.readObject();
                  Map<String, char[]> storedPwds = (Map<String, char[]>) so.getObject(secretKey);
                  iter = storedPwds.entrySet().iterator();
                  while (iter.hasNext()) {
                     pwdsEntry = iter.next();
                     if (!pwds.containsKey(pwdsEntry.getKey())) {
                        vp = new VaultPassword();
                        vp.setUserPass(pwdsEntry.getValue());
                        vaultEntries.put(pwdsEntry.getKey(), vp);
                     }
                  }
               } catch (Exception e) {
                  log.error(e.getMessage(), e);
                  throw new RuntimeException(e.getMessage(), e);
               } finally {
                  IOUtils.close(ois);
               }

               if (master == null) {
                  promptForMasterPassword();
               }
               if (newMaster != null && master != null) {
                  log.error("When converting to the new vault, a replacing master password cannot be supplied via the password file.");
                  throw new RuntimeException("When converting to the new vault, a replacing master password cannot be supplied via the password file.");
               }

               //We have the master password plus all passwords entries available
               //We need to generate the 3 extra passwords/salts
               //1. The automated keystore password
               //2. The password that encrypts the db stored user passwords
               //3. The shared salt used for the hashing of db stored user passwords
               //Persist everything using the scrypt approach

               addAutoPass(vaultEntries);

               JESVault jesVault = new JESVault(vaultEntries);

               Scrypt scrypt = new Scrypt();
               PrintWriter pw = null;
               try {
                  byte[] confidential = scrypt.encodeVault(jesVault, master, Scrypt.Algorithm.SHA256, limitedCryptography, 0, 0.125, 5);

                  File scryptVault = new File(secDir, "pwds.da1");
                  pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(scryptVault), "US-ASCII"));
                  pw.println(new String(Base64.encodeBase64(confidential), "US-ASCII"));
               } catch (Exception ex) {
                  log.error(ex.getLocalizedMessage());
                  throw new RuntimeException(ex);
               } finally {
                  IOUtils.close(pw);
               }
               try {
                  File backupSealerFile = new File(secDir, "pwdsBak.sea");
                  FileUtils.copyFile(sealerFile, backupSealerFile);
               } catch (IOException ex) {
                  log.error("Unable to copy the sealer file.");
                  throw new RuntimeException("Unable to copy the sealer file.", ex);
               }
            }
         }

         File scryptVault = new File(secDir, "pwds.da1");
         //From upgrade or prior startup
         if (scryptVault.exists()) {

            if (master == null) {
               if (sealerFile.exists()) {
                  BufferedReader br = null;
                  try {
                     Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();
                     br = new BufferedReader(new InputStreamReader(new FileInputStream(sealerFile), usAsciiCharset));
                     String line = br.readLine();
                     if (line == null)
                        throw new RuntimeException("Expected entry in pwds.da1 not found.");
                     master = Base64.decodeBase64(line.getBytes(usAsciiCharset));
                  } catch (IOException e) {
                     log.error(e.getMessage(), e);
                  } finally {
                     IOUtils.close(br);
                  }
               }
               if (master == null) {
                  promptForMasterPassword();
               }
            }
            Scrypt scrypt = new Scrypt();
            byte[] input = null;
            BufferedInputStream bis = null;
            try {
               bis = new BufferedInputStream(new FileInputStream(scryptVault));
               int available = bis.available();
               if (available<=0) {
                  throw new IOException("scryptVault file appears to be empty, aborting...");
               }
               input = new byte[available];
               int read = bis.read(input);
               if (read!=available) {
                  byte[] temp = new byte[read];
                  System.arraycopy(input, 0, temp, 0, read);
                  input = temp;
               }
               //Line termination correction
               if (input[read-1]==0x0a) {
                  int offset = input[read-2]==0x0d?2:1;
                  byte[] temp = new byte[read-offset];
                  System.arraycopy(input, 0, temp, 0, read-offset);
                  input = temp;
               }
            } catch (FileNotFoundException fnfe) {
               log.error(fnfe.getLocalizedMessage());
               throw new RuntimeException(fnfe.getLocalizedMessage());
            } catch (IOException ioe) {
               log.error(ioe.getLocalizedMessage());
               throw new RuntimeException(ioe.getLocalizedMessage());
            } finally {
               IOUtils.close(bis);
            }
            try {
               jesVault = scrypt.decodeVault(Base64.decodeBase64(input), master, cm.isLimitedCryptography(), 0, 0.5, 300);
               //Update the vault with the password file entries if not from upgrade
               if (!legacy.exists()) {

                  VaultPassword newVp, storedVp;

                  //Check that the stored credentials are the same as the password
                  //file ones for verification before accepting the new ones and use
                  //these new ones instead
                  Map.Entry<String,char[]> entry, oldEntry;
                  String section;
                  Iterator<Map.Entry<String,char[]>> iterA = newPwds.entrySet().iterator();
                  Iterator<Map.Entry<String,char[]>> iterB;
                  while (iterA.hasNext()) {
                     entry = iterA.next();
                     oldEntry = null;
                     section = entry.getKey().substring(0, entry.getKey().lastIndexOf('.'));
                     iterB = pwds.entrySet().iterator();
                     while (iterB.hasNext()) {
                        oldEntry = iterB.next();
                        if (oldEntry.getKey().startsWith(section)) {

                           //The old one in the password file has to match the one stored in the vault
                           if (!Arrays.equals(oldEntry.getValue(), jesVault.getPassword(oldEntry.getKey()))) {
                              log.error("Verification failed for: " + entry);
                              throw new RuntimeException("Verification failed for: " + entry);
                           }
                           break;
                        }
                     }
                     if (oldEntry == null) {
                        log.error("The old credentials are needed in order to accept the new ones: " + entry);
                        throw new RuntimeException("The old credentials are needed in order to accept the new ones: " + entry);
                     }
                     storedVp = jesVault.vaultEntries.remove(oldEntry.getKey());
                     storedVp.clearUserPass();
                     if (storedVp.getAutoPass() != null) {
                        jesVault.vaultEntries.put(entry.getKey(), storedVp);
                     }
                     iterB.remove();
                     pwds.put(entry.getKey(), entry.getValue());
                  }

                  iterB = pwds.entrySet().iterator();
                  while (iterB.hasNext()) {
                     entry = iterB.next();
                     newVp = new VaultPassword();
                     newVp.setUserPass(entry.getValue());
                     storedVp = jesVault.vaultEntries.get(entry.getKey());
                     if (storedVp != null) {
                        newVp.setAutoPass(storedVp.getAutoPass());
                     }
                     jesVault.vaultEntries.put(entry.getKey(), newVp);
                  }
               }
            }
            catch (GeneralSecurityException gse) {
               log.error(gse.getLocalizedMessage());
               throw new RuntimeException(gse.getLocalizedMessage());
            }
            catch (VaultException ve) {
               log.error(ve.getLocalizedMessage());
               throw new RuntimeException(ve.getLocalizedMessage());
            }
         } //From scratch
         else if (jesVault == null) {

            if (!newPwds.isEmpty()) {
               log.error("When starting fresh with the new vault, a replacing password cannot be supplied via the password file.");
               throw new RuntimeException("When starting fresh with the new vault, a replacing password cannot be supplied via the password file.");
            }
            Map<String, VaultPassword> vaultEntries = new HashMap<String, VaultPassword>();

            //Add the password file entries (if any)
            Iterator<Map.Entry<String,char[]>> iter = pwds.entrySet().iterator();
            Map.Entry<String,char[]> entry;
            VaultPassword vp;
            while (iter.hasNext()) {
               entry = iter.next();
               vp = new VaultPassword();
               vp.setUserPass(entry.getValue());
               vaultEntries.put(entry.getKey(), vp);
            }

            //Add the auto-generated entries
            addAutoPass(vaultEntries);

            jesVault = new JESVault(vaultEntries);
         }

      }

      @Override
      void savePasswords() {

         File sealerFile = new File(secDir, "pwds.sea");
         File scryptVault = new File(secDir, "pwds.da1");
         if (master == null) {
            if (sealerFile.exists() && scryptVault.exists()) {
               BufferedReader br = null;
               try {
                  br = new BufferedReader(new InputStreamReader(new FileInputStream(sealerFile), usAsciiCharset));
                  String line = br.readLine();
                  if (line != null)
                     master = Base64.decodeBase64(line.getBytes(usAsciiCharset));
               } catch (IOException e) {
                  log.error(e.getMessage(), e);
               } finally {
                  IOUtils.close(br);
               }
            }
            if (master == null) {
               promptForMasterPassword();
            }
         }

         Scrypt scrypt = new Scrypt();
         PrintWriter pw = null;
         try {
            byte[] confidential = scrypt.encodeVault(jesVault, newMaster == null ? master : newMaster, Scrypt.Algorithm.SHA256, limitedCryptography, 0, 0.125, 5);

            pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(scryptVault), usAsciiCharset));
            pw.println(new String(Base64.encodeBase64(confidential), usAsciiCharset));
         } catch (Exception ex) {
            log.error(ex.getLocalizedMessage());
            throw new RuntimeException(ex.getLocalizedMessage());
         } finally {
            IOUtils.close(pw);
         }
         if (!saveMaster) {

            File aFile = new File(secDir, "pwds.sea");
            if (aFile.exists()&&!aFile.delete()) {
               log.warn("File pwds.sea was not deleted.");
            }
         } 
         //saveMaster implied true
         else if (master != null) {

            try {
               pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sealerFile), usAsciiCharset));
               saveMasterPassword(pw, master);
            } catch (FileNotFoundException e) {
               log.error(e.getMessage());
               throw new RuntimeException(e.getMessage(), e);
            } finally {
               IOUtils.close(pw);
            }
         }
         File pwdsBak = new File(secDir, "pwdsBak.sea");
         if (!pwdsBak.delete()&&pwdsBak.exists()) {
            log.warn("File pwdsBak.sea was not deleted.");
         }
         File pwds = new File(secDir, "pwds.dat");
         if (!pwds.delete()&&pwds.exists()) {
            log.warn("File pwds.dat was not deleted.");
         }

         //Remove keystore/db passwords from the in-memory vault only if JES
         //will not under any circumstances restart
         if (!cm.isAllowRemoteRestart()) {
            Iterator<String> keys = jesVault.vaultEntries.keySet().iterator();
            String key;
            while (keys.hasNext()) {
               key = keys.next();
               if (isDBStoreKey(key)) {
                  continue;
               }
               jesVault.vaultEntries.get(key).clearPass();
               keys.remove();
            }
         }

         if (master!=null) {
            for (int i = 0; i < master.length; i++) {
               master[i] = 0x00;
            }
            master = null;
         }
         if (newMaster != null) {
            for (int i = 0; i < newMaster.length; i++) {
               newMaster[i] = 0x00;
            }
            newMaster = null;
         }
      }

      private void promptForMasterPassword() {

         if (java.awt.GraphicsEnvironment.isHeadless()) {
            log.error("The JVM is in headless mode but a GUI is required");
            throw new RuntimeException("A GUI is required");
         }

         final VaultPassword vaultPassword = new VaultPassword();
         CallbackHandler cbh = new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) {

               PasswordCallback pw = (PasswordCallback) callbacks[0];
               passwordpopup(vaultPassword);
               pw.setPassword(vaultPassword.getUserPass());
               vaultPassword.clearUserPass();
            }
         };
         try {
            PasswordCallback pcb = new PasswordCallback("master", false);
            cbh.handle(new Callback[]{pcb});

            if (pcb.getPassword() == null) {
               throw new Exception("A password was not entered");
            }
            ByteBuffer bb = java.nio.charset.Charset.forName("UTF-8").encode(CharBuffer.wrap(pcb.getPassword()));
            byte[] b = new byte[bb.remaining()];
            bb.get(b);
            master = b;
            pcb.clearPassword();

         } catch (Exception ex) {
            log.error("An error occured while retreiving the master password", ex);
            throw new RuntimeException("An error occured while retreiving the master password");
         }

      }

      private void addAutoPass(Map<String, VaultPassword> vaultEntries) {

         try {
            VaultPassword vp;
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            //TODO need to take limited crypto into account?
            byte[] autoKeystoreBytes = new byte[32];
            sr.nextBytes(autoKeystoreBytes);
            vp = vaultEntries.get("keystore");
            if (vp != null) {
               vp.setAutoPass(ByteUtils.toHex(autoKeystoreBytes));
            }
            //Breaks the check in Line 332 in class ConfigurationManagerGeneral
            //else {
            //   vp = new VaultPassword();
            //   vp.setAutoPass(ByteUtils.toHex(autoKeystoreBytes));
            //   vaultEntries.put("keystore", vp);
            //}

            byte[] autoDbStoreBytes;
            for (int i = 0; i < 64; i++) {
               sr = SecureRandom.getInstance("SHA1PRNG");
               autoDbStoreBytes = new byte[cm.isLimitedCryptography() ? 16 : 32];
               sr.nextBytes(autoDbStoreBytes);
               vp = new VaultPassword();
               vp.setAutoPass(ByteUtils.toHex(autoDbStoreBytes));
               vaultEntries.put("dbStore" + i, vp);
            }
         } catch (NoSuchAlgorithmException ex) {
            log.error(ex.getLocalizedMessage());
         }
      }
   }

   private boolean isDBStoreKey(String key) {
      return key.startsWith("dbStore");
   }

   private boolean isDBAccessKey(String key) {

      key = key.toLowerCase();
      if (key.startsWith(BACKEND_DB_USERNAME.substring(0, BACKEND_DB_USERNAME.lastIndexOf('.')))) {
         return true;
      } else if (key.startsWith(GUI_DB_USERNAME.substring(0, GUI_DB_USERNAME.lastIndexOf('.')))) {
         return true;
      } else {
         return false;
      }
   }

   private void saveMasterPassword(PrintWriter pw, byte[] master) {

      pw.println(new String(Base64.encodeBase64(master), usAsciiCharset));
      String os = System.getProperty("os.name").toLowerCase();
      if (os.contains("win")) {
         //Requires Windows 2000 or later.
         //The file system, of the storing device the file is persisted to, must be NTFS.
         try {
            new ProcessBuilder("cmd.exe", "/C", "cacls " + secDir
                  + File.separator + "pwds.sea /e /r Users").redirectErrorStream(true).start();
         } catch (IOException e) {
            if (log.isDebugEnabled()) {
               log.debug(e.getMessage(), e);
            }
         }
         try {
            new ProcessBuilder("cmd.exe", "/C", "cacls " + secDir
                  + File.separator + "pwds.sea /e /g " + System.getenv("COMPUTERNAME") + "\\" + System.getProperty("user.name") + ":F").redirectErrorStream(true).start();
         } catch (IOException e) {
            if (log.isDebugEnabled()) {
               log.debug(e.getMessage(), e);
            }
         }
         try {
            new ProcessBuilder("cmd.exe", "/C", "cacls " + secDir
                  + File.separator + "pwds.sea /e /r \"Authenticated Users\"").redirectErrorStream(true).start();
         } catch (IOException e) {
            if (log.isDebugEnabled()) {
               log.debug(e.getMessage(), e);
            }
         }
      } else if (os.contains("os x")) {
         //No OS X unit to test this on
         try {
            new ProcessBuilder("chmod og-rw " + secDir
                  + File.separator + "pwds.sea").redirectErrorStream(true).start();
         } catch (IOException e) {
            if (log.isDebugEnabled()) {
               log.debug(e.getMessage(), e);
            }
         }
      } else {
         try {
            new ProcessBuilder("bash", "-c", "chmod og-rw " + secDir
                  + File.separator + "pwds.sea").redirectErrorStream(true).start();
         } catch (IOException e) {
            if (log.isDebugEnabled()) {
               log.debug(e.getMessage(), e);
            }
         }
      }
   }
   
}
