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

package com.ericdaugherty.mail.server.services.smtp.server.transaction;

//Java imports
import java.io.IOException;
import java.util.Locale;

//Logging imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.errors.TooManyErrorsException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.services.DeliveryService;
import com.ericdaugherty.mail.server.services.smtp.server.SMTPServerSessionControl;
import com.ericdaugherty.mail.server.services.smtp.server.SMTPServerSessionControl.ReplyWriter;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 *
 * @author Andreas Kyrmegalos
 */
class FullTransferModeAction implements TransferModeAction {

   /**
    * Logger Category for this class.
    */
   protected static final Log log = LogFactory.getLog(TransferModeAction.class);
   /**
    * The ConfigurationManager
    */
   protected final ConfigurationManager configurationManager = ConfigurationManager.getInstance();
   
   protected final TransactionControl transactionControl;
   protected ReplyWriter replyWriter;
   protected EmailAddress fromAddress;
   
   public FullTransferModeAction(TransactionControl transactionControl) {
      this.transactionControl = transactionControl;
   }
   
   @Override
   public void init(ReplyWriter replyWriter) {
      this.replyWriter = replyWriter;
   }

   @Override
   public boolean handleMailFrom(String inputString, String[] parameters) throws TooManyErrorsException, IOException, InvalidAddressException {

      handleMailFrom0(inputString);
      return handleMailFrom1(parameters);
   }

   protected void handleMailFrom0(String fromAddress) throws InvalidAddressException {

      //It is legal for the MAIL FROM address to be empty.
      if (fromAddress.isEmpty()) {
         this.fromAddress = new EmailAddress();
         if (log.isDebugEnabled()) {
            log.debug("Receiving mail from <>");
         }
      } //Although this is the normal case...
      else {
         this.fromAddress = new EmailAddress(fromAddress);
         if (log.isDebugEnabled()) {
            log.debug("Receiving mail from " + fromAddress);
         }
      }
      transactionControl.getMessage().setFromAddress(this.fromAddress);
   }

   protected boolean handleMailFrom1(@NonNull String[] parameters) throws TooManyErrorsException, IOException {

      if (parameters.length > 0) {
         for (String parameter : parameters) {
            String parameterUC = parameter.toUpperCase(ConfigurationManager.LOCALE);
            if (parameterUC.startsWith("SIZE")) {
               try {
                  int size = Integer.parseInt(parameter.substring(parameter.indexOf('=') + 1));
                  if (size > (configurationManager.getMaximumMessageSize() * 1024 * 1024)) {
                     log.warn("Message Rejected.  Message larger than max allowed size (" + configurationManager.getMaximumMessageSize() + " MB)");
                     replyWriter.writeAny(SMTPServerSessionControl.MESSAGE_MESSAGE_TOO_LARGE);
                     return false;
                  }
               }catch (NumberFormatException nfe) {
               }
            } else if (configurationManager.is8bitMIME() && parameterUC.startsWith("8BITMIME")) {
               if (log.isDebugEnabled()) {
                  log.debug("Receiving message has 8BITMIME parts");
               }
               transactionControl.setMessage8bitMIME(true);
               transactionControl.getMessage().set8bitMIME(true);
            }
//            else if (parameter.startsWith("AUTH")) {
//              //TODO implement the AUTH MAIL parameter
//            }
         }
      }
      transactionControl.getMessage().setFromAddress(fromAddress);
      replyWriter.writeAny(SMTPServerSessionControl.MESSAGE_OK);
      return true;
   }

   @Override
   public void handleRcptTo(EmailAddress address, String[] parameters) throws TooManyErrorsException, IOException, InvalidAddressException {

      handleRcptTo0(address);
   }

   protected void handleRcptTo0(EmailAddress address) throws TooManyErrorsException, IOException, InvalidAddressException {

      //Check the address to see if we can deliver it.
      DeliveryService deliveryService = DeliveryService.getInstance();
      if (transactionControl.isAuthenticated()
            || deliveryService.acceptAddress(address, transactionControl.getClientIP(), fromAddress)) {

         // Check to see if it is a local user.
         if (configurationManager.getUser(address) == null) {

            if (configurationManager.isLocalDomain(address.getDomain().getDomainName())
                  && configurationManager.isNonExistentLocalRejected()) {
               throw new InvalidAddressException();
            }
         }

         //Perhaps someone is faking a local user in the MAIL FROM command.
         //If so, do not offer a permanent negative reply when checking the MAIL FROM
         //commands but instead reject the RCPT TO commands.
         //If the faked user is a true registered user then the recipient is rejected if:
         //a)there was no successful smtp authentication or
         //b)POPBeforeSMTP is inactive or
         //c)POPBeforeSMTP is active and the user didn't successfully logon using POP3.
         //If the faked local user doesn't exist reject the RCPT TO commands forthwith.
         //This way no information about local users is offered directly since it is
         //impossible to relate the rejection to the existence of a local user.
         //Complemented by the proper rejection of credentials during a POP or SMTP
         //authentication process (that is reject the credentials as a whole and not
         //individually), this approach constitutes a considerable obstacle to anyone
         //attempting to use JES as a spam relay or trying to take control of user
         //accounts.
         if (configurationManager.isLocalDomain(fromAddress.getDomain().getDomainName())) {
            if (configurationManager.getUser(fromAddress) == null
                  && !deliveryService.acceptAddress(null, transactionControl.getClientIP(), fromAddress)) {
               throw new InvalidAddressException();
            }
         }
         if (transactionControl.rcptPolicyActions(address)) {

            replyWriter.writeAny(SMTPServerSessionControl.MESSAGE_OK);
            transactionControl.incrementValidRCPTCount();
            transactionControl.getMessage().addToAddress(address);
            if (log.isDebugEnabled()) {
               log.debug("Recipient " + address + " accepted.");
            }

         }
      } else {

         replyWriter.writeAny(SMTPServerSessionControl.MESSAGE_USER_INVALID);
         transactionControl.incrementFailedRCPTCount();
         transactionControl.rcptPolicyActions(address);
         if (log.isInfoEnabled()) {
            log.info("Invalid delivery address for incoming mail: " + address
                  + " from client: " + transactionControl.getClientIP() + " / " + fromAddress);
         }

      }
   }
}
