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
import com.ericdaugherty.mail.server.configuration.ModuleControl;
import com.ericdaugherty.mail.server.errors.*;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.info.User;
import com.ericdaugherty.mail.server.persistence.localDelivery.LocalDeliveryFactory;
import com.ericdaugherty.mail.server.persistence.localDelivery.LocalDeliveryProcessor;
import com.ericdaugherty.mail.server.persistence.smtp.SMTPMessagePersistenceFactory;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessageFactory;
import com.ericdaugherty.mail.server.services.smtp.client.support.FailedAddressItem;
import com.ericdaugherty.mail.server.utils.ByteUtils;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * A smtp sender meant to distribute incoming mail to the intended recipients.
 *
 * @author Andreas Kyrmegalos
 */
public class SMTPSenderStandard extends SMTPSender {
   
   private enum BounceType {
      FAILED, DELAYED;
   }

   private final boolean testing;
   
   private final DeliverFactory deliverFactory;

   private class DeliverFactory {

      protected SMTPSenderStandard smtpSenderStandard;

      public DeliverFactory(SMTPSenderStandard smtpSenderStandard) {
         this.smtpSenderStandard = smtpSenderStandard;
      }

      public Deliver getDeliverInstance(String messagePersistanceName) {
         return smtpSenderStandard.new StandardDeliver(messagePersistanceName);
      }
   }

   private class TestingDeliverFactory extends DeliverFactory {

      public TestingDeliverFactory(SMTPSenderStandard smtpSenderStandard) {
         super(smtpSenderStandard);
      }

      @Override
      public Deliver getDeliverInstance(String messagePersistanceName) {
         return smtpSenderStandard.new TestingDeliver(messagePersistanceName);
      }
   }
   
   public SMTPSenderStandard(boolean testing) {
      super("DelS:", ConfigurationManager.getInstance().isAmavisSupportActive()||(Mail.isTesting()&&!testing)?
            ConfigurationManager.getInstance().getAmavisSMTPDirectory():
            ConfigurationManager.getInstance().getSMTPDirectory());
      this.testing = testing;
      this.deliverFactory = !ConfigurationManager.getInstance().isLocalTestingMode()?new DeliverFactory(this):new TestingDeliverFactory(this);
   }
   
   @Override
   public final Deliver getNewDeliverInstance(String messagePersistanceName) {
      return deliverFactory.getDeliverInstance(messagePersistanceName);
   }

   public class StandardDeliver extends SMTPSender.Deliver {
      
      private final List<FailedAddressItem> failedAddresses = new ArrayList<FailedAddressItem>();

      public StandardDeliver(String messagePersistanceName) {
         super(messagePersistanceName);
      }

      @Override
      public void run() {
         
         if (!initMessage())
            return;

         // If the next scheduled delivery attempt is still in the future, skip.
         if (message.getScheduledDelivery().getTime() > System.currentTimeMillis()) {
            updateQueueItem();
            if (log.isTraceEnabled()) {
               log.trace("Skipping delivery of message " + message.getSmtpUid()
                     + " because the scheduled delivery time is still in the future: "
                     + message.getScheduledDelivery());
            }
            return;
         }
         long sleepTime = configurationManager.getThrottlingDelay();
         if (sleepTime != 0L) {
            try {
               Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
               //just proceed
            }
            if (!running)
               return;
         }

         Map<Domain, List<EmailAddress>> perDomainAddresses = getPerDomainAddresses(message.getToAddresses());
         for (Entry<Domain, List<EmailAddress>> entry : perDomainAddresses.entrySet()) {
            
            Domain domain = entry.getKey();
            if (configurationManager.isLocalDomain(domain.getDomainName())
                  && (testing ? !domain.getUniqueName().equals("example.com") : true)) {
               handleLocalDelivery(domain, entry.getValue());
            } else {
               handleRemoteDelivery(domain, entry.getValue());
            }
            resolveDelivery();
         }
      }
               
