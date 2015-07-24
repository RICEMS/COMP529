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

package com.ericdaugherty.mail.server.configuration;

import com.ericdaugherty.mail.server.info.db.DomainDb;
import com.xlat4cast.jes.dns.internal.Domain;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class ConfigurationManagerDirectoriesTest {

   @AfterClass
   public static void tearDownClass() {
      
      File tmp = new File(System.getProperty("java.io.tmpdir"));
      File[] files = tmp.listFiles(new FileFilter(){
      
         @Override
         public boolean accept(File file) {
            return file.isDirectory() && file.getName().startsWith("userRepo");
         }
      });
      for (File file : files) {
         deleteUserRepo(file);
      }
   }
   
   private String randomNameGenerator(Random random) {
      
      int length = 5 + random.nextInt(10);
      StringBuilder sb = new StringBuilder(length);
      char c;
      for (int i = 0;i < length;i++) {
         c = (char)(65 + random.nextInt(24));
         sb.append(c);
      }
      return sb.toString();
   }

   //Test the expected outcome
   @Test
   public void testCheckLegacyFileIO1() {
      
      File userRepo = null;
      try {
         Properties log4jProperties = new Properties();
         log4jProperties.setProperty("defaultthreshold", "info");
         log4jProperties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
         log4jProperties.setProperty("log4j.appender.stdout.threshold", "info");
         log4jProperties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
         log4jProperties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{ISO8601} - [%t] %C{1} - %m%n");
         log4jProperties.setProperty("log4j.rootLogger", "debug,stdout");

         org.apache.log4j.PropertyConfigurator.configure(log4jProperties);

         File tmp = new File(System.getProperty("java.io.tmpdir"));

         Random random = new Random();
         userRepo = new File(tmp, "userRepo." + random.nextInt(1000));
         int count = 0;
         while(!userRepo.mkdir()) {
            if (count++ == 10)
               throw new RuntimeException("Too many userRepo directories not deleted.");
            userRepo = new File(tmp, "userRepo." + random.nextInt(1000));
         }

         //Create random dirs and files
         File aRandomDir, aRandomFile;
         Map<File, List<File>> original = new LinkedHashMap<File, List<File>>();
         for (int i = 0;i < 10;i++) {
            aRandomDir = new File(userRepo, randomNameGenerator(random) + '@' + randomNameGenerator(random));
            assertTrue(aRandomDir.mkdir());
            original.put(aRandomDir, new ArrayList<File>());
            for (int j = 0;j < 5;j++) {
               aRandomFile = new File(aRandomDir, randomNameGenerator(random));
               try {
                  assertTrue(aRandomFile.createNewFile());
                  original.get(aRandomDir).add(aRandomFile);
               }
               catch (IOException ioe) {
                  assertTrue(false);
               }
            }
         }
         new ConfigurationManagerDirectories().checkLegacyFileIO(userRepo.getPath(), new HashSet<Domain>());
         //Check that none of the original directories exist and
         //all the files are in their expected destination
         File targetDir, targetUserDir;
         for(Map.Entry<File, List<File>> dirs : original.entrySet()) {
            File dir = dirs.getKey();
            assertFalse(dir.exists());
            targetDir = new File(userRepo, dir.getName().substring(dir.getName().indexOf('@') + 1));
            assertTrue(targetDir.exists());
            for (File file : dirs.getValue()) {
               targetUserDir = new File(targetDir, dir.getName().substring(0, dir.getName().indexOf('@')));
               assertTrue(new File(targetUserDir, file.getName()).exists());
            }
         }
      } finally {
         deleteUserRepo(userRepo);
      }
   }

   //Test a fail case where a domain directory already exists
   @Test
   public void testCheckLegacyFileIO2() {

      File userRepo = null;
      try {
         File tmp = new File(System.getProperty("java.io.tmpdir"));

         Random random = new Random();
         userRepo = new File (tmp, "userRepo."+random.nextInt(1000));
         int count = 0;
         while(!userRepo.mkdir()) {
            if (count++ == 10) {
               throw new RuntimeException("Too many userRepo directories not deleted.");
            }
            userRepo = new File(tmp, "userRepo." + random.nextInt(1000));
         }

         Set<Domain> domains = new HashSet<Domain>();
         Domain domain = null;

         //Create random dirs and files
         File aRandomDir, aRandomFile;
         Map<File, List<File>> original = new LinkedHashMap<File, List<File>>();
         for (int i = 0;i < 10;i++) {
            aRandomDir = new File(userRepo, randomNameGenerator(random) + '@' + randomNameGenerator(random));
            assertTrue(aRandomDir.mkdir());
            if (i == 9) {
               domain = new DomainDb(aRandomDir.getName().substring(aRandomDir.getName().indexOf('@') + 1), -1);
               domains.add(domain);
               if (!new File(userRepo, domain.getDomainName()).mkdir()) {
                  throw new RuntimeException("Folder " + domain.getDomainName()
                        + " under " + userRepo.getName() + " not created.");
               }
            }
            original.put(aRandomDir, new ArrayList<File>());
            for (int j = 0;j < 5;j++) {
               aRandomFile = new File(aRandomDir, randomNameGenerator(random));
               try {
                  assertTrue(aRandomFile.createNewFile());
                  original.get(aRandomDir).add(aRandomFile);
               }
               catch (IOException ioe) {
                  assertTrue(false);
               }
            }
         }
         try {
            new ConfigurationManagerDirectories().checkLegacyFileIO(userRepo.getPath(), domains);
            assertTrue(false);
         }
         catch (RuntimeException re) {
            assertTrue(true);
         }

         //Check that none of the target directories exist and
         //all the files are in their expected original directory
         File targetDir, targetUserDir;
         for(Map.Entry<File, List<File>> dirs : original.entrySet()) {
            File dir = dirs.getKey();
            assertTrue(dir.exists());
            targetDir = new File(userRepo, dir.getName().substring(dir.getName().indexOf('@') + 1));
            if (targetDir.equals(new File(userRepo, domain.getDomainName()))) continue;
            assertFalse(targetDir.exists());
            for (File file : dirs.getValue()) {
               targetUserDir = new File(targetDir, dir.getName().substring(0, dir.getName().indexOf('@')));
               assertFalse(new File(targetUserDir, file.getName()).exists());
               assertTrue(file.exists());
            }
         }
      } finally {
         deleteUserRepo(userRepo);
      }
   }
   
   private static void deleteUserRepo(@CheckForNull File file) {
      if (file == null)
         return;
      for (File aFile:file.listFiles()) {
         if (aFile.isDirectory()) {
            for (File bFile:aFile.listFiles()) {
               if (bFile.isDirectory()) {
                  for (File cFile:bFile.listFiles()) {
                     if (!cFile.delete()&&cFile.exists()) {
                        System.err.println(cFile+" was not deleted.");
                     }
                  }
               }
               if (!bFile.delete()&&bFile.exists()) {
                  System.err.println(bFile+" was not deleted.");
               }
            }
         }
         if (!aFile.delete()&&aFile.exists()) {
            System.err.println(aFile+" was not deleted.");
         }
      }
      if (!file.delete()&&file.exists()) {
         System.err.println(file+" was not deleted.");
      }
   }
}
