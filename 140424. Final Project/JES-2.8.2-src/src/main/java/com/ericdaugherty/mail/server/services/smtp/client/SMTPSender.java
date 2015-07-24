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

package com.ericdaugherty.mail.server.services.smtp.client;

import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.persistence.localDelivery.LocalDeliveryProcessor;
import com.ericdaugherty.mail.server.persistence.smtp.SMTPMessagePersistenceFactory;
import com.ericdaugherty.mail.server.persistence.smtp.SMTPMessagePersistenceFactory.SMTPMessageLister;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessageFactory;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is tasked to collect unsent messages and deliver them
 * to the proper local mailbox or relay them to a remote SMTP server.
 * Only a single instance of this class is meant to be active during the
 * lifetime of the application.
 *
 * @author Eric Daugherty
 * @author Andreas Kyrmegalos (2.x branch)
 */
public abstract class SMTPSender implements Runnable {

   /** Logger */
   protected static final Log log = LogFactory.getLog( SMTPSender.class );
   
   private static final String CHARACTERS = "zyxwvutsrq9876543210";

   /** The ConfigurationManager */
   protected final ConfigurationManager configurationManager = ConfigurationManager.getInstance();

   protected final Charset utf8Charset = ConfigurationManager.getUtf8Charset();
   protected final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();

   protected volatile boolean running = true;
    
   private final Random random = new Random();

   private final String smtpRepository;

   protected final boolean useAmavisSMTPDirectory = configurationManager.isAmavisSupportActive();
   
   protected final Set<LocalDeliveryProcessor> deliveryProcessors = new ConcurrentSkipListSet<LocalDeliveryProcessor>();
   
   private final NamedThreadFactory threadFactory;  
   
   private final ThreadPoolExecutor executor;
   
   private final Object instanceLock = new Object();
   
   private SMTPMessageLister smtpML;
   
   private final Set<String> currentTasks = new ConcurrentSkipListSet<String>();
   
   private final ReentrantLock queueUpdaterLock = new ReentrantLock();
   
   private volatile boolean handlingLeftOvers;
       
   static class NamedThreadFactory implements ThreadFactory {

      final AtomicInteger ai = new AtomicInteger(1);
      private final ThreadGroup tg;

      NamedThreadFactory(String name) {
         tg = new ThreadGroup(name);
         tg.setMaxPriority(Thread.NORM_PRIORITY);
         tg.setDaemon(false);
      }
      
      public int getThreadCount() {
         return tg.activeCount();
      }

      @Override
      public Thread newThread(Runnable runnable) {
         return new Thread(tg, runnable, tg.getName() + ai.getAndIncrement());
      }
   }

   private class DiscardDelivererPolicy extends DiscardPolicy {

      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {

         Deliver deliver = (Deliver) r;
         deliver.updateQueueItem();
      }
   }

   SMTPSender(String name, String smtpRepository) {
      this.smtpRepository = smtpRepository;

      int coreCount = Runtime.getRuntime().availableProcessors();
      executor = new ThreadPoolExecutor(coreCount, 2 * coreCount + 1,
            2, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(500),
            threadFactory = new NamedThreadFactory(name), new DiscardDelivererPolicy()) {

         @Override
         protected void afterExecute(Runnable r, Throwable t) {

            Deliver deliver = (Deliver) r;
            currentTasks.remove(deliver.getMessagePersistanceName());
         }

         @Override
         protected void beforeExecute(Thread t, Runnable r) {

            Deliver deliver = (Deliver) r;
            currentTasks.add(deliver.getMessagePersistanceName());
         }   
      };
   }
   
   public int getThreadCount() {
      return threadFactory.getThreadCount();
   }
    
   public void notifyOnDomainDeleted(Domain domain) {
      for (LocalDeliveryProcessor processor : deliveryProcessors) {
         processor.invalidate(domain);
      }
   }

   public abstract Deliver getNewDeliverInstance(String messagePersistanceName);
    
