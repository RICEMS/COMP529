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

import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.auth.AuthContext;
import com.ericdaugherty.mail.server.auth.CRAMServerMode;
import com.ericdaugherty.mail.server.auth.DigestMd5ServerMode;
import com.ericdaugherty.mail.server.auth.LoginServerMode;
import com.ericdaugherty.mail.server.auth.PlainServerMode;
import com.ericdaugherty.mail.server.auth.SCRAMServerMode;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.ConfigurationParameterConstants.ClearText;
import com.ericdaugherty.mail.server.configuration.MailServicesControl;
import com.ericdaugherty.mail.server.errors.SMTPFatalReplyException;
import com.ericdaugherty.mail.server.errors.SMTPReplyException;
import com.ericdaugherty.mail.server.errors.TooManyErrorsException;
import com.ericdaugherty.mail.server.errors.UnrecognizedCommandException;
import com.ericdaugherty.mail.server.security.transport.TransportLayer;
import com.ericdaugherty.mail.server.services.ConnectionProcessor;
import com.ericdaugherty.mail.server.services.ProcessorStreamHandler;
import com.ericdaugherty.mail.server.services.smtp.client.SMTPSender;
import com.ericdaugherty.mail.server.services.smtp.server.command.Command;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.AuthCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.DataCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.EhloCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.ExpnCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.HeloCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.MailCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.NoopCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.QuitCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.RcptCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.RsetCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.StlsCommand;
import com.ericdaugherty.mail.server.services.smtp.server.command.impl.VrfyCommand;
import com.ericdaugherty.mail.server.services.smtp.server.support.VerifyIP;
import com.ericdaugherty.mail.server.services.smtp.server.support.VerifyIPFactory;
import com.ericdaugherty.mail.server.services.smtp.server.transaction.TransactionControl;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class RFC5321SMTPServerSessionControl implements ConnectionProcessor, SMTPServerSessionControl {

   /**
    * Logger Category for this class.
    */
   private static final Log log = LogFactory.getLog(RFC5321SMTPServerSessionControl.class);
   /**
    * The ConfigurationManager
    */
   protected final ConfigurationManager configurationManager = ConfigurationManager.getInstance();
   
   /**
    * A list of commands allowed when the SMPT extensions are not allowed.
    */
   private static final Set<CommandVerb> baseCommands = EnumSet.of(
         CommandVerb.HELO,
         CommandVerb.MAIL,
         CommandVerb.RCPT,
         CommandVerb.DATA,
         CommandVerb.QUIT,
         CommandVerb.NOOP,
         CommandVerb.RSET,
         CommandVerb.VRFY);
   
   private Command lastCommand;
   private long anyCommand = 0;
   private FlowControl flowControl;
   private SessionState sessionState;
   /**
    * Indicates if this thread should continue to run or shut down
    */
   private volatile boolean running = true;
   /**
    * The server socket used to listen for incoming connections
    */
   private ServerSocket serverSocket;
   /**
    * Socket connection to the client
    */
   private Socket socket;
   /**
    * The IP address of the client
    */
   private String clientIP;
   /**
    * The host name of the client
    */
   private String clientDomain;
   /**
    * The client's host as declared by the EHLO/HELO command.
    */
   private String declaredClientHost;
   /**
    * This setting defines the security state of the session. Please note the following: A
    * connection over a standard port with the setting standardsecure=true starts as non-secure. A
    * connection over a standard port with the setting standardsecure=false is considered secure for
    * the duration of the session. A connection over a secure port is considered secure for the
    * duration of the session.
    */
   private boolean secured;
   /**
    * This setting is used to certify the encryption state of the connection
    */
   private boolean encrypted;
   /**
    * This setting is used to track whether ESMTP was used during the last session
    */
   private boolean eSMTP;
   private boolean rejected;
   private boolean mime8bitSupported;
   private boolean pipeliningSupported;
   private boolean heloEnabled;
   /**
    * The number of errors during a given session
    */
   private int errorCount;
   /**
    * The maximum number of allowed errors
    */
   private int maxErrorCount = configurationManager.getMaxErrorCount();
   private ClearText clearTextAllowed = configurationManager.allowClearTextSMTP();
   private volatile boolean updatingServerSocket;
   private final Object updateLock = new Object();
   protected boolean useAmavisSMTPDirectory;
   private boolean authenticated;
   private boolean authenticating;
   protected VerifyIP verifyIP;
   private ProcessorStreamHandler smtpPSH;
   private StandardReplyWriter replyWriter;
   private String[] instanceAuthMech;
   private SaslServer saslServer;
   private TransactionControl transactionControl;
   private TransportLayer transportLayer;
   private final FlowControl initFlowControl = new InitFlowControl();
   private final FlowControl mailFlowControl = new MailFlowControl();
   
   private final Map<String, Command> commandMap;

   {
      Map<String, Command> tempMap = new LinkedHashMap<String, Command>();
      
      tempMap.put(CommandVerb.EHLO.getLiteral(), new EhloCommand());
      tempMap.put(CommandVerb.STLS.getLiteral(), new StlsCommand());
      tempMap.put(CommandVerb.AUTH.getLiteral(), new AuthCommand());
      tempMap.put(CommandVerb.MAIL.getLiteral(), new MailCommand());
      tempMap.put(CommandVerb.RCPT.getLiteral(), new RcptCommand());
      tempMap.put(CommandVerb.DATA.getLiteral(), new DataCommand());
      tempMap.put(CommandVerb.QUIT.getLiteral(), new QuitCommand());
      tempMap.put(CommandVerb.RSET.getLiteral(), new RsetCommand());
      tempMap.put(CommandVerb.NOOP.getLiteral(), new NoopCommand());
      tempMap.put(CommandVerb.VRFY.getLiteral(), new VrfyCommand());
      tempMap.put(CommandVerb.EXPN.getLiteral(), new ExpnCommand());
      tempMap.put(CommandVerb.HELO.getLiteral(), new HeloCommand());

      commandMap = Collections.unmodifiableMap(tempMap);
   }

   /**
    * Sets the socket used to communicate with the client.
    * @param serverSocket
    */
   @Override
   public void setSocket(ServerSocket serverSocket) {

      this.serverSocket = serverSocket;
      if (log.isDebugEnabled())
         log.debug("serverSocketUpdated");
   }
   
   private boolean isDelayedStart() {
      return updatingServerSocket;
   }

   private boolean isUpdatingServerSocket() {
      return updatingServerSocket;
   }

   @Override
   public void setDelayedStart(boolean delayed) {
      setUpdatingServerSocket(delayed);
   }

   @Override
   public void setUpdatingServerSocket(boolean updating) {
      updatingServerSocket = updating;
      if (!updatingServerSocket) {
         synchronized(updateLock) {
            updateLock.notifyAll();
         }
      }
   }
   
   @Override
   public void invalidate(Domain domain) {}

   @Override
   public boolean getUseAmavisSMTPDirectory() {
      return useAmavisSMTPDirectory;
   }

   public void setUseAmavisSMTPDirectory(boolean useAmavisSMTPDirectory) {
      this.useAmavisSMTPDirectory = useAmavisSMTPDirectory;
   }

   public void setupVerifyIP() {
      verifyIP = VerifyIPFactory.getNewVerifyIPInstance(useAmavisSMTPDirectory
            || !configurationManager.isVerifyIP(), useAmavisSMTPDirectory);
   }

   /**
    * Entrypoint for the Thread, this method handles the interaction with the client socket.
    */
   @Override
   public void run() {
      
      if (isDelayedStart()) {
         synchronized(updateLock) {
            while(isDelayedStart() && running) {
               try {
                  updateLock.wait(500L);
               }
               catch(InterruptedException ie) {
                  if (!running) {
                     break;
                  }
               }
            }
         }
      }

      if (running)
         transactionControl = new TransactionControl(this, configurationManager.isAmavisSupportActive() && useAmavisSMTPDirectory);

      while (running) {
         
         long sleepTime = configurationManager.getThrottlingDelay();
         if (sleepTime != 0L) {
            try {
               Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
               //just proceed
            }
         }

         boolean connected = false;
         boolean forcedExit = false;
         authenticated = false;
         authenticating = false;
         eSMTP = true;
         mime8bitSupported = configurationManager.is8bitMIME();
         pipeliningSupported = configurationManager.isPipelining();
         heloEnabled = configurationManager.isHELOEnabled();
         rejected = false;
         errorCount = 0;
         instanceAuthMech = AuthMech.getActiveAuthMechs(configurationManager);
         try {
            socket = serverSocket.accept();

            secured = serverSocket.getLocalPort() == configurationManager.getSecureSMTPPort()
                  ? true : !configurationManager.isStandardSMTPSecure();
            encrypted = serverSocket.getLocalPort() == configurationManager.getSecureSMTPPort();
            if (secured && encrypted) {
               TransportLayer tLayer = new TransportLayer((SSLSocket) socket);
               try {
                  tLayer.verifyPeer(true, true);
               } catch (SSLPeerUnverifiedException ioe) {
                  log.error(ioe.getMessage());
                  encrypted = false;
                  //replace all the commands with ones that give a 554 reply
                  setFailedTLSHandshake();
               } finally {
                  tLayer.conclude();
               }
            }
            connected = true;

            //Prepare the input and output streams.
            smtpPSH = new ProcessorStreamHandler(socket);
            replyWriter = pipeliningSupported ? new PipelinedReplyWriter() : new StandardReplyWriter();
            transactionControl.setProcessorStreamHandler(smtpPSH);
            transactionControl.setReplyWriter(replyWriter);

            InetAddress remoteAddress = socket.getInetAddress();

            clientIP = remoteAddress.getHostAddress();
            clientDomain = remoteAddress.getHostName();

            log.info(clientDomain + "(" + clientIP + ") socket connected via SMTP.");

            //Output the welcome/reject message.
            if (!(remoteAddress.isLoopbackAddress() || remoteAddress.isSiteLocalAddress())
                  && verifyIP.blockIP(remoteAddress)) {
               replyWriter.writeLast(REJECT_MESSAGE);
               rejected = true;
            } else {
               replyWriter.writeLast(WELCOME_MESSAGE);
            }
            //Parses the input for commands and delegates to the appropriate methods.
            forcedExit = sessionFlowControl();

         } catch (TooManyErrorsException e) {
            log.error("The session generated too many errors.");
         } catch (SocketTimeoutException e) {
            if (connected) {
               log.error(" Timedout waiting for client command.");
               forcedExit = true;
            }
         } catch (IOException e) {
            if (running) {
               if (!updatingServerSocket)
                  log.error(" There was a error with the connection.", e);
               //There is a chance that an instance of this class has been instantiated but
               //its reference in ServiceListener points to null. Check the Mail instance for
               //the case of a server shutdown
               if (Mail.getInstance().isShuttingDown()) {
                  shutdown();
               }
            }
            if (socket != null) {
               if (!socket.isClosed()) {
                  forcedExit = true;
               } else {
                  connected = false;
               }
            }
         } catch (RuntimeException e) {
            //Don't allow unhandled exceptions to propagate.
            log.error("Unexpected Exception", e);
            connected = false;
         } finally {
            if (socket != null && connected) {
               try {
                  if (!forcedExit) {
                     replyWriter.writeLast((errorCount >= maxErrorCount) ? FORCED_EXIT_MESSAGE : MESSAGE_DISCONNECT);
                  } else {
                     if (transactionControl.isForceExitRCPT()) {
                        replyWriter.writeLast(MESSAGE_EXCESS_FAIL_RCPT_DISCONNECT);
                     } else {
                        replyWriter.writeLast(FORCED_EXIT_MESSAGE);
                     }
                  }
               } catch (TooManyErrorsException ex) {
               } catch (IOException ioe) {
               }
            }
            IOUtils.close(socket);
            if (saslServer != null) {
               try {
                  saslServer.dispose();
               } catch (SaslException ex) {
               }
               saslServer = null;
            }
            //reset the initial state
            sessionState = SessionState.INIT;
            transactionControl.resetMessage();
            anyCommand = 0;
            Iterator<Command> iter = commandMap.values().iterator();
            while(iter.hasNext()) {
               iter.next().reset();
            }
         }
         if (isUpdatingServerSocket()) {
            synchronized(updateLock) {
               while(isUpdatingServerSocket()) {
                  try {
                     updateLock.wait(500);
                  }
                  catch(InterruptedException ie) {
                     if (!running) {
                        break;
                     }
                  }
               }
            }
         }
      }
      log.warn("SMTPProcessor shut down gracefully");
   }

   /**
    * Notifies this thread to stop processing and exit.
    */
   @Override
   public void shutdown() {
      log.warn("Shutting down SMTPProcessor.");
      running = false;
      if (transactionControl == null || !transactionControl.isFinishedData()) {
         IOUtils.close(socket);
      }
   }

   private boolean sessionFlowControl() throws TooManyErrorsException,
         SocketTimeoutException, SocketException, IOException {

      String line;
      Command command;
      flowControl = initFlowControl;
      sessionState = SessionState.INIT;
      lastCommand = commandMap.get(CommandVerb.NOOP.getLiteral());

      do {

         line = read();

         try {

            if (line == null) {
               incrementErrorCount();
               throw new SMTPReplyException(MESSAGE_INVALID_COMMAND);
            }

            command = flowControl.getCommand(sessionState, line);

            if (command.isQuit()) {
               quitSession();
               return false;
            } else if (rejected) {
               incrementErrorCount();
               throw new SMTPReplyException(MESSAGE_INVALID_COMMAND);
            }

            if (transactionControl.isForceExitRCPT()) {
               //reply will be sent by the calling code
               return true;
            }

            if (!sessionState.isStateAllowedCommand(command.getCommandVerb())) {
               incrementErrorCount();
               throw new SMTPReplyException(MESSAGE_COMMAND_ORDER_INVALID);
            }
            else if(!eSMTP&&!baseCommands.contains(command.getCommandVerb())) {
               incrementErrorCount();
               throw new SMTPReplyException(MESSAGE_COMMAND_ORDER_INVALID);
            }

            flowControl.checkPrerequisites(command);

            command.parseInput(line);
            command.resetParser();
            command.executeActions(this);

            registerCommand(command.getCommandVerb());
            lastCommand = command;
         } catch (SMTPFatalReplyException e) {
            log.debug(e);
            return true;
         } catch (UnrecognizedCommandException e) {
            if (log.isDebugEnabled()) {
               log.debug("An unrecognized command in \""+line+"\" was received");
            }
            replyWriter.writeAny(e.getMessage());
         } catch (SMTPReplyException e) {
            if (log.isDebugEnabled()) {
               log.debug(e);
            }
            replyWriter.writeAny(e.getMessage());
         }
      } while (true);
   }

   private void registerCommand(CommandVerb commandVerb) {
      List<CommandVerb> commands = Arrays.asList(CommandVerb.values());
      anyCommand |= 1 << commands.indexOf(commandVerb);
   }

   abstract class FlowControl {

      private final CommandVerb[] commands = CommandVerb.values();
      private int index;
      private Pattern commandPattern;
      
      public FlowControl(int index) {
         this.index = index;
         this.commandPattern = Pattern.compile("(?i)" + commands[index].getLiteral() + "(?-i)");
      }

      public abstract void checkPrerequisites(Command command) throws SMTPReplyException;

      public Command getCommand(SessionState sessionState, String line) throws SMTPReplyException, UnrecognizedCommandException {

         int initialIndex = index;
         if (commandPattern == null)
            commandPattern = Pattern.compile("(?i)" + commands[index].getLiteral() + "(?-i)");
         Matcher matcher = commandPattern.matcher(line);
         do {
            if (matcher.find() && matcher.start() == 0) {

               return commandMap.get(commands[index].getLiteral());
            }
            index++;
            if (index == commands.length) {
               index = 0;
            }
            if (index == initialIndex) {
               commandPattern = null;
               incrementErrorCount();
               throw new UnrecognizedCommandException(SMTPServerSessionControl.MESSAGE_INVALID_COMMAND + " " + line);
            }
            commandPattern = Pattern.compile("(?i)" + commands[index].getLiteral() + "(?-i)");
            matcher = commandPattern.matcher(line);
         } while (true);
      }
      
      public void setCommandIndex(int index) {
         this.index = index;
         commandPattern = Pattern.compile("(?i)" + commands[this.index].getLiteral() + "(?-i)");
      }
   }

   private class InitFlowControl extends FlowControl {

      public InitFlowControl() {
         super(0);
      }
      
      protected InitFlowControl(int index) {
         super(index);
      }

      @Override
      public void checkPrerequisites(Command command) throws SMTPReplyException {
         command.checkInitPrerequisites(RFC5321SMTPServerSessionControl.this);
      }
   }

   private class MailFlowControl extends InitFlowControl {

      public MailFlowControl() {
         super(4);
      }

      @Override
      public void checkPrerequisites(Command command) throws SMTPReplyException {
         super.checkPrerequisites(command);
         command.checkMailPrerequisites(RFC5321SMTPServerSessionControl.this);
      }
   }

   @Override
   public boolean handleMailFrom(String address, String[] parameters) throws TooManyErrorsException, IOException {
      return transactionControl.handleMailFrom(address, parameters);
   }

   @Override
   public void handleRcptTo(String address, String[] parameters) throws TooManyErrorsException, IOException {
      transactionControl.handleRcptTo(address, parameters);
   }

   @Override
   public void handleData() throws TooManyErrorsException, SocketTimeoutException,
         SocketException, IOException {
      String messageLocation = transactionControl.handleData();
      if (messageLocation == null)
         return;
      MailServicesControl msc = MailServicesControl.getInstance();
      if (msc == null)
         return;
      SMTPSender smtpSender = !useAmavisSMTPDirectory ? msc.getSmtpSender() : msc.getAmavisSmtpSender();
      if (smtpSender != null)
         smtpSender.submitMessage(messageLocation);
   }

   @Override
   public void startTLSHandshake() throws SMTPReplyException {

      transportLayer = new TransportLayer();

      try {
         transportLayer.init(socket, true, true, true);
      } catch (IOException ioe) {
         log.error(ioe.getMessage());
         transportLayer = null;
         throw new SMTPReplyException(MESSAGE_TLS_NOT_AVAILABLE);
      }
   }

   @Override
   public void concludeTLSHandshake() throws SMTPFatalReplyException {

      boolean acceptable = false;
      try {
         transportLayer.verifyPeer(true, true);
         String cipher = ((SSLSocket) transportLayer.getSocket()).getSession().getCipherSuite();
         log.info("Negotiated Cipher: " + cipher);
         for (String ec : configurationManager.getEnabledCiphers()) {
            if (cipher.equals(ec)) {
               acceptable = true;
               break;
            }
         }
         if (!acceptable) {
            log.info("Negotiated Cipher Suite not acceptable!");
            setFailedTLSHandshake();
            return;
         }

         socket = transportLayer.getSocket();
         try {
            setSuccessTLSHandshake();
         } catch (IOException ioe) {
            log.error(ioe.getLocalizedMessage());
            throw new SMTPFatalReplyException();
         }
      } catch (SSLPeerUnverifiedException ioe) {
         log.error(ioe.getMessage());
         setFailedTLSHandshake();
      } finally {
         transportLayer.conclude();
         transportLayer = null;
      }
   }

   private void setSuccessTLSHandshake() throws IOException {
      secured = true;
      encrypted = true;
      smtpPSH.setSecureStreams(socket);
   }

   private void setFailedTLSHandshake() {
      Iterator<Command> iter = commandMap.values().iterator();
      while (iter.hasNext()) {
         iter.next().setNoEncryptedCommandActions();
      }
      secured = false;
   }

   @Override
   public void incrementErrorCount() {
      errorCount++;
   }

   @Override
   public void setDeclaredClientHost(String declaredClientHost) {
      if (this.declaredClientHost != null) {
         return;
      }
      this.declaredClientHost = declaredClientHost;
   }

   @Override
   public String getDeclaredClientHost() {
      return declaredClientHost;
   }

   @Override
   public String getClientIP() {
      return clientIP;
   }

   @Override
   public String getClientDomain() {
      return clientDomain;
   }

   @Override
   public boolean isCommandReceived(CommandVerb commandVerb) {
      List<CommandVerb> commands = Arrays.asList(CommandVerb.values());
      int pos = 1 << commands.indexOf(commandVerb);
      return ((pos & anyCommand) == pos);
   }

   @Override
   public boolean isLastCommand(CommandVerb commandVERB) {
      return commandVERB == lastCommand.getCommandVerb();
   }

   @Override
   public boolean isSecured() {
      return secured;
   }

   @Override
   public boolean isEncrypted() {
      return encrypted;
   }

   @Override
   public boolean isAuthenticated() {
      return authenticated;
   }

   @Override
   public boolean isMime8bitSupported() {
      return mime8bitSupported;
   }

   @Override
   public boolean isPipeliningSupported() {
      return pipeliningSupported;
   }

   @Override
   public boolean isHeloEnabled() {
      return heloEnabled;
   }

   @Override
   public void setESMTP(boolean eSMTP) {
      this.eSMTP = eSMTP;
      pipeliningSupported = false;
      mime8bitSupported = false;
      replyWriter = new StandardReplyWriter();
      transactionControl.setReplyWriter(replyWriter);
   }

   @Override
   public boolean isESMTP() {
      return eSMTP;
   }

   @Override
   public boolean isTooManyRCPT() {
      return transactionControl.isTooManyRCPT();
   }

   @Override
   public boolean isExcessRCPT() {
      return transactionControl.isExcessRCPT();
   }

   @Override
   public boolean isSingleRCPT() {
      return transactionControl.isSingleRCPT();
   }

   @Override
   public void setSingleRCPT(boolean singleRCPT) {
      transactionControl.setSingleRCPT(singleRCPT);
   }

   @Override
   public boolean isRCPTListEmpty() {
      return transactionControl.isRCPTListEmpty();
   }

   @Override
   public ClearText getClearTextAllowed() {
      return clearTextAllowed;
   }

   @Override
   public String[] getAuthMechs() {
      return instanceAuthMech.clone();
   }

   @Override
   public String getSSLHeaderField() {

      if (secured
            && ((configurationManager.isSecureActive() && serverSocket.getLocalPort() == configurationManager.getSecureSMTPPort())
            || (configurationManager.isStandardSMTPSecure() && serverSocket.getLocalPort() == configurationManager.getSMTPPort()))) {
         StringBuilder sb = new StringBuilder("        (using ");
         sb.append(((SSLSocket) socket).getSession().getProtocol()).append(" protocol with ");
         sb.append(((SSLSocket) socket).getSession().getCipherSuite()).append(" ciphersuite.)");
         return sb.toString();
      }
      return null;
   }

   @Override
   public void setReplyAny(String reply) throws TooManyErrorsException, IOException {
      replyWriter.writeAny(reply);
   }

   @Override
   public void setReplyLast(String reply) throws TooManyErrorsException, IOException {
      replyWriter.writeLast(reply);
   }

   @Override
   public void setMultiReplyLast(List<String> reply) throws TooManyErrorsException, IOException {
      for (int i = 0; i < reply.size(); i++) {
         replyWriter.writeLast(reply.get(i));
      }
   }

   @Override
   public void createSASLServer(SaslServerMode saslServerMode, String mechanism) throws SaslException {
      
      log.debug("Creating new SMTP server mode SASL server: "+saslServerMode);
      switch (saslServerMode) {
         case PLAIN:
            saslServer = new PlainServerMode(true);
            break;
         case SCRAM:
            saslServer = new SCRAMServerMode(true, mechanism);
            break;
         case LOGIN:
            saslServer = new LoginServerMode(true);
            break;
         case CRAM:
            saslServer = new CRAMServerMode(true,
                  configurationManager.getBackEnd().getDefaultDomain().getDomainName(),
                  mechanism);
            break;
         case GSSAPI:
            saslServer = AuthContext.getInstance().getGSSServerMode(false, null);
            break;
         case MD5_DIGEST:
            saslServer = new DigestMd5ServerMode(true);
            break;
         default:
            throw new AssertionError();
      }
      authenticating = true;
   }

   @Override
   public String getClientResponse() throws SocketException, SocketTimeoutException, IOException{
      if (authenticating) {
         return read();
      }
      throw new IllegalStateException("Currently not authenticating.");
   }

   @Override
   public int getGSSResponse(byte[] token, int startIndex, int tokenLength) throws IOException {
      if (saslServer.getMechanismName().equals("GSSAPI") && !saslServer.isComplete()) {
         return smtpPSH.read(token, 0, tokenLength);
      }
      return -1;
   }

   @Override
   public byte[] evaluateSASLResponse(byte[] response) throws SaslException {
      return saslServer.evaluateResponse(response);
   }

   @Override
   public boolean isSASLComplete() {
      return saslServer.isComplete();
   }

   @Override
   public void setSuccessSASLNegotiation() throws IOException {

      String qop = (String) saslServer.getNegotiatedProperty(Sasl.QOP);
      if (qop.equals("auth-int") || qop.equals("auth-conf")) {
         smtpPSH.setSaslServer(saslServer);
      }
      authenticating = false;
      authenticated = true;
   }

   @Override
   public String getAuthorizationID() {
      if (saslServer != null && saslServer.isComplete()) {
         return saslServer.getAuthorizationID();
      }
      return null;
   }

   @Override
   public SessionState getSessionState() {
      return sessionState;
   }

   @Override
   public void setInitState(boolean reset) {
      if (this.sessionState == SessionState.INIT) {
         //if (reset) {
            //According to RFC4954:
            //"When a security layer takes effect, the SMTP protocol is reset to the
            //initial state (the state in SMTP after a server issues a 220 service
            //ready greeting)."
            //That definition includes knowledge of previously issued commands. Un-
            //fortunately, JavaMail ignores this statement and during a session
            //proceeds to issue a MAIL command after a successful AUTH command. For
            //the shake of interoperability, knowledge of previous commands is retained.
            
            //anyCommand = 0;
         //}
         flowControl.setCommandIndex(reset?0:1);
         lastCommand = commandMap.get(CommandVerb.NOOP.getLiteral());
      } else {
         if (reset) {
            throw new IllegalArgumentException("Reseting is not allowed while a mail transaction is"
                  + " in progress.");
         }
         flowControl = initFlowControl;
         flowControl.setCommandIndex(3);

         //Need to unregister MAIL, RCPT, DATA
         List<CommandVerb> commands = Arrays.asList(CommandVerb.values());
         long mask = 0;
         mask |= 1 << commands.indexOf(CommandVerb.MAIL);
         mask |= 1 << commands.indexOf(CommandVerb.RCPT);
         mask |= 1 << commands.indexOf(CommandVerb.DATA);


         mask = ~mask;
         anyCommand &= mask;
      }
      this.sessionState = SessionState.INIT;
      transactionControl.resetMessage();
   }

   @Override
   public void setMailState() {
      flowControl = mailFlowControl;
      flowControl.setCommandIndex(4);
      this.sessionState = SessionState.MAIL;
   }

   @Override
   public void quitSession() {
      if (log.isDebugEnabled()) {
         log.debug("Client " + (declaredClientHost==null?"":declaredClientHost) + " with actual address "
               + socket.getRemoteSocketAddress() + " has QUIT the session.");
      }
   }

   /**
    * Reads a line from the input stream and returns it.
    * @return 
    * @throws java.net.SocketException
    * @throws java.net.SocketTimeoutException
    */
   @Override
   public String read() throws SocketException, SocketTimeoutException, IOException {

      try {
         socket.setSoTimeout(5 * 60 * 1000);
         String inputLine = smtpPSH.readLine();
         //Log the input, unless it is a password.
         if (log.isDebugEnabled() && !inputLine.toUpperCase().startsWith("PASS")) {
            log.debug("Read Input: " + inputLine);
         }
         return inputLine;
      } catch (NullPointerException npe) {
         return null;
      }
   }

   public class StandardReplyWriter implements ReplyWriter {

      /**
       * Writes the specified output message to the client.
       */
      @Override
      public void writeAny(String reply) throws TooManyErrorsException, IOException {
         writeLast(reply);
      }

      /**
       * Writes the specified output message to the client.
       */
      @Override
      public void writeLast(String reply) throws TooManyErrorsException, IOException {
         if (errorCount >= maxErrorCount) {
            throw new TooManyErrorsException();
         }
         if (reply != null) {
            if (log.isDebugEnabled()) {
               log.debug("Writing Output: " + reply);
            }
            smtpPSH.print(reply);
         }
      }
   }

   private final class PipelinedReplyWriter extends StandardReplyWriter {

      private static final String CRLF = "\r\n";
      private int pipelineSize = 0;
      private boolean tooManyErrors = false;
      List<String> pipeline = new ArrayList<String>(10);
      private int supportCount = 0;
      private boolean supported = false;

      /**
       * Writes the specified output message to the client.
       */
      @Override
      public void writeAny(String reply) throws TooManyErrorsException, IOException {
         if (errorCount >= maxErrorCount) {
            tooManyErrors = true;
            throw new TooManyErrorsException();
         }
         if (reply != null) {
            if (!supported) {
               //Handle the SASL PLAIN case. The AUTH is always the last command in a queue except
               //for PLAIN (and EXTERNAL) that may not be. Check for incoming data. The support
               //count is not incremented in this case, but if there is available data, then there
               //is no reason not to accept that the client does indeed support PIPELINING.
               if (authenticating&&saslServer.getMechanismName().equals("PLAIN")) {
                  verifyPipelining(reply, 1, 500);
                  if (!supported) {
                     return;
                  }
               }
               //We need somehow to deal with the fact that the client may not support pipelining.
               else {
                  if (supportCount<3) {
                     supportCount = verifyPipelining(reply, supportCount, 333);
                     if (!supported) {
                        return;
                     }
                  }
                  //If two PIPELINING verifications fail, switch to StandardReplyWriter
                  else {
                     if (log.isDebugEnabled()) {
                        log.debug("Client does not support PIPELINING. Switching to StandardReplyWriter.");
                     }
                     writeLast(reply);
                     replyWriter = new StandardReplyWriter();
                     transactionControl.setReplyWriter(replyWriter);
                     return;
                  }
               }
            }
            if (log.isDebugEnabled()) {
               log.debug("Queuing Output: " + reply);
            }
            pipelineSize += reply.length();
            pipeline.add(reply);
         }
      }
      
      private int verifyPipelining(String reply, int supportCount, int time) throws IOException, TooManyErrorsException{
         int available = smtpPSH.available();
         //Need to account for network latency
         if (available == 0) {
            int count = 1;
            do {
               try {
                  Thread.sleep(1L * supportCount * count * count * time);
                  available = smtpPSH.available();
                  //If it is not the last command, there should be input queuing up.
                  if (available != 0) {
                     break;
                  }
               }
               catch (InterruptedException ie) {
                  if (!running) {
                     writeLast(reply);
                     //TODO In the future come up with something more elegant
                     throw new TooManyErrorsException("Shutting down.");
                  }
               }
               count++;
            }while(count < 4);
         }
         //Treat it as if it were last
         if (available == 0) {
            supportCount++;
            writeLast(reply);
            return supportCount;
         }
         //If even once there is data available, it is undeniable evidence that PIPELINING
         //is indeed supported
         else {
            supported = true;
            return 0;
         }
      }

      /**
       * Writes the specified output message to the client.
       */
      @Override
      public void writeLast(String reply) throws TooManyErrorsException, IOException {
         if (errorCount >= maxErrorCount) {
            if (!tooManyErrors) {
               tooManyErrors = true;
               throw new TooManyErrorsException();
            } else {
               StringBuilder sb = new StringBuilder(pipelineSize);
               pipelineSize = 0;
               for (String line : pipeline) {
                  sb.append(line);
                  sb.append(CRLF);
               }
               sb.append(SMTPServerSessionControl.FORCED_EXIT_MESSAGE);
               pipeline.clear();
               pipeline = null;
               if (log.isDebugEnabled()) {
                  log.debug("Writing Output: " + reply);
               }
               smtpPSH.print(sb.toString());
               return;
            }
         }
         if (reply != null) {
            if (log.isDebugEnabled()) {
               log.debug("Writing Output: " + reply);
            }
            pipelineSize += reply.length();
            StringBuilder sb = new StringBuilder(pipelineSize);
            pipelineSize = 0;
            for (String line : pipeline) {
               sb.append(line);
               sb.append(CRLF);
            }
            sb.append(reply);
            pipeline.clear();
            pipeline = new ArrayList(10);
            smtpPSH.print(sb.toString());
         }

      }
   }

   /**
    * Writes the specified output message to the client.
    * @param reply
    * @param errorIncrement
    * @throws com.ericdaugherty.mail.server.errors.TooManyErrorsException
    * @throws java.io.IOException
    */
   @Override
   public void write(String reply, int errorIncrement) throws TooManyErrorsException, IOException {
      throw new RuntimeException("Replies are exclusively handled by instances of the ResponseHandler class.");
   }
}