      private void handleLocalDelivery(Domain domain, List<EmailAddress> toAddresses) {
         
         for (EmailAddress address : toAddresses) {
            try {
               User user = configurationManager.getUser(address);
               //Local user doesn't exist and server policy possibly allows delivery to a default mailbox
               if (user == null) {
                  deliverLocalMessage(address, message);
               } else {
                  EmailAddress[] deliveryAddresses = user.getForwardAddresses();
                  //No forward addresses, just perform local delivery
                  if (deliveryAddresses.length == 0) {
                     deliverLocalMessage(address, message);
                  } else {
                     int originalPosition = getOriginalRecipientPosition(address, deliveryAddresses);

                     //Forward addresses include the original recipient
                     if (originalPosition != -1) {
                        //The only forward address is the original recipient, perform local delivery
                        if (deliveryAddresses.length == 1) {
                           deliverLocalMessage(address, message);
                        }
                        //The original recipient is included
                        else {
                           //First create the forwarded message
                           createForwardedMessage(address, deliveryAddresses, message);

                           //Now perform the local delivery
                           deliverLocalMessage(address, message);
                        }
                     }
                     //Original recipient is not included
                     else {
                        //Simply create the forwarded message
                        createForwardedMessage(address, deliveryAddresses, message);
                     }
                  }
               }
               log.info("Delivery complete for message " + message.getSmtpUid() + " to: " + address);
            } catch (NotFoundException nfe) {
               log.warn("Delivery attempted to unknown user: " + address);
               //The addressee does not exist. Notify the sender of the error.
               if (message.getFromAddress().isNULL()) {
                  log.warn("Return path is NULL, cannot bounce.");
               } else if (message.getFromAddress().isMailerDaemon()) {
                  log.warn("Return mailbox is Mailer-Daemon, message will not be bounced.");
               } else {
                  List<FailedAddressItem> failedAddress = new ArrayList<FailedAddressItem>(1);
                  failedAddresses.add(new FailedAddressItem(address, "550 " + nfe.getMessage()));
                  bounceMessage(BounceType.FAILED, failedAddress, message, address.getDomain());
               }
            } catch (IOException ioe) {
               log.error("Delivery failed for message from: " + message.getFromAddress()
                     + " to: " + address, ioe);
               failedAddresses.add(new FailedAddressItem(address, ioe.getMessage()));
            }
         }
      }

      private void handleRemoteDelivery(Domain domain, List<EmailAddress> recipientList) {
         
         try {
            deliverRemoteMessage(domain, recipientList, message);
            List<FailedAddressItem> failed550RCPT = new ArrayList<FailedAddressItem>();
            for (Iterator<FailedAddressItem> iter = failedAddresses.iterator(); iter.hasNext();) {
               FailedAddressItem fai = iter.next();
               if (fai.getMessage().startsWith("5")) {
                  failed550RCPT.add(fai);
                  iter.remove();
               }
            }
            if (failed550RCPT.size() > 0) {
               bounceMessage(BounceType.FAILED, failed550RCPT, message, domain);
            }
         } catch (PermanentNegativeException pne) {
            log.warn("Delivery to " + domain + " failed.", pne);
            //There was a permanent error. Notify the mail sender.
            if (message.getFromAddress().isNULL()) {
               log.warn("Return path is NULL, cannot bounce.");
            } else if (message.getFromAddress().isMailerDaemon()) {
               log.warn("Return mailbox is Mailer-Daemon, message will not be bounced.");
            } else if (!failedAddresses.isEmpty()) {
               bounceMessage(BounceType.FAILED, failedAddresses, message, domain);
            }
            failedAddresses.clear();
         } catch (TransientNegativeException tne) {
            log.error("Delivery temporarily failed for message from: " + message.getFromAddress()
                  + " to domain: " + domain, tne);
         }
      }