   private void generateRepoUid(char[] repoUid) {
      for (int i = 0, length = repoUid.length; i < length; i++) {
         repoUid[i] = CHARACTERS.charAt((i % 2) * 10 + random.nextInt(10));
      }
   }
   
   public final void release() {
      synchronized(instanceLock) {
         instanceLock.notifyAll();
      }
   }

   @Override
   public void run() {

      //It is possible that a shutdown command is issued before the thread is activated
      if (Mail.getInstance().isShuttingDown()) {
         return;
      }
      if (!Mail.isStarted()) {
         synchronized (instanceLock) {
            do {
               try {
                  if (log.isDebugEnabled()) {
                     log.debug("SMTPSender standing by.");
                  }
                  instanceLock.wait();
               } catch (InterruptedException ex) {
                  if (!running || Mail.getInstance().isShuttingDown()) {
                     return;
                  }
               }
            } while (!Mail.isStarted());

            if (log.isDebugEnabled())
               log.debug("SMTPSender resuming operation.");
         }
      }
      File[] subDirs = new File(smtpRepository).listFiles(new DirOnlyFileFilter());
      String[] allRepositories = new String[5];
      allRepositories[0] = smtpRepository;
      if (subDirs.length == 0) {
         boolean failed = false;
         int index = 0;
         for (; index < 4; index++) {
            char[] repoUid = new char[4];
            generateRepoUid(repoUid);
            File repo = new File(smtpRepository, new String(repoUid));
            allRepositories[1 + index] = repo.getPath();
            if (repo.exists()) {
               index--;
               continue;
            }
            if (!repo.mkdir()) {
               failed = true;
               break;
            }
         }
         if (failed) {//rollback
            log.warn("Not all smtp sub-directories could be created. Using only the primary.");
            int length = index;
            index = 0;
            for (; index < length; index++) {
               File temp = new File(allRepositories[1 + index]);
               if (!temp.delete()) 
                  temp.deleteOnExit();
            }
            allRepositories = new String[1];
            allRepositories[0] = smtpRepository;
         }
      } else {
         for (int index = 0; index < 4; index++) {
            allRepositories[1 + index] = subDirs[index].getPath();
         }
      }

      SMTPMessageFactory.instantiate();

      SMTPMessagePersistenceFactory.instantiate(allRepositories);
      smtpML = SMTPMessagePersistenceFactory.getInstance()
            .getSMTPMessageLister(allRepositories);
      
      final Set<String> leftOverItems = new LinkedHashSet<String>(100, 1.00f);
      smtpML.populateSMTPMessageList(leftOverItems);

      Iterator<String> iter = leftOverItems.iterator();
      while (iter.hasNext()) {
         String message = iter.next();
         submitMessage(message);
      }
   
      synchronized (instanceLock) {
         while (running) {
            try {
               instanceLock.wait();
            } catch (InterruptedException ie) {
               //let loop decide
            }
         }
      }
      
      log.warn("SMTPSender shut down gracefully.");
   }
   
