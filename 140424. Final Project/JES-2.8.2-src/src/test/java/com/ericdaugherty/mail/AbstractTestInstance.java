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
 * $Rev: $
 * $Date: $
 *
 ******************************************************************************/

package com.ericdaugherty.mail;

import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.utils.FileUtils;
import com.ericdaugherty.mail.server.utils.IOUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.util.Properties;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author Andreas Kyrmegalos
 */
public abstract class AbstractTestInstance implements TestInstance {
   
//   protected final String name;
   protected final String server;
   protected final Properties userInfo;
   protected final Properties configurationProperties;
   protected final PasswordAuthenticator senderCredentials;
   protected final int smtpPort, pop3Port;
   protected final Profile profile;
   protected final String baseDir;
   protected final File tempJESDir;

   AbstractTestInstance(TestParameters parameters, Properties userInfo, PasswordAuthenticator senderCredentials) {
//      this.name = parameters.getName();
      this.server = parameters.getServer();
      this.userInfo = userInfo;
      this.configurationProperties = parameters.getJavaMailSettings();
      this.senderCredentials = senderCredentials;
      this.smtpPort = parameters.getSmtpPort();
      this.pop3Port = parameters.getPop3Port();
      this.profile = parameters.getProfile();

      this.baseDir = System.getProperty("basedir");
      int count = 0;
      File tempJESDirTemp = null;
      do {
         tempJESDirTemp = new File(System.getProperty("java.io.tmpdir"), "jes" + count);
         if (!tempJESDirTemp.exists()) {
            if (tempJESDirTemp.mkdir()) break;
         }
         count++;
         if (count == 1000) {
            throw new RuntimeException("Unable to create a test directory. Aborting...");
         }
      } while (true);
      this.tempJESDir = tempJESDirTemp;
   }
   
