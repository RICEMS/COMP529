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

//Java imports
import java.io.IOException;
import java.net.Socket;
import java.util.*;

//Local imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.PermanentNegativeException;
import com.ericdaugherty.mail.server.errors.TransientNegativeException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import static com.ericdaugherty.mail.server.services.smtp.client.SMTPSender.log;
import com.ericdaugherty.mail.server.services.smtp.client.support.FailedAddressItem;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 *A SMTPSENDER class meant to send mail to amavisd-new.
 *
 * @author Andreas Kyrmegalos
 */
public class SMTPSenderAmavis extends SMTPSender{

   public SMTPSenderAmavis() {
      super("DelA:", ConfigurationManager.getInstance().getSMTPDirectory());
   }
   
   @Override
   public final Deliver getNewDeliverInstance(String messagePersistanceName) {
      return this.new AmavisDeliver(messagePersistanceName);
   }

   public class AmavisDeliver extends SMTPSender.Deliver {

      private final List<FailedAddressItem> failedAddresses = new ArrayList<FailedAddressItem>();

      private AmavisDeliver(String messagePersistanceName) {
         super(messagePersistanceName);
      }

      @Override
      public void run() {

         if (!initMessage())
            return;

         // If the next scheduled delivery attempt is still in the future, skip.
         if( message.getScheduledDelivery().getTime() > System.currentTimeMillis() ) {
             updateQueueItem();
             if(log.isTraceEnabled()) 
                log.trace( "Skipping delivery of message " + message.getSmtpUid()
                      + " because the scheduled delivery time is still in the future: "
                      + message.getScheduledDelivery() );
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

         List<EmailAddress> toAddresses = message.getToAddresses();
         
         if (handlePostmasterDelivery(toAddresses))
            return;
         handleAmavisDelivery(toAddresses);
         resolveDelivery(toAddresses);
      }
      
      // Handling of a special case where there is only one recipient, it belongs to a local domain and the user does
      // not exist. Message is resaved in the amavis.incoming.directory to be delivered immediatelly to the postmaster.
      private boolean handlePostmasterDelivery(List<EmailAddress> toAddresses) {
         if (!configurationManager.isNonExistentLocalRejected()
                  && toAddresses.size() == 1
                  && configurationManager.isLocalDomain(toAddresses.get(0).getDomain().getDomainName())
                  && configurationManager.getUser(toAddresses.get(0)) == null) {
            try {
               message.redirectToPostmaster();
               deleteQueueItem();
               return true;
            } catch (IOException ioe) {
               log.error("Error delivering to POSTMASTER address for domain: "
                     + toAddresses.get(0).getDomain().getDomainName(), ioe);
               updateQueueItem();
            }
         }
         return false;
      }
      
      private void handleAmavisDelivery(List<EmailAddress> toAddresses) {
         try {
            if( log.isDebugEnabled())
               log.debug( "Attempting to deliver message to amavisd-new for processing." );

            Socket socket;
            try {
               socket = new Socket(configurationManager.getAmavisListenAddress(), configurationManager.getAmavisSMTPPort());
            } catch (IOException ioe) {
               Iterator<EmailAddress> iter = toAddresses.iterator();
               while (iter.hasNext()) {
                  failedAddresses.add(new FailedAddressItem(iter.next(), ""));
               }
               throw new TransientNegativeException("Could not connect to amavisd-new.", ioe);
            }
            //Delegate this request to the SMTPRemoteSender class.
            SMTPRemoteSender smtpRemoteSender = new SMTPRemoteSender();
            try {
               smtpRemoteSender.sendMessage( socket, Domain.getNullDomain(), toAddresses, message );
               log.info( "Delivery complete for message " + message.getSmtpUid() + " to amavisd-new." );
            }
            finally {
               failedAddresses.addAll(smtpRemoteSender.getFailedAddresses());
               smtpRemoteSender.cleanUp();
            }
         } catch( PermanentNegativeException pne) {
            log.info( "Caught a PNE off a connection to amavisd-new." );
            failedAddresses.clear();
         } catch( TransientNegativeException tne) {
            log.error("Delivery failed for message from: " + message.getFromAddress() + " to amavisd-new.", tne);
         }
      }
      
      private void resolveDelivery(List<EmailAddress> toAddresses) {

         // If all addresses were successful, remove the message from the spool
         if(failedAddresses.isEmpty()) {
            // Log an error if the delete fails.  This will cause the message to get
            // delivered again, but it is too late to roll back the delivery.
            if(!message.deleteMessage())
               log.error( "Could not remove SMTP message after delivery!"
                     + " This message may be redelivered. " + message.getSmtpUid() );
            deleteQueueItem();
         }
         // Update the message with any changes. Since the receiver is amavisd-new (and the following
         // is based on my extremely limited knowledge of the workings of amavisd-new) TO: address can
         // not be rejected. If this does not hold true the message is updated. NO message should be
         // deleted. Just requeue it endlessly.
         else if (failedAddresses.size() != toAddresses.size()) {

            List<EmailAddress> failedToAddresses = new ArrayList<EmailAddress>(failedAddresses.size());
            Iterator<FailedAddressItem> iter = failedAddresses.iterator();
            while (iter.hasNext()) {
               failedToAddresses.add( iter.next().getAddress());
            }

            message.setToAddresses( failedToAddresses );
            int deliveryAttempts = message.getDeliveryAttempts();

            message.setDeliveryAttempts( deliveryAttempts + 1 );

            // Reschedule later, 1 min, 2 min, 4 min, 8 min, ... 2^n
            // Cap delivery interval at 2^10 minutes. (about 17 hours)
            if( deliveryAttempts > 10 ) {
               deliveryAttempts = 10;
            }
            long offset = (long)Math.pow( 2, deliveryAttempts);
            Date schedTime = new Date(System.currentTimeMillis() + offset*60*1000);
            message.setScheduledDelivery( schedTime );

            try {
               message.save(false);
            }
            catch(IOException ioe) {
                log.error( "Error updating spooled message for next delivery.  Message may be re-delivered.", ioe );
            }
            updateQueueItem();
         } else {// Amavisd-new not running, just update queue entry
            updateQueueItem();
         }
         failedAddresses.clear();
      }
   }
}
