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

package com.ericdaugherty.mail.server.persistence.smtp;

import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileFilter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local Imports

/**
 * A factory class to generate classes that persist SMTP messages.
 *
 * @author Andreas Kyrmegalos
 */
public final class SMTPMessagePersistenceFactory {

   /** Logger */
   private static final Log log = LogFactory.getLog(SMTPMessagePersistenceFactory.class);
   
   private static final String CHARACTERS = "zyxwvutsrqponmlkjihgfedcba9876543210";

   private static SMTPMessagePersistenceFactory instance;

   private static volatile boolean running;

   private final SMTPPersistenceEngine smtpPersistenceEngine = SMTPPersistenceEngine.FILEIO;
   
   private final SmtpGetter smtpGetter;

   public static void shutdown() {
      log.warn("SMTPMessagePersistenceFactory going offline");
      running = false;
      instance = null;
   }

   private SMTPMessagePersistenceFactory(@NonNull String[] smtpDirectories) {
      this.smtpGetter = smtpDirectories.length == 1
            ? new SingleSmtpGetter(smtpDirectories[0])
            : new RandomSmtpGetter(smtpDirectories);
   }
   
   public static void instantiate(@NonNull String[] smtpDirectories) {
      if (!running) {
         instance = new SMTPMessagePersistenceFactory(smtpDirectories);
         running = true;
      }
   }

   public static SMTPMessagePersistenceFactory getInstance() {
      return instance;
   }

   public SMTPPersistenceEngine getSMTPPersistenceEngine() {
      return smtpPersistenceEngine;
   }
   
   private static final ThreadLocal<Random> random = new ThreadLocal<Random>() {

      @Override
      protected Random initialValue() {
         return new Random();
      }
   };
    
   public static void generateSmtpUid(char[] smtpUid) {
      for (int i = 0, length = smtpUid.length;i < length;i++) {
         smtpUid[i] = CHARACTERS.charAt(random.get().nextInt(36));
      }
   }
   
   private abstract class SmtpGetter {
      
      abstract String getSmtpDirectory();
   }
   
   private class SingleSmtpGetter extends SmtpGetter {
      
      private final String smtpDirectory;
      
      private SingleSmtpGetter(String smtpDirectory) {
         this.smtpDirectory = smtpDirectory;
      }
      
      @Override
      String getSmtpDirectory() {
         return smtpDirectory;
      }
   }
   
   private class RandomSmtpGetter extends SmtpGetter {
      
      private final int length;
      private final String[] smtpDirectories;
      
      private RandomSmtpGetter(String[] smtpDirectories) {
         this.smtpDirectories = smtpDirectories;
         this.length = smtpDirectories.length;
      }
      
      @Override
      String getSmtpDirectory() {
         return smtpDirectories[random.get().nextInt(length)];
      }
   }
   
   public String getSmtpDirectory() {
      return smtpGetter.getSmtpDirectory();
   }
   
   private static final Set<String> smtpUids = new LinkedHashSet<String>(25);
   
   public static synchronized File getSafeToUseFile(File parent, String smtpUid) {
      
      File messageFile = new File(parent, "smtp" + smtpUid + ".tmp");
      if (messageFile.exists() || new File(parent, "smtp" + smtpUid + ".ser").exists())
            return null;
      return smtpUids.add(smtpUid) ? messageFile : null;
   }
   
   public static synchronized void removeUniqueId(String smtpUid) {
      if (smtpUid == null)
         return;
      smtpUids.remove(smtpUid);
   }

   public SMTPMessagePersistenceProcessor getSMTPPersistenceProccessor(SMTPMessage message) {
      IncrementalFileIOProcessor processor = new IncrementalFileIOProcessor(message);
      return processor;
   }
   
   public SMTPMessageLister getSMTPMessageLister(String[] smtpRepositories) {
      return new SMTPFileMessageLister(smtpRepositories);
   }

   public abstract class SMTPMessageLister {

      File[] smtpDirectories;

      private SMTPMessageLister(String[] smtpDirectories) {
         this.smtpDirectories = new File[smtpDirectories.length];
         int index = 0;
         for (String smtpDirectory : smtpDirectories) {
            this.smtpDirectories[index++] = new File(smtpDirectory);
         }
      }

      public abstract void populateSMTPMessageList(final Set<String> leftOverItems);
   }

   public final class SMTPFileMessageLister extends SMTPMessageLister {

      private final FileFilter fileFilter = new FileFilter(){

         @Override
         public boolean accept(File file) {
            if (file.isDirectory())
               return false;
            return file.getName().toLowerCase(ConfigurationManager.LOCALE).endsWith(".ser");
         }
      };

      private SMTPFileMessageLister(String[] smtpDirectories) {
         super(smtpDirectories);
      }

      @Override
      public void populateSMTPMessageList(final Set<String> leftOverItems) {

         if (!running) {
            return;
         } else if (!smtpDirectories[0].exists()) {
            if (log.isDebugEnabled())
               log.debug("SMTP DIRECTORY DOES NOT EXIST!");
            return;
         }
         int length = smtpDirectories.length;
         
         for (int index = 0;index < length;index++) {

            File[] files = smtpDirectories[index].listFiles(fileFilter);
            if (files == null)
               continue;
            int numFiles = files.length;
            for(int index2 = 0; index2 < numFiles; index2++) {
               String path = files[index2].getPath();
               leftOverItems.add(path);
            }
         }
      }
   }

   public enum SMTPPersistenceEngine {
      FILEIO
   }
}
