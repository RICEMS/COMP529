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

package com.ericdaugherty.mail.server.utils;

//Java Imports
import java.io.*;
import java.util.*;

//Local Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.services.smtp.support.Utils;

/**
 * This class is used to convert SMTP and local messages and password hashes from
 * version 1.6.1 to version 2.x format
 *
 * @author Andreas Kyrmegalos
 */
public class Migrate {

   public static void main(String[] args) {
      if (args.length!=2) {
         throw new RuntimeException("Two arguments, the first specifying the JES 1.6.1 " +
               "install dir and the second defining a temporary directory to store the " +
               "converted items into, are required.");

      }
      File[] smtpMessages = new File(args[0], "smtp").listFiles();

      if (smtpMessages.length!=0) {
         File targetSMTP = new File(args[1], "smtp");
         if (!targetSMTP.mkdirs()) {
            throw new RuntimeException("Unable to create the temporary smtp directory, aborting...");
         }
         for (File smtpMessage : smtpMessages) {
            String uid;
            File messageFile;
            do {
               StringBuilder sb = new StringBuilder(8);
               for (int j=0;j<8;j++) {
                  sb.append(characters.charAt(random.nextInt(16)));
               }

               uid = sb.toString();
               messageFile = new File(targetSMTP, "smtp"+uid+".ser");
               if (!messageFile.exists() && !new File(targetSMTP, "smtp"+uid+".ser").exists()) break;
            }while(true);
            BufferedReader reader = null;
            FileWriter writer = null;
            try {
               reader = new BufferedReader(new FileReader(smtpMessage));
               writer = new FileWriter(messageFile);
               reader.readLine();
               String line;
               writer.write("X-JES-File-Version: "+Utils.FILE_VERSION);
               writer.write(EOL);
               writer.write("X-JES-UID: "+uid);
               writer.write(EOL);
               line = reader.readLine();
               writer.write("X-JES-MAIL-FROM: "+line);
               writer.write(EOL);
               line = reader.readLine();
               writer.write("X-JES-RCPT-TO: "+line);
               writer.write(EOL);
               line = reader.readLine();
               writer.write("X-JES-Date: "+line);
               writer.write(EOL);
               writer.write("X-JES-8bitMIME: false");
               writer.write(EOL);
               line = reader.readLine();
               writer.write("X-JES-Delivery-Date: "+line);
               writer.write(EOL);
               line = reader.readLine();
               writer.write("X-JES-Delivery-Count: "+line);
               writer.write(EOL);
               line = reader.readLine();
               while (line!=null) {
                  writer.write(line);
                  writer.write(EOL);
                  line = reader.readLine();
               }
            } catch (IOException ioe) {
               System.out.println("Unable to convert message " + smtpMessage);
            } finally {
               IOUtils.close(reader);
               IOUtils.close(writer);
            }
         }
      }

      File users = new File(args[0], "conf"+File.separator+"user.conf");
      Properties properties = new Properties();
      FileInputStream fis = null;
      try {
         fis = new FileInputStream(users);
         properties.load(fis);
      }
      catch (IOException ioe) {
        throw new RuntimeException("Unable to load users.conf");
      }
      finally {
         IOUtils.close(fis);
      }

      Properties converted = new Properties();
      Enumeration keys = properties.keys();
      String user, pwd;
      while (keys.hasMoreElements()) {
         user = (String)keys.nextElement();
         pwd = properties.getProperty(user);
         converted.put(user, (pwd.length()==60?"{SHA}":"")+properties.getProperty(user));
      }
      File confDirectory = new File(args[1], "conf");
      if (!confDirectory.mkdirs()) {
         throw new RuntimeException("Unable to create the temporary conf directory, aborting...");
      }
      
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(new File(confDirectory, "user.conf"));
         converted.store(fos, ConfigurationManager.USER_PROPERTIES_HEADER);
      }
      catch (IOException ioe) {
         System.out.println("Unable to load user.conf");
      }
      finally {
         IOUtils.close(fos);
      }

      File[] userMBXs = new File(args[0], "users").listFiles();

      if (userMBXs.length != 0) {
         File targetLocal = new File(args[1], "users");
         if (!targetLocal.mkdirs()) {
            throw new RuntimeException("Unable to create the temporary smtp directory, aborting...");
         }
         FilenameFilter ff = new FilenameFilter() {
            @Override
            public boolean accept(File directory, String filename) {
               return filename.endsWith(".jmsg");

            }
         };
         for (File userMBX : userMBXs) {
            if (userMBX.isDirectory()) {
               File[] userMessages = userMBX.listFiles(ff);
               if (userMessages.length>0) {
                  File targetUser = new File(targetLocal, userMBX.getName());
                  if (!targetUser.mkdirs()) {
                     throw new RuntimeException("Unable to create temporary user directory, aborting...");
                  }
                  for (File userMessage : userMessages) {
                     File messageFile;
                     do {
                        StringBuilder sb = new StringBuilder(8);
                        for (int k=0;k<8;k++) {
                           sb.append(characters.charAt(random.nextInt(16)));
                        }

                        String uid = sb.toString();
                        messageFile = new File(targetUser, uid+".loc");
                        if (!messageFile.exists() && !new File(targetUser, uid+".loc").exists()) break;
                     }while(true);
                     try {
                        FileUtils.copyFile(userMessage, messageFile);
                     }catch (IOException ioe) {
                        throw new RuntimeException("Unable to create temporary user message, aborting...", ioe);
                     }
                  }
               }
            }
         }
      }

      System.out.println("All convertion operations completed successfully");

   }

   private static final String EOL = System.getProperty("line.separator");
   private static final String characters = "fedcba9876543210";
   private static final Random random = new Random();
}
