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

import com.ericdaugherty.mail.server.persistence.smtp.SMTPMessagePersistenceFactory;
import com.ericdaugherty.mail.server.services.ServiceListener;
import com.ericdaugherty.mail.server.services.pop3.Pop3Processor;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessageFactory;
import com.ericdaugherty.mail.server.services.smtp.client.SMTPSender;
import com.ericdaugherty.mail.server.services.smtp.client.SMTPSenderAmavis;
import com.ericdaugherty.mail.server.services.smtp.client.SMTPSenderStandard;
import com.ericdaugherty.mail.server.services.smtp.server.RFC5321SMTPServerSessionControl;
import com.xlat4cast.jes.dns.internal.Domain;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * All ServiceListener and Sender instances and the associated Threads are
 * administered via this class.
 *
 * @author Andreas Kyrmegalos
 */
public class MailServicesControl {
   
   /** Logger for this class. **/
   private static final Log log = LogFactory.getLog( MailServicesControl.class );
   
   private static MailServicesControl instance;
   
   private final ConfigurationManager configurationManager = ConfigurationManager.getInstance();

   public boolean testing;
   
   private enum Status {
      INITIALIZED, STARTED, SHUTTING_DOWN, FAILED; 
   }
   
   private volatile Status status;

   /** The threadGroup that all listener/sender threads are attached to. **/
   private final ThreadGroup threadGroup = new ThreadGroup("JESThreadGroup");
    
   /** The various e-mail ServiceListeners **/
   private final Set<ServiceListener> listeners = new LinkedHashSet<ServiceListener>(5);
    
   /** The SMTP senders */
   private final SMTPSender smtpSender, amavisSmtpSender;
    
   /** The SMTP sender threads */
   private final Thread smtpSenderThread, amavisSmtpSenderThread;
   
   public static synchronized void instantiate(boolean testing) {
      if (instance == null) {
         instance = new MailServicesControl(testing);
         instance.init();
      }
   }
   
   public static MailServicesControl getInstance() {
      return instance;
   }

   private MailServicesControl(boolean testing) {
      
      this.testing = testing;
      
      Integer totalSL = Integer.valueOf(1+(configurationManager.getRetrievalMode() == ConfigurationManager.RetrievalMode.POP3 ? 1 : 0)
            + (configurationManager.isAmavisSupportActive()||testing ? 1 : 0)
            + (configurationManager.isSecureActive()? 1 + ((configurationManager.getRetrievalMode() == ConfigurationManager.RetrievalMode.POP3 ? 1 : 0)) : 0));
      if (log.isDebugEnabled())
         log.debug("Total number of service listeners to be instantiated " + totalSL);
      ServiceListener.setTotalSL(totalSL);

      //Start the threads.
      int port;
      int executeThreads = configurationManager.getExecuteThreadCount();

      //Start the Pop3 Thread.
      if (configurationManager.getRetrievalMode() == ConfigurationManager.RetrievalMode.POP3) {
         port = configurationManager.getPOP3Port();
         if(log.isDebugEnabled())
            log.debug( "Starting POP3 Service on port: " + port );
         ServiceListener popListener = new ServiceListener("POP3", port,
               Pop3Processor.class, executeThreads, false , true);
         listeners.add(popListener);
      }

      //Start the SMTP Threads.
      port = configurationManager.getSMTPPort();
      if(log.isDebugEnabled())
         log.debug( "Starting SMTP Service on port: " + port );
      ServiceListener smtpListener = new ServiceListener("SMTP", port,
            RFC5321SMTPServerSessionControl.class, false, executeThreads, false, true );
      listeners.add(smtpListener);
      
      if (configurationManager.isAmavisSupportActive() || testing) {
         port = !testing ? configurationManager.getAmavisFilteredSMTPPort() : (configurationManager.getSMTPPort() + 1);
         if( log.isDebugEnabled() ) log.debug( "Starting Transmiting MTA's SMTP Service on port: " + port );
         ServiceListener amavisSmtpListener = new ServiceListener(!testing ? "Amavis SMTP" : "Testing SMTP", port,
               RFC5321SMTPServerSessionControl.class, true, executeThreads, false, true );
         listeners.add(amavisSmtpListener);
      }

      if (configurationManager.isSecureActive()) {

         int secureExecuteThreads = configurationManager.getSecureExecuteThreadCount();
         //Start secure Pop3 threads.
         if (configurationManager.getRetrievalMode()==ConfigurationManager.RetrievalMode.POP3) {
            port = configurationManager.getSecurePOP3Port();
            if( log.isDebugEnabled() ) log.debug( "Starting secure POP3 Service on port: " + port );
            ServiceListener securepopListener = new ServiceListener("secure POP3", port,
                  Pop3Processor.class, secureExecuteThreads, true, true );
            listeners.add(securepopListener);
         }

         //Start secure SMTP Threads.
         port = configurationManager.getSecureSMTPPort();
         if( log.isDebugEnabled() ) log.debug( "Starting secure SMTP Service on port: " + port );
         ServiceListener securesmtpListener = new ServiceListener("secure SMTP", port,
               RFC5321SMTPServerSessionControl.class, secureExecuteThreads, true, true );
         listeners.add(securesmtpListener);
      }
      
      //Start the SMTPSender thread (This thread actually delivers the mail received
      //by the SMTP threads).
      if (configurationManager.isAmavisSupportActive()) {
         smtpSenderThread = new Thread( threadGroup, smtpSender = new SMTPSenderAmavis(), "SMTPSender" );
         amavisSmtpSenderThread = new Thread( threadGroup,
               amavisSmtpSender = new SMTPSenderStandard(false), "SMTPSender2" );
      }
      else if (testing) {
         smtpSenderThread = new Thread( threadGroup, smtpSender = new SMTPSenderStandard(true), "SMTPSenderTest" );
         amavisSmtpSenderThread = new Thread( Thread.currentThread().getThreadGroup(),
               amavisSmtpSender = new SMTPSenderStandard(false), "SMTPSender" );
      }
      else {
         smtpSenderThread = new Thread( threadGroup, smtpSender = new SMTPSenderStandard(false), "SMTPSender" );
         amavisSmtpSender = null;
         amavisSmtpSenderThread = null;
      }
      status = Status.INITIALIZED;
   }
   
