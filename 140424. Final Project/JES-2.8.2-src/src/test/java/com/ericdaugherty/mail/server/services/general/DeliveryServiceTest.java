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

package com.ericdaugherty.mail.server.services.general;

import com.ericdaugherty.mail.server.services.DeliveryService;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.xlat4cast.jes.dns.internal.Domain;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class DeliveryServiceTest {
   
   private static final Log log = LogFactory.getLog(DeliveryServiceTester.class);
      
   @BeforeClass
   public static void setUpClass() {
      
      Properties log4jProperties = new Properties();
      log4jProperties.setProperty("defaultthreshold", "info");
      log4jProperties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
      log4jProperties.setProperty("log4j.appender.stdout.threshold", "trace");
      log4jProperties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
      log4jProperties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{ISO8601} - [%t] %C{1} - %m%n");
      log4jProperties.setProperty("log4j.rootLogger", "trace,stdout");

      org.apache.log4j.PropertyConfigurator.configure(log4jProperties);
      
      DeliveryService.instantiate(null);
   }
   
   @AfterClass
   public static void tearDownClass() {
      DeliveryService.shutdown();
   }
   
   private static final long TEST_DURATION = 15;//value in secods
   
   private static final char[] CHARACTER_POOL = {'a', 'b', 'c', 'd', 'e', 'f', 'g'};
   
   private volatile boolean failed;
   
   @Test
   public void test() {
      Domain[] domains = {
         new Domain("example1.com"),
         new Domain("example2.com"),
         new Domain("example3.com"),
         new Domain("example4.com"),
         new Domain("example5.com"),
      };
      Random random = new Random();
      EmailAddress[] emailAddresses = {
         EmailAddress.getEmailAddress(getLocalPart(random), domains[0]),
         EmailAddress.getEmailAddress(getLocalPart(random), domains[1]),
         EmailAddress.getEmailAddress(getLocalPart(random), domains[0]),
         EmailAddress.getEmailAddress(getLocalPart(random), domains[1]),
         EmailAddress.getEmailAddress(getLocalPart(random), domains[0]),
         EmailAddress.getEmailAddress(getLocalPart(random), domains[1]),
      };
      
      new Thread(new ConfigurationManagerTester(domains, TEST_DURATION)).start();
      
      DeliveryServiceTester[] testers = new DeliveryServiceTester[Runtime.getRuntime().availableProcessors() * 2];
      for (int i = 0; i < testers.length;i++)
         testers[i] = new DeliveryServiceTester(domains, TEST_DURATION, emailAddresses);
      for (DeliveryServiceTester tester : testers)
         new Thread(tester).start();
      try {
         Thread.sleep(TEST_DURATION * 1000L);
         assertFalse("At least one error occured.", failed);
      } catch (InterruptedException ex) {
         fail("Unexpected interruption.");
      }
   }

   private static final class ConfigurationManagerTester implements Runnable {
      
      private final Domain[] domains;
      private final long duration;
      private final Random random = new Random();

      private ConfigurationManagerTester(Domain[] domains, long duration) {
         this.domains = domains;
         this.duration = duration;
      }

      @Override
      public void run() {
         
         long targetTime = System.currentTimeMillis() + duration * 1000L;
         DeliveryService service = DeliveryService.getInstance();
         service.setLocalDomains(new LinkedHashSet<Domain>(Arrays.asList(domains)));
         while (System.currentTimeMillis() <= targetTime) {
            
            try {
               Thread.sleep(100 + ((1 + random.nextInt(2)) * 50));
            } catch (InterruptedException ie) {
               ie.printStackTrace(System.err);
               fail("Unexpected interruption.");
            }
            
            Domain[] localDomains = new Domain[4];
            int base = random.nextInt(2);
            for (int i = 0;i < 4;i++)
               localDomains[i] = this.domains[base + i];
            service.setLocalDomains(new LinkedHashSet<Domain>(Arrays.asList(localDomains)));
         }
      }
   }

   private final class DeliveryServiceTester implements Runnable {
      
      private final Domain[] domains;
      private final long duration;
      private final EmailAddress[] emailAddresses;
      private final Random random = new Random();
      
      private DeliveryServiceTester(Domain[] domains, long duration, EmailAddress[] emailAddresses) {
         this.domains = domains;
         this.duration = duration;
         this.emailAddresses = emailAddresses;
      }
      
      @Override
      public void run() {
         
         int domainCount = domains.length;
         int emailCount = emailAddresses.length;
         long targetTime = System.currentTimeMillis() + duration * 1000L;
         DeliveryService service = DeliveryService.getInstance();
         while (System.currentTimeMillis() <= targetTime) {
            
            EmailAddress emailAddress;
            Domain domain;
            boolean useRandomEmail = random.nextBoolean();
            if (useRandomEmail) {
               //randomize email address
               String localPart = getLocalPart(random);
               domain = domains[random.nextInt(domainCount)];
               emailAddress = EmailAddress.getEmailAddress(localPart, domain);
            } else {
               emailAddress = emailAddresses[random.nextInt(emailCount)];
               domain = emailAddress.getDomain();
            }
            
            //randomize test task
            int task = random.nextInt(50);

            //lockMailbox, unlockMailbox
            if (task < 49) {
               if (service.lockMailbox(emailAddress)) {
                  boolean unlockMailbox = service.unlockMailbox(emailAddress);
                  if (!unlockMailbox)
                     failed = true;
                  assertTrue(emailAddress + " should have been unlocked.", unlockMailbox);
               } else {
                  log.info(emailAddress + " not locked");
               }
            }

            //remove domain
            else {
               Set<Domain> toRemove = new LinkedHashSet<Domain>(1);
               toRemove.add(domain);
               service.removeLocalDomains(toRemove);
            }
         }
      }
   }

   private String getLocalPart(Random random) {
      int variability = random.nextInt(4);
      char[] localPart = new char[5 + variability];
      for (int i = 0;i < 5 + variability;i++) {
         localPart[i] = CHARACTER_POOL[random.nextInt(CHARACTER_POOL.length)];
      }
      return new String(localPart);
   }
}
