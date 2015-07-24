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

package com.ericdaugherty.mail.server.services.smtp.server;

import com.ericdaugherty.mail.server.configuration.ConfigurationParameterConstants.ClearText;
import com.ericdaugherty.mail.server.errors.SMTPFatalReplyException;
import com.ericdaugherty.mail.server.errors.SMTPReplyException;
import com.ericdaugherty.mail.server.errors.TooManyErrorsException;
import com.ericdaugherty.mail.server.services.smtp.SMTPSessionControl;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import javax.security.sasl.SaslException;

/**
 *
 * @author Andreas Kyrmegalos
 */
public interface SMTPServerSessionControl extends SMTPSessionControl {

   boolean getUseAmavisSMTPDirectory();
   
   void startTLSHandshake() throws SMTPReplyException;
   
   void concludeTLSHandshake()throws SMTPFatalReplyException;

   void incrementErrorCount();

   void setDeclaredClientHost(String declaredClientHost);

   String getDeclaredClientHost();
   
   String getClientIP();
   
   String getClientDomain();

   boolean isCommandReceived(CommandVerb commandVerb);

   boolean isLastCommand(CommandVerb commandVerb);

   boolean isSecured();

   boolean isEncrypted();
   
   boolean isAuthenticated();

   boolean isMime8bitSupported();

   boolean isPipeliningSupported();

   boolean isHeloEnabled();

   void setESMTP(boolean esmtp);

   boolean isESMTP();

   boolean isTooManyRCPT();

   boolean isExcessRCPT();

   public boolean isSingleRCPT();

   public void setSingleRCPT(boolean singleRCPT);

   boolean isRCPTListEmpty();

   ClearText getClearTextAllowed();

   String[] getAuthMechs();
   
   String getSSLHeaderField();

   boolean handleMailFrom(String address, String[] parameters) throws TooManyErrorsException, IOException;

   void handleRcptTo(String address, String[] parameters) throws TooManyErrorsException, IOException;
   
   void handleData() throws TooManyErrorsException, SocketTimeoutException,
         SocketException, IOException;

   void setReplyAny(String reply) throws TooManyErrorsException, IOException;

   void setReplyLast(String reply) throws TooManyErrorsException, IOException;

   void setMultiReplyLast(List<String> reply) throws TooManyErrorsException, IOException;
   
   void createSASLServer(SaslServerMode saslServer, String mechanism) throws SaslException;
   
   String getClientResponse() throws SocketException, SocketTimeoutException, IOException;
   
   int getGSSResponse(byte[] token, int startIndex, int tokenLength) throws IOException;
   
   byte[] evaluateSASLResponse(byte[] response) throws SaslException;
   
   boolean isSASLComplete();
   
   void setSuccessSASLNegotiation() throws IOException;
   
   String getAuthorizationID();
   
   SessionState getSessionState();

   void setInitState(boolean reset);

   void setMailState();

   void quitSession();
   
   String WELCOME_MESSAGE = "220 ESMTP Server";
   String REJECT_MESSAGE = "554 The connecting IP has been identified as a source of unsolicited mail";
   String FORCED_EXIT_MESSAGE = "421 Service not available, closing transmission channel";
   String MESSAGE_DISCONNECT = "221 ESMTP server signing off";
   String MESSAGE_OK = "250 OK";
   String MESSAGE_COMMAND_ORDER_INVALID = "503 Bad sequence of commands";
   String MESSAGE_USER_INVALID = "550 Requested action not taken: mailbox unavailable";
   String MESSAGE_RCPT_DOMAIN_REJECTED = "550 Recipient Domain not accepted; a policy restriction is in place";
   String MESSAGE_SEND_DATA = "354 Start mail input; end with <CRLF>.<CRLF>";
   String MESSAGE_SAVE_MESSAGE_ERROR = "451 Requested action aborted: local error in processing";
   String MESSAGE_INVALID_COMMAND = "500 Command Unrecognized: ";
   String MESSAGE_AUTH_TOO_LONG_COMMAND = "5.5.6  Authentication Exchange line is too long";
   String MESSAGE_SYNTAX_ERROR_PARAMETER = "501 Syntax error in parameters or arguments";
   String MESSAGE_NO_PARAMETER = "501 Syntax error (no parameters allowed)";
   String MESSAGE_MESSAGE_TOO_LARGE = "552 Message size exceeds fixed maximum message size";
   String MESSAGE_NO_VRFY_SUPPORT = "252 Cannot VRFY user, but will accept message and attempt delivery";
   String MESSAGE_NO_EXPN_SUPPORT = "252 Cannot EXPN list";
   String MESSAGE_NO_VALID_RCPT = "554 no valid recipients given";
   String MESSAGE_FAILED_TRANSACTION = "554 Transaction Failed. ";
   String MESSAGE_TLS_NOT_AVAILABLE = "454 TLS not available due to temporary reason";
   String MESSAGE_NO_HELO_ACCEPTED = "554 Active Security Context rejects the HELO command";
   String MESSAGE_NO_SECURITY = "554 Command refused due to lack of security";
   String MESSAGE_AUTH_SUCCESS = "235 ";
   String MESSAGE_INTERMEDIATE = "334 ";
   String MESSAGE_ALREADY_AUTHENTICATED = "503 Already authenticated";
   String MESSAGE_UNRECOGNIZED_AUTH_MECH = "504 Unrecognized authentication type";
   String MESSAGE_AUTH_FAILED = "535 Authentication credentials invalid";
   String MESSAGE_AUTH_CANCELLED = "501 ";
   String MESSAGE_AUTH_FAILED_CUSTOM = "535 ";
   String MESSAGE_TOO_MANY_RCPT = "452 Too many recipients";
   String MESSAGE_EXCESS_RCPT = "503 Security policy: Excessive number of recipients";
   String MESSAGE_EXCESS_FAIL_RCPT_DISCONNECT = "421 Security policy: Excessive number of failed recipients";

   public static interface ReplyWriter {

      /**
       * Writes the specified output message to the client.
       * @param reply
       * @throws com.ericdaugherty.mail.server.errors.TooManyErrorsException
       * @throws java.io.IOException
       */
      public void writeAny(String reply) throws TooManyErrorsException, IOException;

      /**
       * Writes the specified output message to the client.
       * @param reply
       * @throws com.ericdaugherty.mail.server.errors.TooManyErrorsException
       * @throws java.io.IOException
       */
      public void writeLast(String reply) throws TooManyErrorsException, IOException;
   }
}