   private void init() {
      
      if (status != Status.INITIALIZED)
         throw new IllegalStateException("The service is not yet initialized");
      
      for(ServiceListener listener : listeners) {
         if (log.isDebugEnabled())
            log.debug("Starting " + listener.getName() + " thread.");
         new Thread( threadGroup, listener, listener.getName() ).start();
      }
      if (smtpSenderThread != null) {
         if (log.isDebugEnabled())
            log.debug("Starting SMTPSender thread.");
         smtpSenderThread.start();
      }
      if (amavisSmtpSenderThread != null) {
         if (log.isDebugEnabled())
            log.debug("Starting POP3Sender thread.");
         amavisSmtpSenderThread.start();
      }
   }
   
   public boolean isListenerServiceInitVerified() {
      
      if (status != Status.INITIALIZED)
         throw new IllegalStateException("The service is not yet initialized.");
      ServiceListener.waitAllStart();
      boolean failedListenerInitialization = false;
      for(ServiceListener listener : listeners)
         failedListenerInitialization |= !listener.isInitialized();
      status = !failedListenerInitialization ? Status.STARTED : Status.FAILED;
      return !failedListenerInitialization;
   }
   
   public void releaseListeners() {
      
      if (status != Status.STARTED)
         return;
      if (log.isDebugEnabled())
         log.debug("Bringing Receiver(s) out of stand by.");
      for(ServiceListener listener : listeners)
         listener.start();
      if (log.isDebugEnabled())
         log.debug("Bringing SMTPSender(s) out of stand by.");
      smtpSender.release();
      if (configurationManager.isAmavisSupportActive() || testing)
         amavisSmtpSender.release();
   }
   
   public void shutdown() {
      
      if (status == Status.SHUTTING_DOWN)
         return;
      status = Status.SHUTTING_DOWN;
      for(ServiceListener listener : listeners)
         listener.notifyShutdown();
      if (smtpSender != null)
         smtpSender.notifyShutdown();
      if (amavisSmtpSender != null)
         amavisSmtpSender.notifyShutdown();
      for(ServiceListener listener : listeners)
         listener.initiateShutdown();
      ServiceListener.shutdown();

      if (smtpSenderThread != null) {
         smtpSender.shutdown();
         try {
            smtpSenderThread.join();
         } catch (InterruptedException ie) {
            //proceed
         }
      }
      if (amavisSmtpSenderThread != null) {
         amavisSmtpSender.shutdown();
         try {
            amavisSmtpSenderThread.join();
         } catch (InterruptedException ie) {
            //proceed
         }
      }
      SMTPMessagePersistenceFactory.shutdown();
      SMTPMessageFactory.shutdown();
      instance = null;
   }
   
   public void notifyOnDomainDeleted(Domain domain) {
      
      for(ServiceListener listener : listeners) {
         if ("POP3".equals(listener.getName()) || "secure POP3".equals(listener.getName())) {
            listener.notifyOnDomainDeleted(domain);
         }
      }
      if (smtpSender != null) {
         smtpSender.notifyOnDomainDeleted(domain);
      }
      if (amavisSmtpSender != null) {
         amavisSmtpSender.notifyOnDomainDeleted(domain);
      }
   }

    public void notifyChange() {
       
      if (status != Status.STARTED)
         return;
      Map<String, Integer> portMap = new LinkedHashMap<String, Integer>(8);
      portMap.put("POP3", configurationManager.getPOP3Port());
      portMap.put("SMTP", configurationManager.getSMTPPort());
      if (configurationManager.isSecureActive()) {
         portMap.put("secure POP3", configurationManager.getSecurePOP3Port());
         portMap.put("secure SMTP", configurationManager.getSecureSMTPPort());
      }
      portMap = Collections.unmodifiableMap(portMap);
      
      for(ServiceListener listener : listeners)
         listener.updateServerSocket(portMap);
    }
    