      private void resolveDelivery() {
         
         // If all addresses were successful, remove the message from the spool
         if (failedAddresses.isEmpty()) {
            // Log an error if the delete fails.  This will cause the message to get
            // delivered again, but it is too late to roll back the delivery.
            if (!message.deleteMessage())
               log.error("Error removing SMTP message after delivery!  This message may be redelivered. "
                     + message.getSmtpUid());
            deleteQueueItem();
         }
         // Update the message with any changes.
         else {
            List<EmailAddress> failedToAddresses = new ArrayList<EmailAddress>(failedAddresses.size());
            for (FailedAddressItem item : failedAddresses) {
               failedToAddresses.add(item.getAddress());
            }

            message.setToAddresses(failedToAddresses);
            int deliveryAttempts = message.getDeliveryAttempts();

            // If the message is a bounced email, just give up and move it to the failed directory.
            if (message.getFromAddress().isMailerDaemon()) {
               try {
                  log.info("Delivery of message from MAILER_DAEMON failed, moving to failed folder.");
                  message.moveToFailedFolder();
               } catch (IOException ioe) {
                  log.error("Unable to move failed message to 'failed' folder.", ioe);
               }
               deleteQueueItem();
            }
            // If we have not passed the maximum delivery count, calculate the
            // next delivery time and save the message.
            else if (deliveryAttempts < configurationManager.getDeliveryAttemptThreshold()) {
               
               message.setDeliveryAttempts(deliveryAttempts + 1);
               // Reschedule later, 1 min, 2 min, 4 min, 8 min, ... 2^n
               // Cap delivery interval at 2^10 minutes. (about 17 hours)
               if (deliveryAttempts > 10) {
                  deliveryAttempts = 10;
               }
               long offset = (long) Math.pow(2, deliveryAttempts);
               Date schedTime = new Date(System.currentTimeMillis() + offset * 60 * 1000);
               message.setScheduledDelivery(schedTime);

               try {
                  message.save(useAmavisSMTPDirectory);
               } catch (IOException ioe) {
                  log.error("Error updating spooled message for next delivery. Message may be re-delivered.", ioe);
               }
               updateQueueItem();
               if (deliveryAttempts == 4) {
                  bounceMessage(BounceType.DELAYED, failedAddresses, message,
                        configurationManager.isLocalDomain(message.getFromAddress().getDomain().getDomainName())
                        ? message.getFromAddress().getDomain()
                        : failedToAddresses.get(0).getDomain());
               }
            }
            // All delivery attempts failed, bounce message.
            else {
               // Send a bounce message for all failed addresses.
               bounceMessage(BounceType.FAILED, failedAddresses, message,
                     configurationManager.isLocalDomain(message.getFromAddress().getDomain().getDomainName())
                     ? message.getFromAddress().getDomain()
                     : failedToAddresses.get(0).getDomain());
               deleteQueueItem();

               // Remove the original message.
               if (!message.deleteMessage())
                  log.error("Could not remove SMTP message after bounce! This message may be re-bounced: "
                        + message.getSmtpUid());
            }
         }
      }

      private int getOriginalRecipientPosition(EmailAddress originalRecipient, EmailAddress[] deliveryAddresses) {
         for (int i = 0; i < deliveryAddresses.length; i++) {
            if (originalRecipient.equals(deliveryAddresses[i]))
               return i;
         }
         return -1;
      }

