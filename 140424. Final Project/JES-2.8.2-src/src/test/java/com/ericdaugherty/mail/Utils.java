/******************************************************************************
 * This program is a 100% Java Email Server.
 ******************************************************************************
 * Copyright (c) 2001-2013, Eric Daugherty (http://www.ericdaugherty.com)
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

package com.ericdaugherty.mail;

//Java Imports
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;

//Local Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.utils.FileUtils;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.crypto.digest.JESMessageDigest;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class Utils {
   
   public static String getSupportedCiphers() {
      
      String[] enabledCiphers = {"TLS_DHE_RSA_WITH_AES_256_CBC_SHA","TLS_DHE_DSS_WITH_AES_256_CBC_SHA","TLS_RSA_WITH_AES_256_CBC_SHA",
                                 "TLS_DHE_RSA_WITH_AES_128_CBC_SHA","TLS_DHE_DSS_WITH_AES_128_CBC_SHA","TLS_RSA_WITH_AES_128_CBC_SHA"};
      SSLContext ctx;
      try {
         ctx = SSLContext.getInstance("TLS");
         ctx.init(null, null, null);
         String [] dcs = ctx.getServerSocketFactory().getDefaultCipherSuites();
         List<String> temp = new ArrayList<String>(enabledCiphers.length);
         for (String cipher:enabledCiphers) {
            for (String dCipher:dcs) {
               if (cipher.equals(dCipher)) {
                  temp.add(dCipher);
                  break;
               }
            }
         }
         enabledCiphers = temp.toArray(new String[temp.size()]);
      }
      catch (GeneralSecurityException gse) {
         List<String> temp = new ArrayList<String>(enabledCiphers.length);
         for (String cipher:enabledCiphers) {
            if (cipher.indexOf("ECDH")==-1) {
               temp.add(cipher);
            }
         }
         enabledCiphers = temp.toArray(new String[temp.size()]);
      }
      StringBuilder sb = new StringBuilder();
      for (String ec:enabledCiphers) {
         sb.append(ec).append(" ");
      }
      sb.deleteCharAt(sb.length()-1);
      return sb.toString();
   }

   public static void copyFiles(File directory, File target) throws IOException{
      File[] allTestFiles = directory.listFiles();
      boolean dirMade;
      for (File allTestFile : allTestFiles) {
         if (allTestFile.isDirectory()) {
            String newSubDir = allTestFile.getPath();
            newSubDir = newSubDir.substring(newSubDir.lastIndexOf(File.separator)+1);
            File targetDir = new File(target, newSubDir);
            dirMade = targetDir.mkdir();
            if (!dirMade) {
               throw new IOException("Directory "+targetDir.getName()+" could not be created.");
            }
            copyFiles(allTestFile, targetDir);
         } else {
            FileUtils.copyFile(allTestFile, new File(target, allTestFile.getName()));
         }
      }
   }

   public static void deleteFiles(File directory) throws IOException{
      deleteFiles(directory, false);
   }

   public static void deleteFiles(File directory, boolean all) throws IOException{
      File[] allTestFiles = directory.listFiles();
      for (File allTestFile : allTestFiles) {
         if (allTestFile.isDirectory()) {
            //Don't delete the database folder
            if (allTestFile.getName().equals("JES") && ! all) {
               continue;
            }
            deleteFiles(allTestFile);
            if (!allTestFile.delete()) {
               System.err.println("unable to delete " + allTestFile);
            }
         } else {
            if (allTestFile.getName().toLowerCase().startsWith("derby")) {
               continue;
            }
            if (!allTestFile.delete()) {
               System.err.println("unable to delete " + allTestFile);
            }
         }
      }
   }

   public static void deleteFilesOnExit(File directory) throws IOException{
      File[] allTestFiles = directory.listFiles();
      for (File allTestFile : allTestFiles) {
         if (allTestFile.isDirectory()) {
            deleteFilesOnExit(allTestFile);
            allTestFile.deleteOnExit();
         } else {
            allTestFile.deleteOnExit();
         }
      }
   }

   public static boolean mkDir(File directory) {
      
      boolean dirMade = false;
      if (!directory.exists()) {
         dirMade = directory.mkdir();
         if (!dirMade) {
            throw new RuntimeException("Unable to create a "+directory.getName()+" directory. Aborting...");
         }
      }
      return dirMade;
   }

   public static byte[] getOriginalMD5(File input) throws IOException {

      InputStream is = null;
      byte[] result = new byte[16];
      int count = 0;
      try {
         is = new FileInputStream(input);
         int nextByte;
         for (;count<16;) {
            nextByte = is.read();
            result[count++] = (byte)(nextByte&0xff);
         }
         return result;
      }
      finally {
         IOUtils.close(is);
      }
   }

   public static byte[] getDerivedMD5(File input) throws IOException, GeneralSecurityException{
      MessageDigest md = JESMessageDigest.getInstance("MD5");
      InputStream is = null;
      try {
         is = new FileInputStream(input);
         int nextByte;
         outer:
         while((nextByte=is.read())!=-1) {
            if (nextByte==0x0a||nextByte==0x0d) continue;
            for (int i=0;i<initialField.length;i++) {
               if (nextByte!=initialField[i]) continue outer;
               if ((nextByte=is.read())==-1) {
                  throw new IOException("Reached end of file before discovering initial field");
               }
            }
            for (int i=0;i<initialField.length;i++) {
               md.update((byte)(initialField[i]&0xff));
            }
            break;
         }
         while((nextByte=is.read())!=-1) {
            if (nextByte==0x0a||nextByte==0x0d) continue;
            md.update((byte)(nextByte&0xff));
         }
         return md.digest();
      }
      finally {
         IOUtils.close(is);
      }
   }

   private static final byte[] initialField = "X-Priority: Normal".getBytes(ConfigurationManager.getUtf8Charset());
}
