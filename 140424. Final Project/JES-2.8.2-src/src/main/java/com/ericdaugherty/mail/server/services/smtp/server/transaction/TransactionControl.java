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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.RcptPolicy;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.errors.TooManyErrorsException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.persistence.smtp.SMTPMessagePersistenceFactory;
import com.ericdaugherty.mail.server.services.ProcessorStreamHandler;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessageFactory;
import com.ericdaugherty.mail.server.services.smtp.server.RecipientPolicyHandler;
import com.ericdaugherty.mail.server.services.smtp.server.SMTPServerSessionControl;
import com.ericdaugherty.mail.server.services.smtp.server.SMTPServerSessionControl.ReplyWriter;
import com.ericdaugherty.mail.server.services.smtp.server.support.AddDataLineDebug;
import com.ericdaugherty.mail.server.services.smtp.server.support.AddDataLineDefault;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class TransactionControl extends MIMETransactionControl {

   /**
    * Logger Category for this class.
    */
   private static final Log log = LogFactory.getLog(TransactionControl.class);
   /**
    * The ConfigurationManager
    */
   private final ConfigurationManager configurationManager = ConfigurationManager.getInstance();
   
   private final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();

   private final SMTPServerSessionControl sessionControl;
   private ReplyWriter replyWriter;
   private ProcessorStreamHandler smtpSH;
   private TransferModeAction transferModeAction;
   private RecipientPolicyHandler rph = new RecipientPolicyHandler();

   public TransactionControl(SMTPServerSessionControl sessionControl, boolean amavisMode) {
      this.sessionControl = sessionControl;
      
      if (amavisMode) {

         transferModeAction = new FullTransferModeAction(this);
      } else {

         switch (configurationManager.getTransferMode()) {
            case FULL: {
               transferModeAction = new FullTransferModeAction(this);
            }
            break;
            case LOCAL: {
               transferModeAction = new LocalTransferModeAction(this);
            }
            break;
            case REMOTE: {
               transferModeAction = new RemoteTransferModeAction(this);
            }
            break;
            case TESTING: {
               transferModeAction = new TestingTransferModeAction(this);
            }
            break;
            default:
               throw new AssertionError();
         }
      }
   }
   
   public void setProcessorStreamHandler(ProcessorStreamHandler smtpSH) {
      this.smtpSH = smtpSH;
   }

   public void setReplyWriter(ReplyWriter replyWriter) {
      this.replyWriter = replyWriter;
      transferModeAction.init(replyWriter);
   }
   
   /**
    * Message specific variables
    */
   private SMTPMessage message;
   private boolean singleRCPT;
   private int validRCPT, failedRCPT;
   private boolean tooManyRCPT, excessRCPT;
   private boolean forceExitRCPT;
   /**
    * This setting is used to prevent a shutdown after all DATA have been received.
    */
   private boolean finishedData;
   private byte[] output;
   private int finalBufferSize;
   private boolean endOfMessage;
   
   public boolean isFinishedData() {
      return finishedData;
   }

   public boolean isTooManyRCPT() {
      return tooManyRCPT;
   }

   public boolean isExcessRCPT() {
      return excessRCPT;
   }
   
   public boolean isSingleRCPT() {
      return singleRCPT;
   }
   
   public void setSingleRCPT(boolean singleRCPT) {
      this.singleRCPT = singleRCPT;
   }
   
   public boolean isRCPTListEmpty() {
      return message.getToAddresses().isEmpty();
   }
   
   public boolean isForceExitRCPT() {
      return forceExitRCPT;
   }

   public void resetMessage() {
      message = SMTPMessageFactory.getInstance().getNewMessage();
      output = null;
      finalBufferSize = 32;
      mimeBody = new MIMETransactionControl.MIMEBody(null);
      endOfMessage = false;
      message8bitMIME = false;
      singleRCPT = false;
      validRCPT = 0;
      failedRCPT = 0;
      tooManyRCPT = false;
      excessRCPT = false;
      forceExitRCPT = false;
      finishedData = false;
      addDataLine = log.isDebugEnabled() ? new AddDataLineDebug(message, log) : new AddDataLineDefault(message);
   }

   public boolean handleMailFrom(String inputString, String[] parameters) throws TooManyErrorsException, IOException {

      //The inputString is always inside a <> block 
      String fromAddress = inputString.substring(1, inputString.length() - 1);

      try {
         return transferModeAction.handleMailFrom(fromAddress, parameters);
      } catch (InvalidAddressException iae) {
         log.error("Unable to parse From Address: " + fromAddress);
         sessionControl.incrementErrorCount();
         replyWriter.writeAny(SMTPServerSessionControl.MESSAGE_SYNTAX_ERROR_PARAMETER);
         return false;
      }
   }

   public void handleRcptTo(String inputString, String[] parameters) throws TooManyErrorsException, IOException {

      //The inputString is always inside a <> block 
      String toAddress = inputString.substring(1, inputString.length() - 1);

      try {
         EmailAddress address = new EmailAddress(toAddress);
         transferModeAction.handleRcptTo(address, parameters);
      } catch (InvalidAddressException iae) {
         sessionControl.incrementErrorCount();
         replyWriter.writeAny(SMTPServerSessionControl.MESSAGE_USER_INVALID);
         incrementFailedRCPTCount();
         rcptPolicyActions((EmailAddress) null);
         log.error("RCTP TO: " + toAddress + " rejected.");
      }
   }

   boolean rcptPolicyActions(EmailAddress toAddress) throws IOException, TooManyErrorsException {
      if (validRCPT > configurationManager.getMaxValidRCPT()) {
         tooManyRCPT = true;
         if (((validRCPT + failedRCPT) * 100 / validRCPT) > 100 + configurationManager.getAddPctRCPT()) {
            excessRCPT = true;
         }
      }
      if (failedRCPT > configurationManager.getMinTotFailRCPT() && failedRCPT * 100 / (failedRCPT + validRCPT) > configurationManager.getMinPctFailRCPT()) {
         forceExitRCPT = true;
      }
      if (toAddress == null) {
         return true;
      }
      Domain senderDomain = message.getFromAddress().getDomain();
      if (!configurationManager.isLocalDomain(senderDomain.getDomainName())) {
         return true;
      }
      Domain rcptDomain = toAddress.getDomain();
      RcptPolicy<String> domain = configurationManager.getRcptPolicyMap().get(senderDomain.getDomainName());
      RcptPolicy<String> global = configurationManager.getRcptPolicyMap().get("#####");

      boolean pass = rph.rcptPolicyActions(senderDomain.getDomainName(), rcptDomain.getDomainName(), domain, global);
      if (!pass) {
         sessionControl.incrementErrorCount();
         replyWriter.writeAny(SMTPServerSessionControl.MESSAGE_RCPT_DOMAIN_REJECTED);
         return false;
      }
      return true;

   }

   /**
    * Handles the SMTP DATA being read from the socket.
    * 
    * @return 
    * @throws com.ericdaugherty.mail.server.errors.TooManyErrorsException
    * @throws java.net.SocketTimeoutException
    * @throws java.net.SocketException
    */
   public String handleData() throws TooManyErrorsException, IOException {

      // Get the current maxSize setting and convert to bytes.
      long maxSize = configurationManager.getMaximumMessageSize() * 1024L * 1024L;

      replyWriter.writeLast(SMTPServerSessionControl.MESSAGE_SEND_DATA);

      try {
         message.saveBegin(sessionControl.getUseAmavisSMTPDirectory());

         // Add a "Received:" line as trace information per rfc 2821/4.4
         addDataLine.addDataLine(("Received: from " + sessionControl.getDeclaredClientHost() + " ([" + sessionControl.getClientIP() + "])").getBytes(usAsciiCharset));
         String sslHeaderField = sessionControl.getSSLHeaderField();
         if (sslHeaderField != null){
            addDataLine.addDataLine(sslHeaderField.getBytes(usAsciiCharset));
         }

         Domain domain = ((EmailAddress) message.getToAddresses().get(0)).getDomain();
         setDomain(domain);

         addDataLine.addDataLine(("        by " + domain.getDomainName() + " with "
               + (sessionControl.isESMTP() ? "E" : "") + "SMTP id <REPLACE-ID>" + (singleRCPT ? "" : ";")).getBytes(usAsciiCharset));

         addDataLine.addDataLine(((singleRCPT ? "        for <REPLACE-RCPT>; " : "        ")
               + new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", ConfigurationManager.LOCALE).format(message.getTimeReceived())).getBytes(usAsciiCharset));

         readOnceInputStream();
         boolean sizeExceeded = false;
         if (!endOfMessage) {
            for (;;) {
               readInputStream();
               if (sizeExceeded = sizeExceeded(maxSize)) {
                  break;
               }
               if (endOfMessage) {
                  break;
               }
            }
         }
         finishedData = true;
      
         if (endOfMessage) {
            addDataLine.flush();
            if (!message.saveFinish()) {
               throw new Exception();
            }
            replyWriter.writeLast(SMTPServerSessionControl.MESSAGE_OK);
            if (log.isInfoEnabled()) {
               log.info("Message " + message.getSmtpUid() + " accepted for delivery.");
            }
            return message.getMessageLocation();
         } else {
            if (sizeExceeded) {
               log.warn("Message Rejected. Message larger than max allowed size (" + configurationManager.getMaximumMessageSize() + " MB)");
               if (sessionControl.isESMTP()) {
                  replyWriter.writeLast(SMTPServerSessionControl.MESSAGE_MESSAGE_TOO_LARGE);
                  return null;
               }
            }
            throw new Exception();
         }
      } catch (TooManyErrorsException tmee) {
         throw tmee;
      } catch (IOException ioe) {
         throw ioe;
      } catch (Exception e) {
         log.error("Unable to save incoming message.", e);
         replyWriter.writeLast(SMTPServerSessionControl.MESSAGE_SAVE_MESSAGE_ERROR);
         return null;
      } finally {
         SMTPMessagePersistenceFactory.removeUniqueId(message.getSmtpUid());
      }
   }

   private boolean sizeExceeded(long maxSize) {
      
      return message.getSize() > maxSize;
   }

   private void readOnceInputStream() throws IOException {

      byte[] buffer = new byte[finalBufferSize + 1];
      int currentRead = smtpSH.read(buffer, 0, finalBufferSize);
      if (currentRead == -1) {
         return;
      }
      currentRead = removeInitialNull(buffer, currentRead);

      constructLineOfText(currentRead, buffer);
   }

   private void readInputStream() throws IOException {

      byte[] buffer = new byte[finalBufferSize + 1];
      int currentRead = smtpSH.read(buffer, 0, finalBufferSize);
      if (currentRead == -1) {
         return;
      }

      constructLineOfText(currentRead, buffer);
   }

   private void constructLineOfText(int currentRead, byte[] buffer) throws IOException {

      int nextByte, previousRead = 0;
      for (int i = 0; i < currentRead; i++) {
         if (buffer[i] == 0x0d || buffer[i] == 0x0a) {
            if (i + 1 == currentRead) {
               nextByte = smtpSH.read();
               if (nextByte != -1) {
                  buffer[i + 1] = (byte) nextByte;
                  constructLineOfText3(buffer, true, previousRead, i);
               } //Perhaps a truncated end of DATA transmission. Check if it so.
               else if (output == null && i - previousRead == 1 && buffer[previousRead] == 0x2E) {
                  endOfMessage = true;
               }
               break;
            } else {
               i = constructLineOfText3(buffer, false, previousRead, i);
               if (endOfMessage) {
                  return;
               }
               previousRead = i + 1;
            }
         }
         if (i == currentRead - 1) {
            output = constructLineOfText2(output, buffer, previousRead, currentRead);
            finalBufferSize *= 2;
         }
      }
   }

   private byte[] constructLineOfText2(byte[] output, byte[] buffer, int startSegmentCount, int currentSegmentCount) {

      if (output != null) {
         int tempOutputLength;
         byte[] tempOutput;
         tempOutputLength = output.length;
         tempOutput = new byte[tempOutputLength];
         System.arraycopy(output, 0, tempOutput, 0, tempOutputLength);
         output = new byte[tempOutputLength + currentSegmentCount - startSegmentCount];
         System.arraycopy(tempOutput, 0, output, 0, tempOutputLength);
         System.arraycopy(buffer, startSegmentCount, output, tempOutputLength, currentSegmentCount - startSegmentCount);
      } else {
         output = new byte[currentSegmentCount - startSegmentCount];
         System.arraycopy(buffer, startSegmentCount, output, 0, currentSegmentCount - startSegmentCount);
      }
      return output;
   }

   private int constructLineOfText3(byte[] buffer, boolean increment, int previousRead, int i) throws IOException {

      if (buffer[i + 1] == 0x0a) {
         i++;
         output = constructLineOfText2(output, buffer, previousRead, i - 1);
         int outputLength = output.length;
         if (outputLength > 16 && outputLength <= 128) {
            finalBufferSize = outputLength + 2;
         }
         if (endOfMessage = checkEndOfDATA(output)) {
            return -1;
         }
         processDATA(output);
         if (buffer[i - 1] == 0x0a) {
            processDATA(new byte[0]);
         }
         output = null;
      } else {
         if (increment) {
            i++;
         }
         if (buffer[i] == 0x0a && output != null && output[output.length - 1] == 0x0d) {
            output = constructLineOfText2(output, output, 0, output.length);
            processDATA(output);
            output = null;
         } else {
            output = constructLineOfText2(output, buffer, previousRead, i + 1);
         }
      }
      return i;
   }

   private int removeInitialNull(byte[] input, int currentRead) {

      if (input[0] >= 0x20) {
         return currentRead;
      }
      int length = input.length - 1, count = 1;
      for (; count < length; count++) {
         if (input[count] >= 0x20) {
            break;
         }
      }
      int newCount = count;
      for (; newCount < length; newCount++) {
         input[newCount - count] = input[newCount];
      }
      newCount = length - count;
      for (; newCount < length; newCount++) {
         input[newCount] = 0x00;
      }

      return length - count;
   }

   SMTPMessage getMessage() {
      return message;
   }

   void setMessage8bitMIME(boolean message8bitMIME) {
      this.message8bitMIME = message8bitMIME;
   }

   String getClientIP() {
      return sessionControl.getClientIP();
   }

   boolean isAuthenticated() {
      return sessionControl.isAuthenticated();
   }

   void incrementValidRCPTCount() {
      validRCPT++;
   }

   void incrementFailedRCPTCount() {
      failedRCPT++;
   }
}