      private void createForwardedMessage(EmailAddress address, EmailAddress[] deliveryAddresses, SMTPMessage message) throws IOException {

         String US_ASCII = "US-ASCII";
         String boundary = createBoundary();
         List<byte[]> dataLines = null;
         List<byte[]> forwardDataLines = new ArrayList<byte[]>();

         SMTPMessage forwardedMessage = SMTPMessageFactory.getInstance().getNewMessage();
         try {
            //First get the subject
            dataLines = message.loadIncrementally(8);
            Iterator<byte[]> iter = dataLines.iterator();
            String subject = "";
            while (iter.hasNext()) {
               subject = new String(iter.next(), US_ASCII);
               if (subject.trim().toLowerCase(ConfigurationManager.LOCALE).startsWith("subject:")) {
                  subject = subject.substring(subject.indexOf(':') + 1);
                  break;
               }
            }
            if (subject.length() == 0) {
               subject = "[ FW: Message forwarded from " + address.getUsername() + " ]";
            } else {
               subject = "[ FW: " + subject + " ]";
            }

            forwardedMessage.setFromAddress(address);
            for (EmailAddress deliveryAddress : deliveryAddresses) {
               if (!deliveryAddress.equals(address))
                  forwardedMessage.addToAddress(deliveryAddress);
            }
            forwardedMessage.saveBegin(configurationManager.isAmavisSupportActive());

            forwardDataLines.add(string2Bytes("From: <" + address + ">"));
            forwardDataLines.add(string2Bytes("To: undisclosed recipients;"));
            forwardDataLines.add(string2Bytes("Subject: " + subject));
            forwardDataLines.add(string2Bytes("Date: " + new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", ConfigurationManager.LOCALE).format(forwardedMessage.getTimeReceived())));
            forwardDataLines.add(string2Bytes("MIME-Version: 1.0"));
            forwardDataLines.add(string2Bytes("Content-Type: multipart/mixed;"));
            forwardDataLines.add(string2Bytes(" boundary=\"" + boundary + "\""));
            forwardDataLines.add(string2Bytes(""));
            forwardDataLines.add(string2Bytes("--" + boundary));
            forwardDataLines.add(string2Bytes("Content-Type: text/plain; charset=ISO-8859-1"));
            forwardDataLines.add(string2Bytes("Content-Transfer-Encoding: 7bit"));
            forwardDataLines.add(string2Bytes(""));
            forwardDataLines.add(string2Bytes(""));
            forwardDataLines.add(string2Bytes("--" + boundary));
            forwardDataLines.add(string2Bytes("Content-Type: message/rfc822;"));
            forwardDataLines.add(string2Bytes(" name=\"FW: " + subject.substring(6, subject.lastIndexOf(' ')) + ".eml\""));
            forwardDataLines.add(string2Bytes("Content-Transfer-Encoding: 7bit"));
            forwardDataLines.add(string2Bytes(""));

            forwardedMessage.saveIncrement(forwardDataLines, true, false);
            forwardDataLines.clear();

            int count = 8;
            while (dataLines.size() > 0) {

               forwardedMessage.saveIncrement(dataLines, false, true);
               count += 250;
               dataLines.clear();
               dataLines = message.loadIncrementally(count);
            }

            forwardDataLines.add(string2Bytes("--" + boundary + "--"));

            forwardedMessage.saveIncrement(forwardDataLines, false, true);
            forwardDataLines.clear();

            if (!forwardedMessage.saveFinish())
               throw new IOException("Forwarded message not saved, aborting.");

         } finally {
            SMTPMessagePersistenceFactory.removeUniqueId(forwardedMessage.getSmtpUid());
            if (dataLines != null) {
               dataLines.clear();
            }
            forwardDataLines.clear();
         }
      }

      /**
       * This method takes a local SMTPMessage and attempts to deliver it.
       */
      void deliverLocalMessage(EmailAddress address, SMTPMessage message)
            throws NotFoundException, IOException {

         if (log.isDebugEnabled()) {
            log.debug("Delivering Message to local user: " + address);
         }

         //Load the user.  If the user doesn't exist, a not found exception will
         //be thrown and the deliver() message will deal with the notification.
         User user = configurationManager.getUser(address);
         if (user == null) {
            if (log.isDebugEnabled())
               log.debug("User " + address + " not found, checking for default delivery options.");
            //Check to see if a default delivery mailbox exists, and if so, deliver it.
            //Otherwise, just throw the NotFoundException to bounce the email.
            EmailAddress defaultAddress = configurationManager.getDefaultMailbox(address.getDomain().getDomainName());
            if (!defaultAddress.isNULL() && (user = configurationManager.getUser(defaultAddress)) != null) {
               address = defaultAddress;
               if (log.isDebugEnabled())
                  log.debug("Redirecting message addressed to: " + address + " to default user: " + defaultAddress);
            } else {
               throw new NotFoundException("User " + address + " does not exist and no default delivery options found.");
            }
         }
         LocalDeliveryProcessor processor = LocalDeliveryFactory.getInstance().getLocalDeliveryProccessor(user);
         deliveryProcessors.add(processor);
         try {
            Object persistedID = processor.persistLocalMessage(message, address);
            ModuleControl.getInstance().getPassReceivedLocalMessage().passMessage(persistedID);
         } finally {
            deliveryProcessors.remove(processor);
         }
      }

      /**
       * Handles delivery of messages to addresses not handled by this server.
       */
      private void deliverRemoteMessage(Domain domain, List<EmailAddress> addresses, SMTPMessage message)
            throws TransientNegativeException, PermanentNegativeException {

         if (log.isDebugEnabled())
            log.debug("Delivering Message to remote domain: " + domain);

         //Delegate this request to the SMTPRemoteSender class.
         SMTPRemoteSender smtpRemoteSender = new SMTPRemoteSender();
         try {
            smtpRemoteSender.sendMessage(domain, addresses, message);
            log.info("Delivery complete for message " + message.getSmtpUid() + " to recipient(s) at " + domain);
         } finally {
            failedAddresses.addAll(smtpRemoteSender.getFailedAddresses());
            if (log.isDebugEnabled() && failedAddresses.size() > 0) {
               for (FailedAddressItem fai : failedAddresses) {
                  log.debug("Recipient " + (fai.getAddress()).getUsername()
                        + " " + (fai.getMessage().startsWith("5")
                        ? " was rejected."
                        : "is delayed."));
               }
            }
            smtpRemoteSender.cleanUp();
         }
      }

      private static final String characterPool = "0123456789";

      private String createBoundary() {

         Random random = new Random();
         char[] boundary = new char[24];
         for (int i = 0; i < 24; i++) {
            boundary[i] = characterPool.charAt(random.nextInt(10));
         }
         return new String(boundary);
      }

      private String getDeliveryAttemptsTotalDuration() {

         int timeInMinutes = 0;
         for (int i = 0; i < configurationManager.getDeliveryAttemptThreshold(); i++) {
            timeInMinutes += Math.pow(2, configurationManager.getDeliveryAttemptThreshold());
         }
         int days = 0, hours = 0;
         if (timeInMinutes >= 1440) {
            days = timeInMinutes / 1440;
            timeInMinutes -= days * 1440;
         }
         if (timeInMinutes >= 60) {
            hours = timeInMinutes / 60;
            timeInMinutes -= hours * 60;
         }
         int minutes = timeInMinutes;

         String durationMessage = "";
         if (days != 0) {
            if (hours != 0 || minutes != 0) {
               durationMessage = "" + days + " days, ";
            } else {
               durationMessage = "" + days + " days.";
            }
         }
         if (hours != 0) {
            if (minutes != 0) {
               durationMessage += "" + hours + " hours, ";
            } else {
               durationMessage += "" + hours + " hours.";
            }
         }
         if (minutes != 0) {
            durationMessage += "" + minutes + " minutes.";
         }

         return durationMessage;
      }

      private byte[] string2Bytes(String line) {
         return line.getBytes(usAsciiCharset);
      }

      private String generateMUID() {

         byte[] salt = new byte[8];
         try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(System.currentTimeMillis());
            sr.nextBytes(salt);
         } catch (NoSuchAlgorithmException ex) {
            //SHA1PRNG will not go away any time soon
         }

         return new String(ByteUtils.toHex(salt));
      }

       private void bounceMessage(BounceType bounceType, List<FailedAddressItem> failedAddresses, SMTPMessage message, Domain msgIdDomain) {

         log.info("Bouncing Messsage from " + message.getFromAddress() + (failedAddresses.isEmpty()
               ? " to mailbox at domain " + message.getToAddresses().get(0).getDomain()
               : (" to at least " + ((EmailAddress) ((FailedAddressItem) failedAddresses.get(0)).getAddress()))));

         SMTPMessage bounceMessage = SMTPMessageFactory.getInstance().getNewMessage();
         String boundary = createBoundary();

         List<FailedAddressItem> remaining = new ArrayList<FailedAddressItem>(failedAddresses);
         Iterator<FailedAddressItem> iter;

         //Set the from address as mailserver@ the first (default) local domain.
         EmailAddress fromAddress = EmailAddress.getEmailAddress("MAILER_DAEMON", message.getFromAddress().getDomain());

         bounceMessage.setFromAddress(fromAddress);
         bounceMessage.addToAddress(message.getFromAddress());
         bounceMessage.addDataLine(string2Bytes("From: Mail Delivery Subsystem <MAILER-DAEMON@" + message.getFromAddress().getDomain().getDomainName() + ">"));
         bounceMessage.addDataLine(string2Bytes("Message-ID: <" + generateMUID() + '@' + msgIdDomain.getDomainName() + ">"));
         bounceMessage.addDataLine(string2Bytes("Auto-Submitted: auto-replied"));
         bounceMessage.addDataLine(string2Bytes("To: " + message.getFromAddress().getAddress()));
         bounceMessage.addDataLine(string2Bytes("Subject: Message Delivery " + (bounceType == BounceType.FAILED ? "Error." : "Delayed.")));
         bounceMessage.addDataLine(string2Bytes("Date: " + new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", ConfigurationManager.LOCALE).format(new Date())));
         bounceMessage.addDataLine(string2Bytes("MIME-Version: 1.0"));
         bounceMessage.addDataLine(string2Bytes("Content-Type: multipart/report; report-type=delivery-status;"));
         bounceMessage.addDataLine(string2Bytes(" boundary=\"" + boundary + "\""));
         bounceMessage.addDataLine(string2Bytes(""));
         bounceMessage.addDataLine(string2Bytes("--" + boundary));
         bounceMessage.addDataLine(string2Bytes(""));
         
         if (bounceType == BounceType.FAILED) {
            bounceMessage.addDataLine(string2Bytes("Error delivering message to: " + ((EmailAddress) failedAddresses.get(0).getAddress()).getAddress()));
            iter = remaining.iterator();
            if (iter.hasNext()) {
               iter.next();
            }
            while (iter.hasNext()) {
               bounceMessage.addDataLine(string2Bytes("                             " + ((EmailAddress) iter.next().getAddress()).getAddress()));
            }
            bounceMessage.addDataLine(string2Bytes("This message will not be delivered."));
         } else {
            bounceMessage.addDataLine(string2Bytes("Message has not been delivered to the intended recipient: " + ((EmailAddress) failedAddresses.get(0).getAddress()).getAddress()));
            iter = remaining.iterator();
            if (iter.hasNext()) {
               iter.next();
            }
            while (iter.hasNext()) {
               bounceMessage.addDataLine(string2Bytes("                                                          " + ((EmailAddress) iter.next().getAddress()).getAddress()));
            }
            bounceMessage.addDataLine(string2Bytes("Attempts to be delivered will continue for up to " + getDeliveryAttemptsTotalDuration()));
         }
         bounceMessage.addDataLine(string2Bytes("------------------"));
         bounceMessage.addDataLine(string2Bytes(""));
         bounceMessage.addDataLine(string2Bytes("--" + boundary));
         bounceMessage.addDataLine(string2Bytes("content-type: message/delivery-status"));
         bounceMessage.addDataLine(string2Bytes(""));
         bounceMessage.addDataLine(string2Bytes("Reporting-MTA: dns; " + message.getFromAddress().getDomain()));
         bounceMessage.addDataLine(string2Bytes(""));
         
         while ((iter = remaining.iterator()).hasNext()) {

            FailedAddressItem item = iter.next();
            bounceMessage.addDataLine(string2Bytes("Final-Recipient: rfc822; " + ((EmailAddress) item.getAddress()).getAddress()));
            bounceMessage.addDataLine(string2Bytes("Action: " + (bounceType == BounceType.FAILED ? "failed" : "delayed")));
            bounceMessage.addDataLine(string2Bytes("Status: " + item.getMessage().charAt(0) + ".0.0"));
            bounceMessage.addDataLine(string2Bytes("Diagnostic-Code: " + item.getMessage()));
            bounceMessage.addDataLine(string2Bytes(""));
         }
         if (bounceType == BounceType.FAILED) {
            bounceMessage.addDataLine(string2Bytes("--" + boundary));
            bounceMessage.addDataLine(string2Bytes("content-type: message/rfc822"));
            bounceMessage.addDataLine(string2Bytes(""));

            List<byte[]> dataLines;
            try {
               dataLines = message.loadIncrementally(8);
               int numLines = Math.min(dataLines.size(), 30);

               for (int index = 0; index < numLines; index++) {
                  if (dataLines.get(index).length == 0) {
                     break;
                  }
                  bounceMessage.addDataLine(dataLines.get(index));
               }
               bounceMessage.addDataLine(string2Bytes(""));
            } catch (IOException ex) {
               log.error("", ex);
            }

         }
         bounceMessage.addDataLine(string2Bytes("--" + boundary + "--"));

         //Save this message so it will be delivered.
         try {
            bounceMessage.saveBegin(useAmavisSMTPDirectory);
            bounceMessage.saveIncrement(bounceMessage.getDataLines(), true, false);
            bounceMessage.saveFinish();
         } catch (IOException ioe) {
            log.error("Error storing outgoing 'bounce' email message", ioe);
         } finally {
            SMTPMessagePersistenceFactory.removeUniqueId(bounceMessage.getSmtpUid());
         }
      }
   }

   private Map<Domain, List<EmailAddress>> getPerDomainAddresses(List<EmailAddress> toAddresses) {

      Map<Domain, List<EmailAddress>> perDomainAddresses = new HashMap<Domain, List<EmailAddress>>();
      for (EmailAddress address : toAddresses) {
         if (!perDomainAddresses.containsKey(address.getDomain())) {
            perDomainAddresses.put(address.getDomain(), new ArrayList<EmailAddress>());
         }
         if (!perDomainAddresses.get(address.getDomain()).contains(address)) {
            perDomainAddresses.get(address.getDomain()).add(address);
         }
      }
      return perDomainAddresses;
   }

   public class TestingDeliver extends StandardDeliver {

      public TestingDeliver(String messagePersistanceName) {
         super(messagePersistanceName);
      }

      @Override
      public void run() {

         if (!initMessage())
            return;

         // If the next scheduled delivery attempt is still in the future, skip.
         if (message.getScheduledDelivery().getTime() > System.currentTimeMillis()) {
            updateQueueItem();
            if (log.isTraceEnabled()) {
               log.trace("Skipping delivery of message " + message.getSmtpUid()
                     + " because the scheduled delivery time is still in the future: " + message.getScheduledDelivery());
            }
            return;
         }

         Map<Domain, List<EmailAddress>> perDomainAddresses = getPerDomainAddresses(message.getToAddresses());
         
         for (Entry<Domain, List<EmailAddress>> entry : perDomainAddresses.entrySet()) {
            
            Domain domain = entry.getKey();
            for (EmailAddress address : entry.getValue()) {
               try {
                  deliverLocalMessage(address, message);
                  log.info("Delivery complete for message " + message.getSmtpUid() + " to: " + address);
               } catch (NotFoundException nfe) {
                  log.error("Delivery attempted to unknown user: " + address, nfe);
                  //The addressee does not exist.  Notify the sender of the error.
               } catch (IOException ioe) {
                  log.error("Delivery failed for message from: " + message.getFromAddress() + " to: " + address, ioe);
               }
            }
         }

         // Log an error if the delete fails.  This will cause the message to get
         // delivered again, but it is too late to roll back the delivery.
         if (!message.deleteMessage()) {
            log.error("Error removed SMTP message after delivery!  This message may be redelivered. " + message.getSmtpUid());
         }
         deleteQueueItem();
      }

      @Override
      void deliverLocalMessage(EmailAddress address, SMTPMessage message)
            throws NotFoundException, IOException {

         LocalDeliveryFactory.getInstance().getLocalDeliveryProccessor().persistLocalMessage(message, address);
      }
   }
}