   /**
    * The smtp directory (as well as the amavis smtp directory, if amavisd-new
    * support is enabled) is scanned at regular intervals to determine if left
    * over messages exist. 
    */
   public void cleanup() {
      
      if (handlingLeftOvers || !running)
         return;
      
      Runnable runnable = new Runnable() {
         
         @Override
         public void run() {
            handlingLeftOvers = true;
            try {
               Set<String> leftOverItems = new LinkedHashSet<String>(100, 1.00f);
               smtpML.populateSMTPMessageList(leftOverItems);
               try {
                  Thread.sleep(2 * 60 * 1000L);
               } catch (InterruptedException ie) {
                  if (!running)
                     return;
               }
               Set<String> newLeftOverItems = new LinkedHashSet<String>(100, 1.00f);
               smtpML.populateSMTPMessageList(newLeftOverItems);
               leftOverItems.retainAll(newLeftOverItems);
               if (leftOverItems.isEmpty())
                  return;
               //If, after two minutes, left over messages exist resubmit them provided
               //they are not currently being processed or awaiting to be processed
               queueUpdaterLock.lock();
               try {
                  leftOverItems.removeAll(currentTasks);
                  if (leftOverItems.isEmpty())
                     return;
                  Set<String> currentItems = new LinkedHashSet<String>(100, 1.00f);
                  BlockingQueue<Runnable> queue = executor.getQueue();
                  for (Runnable item : queue) {
                     Deliver deliver = (Deliver)item;
                     String messagePersistanceName = deliver.getMessagePersistanceName();
                     currentItems.add(messagePersistanceName);
                  }
                  if (!currentItems.isEmpty())
                     leftOverItems.removeAll(currentItems);
                  if (leftOverItems.isEmpty())
                     return;
                  Iterator<String> iter = leftOverItems.iterator();
                  while (iter.hasNext()) {
                     String message = iter.next();
                     submitMessage(message);
                  }
               } finally {
                  queueUpdaterLock.unlock();
               }
            } finally {
               handlingLeftOvers = false;
            }
         }
      };
      new Thread(runnable).start();
   }
   
   public void submitMessage(String messagePersistanceName) {
      executor.execute(getNewDeliverInstance(messagePersistanceName));
   }

   private static final class DirOnlyFileFilter implements FileFilter {

      @Override
      public boolean accept(File file) {
         return file.isDirectory();
      }
   };

    public void notifyShutdown() {
        log.warn( "Attempting to shut down SMTPSender." );
        running = false;
        executor.shutdownNow();
    }
    
    public void shutdown() {
       release();
    }

   /**
    * This class (or rather concrete implementations of this class) performs the
    * actual delivery/relaying. It acts upon the assumption that all the
    * addresses were validated beforehand. Further, no delivery rules are
    * applied.
    */
   public abstract class Deliver implements Runnable {

      final String messagePersistanceName;
      final SMTPMessage message;

      public Deliver(String messagePersistanceName) {
         this.messagePersistanceName = messagePersistanceName;
         //No loading is done before the executor task is actually started
         message = SMTPMessageFactory.getInstance().getStoredMessage();
      }

      protected final boolean initMessage() {
         try {
            message.initializePersistedMessage(messagePersistanceName, true);
            return true;
         } catch (IOException ioe) {
            handleFailedInit(ioe);
            return false;
         }
      }

      private void handleFailedInit(IOException ioe) {
         File file = new File(messagePersistanceName);
         if (!file.exists())
            return;
         log.error("Error reading message " + messagePersistanceName, ioe);
         String filename = file.getPath();
         filename = filename.substring(0, filename.lastIndexOf(".ser"));
         String originalFilename = filename;
         char[] uid = new char[4];
         int count = 0;
         boolean result = false;
         do {
            if (5 == count++)
               break;
            SMTPMessagePersistenceFactory.generateSmtpUid(uid);
            StringBuilder sb = new StringBuilder();
            sb.append(originalFilename).append(uid).append(".fail");
            filename = sb.toString();
         } while (!(result = file.renameTo(new File(filename))));
         if (result) {
            log.info("Non-readable message saved as " + filename);
         } else {
            log.error("Unable to rename non-readable message " + messagePersistanceName);
         }
         deleteQueueItem();
      }

      @Override
      public abstract void run();

      void updateQueueItem() {
         try {
            Thread.sleep(1 * 1000L);
         } catch (InterruptedException ie) {
            //just proceed
         }
         if (running) {
            queueUpdaterLock.lock();
            try {
               submitMessage(messagePersistanceName);
            } finally {
               queueUpdaterLock.unlock();
            }
         }
      }

      void deleteQueueItem() {
//          queuedItems.remove(messagePersistanceName);
      }

      public String getMessagePersistanceName() {
         return messagePersistanceName;
      }
   }
}