   @Override
   public void setup() throws Exception {
      
      File conf = new File(tempJESDir, "conf");
      Utils.mkDir(conf);

      File resourceDir = new File(baseDir, "src" + File.separator + "test" + File.separator + "resources");
      
      File file = new File(resourceDir, "user.conf");
      FileUtils.copyFile(file, new File(conf, "user.conf"));
      file = new File(resourceDir, "realms.conf");
      FileUtils.copyFile(file, new File(conf, "realms.conf"));
      file = new File(resourceDir, "mail-instance.xml");
      FileUtils.copyFile(file, new File(conf, "mail.xml"));
      
      file = new File(resourceDir, "truststore.jks");
      FileUtils.copyFile(file, new File(tempJESDir, "truststore.jks"));
      
      File security = new File(tempJESDir, "security");
      Utils.mkDir(security);
      Utils.copyFiles(new File(resourceDir, "security"), security);
      
      resourceDir = new File(baseDir, "docs" + File.separator + "conf");
      
      file = new File(resourceDir, "dnsBWLists.conf");
      FileUtils.copyFile(file, new File(conf, "dnsBWLists.conf"));
      
      file = new File(resourceDir, "mail.xsd");
      FileUtils.copyFile(file, new File(conf, "mail.xsd"));
      
      file = new File(resourceDir, "rcptPolicy.conf");
      FileUtils.copyFile(file, new File(conf, "rcptPolicy.conf"));
      
      file = new File(resourceDir, "reverseDNS.conf");
      FileUtils.copyFile(file, new File(conf, "reverseDNS.conf"));
      
      File lib = new File(tempJESDir, "lib");
      if (!lib.mkdir() || !lib.exists()) {
         throw new RuntimeException("Unable to create folder "+lib.getName()+" under "+tempJESDir.getName()+". Aborting...");
      }
      
      File forTest = new File(baseDir, "forTest");

      Utils.copyFiles(forTest, lib);
      
      for (Object aUser : userInfo.keySet()) {
         File userTempDir = new File(tempJESDir, (String)aUser);
         if (!userTempDir.exists()) {
            if (!userTempDir.mkdir()) {
               throw new RuntimeException("Unable to create folder "+userTempDir.getName()+" under "+tempJESDir.getName()+". Aborting...");
            }
         }
      }

      File testJESFile = new File(baseDir, "pom.xml");
      BufferedReader br = null;
      String line, name = null, version = null, pckg = null;
      try {
         br = new BufferedReader(new InputStreamReader(new FileInputStream(testJESFile), "UTF-8"));
         int count = 0;
         do {
            line = br.readLine();
            if (line==null) {
               if (version == null || name == null || pckg == null) {
                  throw new IOException();
               }
               break;
            }
            line = line.trim();
            if (line.startsWith("<version>")) {
               version = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
            } else if (line.startsWith("<name>")) {
               name = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
            } else if (line.startsWith("<packaging>")) {
               pckg = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
            }
            if (version != null && name != null && pckg != null) {
               break;
            } else if (count == 30) {
               throw new IOException();
            }
            count++;
         } while (true);
      } finally {
         IOUtils.close(br);
      }
      testJESFile = new File(baseDir, "target" + File.separator + "jes." + pckg);
      FileUtils.copyFile(testJESFile, new File(tempJESDir, testJESFile.getName()));

      final Properties properties = System.getProperties();

      properties.setProperty("mail.smtp.host", server);
      properties.setProperty("mail.smtp.port", String.valueOf(smtpPort));
      properties.setProperty("mail.smtp.localaddress", server);

      properties.setProperty("mail.pop3.host", server);
      properties.setProperty("mail.pop3.port", String.valueOf(pop3Port));
      properties.setProperty("mail.pop3.localaddress", server);

      if (configurationProperties.getProperty("SASL") != null) {

         properties.setProperty("mail.smtp.auth", "true");
         properties.setProperty("mail.smtp.auth.mechanisms", configurationProperties.getProperty("SASL"));
         if (configurationProperties.getProperty("REALM") != null) {
            properties.setProperty("mail.smtp.sasl.realm", configurationProperties.getProperty("REALM"));
         }
      }

      if (configurationProperties.getProperty("STARTTLS") != null) {

         properties.setProperty("mail.smtp.starttls.enable", "true");
         properties.setProperty("mail.smtp.ssl.protocols", configurationProperties.getProperty("PROTOCOL"));
         properties.setProperty("mail.smtp.ssl.ciphersuites", configurationProperties.getProperty("CIPHERS"));
         properties.setProperty("mail.pop3.starttls.enable", "true");
         properties.setProperty("mail.pop3.ssl.protocols", configurationProperties.getProperty("PROTOCOL"));
         properties.setProperty("mail.pop3.ssl.ciphersuites", configurationProperties.getProperty("CIPHERS"));

         SSLContext sslContext = SSLContext.getInstance("TLS");
         KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
         kmf.init(null, null);
         TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
         File truststore = new File(tempJESDir, "truststore.jks");
         if (truststore.exists()) {
            KeyStore ks = KeyStore.getInstance("JKS", "SUN");
            FileInputStream fis = null;
            try {
               fis = new FileInputStream(truststore);
               ks.load(fis, null);
               tmf.init(ks);
            } finally {
               IOUtils.close(fis);
            }
         } else {
            tmf.init((KeyStore) null);
         }

         sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
         SSLSocketFactory sslsf = sslContext.getSocketFactory();
         properties.put("mail.smtp.ssl.socketFactory", sslsf);
         properties.put("mail.pop3.ssl.socketFactory", sslsf);

      }

      System.setProperty("java.security.policy", tempJESDir.getPath() + File.separator + "jes.policy");

      Properties log4jProperties = new Properties();
      log4jProperties.setProperty("defaultthreshold", "info");
      log4jProperties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
      log4jProperties.setProperty("log4j.appender.stdout.threshold", "trace");
      log4jProperties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
      log4jProperties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{ISO8601} - [%t] %C{1} - %m%n");
      log4jProperties.setProperty("log4j.appender.file", "org.apache.log4j.RollingFileAppender");
      log4jProperties.setProperty("log4j.appender.file.File", tempJESDir.getPath() + File.separator + "logs" + File.separator + "jes.log");
      log4jProperties.setProperty("log4j.appender.file.MaxFileSize", "10000KB");
      log4jProperties.setProperty("log4j.appender.file.MaxBackupIndex", "20");
      log4jProperties.setProperty("log4j.appender.file.threshold", "debug");
      log4jProperties.setProperty("log4j.appender.file.layout", "org.apache.log4j.PatternLayout");
      log4jProperties.setProperty("log4j.appender.file.layout.ConversionPattern", "%d{ISO8601} - [%t] %C{1} - %m%n");
      log4jProperties.setProperty("log4j.appender.file2","org.apache.log4j.RollingFileAppender");
      log4jProperties.setProperty("log4j.appender.file2.File", tempJESDir.getPath() + File.separator + "logs" + File.separator + "jes2.log");
      log4jProperties.setProperty("log4j.appender.file2.MaxFileSize", "1000KB");
      log4jProperties.setProperty("log4j.appender.file2.MaxBackupIndex", "20");
      log4jProperties.setProperty("log4j.appender.file2.threshold", "error");
      log4jProperties.setProperty("log4j.appender.file2.layout", "org.apache.log4j.PatternLayout");
      log4jProperties.setProperty("log4j.appender.file2.layout.ConversionPattern","%d{ISO8601} - [%t] %C{1} - %m%n");
      log4jProperties.setProperty("log4j.logger.com.ericdaugherty.mail.server.services.smtp.server.command.impl.AuthCommand", "info, auth");
      log4jProperties.setProperty("log4j.additivity.com.ericdaugherty.mail.server.services.smtp.server.command.impl.AuthCommand", "false");
      log4jProperties.setProperty("log4j.appender.auth", "org.apache.log4j.RollingFileAppender");
      log4jProperties.setProperty("log4j.appender.auth.File", tempJESDir.getPath() + File.separator + "logs" + File.separator + "auth.log");
      log4jProperties.setProperty("log4j.appender.auth.MaxFileSize", "1000KB");
      log4jProperties.setProperty("log4j.appender.auth.MaxBackupIndex", "20");
      log4jProperties.setProperty("log4j.appender.auth.threshold", "info");
      log4jProperties.setProperty("log4j.appender.auth.layout","org.apache.log4j.PatternLayout");
      log4jProperties.setProperty("log4j.appender.auth.layout.ConversionPattern", "%d{ISO8601} - [%t] %C{1} - %m%n");
      log4jProperties.setProperty("log4j.rootLogger", "trace,stdout,file,file2");

      org.apache.log4j.PropertyConfigurator.configure(log4jProperties);
      
      new Thread(
            new Runnable() {

               @Override
               public void run() {
                  com.ericdaugherty.mail.server.Mail.instantiate(new String[]{tempJESDir.getPath(), ""});
               }
            }).start();

      do {
         try {
            Thread.sleep(1000);
         }
         catch (InterruptedException e){}
         com.ericdaugherty.mail.server.Mail instance = com.ericdaugherty.mail.server.Mail.getInstance();
         if (instance == null || instance.isShuttingDown())
            break;
      } while(!Mail.isStarted());
      
      if (!Mail.isStarted()) {
         finish(true);
         throw new Exception("Unable to start the Mail Server. Aborting...");
      }
   }
   
   public Profile getProfile() {
      return profile;
   }
   
   public void clearResources() throws IOException {
      
      org.apache.log4j.Logger.getRootLogger().getAppender("file").close();
      org.apache.log4j.Logger.getRootLogger().getAppender("file2").close();
      org.apache.log4j.Logger.getLogger("com.ericdaugherty.mail.server.services.smtp.server.command.impl.AuthCommand").getAppender("auth").close();

      Utils.deleteFiles(tempJESDir);
      if (!tempJESDir.delete() && tempJESDir.exists()) {
         System.err.println("Unable to delete folder "+tempJESDir.getName()+". Ignoring...");
      }
   }
}