   public void updateServiceListeners() {
       
      if (status != Status.STARTED)
         return;
      Map<String, Integer> portMap = new LinkedHashMap<String, Integer>(8);
      portMap.put("POP3", -1);
      portMap.put("SMTP", -1);
      if (configurationManager.isSecureActive()) {
         portMap.put("secure POP3", -1);
         portMap.put("secure SMTP", -1);
      }
      portMap = Collections.unmodifiableMap(portMap);
      
      for(ServiceListener listener : listeners)
         listener.updateServerSocket(portMap);
   }
   
   public void logStatus() {
      
      if (status != Status.STARTED) {
         log.warn("A request for a status report, without the services being started, is a request ignored...");
         return;
      }
      int maxthreadcount = 5+
            configurationManager.getExecuteThreadCount()*(1+(configurationManager.getRetrievalMode()==ConfigurationManager.RetrievalMode.POP3?1:0))+
            (configurationManager.getConfigurationAddress()!=null?1:0)+
            (configurationManager.isSecureActive()?configurationManager.getSecureExecuteThreadCount()*(1+(configurationManager.getRetrievalMode()==ConfigurationManager.RetrievalMode.POP3?1:0)):0) +
            ((configurationManager.isAmavisSupportActive()||testing)?configurationManager.getExecuteThreadCount()+5:0)+
            (configurationManager.isConfigurationEnabled()?1:0);
      int maxstandardsmtp,maxstandardpop3,maxsecuresmtp,maxsecurepop3,maxamavissmtp;
      int curstandardsmtp,curstandardpop3,cursecuresmtp,cursecurepop3,curamavissmtp,curdeliversmtp, curamavisdeliversmtp;
      maxstandardpop3 = maxstandardsmtp = configurationManager.getExecuteThreadCount();
      maxsecurepop3 = maxsecuresmtp = configurationManager.isSecureActive()? configurationManager.getSecureExecuteThreadCount():0;
      maxamavissmtp = (configurationManager.isAmavisSupportActive()||testing)?configurationManager.getExecuteThreadCount():0;
      
      curamavissmtp = curstandardsmtp = curstandardpop3 = cursecuresmtp =
            cursecurepop3 = curdeliversmtp = curamavisdeliversmtp = 0;
      Thread[] threadlist = new Thread[maxthreadcount];
      int threadcount = threadGroup.enumerate(threadlist);
      if (smtpSender != null) {
         curdeliversmtp = smtpSender.getThreadCount();
      }
      if (amavisSmtpSender != null) {
         curamavisdeliversmtp = amavisSmtpSender.getThreadCount();
      }
      if (threadcount > 0) {
         StringBuilder jesstatus = new StringBuilder(" Active Modules: ");
         for (int i = 0;i<threadcount;i++) {
            String name = threadlist[i].getName();
            if (name.startsWith("SMTP:")) {
               curstandardsmtp++;
            } else if (name.startsWith("POP3:")) {
               curstandardpop3++;
            } else if (name.startsWith("Amavis")
                  || name.startsWith("Testing")) {
               curamavissmtp++;
            } else {
               if (name.startsWith("secure S")) {
                  cursecuresmtp++;
               } else if (name.startsWith("secure P")) {
                  cursecurepop3++;
               }
            }
         }
         if (curdeliversmtp + curamavisdeliversmtp > 0) {
            jesstatus.append(" [SmtpSender ");
            jesstatus.append("Deliver Standard:").append(curdeliversmtp).append(" (dynamic)");
            if (configurationManager.isAmavisSupportActive()) {
               jesstatus.append(" Deliver Amavis:").append(curamavisdeliversmtp).append(" (dynamic)");
            }
            jesstatus.append("] ");
         }
         jesstatus.append("SMTP:").append(curstandardsmtp).append("/").append(maxstandardsmtp).append(" ");
         if (configurationManager.getRetrievalMode() == ConfigurationManager.RetrievalMode.POP3) {
            jesstatus.append("POP3: ").append(curstandardpop3).append("/").append(maxstandardpop3).append(" ");
         }
         if (configurationManager.isSecureActive()) {
            jesstatus.append("secure SMTP: ").append(cursecuresmtp).append("/").append(maxsecuresmtp).append(" ");
            if (configurationManager.getRetrievalMode() == ConfigurationManager.RetrievalMode.POP3) {
               jesstatus.append("secure POP3: ").append(cursecurepop3).append("/").append(maxsecurepop3).append(" ");
            }
         }
         if (configurationManager.isAmavisSupportActive()) {
            jesstatus.append("SMTP Amavis:").append(curamavissmtp).append("/").append(maxamavissmtp).append(" ");
         } else if (testing) {
            jesstatus.append("SMTP Testing:").append(curamavissmtp).append("/").append(maxamavissmtp).append(" ");
         }
         log.info(jesstatus);
      }
      else {
         log.info(" No active threads");
      }
   }
   
   public SMTPSender getSmtpSender() {
      return smtpSender;
   }
   
   public SMTPSender getAmavisSmtpSender() {
      return amavisSmtpSender;
   }
}
