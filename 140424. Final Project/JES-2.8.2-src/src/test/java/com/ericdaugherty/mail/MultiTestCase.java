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

import com.ericdaugherty.mail.server.utils.IOUtils;
import com.sun.mail.smtp.SMTPMessage;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.util.SharedFileInputStream;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class MultiTestCase extends AbstractTestCase {
   
   final AtomicInteger ai = new AtomicInteger();
   private final int threadCount;
   
   MultiTestCase(String name, AbstractTestInstance testInstance, int threadCount) {
      super(name, testInstance);
      
      this.threadCount = threadCount;
   }
   
   @Override
   public boolean execute(final List<Request> tasks) throws Exception{

      final Map<String, AtomicInteger> userMessages = new HashMap<String, AtomicInteger>(6, 0.75f);
      for (Object aUser:userInfo.values()) {
         userMessages.put((String)aUser, new AtomicInteger());
      }
      
      final Properties properties = System.getProperties();

      List<Runnable> runnables = new ArrayList<Runnable>(threadCount);

      Random random = new Random();
      for (int i = 0; i < threadCount; i++) {
         List<Request> requests = new ArrayList<Request>();
         for (int j = 0; j < tasks.size(); j++) {
            Request task = tasks.get(j);
            Request request = new Request(task.getUsername(), task.getMessage());
            requests.add(request);
         }
         int shufflingCount = Math.max(threadCount * 4, 8);
         for (int j = 0; j < shufflingCount; j++) {
            requests.add(requests.remove(random.nextInt(requests.size())));
         }
         Runnable runnable = getJavaMailRunnable(requests, tempJESDir, userMessages, threadCount, server, tasks.size() / 4 * threadCount, properties,
               configurationProperties, senderCredentials);

         runnables.add(runnable);
      }

      for (int i = 0; i < threadCount; i++) {
         new Thread(runnables.get(i)).start();
      }
      runnables.clear();

      synchronized (lock) {
         try {
            lock.wait();
         } catch (InterruptedException ex) {
            System.out.println(ex.getMessage());
         }
      }

      try {
         Thread.sleep(2 * 1000);
      } catch (InterruptedException ex) {
      }
      
      testInstance.finish(false);
      return verify(tasks);
   }
   
   public boolean verify(List<Request> tasks) {
      
      File usersDir = new File(tempJESDir, "users" + File.separator + server);
      FileFilter ff = new FileFilter() {

         @Override
         public boolean accept(File file) {
            if (file.isDirectory()) {
               Iterator iter = userInfo.keySet().iterator();
               String filename;
               while (iter.hasNext()) {
                  filename = file.getName();
                  if (filename.startsWith((String) iter.next())) {
                     return true;
                  }
               }
               return false;
            }
            return false;
         }
      };
      File[] users = usersDir.listFiles(new LocFileFilter());
      File[] messages;
      for (File user : users) {
         messages = user.listFiles(ff);
         if (messages.length != tasks.size() / 4 * threadCount) {
            return false;
         }
      }
      return true;
   }
   
   private static final class LocFileFilter implements FileFilter {

      @Override
      public boolean accept(File file) {
         return !file.isDirectory() && file.getPath().toLowerCase().endsWith(".loc");
      }
   }
   
   private static class RequestFilenameFilter implements FilenameFilter {
      
      private final Map<String, AtomicInteger> userMessages;
      private final Request request;
      
      private RequestFilenameFilter(Map<String, AtomicInteger> userMessages,
            Request request) {
         this.userMessages = userMessages;
         this.request = request;
      }

      @Override
      public boolean accept(File directory, String filename) {
         if (filename.toLowerCase().endsWith(".loc")) {
            userMessages.get(request.getUsername()).incrementAndGet();
            return true;
         }
         return false;
      }
   };

   private Runnable getJavaMailRunnable(final List<Request> tasks, final File tempJESDir, final Map<String, AtomicInteger> userMessages,
           final int threadCount, final String server, final int messageCountPerUser, final Properties properties, final Properties configurationProperties,
           final PasswordAuthenticator senderCredentials) {

      return new Runnable() {

         @Override
         @SuppressWarnings("CallToThreadDumpStack")
         public void run() {

            int previousCount = 0;
            while (tasks.size() > 0) {
               final Request request = tasks.remove(0);
               System.err.println("Checking out " + request.getUsername() + "'s message");
               Session session = Session.getInstance(properties, senderCredentials);

               InputStream is = null;
               try {
                  is = new SharedFileInputStream(request.getMessage());
                  SMTPMessage messageSMTP = new SMTPMessage(session, is);

                  messageSMTP.setFrom(new InternetAddress(senderCredentials.getEmailAddress()));

                  messageSMTP.setRecipient(Message.RecipientType.TO, new InternetAddress(request.getUsername() + '@' + server));
                  
                  Transport.send(messageSMTP);
               }
               catch (IOException ioe) {
                  throw new RuntimeException(ioe);
               }
               catch (MessagingException me) {
                  throw new RuntimeException(me);
               }
               finally {
                  IOUtils.close(is);
               }

               File userDirJES = new File(tempJESDir, "users" + File.separator + server + File.separator + request.getUsername() );
               int count = 0;
               while (!userDirJES.exists()) {
                  System.out.println(userDirJES + " not yet created, sleeping...");
                  try {
                     Thread.sleep(5 * 1000);
                  } catch (InterruptedException ex) {
                  }
                  count++;
                  assertTrue("Unable to create user dir"+server + File.separator + request.getUsername()+" in test dir "+tempJESDir+". Aborting...", count < 10);
               }
               RequestFilenameFilter rff = new RequestFilenameFilter(userMessages, request);
               count = 0;
               while (userDirJES.listFiles(rff).length == previousCount) {
                  count++;
                  assertFalse("Waited too long to receive all of "+request.getUsername()+"'s messages. Aborting...", count == 10);
                  System.out.println(request.getUsername() + "'s mail not yet received, sleeping...");
                  try {
                     Thread.sleep(5 * 1000);
                  } catch (InterruptedException ex) {
                  }
               }
               previousCount++;
               if (tasks.isEmpty()) {
                  while (userDirJES.listFiles(rff).length < messageCountPerUser) {
                     System.out.println(userDirJES + " not all user messages received, sleeping...");
                     try {
                        Thread.sleep(5 * 1000);
                     } catch (InterruptedException ex) {
                     }
                  }
               }
            }

            System.out.println("Going to sleep once more");
            try {
               Thread.sleep(5 * 1000);
            } catch (InterruptedException ex) {
            }
               
            if (ai.incrementAndGet() == threadCount) {
               synchronized (lock) {
                  lock.notifyAll();
               }
            }
         }
      };
   }
}
