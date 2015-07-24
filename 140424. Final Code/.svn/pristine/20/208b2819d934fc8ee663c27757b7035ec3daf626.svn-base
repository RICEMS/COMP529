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

//Java Imports
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import javax.net.ssl.SSLSocket;
import javax.security.auth.callback.*;
import javax.security.sasl.*;

//Log imports
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

//Encoding imports
import org.apache.commons.codec.binary.Base64;

//Local Imports
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.DefaultSMTPServer;
import com.ericdaugherty.mail.server.errors.AuthenticationException;
import com.ericdaugherty.mail.server.errors.PermanentNegativeException;
import com.ericdaugherty.mail.server.errors.TransientNegativeException;
import com.ericdaugherty.mail.server.security.transport.TransportLayer;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;
import com.ericdaugherty.mail.server.services.smtp.client.auth.ClientSMTPAuthenticate;
import com.ericdaugherty.mail.server.services.smtp.client.auth.impl.ClientSMTPAuthenticatorFactory;
import com.ericdaugherty.mail.server.services.smtp.client.support.FailedAddressItem;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.ClientRequest;
import com.xlat4cast.jes.dns.Resolver;
import com.xlat4cast.jes.dns.client.IPReply;
import com.xlat4cast.jes.dns.client.MXReply;
import com.xlat4cast.jes.dns.internal.DNSException;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 * This class handles sending messages to external SMTP servers for delivery.
 *
 * @author Eric Daugherty
 * @author Andreas Kyrmegalos (2.x branch)
 */
public class SMTPRemoteSender extends MIMESender implements SMTPClientSessionControl {

    /** Logger */
    private static final Log log = LogFactory.getLog( SMTPRemoteSender.class );

    /** ConfigurationManager */
    private final ConfigurationManager configurationManager = ConfigurationManager.getInstance();

    /** Socket connection to the client */
    private Socket socket;

    // Credentials for authentication with the default SMTP server
    private String username;
    private String password;
    private String serverName;
    private String realm;
    
    private int session_stage = SESSION_PREBG;
    private boolean success, conclude, proceed, atLeastOneRCPTAccepted, mime8bit;
    private boolean ehlo = true;
    private List<String> capabilities;
    private List<String> tempResponse = new ArrayList<String>(0);
    private List<EmailAddress> originalAddresses;
    private List<EmailAddress> remainingAddresses;
    private List<FailedAddressItem> failedAddresses = new ArrayList<FailedAddressItem>();
    private Prepareaction prepareaction;

    public List<FailedAddressItem> getFailedAddresses() {
       return failedAddresses;
    }

    public void cleanUp() {
       tempResponse.clear();tempResponse = null;
       originalAddresses.clear();originalAddresses = null;
       remainingAddresses.clear();remainingAddresses = null;
       if (capabilities!=null) {
          capabilities.clear();
          capabilities = null;
       }
       failedAddresses.clear();failedAddresses = null;
    }
    
    public SMTPRemoteSender() {}

   /**
    * Handles delivery of messages to addresses not handled by this server.
    * @param address
    * @param message
    * @throws com.ericdaugherty.mail.server.errors.TransientNegativeException
    * @throws com.ericdaugherty.mail.server.errors.PermanentNegativeException
    */
   public void sendMessage( EmailAddress address, SMTPMessage message ) throws TransientNegativeException, PermanentNegativeException {
      Domain remoteDomain = address.getDomain();
      List<EmailAddress> addresses = new ArrayList<EmailAddress>(1);
      addresses.add(address);
      sendMessage(null, remoteDomain, addresses, message);
   }
    
   public final void sendMessage( Domain remoteDomain, List<EmailAddress> addresses, SMTPMessage message )
         throws TransientNegativeException, PermanentNegativeException {
      sendMessage(null, remoteDomain, addresses, message);
   }
    
    protected void sendMessage( Socket socketPre, Domain remoteDomain, List<EmailAddress> addresses, SMTPMessage message )
          throws TransientNegativeException, PermanentNegativeException {

       originalAddresses = new ArrayList<EmailAddress>(addresses);
       remainingAddresses = new ArrayList<EmailAddress>(addresses);

       StringBuilder localDomain = new StringBuilder(message.getFromAddress().getDomain().getDomainName());
       //Provision for proper identification of mail sender when on a dynamic DNS where the mail receiver uses reverseDNS
       //to identify the sender
       if (isReverseDNSserver(remoteDomain)) {
          try {
             Resolver dnsResolver = Mail.getInstance().getDNSResolver();
             IPReply reply = dnsResolver.submitRequest(new ClientRequest.IPRequest(message.getFromAddress().getDomain().getDomainName()));
             List<InetAddress> ipAddresses = reply.getIPAddresses();
             if (ipAddresses.isEmpty())
                throw new DNSException("No IP address was found.");
             localDomain = new StringBuilder("[").append(ipAddresses.get(0).getHostAddress()).append("]");
          } catch (DNSException ex) {
             log.error( ex.getLocalizedMessage() );
             //If there is an error here it's better ro retry sending the message.
             //It most likely has to do with temporary improper lookup results.
             throw new TransientNegativeException( "Unable to complete the transaction due to DNS lookup error.", null );
          }
       }
       
        //Open the connection to the server.
        if (socketPre == null) {
           try {
              socket = connect( remoteDomain );
           }
           catch(PermanentNegativeException pne) {
              while(remainingAddresses.size()>0) {
                 failedAddresses.add(new FailedAddressItem(remainingAddresses.remove(0), "554 "+pne.getLocalizedMessage()));
              }
              throw pne;
           }
        }
        else {
           socket = socketPre;
        }

        try {
            reset();
            //Get the input and output streams.
            smtpSSH = new SenderStreamHandler(socket);
            
            Waitforconnection();
            
            processreply();
            if (prepareaction.resend) {
               throw new TransientNegativeException( new StringBuilder(prepareaction.replycode).append(prepareaction.replyargument).toString(), null );
            }
            do {
               sendcommand( localDomain, message );
               processreply();
               if (conclude) {
                  if (success) break;
                  else {
                     StringBuilder exceptionmessage =
                           new StringBuilder(prepareaction.replycode).append(prepareaction.replyargument);
                     if (prepareaction.resend) {
                        throw new TransientNegativeException( exceptionmessage.toString(), null );
                     }
                     else {
                        throw new PermanentNegativeException( exceptionmessage.toString() );
                     }
                  }
               }
            }while(true);
        }
        catch (IOException ioe) {
           log.error( ioe.getMessage() );
           throw new TransientNegativeException( "Unable to complete the transaction due to internal error.", ioe );
        }
        finally {
            IOUtils.close(socket);
        }
    }

    private void Waitforconnection() throws SocketException, TransientNegativeException {
       int count = 0;
       while (!socket.isConnected()) {
          try {
             Thread.sleep(500);
          } catch (InterruptedException ex) {
             log.error( ex.getMessage() );
             throw new TransientNegativeException( "Unable to complete the transaction due to internal error.", null );
          }
          if (count++> 120)
             throw new TransientNegativeException( "No connection established. Retrying later.", null );
       }
       socket.setSoTimeout(5 * 60 * 1000);
    }
    
    private void reset() {
       prepareaction = new Prepareaction();
       session_stage = SESSION_PREBG;
       success = false;
       conclude = false;
       proceed = false;
       mime8bit = false;
       atLeastOneRCPTAccepted = false;
       ehlo = true;
       capabilities = null;
       tempResponse = new ArrayList<String>(4);
       failedAddresses = new ArrayList<FailedAddressItem>();
       remainingAddresses = new ArrayList<EmailAddress>(originalAddresses);
       initialHeaders = true;
       readingHeaders = false;
       convertNextPart = false;
       if (boundaries!=null) boundaries.clear();
       boundaries = new ArrayList<String>();
       mime = MIME_UNDEFINED;
       emptyStringCount = currentRead = previousRead = currentTotal = 0;
       if (bufferForb64os != null) {
          for (int i=bufferForb64os.length-1;i>=0;i--) {
             bufferForb64os[i] = 0;
          }
          bufferForb64os = null;
       }
    }
    
    private void sendcommand( StringBuilder localDomain, SMTPMessage message )
          throws SocketException, IOException{
       
       switch (session_stage) {
          case SESSION_EHLO: {
             if (ehlo) write( "EHLO " + localDomain );
             else write( "HELO " + localDomain );
             socket.setSoTimeout(5 * 60 * 1000);
          }break;
          case SESSION_MAIL: {
             if (ehlo) {
                if (supportedCapability("8BITMIME")) {
                   mime8bit = true;
                }
                if (supportedCapability("SIZE")) {
                   write( "MAIL FROM:<" + message.getFromAddress().getAddress() + "> SIZE="+(message.getPersistedSize()-20L) );
                   socket.setSoTimeout(5 * 60 * 1000);
                   return;
                }
             }
             write( "MAIL FROM:<" + message.getFromAddress().getAddress() + ">" );
             socket.setSoTimeout(5 * 60 * 1000);
          }break;
          case SESSION_RCPT: {
             write( "RCPT TO:<" + remainingAddresses.get(0) + ">" );
             socket.setSoTimeout(5 * 60 * 1000);
          }break;
          case SESSION_DATA: {
             if (!proceed) {
                write( "DATA" );
                socket.setSoTimeout(2 * 60 * 1000);
             }
             else {
                sendData( message );
                socket.setSoTimeout(10 * 60 * 1000);
             }
          }break;
          case SESSION_QUIT: {
             conclude = true;
             write( "QUIT" );
             socket.setSoTimeout(5 * 60 * 1000);

          }break;
          case SESSION_RESET: {
             write( "RSET" );
             socket.setSoTimeout(5 * 60 * 1000);
          }break;
          default: {
             throw new AssertionError("Illegal session stage value: "+session_stage);
          }
       }
    }
    
    private void authenticate() throws TransientNegativeException, SocketException, SocketTimeoutException, IOException{
       
       if (supportedCapability("STARTTLS")&&configurationManager.isOutgoingSecure()) {
          
          write( "STARTTLS" );
          socket.setSoTimeout(5 * 60 * 1000);
          String replycode = read();
          if (replycode.length() != 0) {
             char firstdigit = replycode.charAt(0);
             if (firstdigit == TWO) {
                TransportLayer transportLayer = new TransportLayer();
                try {
                   boolean acceptable = false;
                   transportLayer.init(socket,false,true,true);
                   String cipher = ((SSLSocket)transportLayer.getSocket()).getSession().getCipherSuite();
                   log.info("Negotiated Cipher: " + cipher);
                   for (String ec : configurationManager.getEnabledCiphers()) {
                      if (cipher.equals(ec)) {
                         acceptable = true;
                         break;
                      }
                   }
                   if (!acceptable) {
                      session_stage = SESSION_RESET;
                      prepareaction.setup(replycode, true);
                      return;
                   }
                   socket = transportLayer.getSocket();
                   smtpSSH.setSecureStreams(socket);
                   session_stage = SESSION_EHLO;
                } catch (IOException ex) {
                   //Per RFC 3207:
                   //If, after having issued the STARTTLS command, the client finds out that
                   //some failure prevents it from actually starting a TLS handshake, then
                   //it SHOULD abort the connection.
                   throw new TransientNegativeException(ex.getMessage(), ex);
                }
                finally {
                   transportLayer.conclude();
                }
             }
             else if (firstdigit == FOUR) {
                session_stage = SESSION_RESET;
                prepareaction.setup(replycode, true);
             }
//             else if (firstdigit == FIVE) {
//                session_stage = SESSION_RESET;
//                prepareaction.setup(replycode, false);
//             }
             else {
                session_stage = SESSION_RESET;
                prepareaction.setup(replycode, false);
             }
          }
          else {
             session_stage = SESSION_QUIT;
             prepareaction.setup(replycode, false);
          }
       }
       else if (username != null) {
           authenticateCredentials();
       }
       else {
          session_stage = SESSION_MAIL;
       }
    }
    
    private void authenticateCredentials() throws TransientNegativeException, SocketException, SocketTimeoutException, IOException{

       class SaslCallbackHandler implements CallbackHandler{
          
          private String username;
          
          public SaslCallbackHandler(String username) {
             this.username = username;
          }
          
          @Override
          public void handle(Callback[] callbacks) throws java.io.IOException, UnsupportedCallbackException {
             
             callbackLoop:
             for (Callback cb:callbacks) {
                if (cb instanceof NameCallback) {
                   ((NameCallback)cb).setName(username);
                }
                else if (cb instanceof PasswordCallback) {
                   ((PasswordCallback)cb).setPassword(password.toCharArray());
                }
                //TODO Account for case-insesitivity
                else if (cb instanceof RealmCallback) {
                   String defaultRealm = ((RealmCallback)cb).getDefaultText();
                   if (defaultRealm!=null) {
                      if (!(realm==null||defaultRealm.equals(realm))) {
                         if (log.isDebugEnabled()) {
                            log.debug("Server supplied realm "+defaultRealm+
                                  " doesn't match client defined realm "+realm);
                         }
                      }
                      ((RealmCallback)cb).setText(defaultRealm);
                   }
                   else {
                      if (realm!=null) {
                         ((RealmCallback)cb).setText(realm);
                      }
                      else {
                         log.warn("No realm supplied by either the"+
                               " server or the client");
                      }
                   }
                }
                //TODO Account for case-insesitivity
                else if (cb instanceof RealmChoiceCallback) {
                   RealmChoiceCallback rccb = (RealmChoiceCallback)cb;
                   String defaultRealm = rccb.getChoices()[rccb.getDefaultChoice()];
                   
                   if (realm!=null) {
                      if (!defaultRealm.equals(realm)) {
                         if (log.isDebugEnabled()) {
                            log.debug("Server supplied default realm "+defaultRealm+
                                  " doesn't match client defined realm "+realm);
                         }
                         //Maybe another Realm supplied by the server is a 100% match
                         for (int i=0;i<rccb.getChoices().length;i++) {
                            if (i==rccb.getDefaultChoice()) continue;
                            if (rccb.getChoices()[i].equals(realm)) {
                               rccb.setSelectedIndex(i);
                               continue callbackLoop;
                            }
                         }
                         //Any Realm that contains/is equal to the serverName will do
                         for (int i=0;i<rccb.getChoices().length;i++) {
                            if (rccb.getChoices()[i].contains(serverName)||
                                  serverName.contains(rccb.getChoices()[i])) {
                               rccb.setSelectedIndex(i);
                               continue callbackLoop;
                            }
                         }
                         throw new IOException("Possible server mismatch. "+ serverName +
                               " not part of any realm offered in digest-challenge.");
                      }
                      else {
                         rccb.setSelectedIndex(rccb.getDefaultChoice());
                      }
                   }
                   else {
                      if (defaultRealm.contains(serverName)||
                            serverName.contains(defaultRealm)) {
                         rccb.setSelectedIndex(rccb.getDefaultChoice());
                      }
                      else {
                         //Any Realm that contains/is equal to the serverName will do
                         for (int i=0;i<rccb.getChoices().length;i++) {
                            if (i==rccb.getDefaultChoice()) continue;
                            if (rccb.getChoices()[i].contains(serverName)||
                                  serverName.contains(rccb.getChoices()[i])) {
                               rccb.setSelectedIndex(i);
                               continue callbackLoop;
                            }
                         }
                         throw new IOException("Possible server mismatch. "+ serverName +
                               " not part of any realm offered in digest-challenge.");
                      }
                   }
                }
                else {
                   throw new UnsupportedCallbackException (cb, "Unrecognized Callback");
                }
             }
          }
       }

       SaslCallbackHandler sch;
       SaslClient saslClient;
       ArrayList<String> mechs = new ArrayList<String>(5);
       if (supportedCapability(AUTH_MECH.DIGEST_MD5.getName())) {
          mechs.add(AUTH_MECH.DIGEST_MD5.getName());
          if (username.indexOf('@')!=-1) {
             sch = new SaslCallbackHandler(username.substring(0, username.indexOf('@')));
          }
          else {
             sch = new SaslCallbackHandler(username);
          }
       }
       else {
          if (supportedCapability(AUTH_MECH.CRAM_MD5.getName())) {
             mechs.add(AUTH_MECH.CRAM_MD5.getName());
          }
          if (supportedCapability(AUTH_MECH.PLAIN.getName())) {
             mechs.add(AUTH_MECH.PLAIN.getName());
          }
          if (username.indexOf('@')!=-1) {
             sch = new SaslCallbackHandler(username);
          }
          else {
             sch = new SaslCallbackHandler(username+'@'+serverName);
          }
       }
       if (!mechs.isEmpty()) {
          saslClient = Sasl.createSaslClient(mechs.toArray(new String[mechs.size()]), null, "smtp", serverName, null, sch);
          ClientSMTPAuthenticate cSMTPa = ClientSMTPAuthenticatorFactory.getInstance().
                getClientSMTPAuthenticator(saslClient.getMechanismName(), saslClient);
          
          String replyCode = "";
          String response;
          
          try {
             
             // If the SASL Mechanism has an initial response the returned value is
             // AUTH MECH-NAME RESPONSE
             // otherwise it is
             // AUTO MECH-NAME
             response = cSMTPa.generateResponse(null, null);
                
             write ( response );
             socket.setSoTimeout(2 * 60 * 1000);

             while (!cSMTPa.isComplete()) {
                
                replyCode = read();
                response = cSMTPa.generateResponse(replyCode, tempResponse.get(0));

                if (cSMTPa.isComplete()) {

                   if (response!=null) {
                      throw new AuthenticationException();
                   }
                   session_stage = SESSION_MAIL;
                   return;
                }
                write ( response );
             }
          }
          catch (AuthenticationException ae) {
             log.error(ae);

             write ( "*" );
             String replycode = read();
             if (replycode.length() != 0) {
                char firstdigit = replycode.charAt(0);
                if (firstdigit == FIVE) {
                   session_stage = SESSION_RESET;
                   prepareaction.setup(replyCode, false);
                }
                else {
                   session_stage = SESSION_QUIT;
                   prepareaction.setup(replyCode, false);
                }
             }
             return;
          }
          session_stage = SESSION_QUIT;
          prepareaction.setup(replyCode, false);
       }
       else if (supportedCapability(AUTH_MECH.LOGIN.getName())) {
          
          write( "AUTH LOGIN" );
          socket.setSoTimeout(2 * 60 * 1000);
          String replycode = read();
          if (replycode.length() != 0) {
             char firstdigit = replycode.charAt(0);
             if (firstdigit == THREE) {
                String login;
                if (username.indexOf('@')!=-1) {
                   login = username;
                }
                else {
                   login = username+'@'+serverName;
                }
                write( new String(Base64.encodeBase64(login.getBytes(utf8Charset))) );
                socket.setSoTimeout(2 * 60 * 1000);
                replycode = read();
                if (replycode.length() != 0) {
                   firstdigit = replycode.charAt(0);
                   if (firstdigit == THREE) {
                      write( new String(Base64.encodeBase64(password.getBytes(utf8Charset))) );
                      socket.setSoTimeout(2 * 60 * 1000);
                      replycode = read();
                      if (replycode.length() != 0) {
                         firstdigit = replycode.charAt(0);
                         if (firstdigit == TWO) {
                            session_stage = SESSION_MAIL;
                            return;
                         }
                      }
                   }
                }
             }
             session_stage = SESSION_RESET;
             prepareaction.setup(replycode, false);
          }
          session_stage = SESSION_QUIT;
          prepareaction.setup(replycode, false);
       }
       else {
          session_stage = SESSION_MAIL;
       }
    }
    
    private class Prepareaction {
       
       boolean resend;
       String replycode = "";
       String replyargument = "";
       
       private void setup(String replycode, boolean resend) {
          this.resend = replycode.equals("421") ? true:resend;
          this.replycode = replycode;
          this.replyargument = " " + tempResponse.get(0);
       }
    }
    
    private void processreply() throws TransientNegativeException, SocketException, SocketTimeoutException, IOException{
       String replyCode = null;
       try {
          replyCode = read();
       }
       catch (TransientNegativeException tne) {
          while(remainingAddresses.size()>0) {
             failedAddresses.add(new FailedAddressItem(remainingAddresses.remove(0), "451 "+(tne.getException()!=null?tne.getException().getMessage():"")));
          }
          throw tne;
       }
       if (replyCode.length() != 0) {
          char firstdigit = replyCode.charAt(0);
          switch (session_stage) {
             case SESSION_PREBG: {
                if (firstdigit == TWO) {
                   session_stage = SESSION_EHLO;
                }
                else if (firstdigit == FIVE) {
                   session_stage = SESSION_QUIT;
                   prepareaction.setup(replyCode, false);
                }
                else {
                   session_stage = SESSION_QUIT;
                   prepareaction.setup(replyCode, false);
                }
             }break;
             case SESSION_EHLO: {
                if (firstdigit == TWO) {
                   if (ehlo) {
                      capabilities = new ArrayList<String>(tempResponse);
                      authenticate();
                   }
                   else {
                      session_stage = SESSION_MAIL;
                   }
                }
                else if (firstdigit == FIVE) {
                   char seconddigit = replyCode.charAt(1);
                   if (seconddigit == ZERO) {
                      char thirddigit = replyCode.charAt(2);
                      if (thirddigit == FOUR) {
                         ehlo = false;
                      }
                      else {
                         session_stage = SESSION_QUIT;
                         prepareaction.setup(replyCode, false);
                      }
                   }
                   else if (seconddigit == FIVE) {
                      char thirddigit = replyCode.charAt(2);
                      if (thirddigit == ZERO) {
                         session_stage = SESSION_RESET;
                         prepareaction.setup(replyCode, false);
                      }
                      else {
                         session_stage = SESSION_QUIT;
                         prepareaction.setup(replyCode, false);
                      }
                   }
                   else {
                      session_stage = SESSION_QUIT;
                      prepareaction.setup(replyCode, false);
                   }
                }
                else {
                   session_stage = SESSION_QUIT;
                   prepareaction.setup(replyCode, false);
                }
             }break;
             case SESSION_MAIL: {
                if (firstdigit == TWO) {
                   session_stage = SESSION_RCPT;
                }
                else if (firstdigit == FOUR) {
                   char seconddigit = replyCode.charAt(1);
                   if (seconddigit == FIVE) {
                      char thirddigit = replyCode.charAt(2);
                      if (thirddigit == ONE) {
                         session_stage = SESSION_RESET;
                         prepareaction.setup(replyCode, true);
                      }
                      else if (thirddigit == TWO) {
                         session_stage = SESSION_RESET;
                         prepareaction.setup(replyCode, true);
                      }
                      else {
                         session_stage = SESSION_QUIT;
                         prepareaction.setup(replyCode, true);
                      }
                   }
                   else {
                      session_stage = SESSION_QUIT;
                      prepareaction.setup(replyCode, true);
                   }
                }
                //Implies permanent negative completion reply. Some mail receiving servers reject dynamic ip ranges at this point.
                else {
                   String response = replyCode+" "+ tempResponse.get(0);
                   while(remainingAddresses.size()>0) {
                      failedAddresses.add(new FailedAddressItem(remainingAddresses.remove(0), response));
                   }
                   session_stage = SESSION_QUIT;
                   prepareaction.setup(replyCode, false);
                }
             }break;
             case SESSION_RCPT: {
                if (firstdigit == TWO) {
                   char seconddigit = replyCode.charAt(1);
                   if (seconddigit == FIVE) {
                      char thirddigit = replyCode.charAt(2);
                      if (thirddigit == ZERO) {
                         atLeastOneRCPTAccepted = true;
                         remainingAddresses.remove(0);
                         if (remainingAddresses.isEmpty()) {
                            session_stage = SESSION_DATA;
                         }
                         else {
                            session_stage = SESSION_RCPT;
                         }
                      }
                      else {
                         processReply0(SESSION_QUIT, replyCode, false);
                      }
                   }
                   else {
                      processReply0(SESSION_QUIT, replyCode, false);
                   }
                }
                else if (firstdigit == FOUR) {
                   char seconddigit = replyCode.charAt(1);
                   if (seconddigit == FIVE) {
                      char thirddigit = replyCode.charAt(2);
                      if (thirddigit == ZERO) {
                         processReply0(SESSION_RESET, replyCode, true);
                      }
                      else if (thirddigit == ONE) {
                         processReply0(SESSION_RESET, replyCode, true);
                      }
                      else if (thirddigit == TWO) {
                         processReply0(SESSION_RESET, replyCode, true);
                      }
                      else {
                         processReply0(SESSION_QUIT, replyCode, true);
                      }
                   }
                   else {
                      processReply0(SESSION_QUIT, replyCode, true);
                   }
                }
                else if (firstdigit == FIVE) {
                   char seconddigit = replyCode.charAt(1);
                   if (seconddigit == FIVE) {
                      char thirddigit = replyCode.charAt(2);
                      if (thirddigit == ZERO) {
                         processReply0(SESSION_RESET, replyCode, false);
                      }
                      else if (thirddigit == ONE) {
                         processReply0(SESSION_RESET, replyCode, false);
                      }
                      else if (thirddigit == TWO) {
                         processReply0(SESSION_RESET, replyCode, false);
                      }
                      else if (thirddigit == THREE) {
                         processReply0(SESSION_RESET, replyCode, false);
                      }
                      else {
                         processReply0(SESSION_QUIT, replyCode, false);
                      }
                   }
                   else if (seconddigit == ZERO) {
                      char thirddigit = replyCode.charAt(2);
                      if (thirddigit == THREE) {
                         processReply0(SESSION_RESET, replyCode, false);
                      }
                      else {
                         processReply0(SESSION_QUIT, replyCode, false);
                      }
                   }
                   else {
                      processReply0(SESSION_QUIT, replyCode, false);
                   }
                }
                else {
                   processReply0(SESSION_QUIT, replyCode, false);
                }
             }break;
             case SESSION_DATA: {
                if (!proceed) {
                   if (firstdigit == THREE) {
                      proceed = true;
                   }
                   else if (firstdigit == FOUR) {
                      char seconddigit = replyCode.charAt(1);
                      if (seconddigit == FIVE) {
                         char thirddigit = replyCode.charAt(2);
                         if (thirddigit == ONE) {
                            session_stage = SESSION_RESET;
                            prepareaction.setup(replyCode, true);
                         }
                         else {
                            session_stage = SESSION_QUIT;
                            prepareaction.setup(replyCode, true);
                         }
                      }
                      else {
                         session_stage = SESSION_QUIT;
                         prepareaction.setup(replyCode, true);
                      }
                   }
                   else if (firstdigit == FIVE) {
                      char seconddigit = replyCode.charAt(1);
                      if (seconddigit == FIVE) {
                         char thirddigit = replyCode.charAt(2);
                         if (thirddigit == FOUR) {
                            session_stage = SESSION_RESET;
                            prepareaction.setup(replyCode, false);
                         }
                         else {
                            session_stage = SESSION_QUIT;
                            prepareaction.setup(replyCode, false);
                         }
                      }
                      else if (seconddigit == ZERO) {
                         char thirddigit = replyCode.charAt(2);
                         if (thirddigit == THREE) {
                            session_stage = SESSION_RESET;
                            prepareaction.setup(replyCode, false);
                         }
                         else {
                            session_stage = SESSION_QUIT;
                            prepareaction.setup(replyCode, false);
                         }
                      }
                   }
                   else {
                      session_stage = SESSION_QUIT;
                      prepareaction.setup(replyCode, false);
                   }
                }
                else {
                   if (firstdigit == TWO) {
                      success = true;
                      session_stage = SESSION_QUIT;
                   }
                   else if (firstdigit == FOUR) {
                      char seconddigit = replyCode.charAt(1);
                      if (seconddigit == FIVE) {
                         char thirddigit = replyCode.charAt(2);
                         if (thirddigit == ONE) {
                            session_stage = SESSION_RESET;
                            prepareaction.setup(replyCode, true);
                         }
                         else if (thirddigit == TWO) {
                            session_stage = SESSION_RESET;
                            prepareaction.setup(replyCode, true);
                         }
                         else {
                            session_stage = SESSION_QUIT;
                            prepareaction.setup(replyCode, true);
                         }
                      }
                      else {
                         session_stage = SESSION_QUIT;
                         prepareaction.setup(replyCode, true);
                      }
                   }
                   else if (firstdigit == FIVE) {
                      char seconddigit = replyCode.charAt(1);
                      if (seconddigit == FIVE) {
                         char thirddigit = replyCode.charAt(2);
                         if (thirddigit == TWO) {
                            session_stage = SESSION_RESET;
                            prepareaction.setup(replyCode, false);
                         }
                         else if (thirddigit == FOUR) {
                            session_stage = SESSION_RESET;
                            prepareaction.setup(replyCode, false);
                         }
                         else {
                            session_stage = SESSION_QUIT;
                            prepareaction.setup(replyCode, false);
                         }
                      }
                      else {
                         session_stage = SESSION_QUIT;
                         prepareaction.setup(replyCode, false);
                      }
                   }
                   else {
                      session_stage = SESSION_QUIT;
                      prepareaction.setup(replyCode, false);
                   }
                }
             }break;
             case SESSION_QUIT: {
                if (firstdigit != TWO) {
                   success = false;
                }
                log.info("Session has ended");
             }break;
             case SESSION_RESET: {
                success = false;
                session_stage = SESSION_QUIT;
             }break;
             default: {
                throw new AssertionError();
             }
          }
       }
       else if (session_stage != SESSION_QUIT) {
          session_stage = SESSION_QUIT;
          prepareaction.setup(replyCode, false);
       }
    }

    private void processReply0(int nextStage, String replyCode, boolean reSend) {

       failedAddresses.add(new FailedAddressItem(remainingAddresses.remove(0), replyCode+" "+tempResponse.get(0)));
       if (remainingAddresses.isEmpty() && atLeastOneRCPTAccepted) {
          session_stage = SESSION_DATA;
       }
       else if (remainingAddresses.size()>0) {
          session_stage = SESSION_RCPT;
       }
       else {
          session_stage = nextStage;
          prepareaction.setup(replyCode, reSend);
       }
    }
    
    private boolean supportedCapability(String input) {
       for (int i=capabilities.size()-1;i>=0;i--) {
          if (capabilities.get(i).toUpperCase(ConfigurationManager.LOCALE).contains(input)) {
             return true;
          }
       }
       return false;
    }
    
   /**
    * Determines the MX entries for this domain and attempts to open
    * a socket.  If no connections can be opened, a SystemException is thrown.
    * @param domain
    * @return 
    * @throws com.ericdaugherty.mail.server.errors.PermanentNegativeException 
    */
    protected Socket connect( Domain domain ) throws PermanentNegativeException {

       if (domain.getDomainName().equals("example.com")) {
          try {
             return new Socket((String)null, configurationManager.getSMTPPort()+1);
          }
          catch( IOException e ) {
             log.error( "Connection to SMTP Server: example.com failed with exception: " + e ) ;
             throw new PermanentNegativeException( "Could not connect to any SMTP server for domain example.com" );
          }
       }

        //Check to see if a default smtp server is configured before performing
        //the DNS lookup.
        if( configurationManager.isDefaultSmtpServerEnabled() ) {
            List<DefaultSMTPServer> defaultServerEntries = configurationManager.getDefaultSmtpServers();
            for (DefaultSMTPServer serverEntry:defaultServerEntries) {

                try {
                    username = serverEntry.getUsername();
                    password = serverEntry.getPassword();
                    serverName = serverEntry.getHost();
                    realm = serverEntry.getRealm();
                    return new Socket( serverEntry.getHost(), serverEntry.getPort() );
                }
                catch( IOException e ) {
                    log.error( "Connection to SMTP Server: " + serverEntry + " failed with exception: " + e ) ;
                }
            }
            
           // Going ahead  with a lookup of the target SMTP Server, clear DNS passwords.
           username = null;
           password = null;
        }
        
        //If there are no default SMTP servers defined, or attempting to use them
        //is unsuccessful, proceed with a lookup of the target SMTP server anyway
        
         Resolver dnsResolver = Mail.getInstance().getDNSResolver();
         try {
            MXReply reply = dnsResolver.submitRequest(new ClientRequest.MXRequest(domain));
            int port = 25;
            for (Entry<Integer, Set<InetAddress>> entry : reply.getMXs().entrySet()) {
               for (InetAddress address : entry.getValue()) {
                  try {
                     return new Socket( address, port );
                  }
                  catch( IOException e ) {
                      log.error("Delivering to domain " + domain.getDomainName()
                            + " through " + entry.getValue().toString()
                            + " failed.", e);
                  }
               }
            }
            throw new DNSException("");
         } catch (DNSException ex) {
            throw new PermanentNegativeException("Could not connect to any SMTP server for domain: " + domain);
         }   
    }
    
    /**
     * This method searches for the server in the reverse DNS servers file
     * 
     * @param domain the RCPT TO: server domain
     * @return true if the server is in the reverse DNS servers file
     */
    private boolean isReverseDNSserver(Domain domain) {
       if (configurationManager.getReverseDNSServers()!=null) {
          String[] servers = configurationManager.getReverseDNSServers();
          int length = servers.length;
          String checkserver = domain.getDomainName().toUpperCase(ConfigurationManager.LOCALE);
          for (int i=0;i<length;i++) {
             if (checkserver.contains(servers[i].toUpperCase(ConfigurationManager.LOCALE))) {
                return true;
             }
          }
       }
       return false;
    }

   /**
    * This method sends the DATA only part of the message to the remote server.
    * @param message
    * @throws java.io.IOException
    */
   protected void sendData( SMTPMessage message) throws IOException {

      int count = 8;
      List<byte[]> dataLines = null;
      try {
         dataLines = message.loadIncrementally(count);
         int numDataLines = Math.min(30,dataLines.size());

         boolean foundRPLCRCPT = false, foundRPLCID = false;

         String iterString;
         for (int iterCount = 0; iterCount< numDataLines; iterCount++) {
            iterString = new String(dataLines.get(iterCount),usAsciiCharset);
            if (iterString.indexOf("<REPLACE-RCPT>")!=-1) {
               iterString = "        for <" + (originalAddresses.get(0)).getAddress() + ">"+iterString.substring(iterString.indexOf(';'));
               dataLines.set(iterCount, iterString.getBytes(usAsciiCharset));
               foundRPLCRCPT = true;
            }
            else if (iterString.indexOf("<REPLACE-ID>")!=-1) {
               iterString = iterString.substring(0,iterString.indexOf('<'))+message.getSmtpUid()+
                     (iterString.charAt(iterString.length()-1)==';'?";":"");
               dataLines.set(iterCount, iterString.getBytes(usAsciiCharset));
               foundRPLCID = true;
            }
            if (foundRPLCRCPT&&foundRPLCID) break;
         }

         smtpSSH.getActiveOutputStream().flush();

         if (!mime8bit&&message.is8bitMIME()) {

            long start = System.nanoTime();
            int dataSize = 0, index;
            byte[] singleLine;
            while (dataLines.size()>0) {
               numDataLines = dataLines.size();
               //Write the data.
               for( index = 0; index < numDataLines; index++ ) {
                  singleLine = dataLines.get( index );
                  processDATA( singleLine );
                  dataSize += singleLine.length;
                  if (log.isDebugEnabled()) {
                     if (System.nanoTime()-start>5000000000L) {
                        start = System.nanoTime();
                        log.debug(message.getSmtpUid()+" data sent: "+(dataSize/1024)+" KiB");
                     }
                  }
               }
               count+=250;
               dataLines.clear();
               dataLines = message.loadIncrementally(count);
            }
         }
         else {
            BufferedOutputStream out = new BufferedOutputStream(smtpSSH.getActiveOutputStream(),4096);
            long start = System.nanoTime();
            int dataSize = 0, index;
            byte[] singleLine;
            while (dataLines.size()>0) {
               numDataLines = dataLines.size();
               //Write the data.
               for( index = 0; index < numDataLines; index++ ) {
                  singleLine = dataLines.get( index );
                  out.write( singleLine );
                  out.write( CRLF_BYTES );
                  out.flush();
                  dataSize += singleLine.length;
                  if (log.isDebugEnabled()) {
                     if (System.nanoTime()-start>5000000000L) {
                        start = System.nanoTime();
                        log.debug(message.getSmtpUid()+" data sent: "+(dataSize/1024)+" KiB");
                     }
                  }
               }
               count+=250;
               dataLines.clear();
               dataLines = message.loadIncrementally(count);
            }
            //Send the command end data transmission.
            out.write(new byte[]{0x2e,0x0d,0x0a});
            out.flush();
         }
      }
      finally {
         if (dataLines != null)
            dataLines.clear();
      }
   }

    /**
     * Returns the response code generated by the server.
     * This method will handle multi-line responses, but will
     * only log the responses, and discard the text, returning
     * only the 3 digit response code.
     *
     * @return 3 digit response string.
     */
    private String read() throws TransientNegativeException {
        try {
            String responseCode;

            //Read in the first line.  This is the only line
            //we really care about, since the response code
            //must be the same on all lines.
            String inputText = smtpSSH.readLine();
            if( inputText == null ) {
                inputText = "";
            }
            else {
                inputText = inputText.trim();
            }
            
            if( log.isDebugEnabled() ) { log.debug( "Read Input: " + inputText ); }
            if( inputText.length() < 3 ) {
                if (session_stage == SESSION_QUIT) {
                    return "";
                }
                else {
                   throw new TransientNegativeException( "SMTP Response too short. Aborting Send. Response: " + inputText, null );
                }
            }

            //Strip off the response code.
            responseCode = inputText.substring( 0, 3 );
            tempResponse.clear();
            tempResponse = new ArrayList<String>(4);
            if (responseCode.charAt(0)=="4".charAt(0)||responseCode.charAt(0)=="5".charAt(0)) {
               tempResponse.add(inputText.substring(4));
            }
            read0(inputText.substring(4));
            //Handle Multi-Line Responses.
            while( ( inputText.length() >= 4 ) && inputText.substring( 3, 4 ).equals( "-" ) ) {
                inputText = smtpSSH.readLine();
                if (inputText==null) {
                   throw new IOException("Client has closed the connection.");
                }
                inputText = inputText.trim();
                read0(inputText.substring(4));
                if( log.isDebugEnabled() ) { log.debug( "Read Input: " + inputText ); }
            }

            return responseCode;
        }
        catch( IOException ioe ) {
            log.error( "Error reading from socket.", ioe );
            throw new TransientNegativeException( "Error reading from socket.", ioe );
        }
    }
    
    private void read0(String input) {

       String inputtUC = input.toUpperCase(ConfigurationManager.LOCALE);
       if (inputtUC.startsWith("AUTH")) {
          StringTokenizer st = new StringTokenizer(input, " ");
          st.nextToken();
          while (st.hasMoreTokens()) {
             tempResponse.add(st.nextToken());
          }
          return;
       }

       int stop = input.indexOf(' ');
       if (stop == -1) {
          tempResponse.add(input);
       }
       else {
          if (inputtUC.startsWith("HELLO")) {}
          else {
             tempResponse.add(input.substring(0, stop));
          }
       }
    }

    /**
     * Writes the specified output message to the client.
     */
    private void write( String message ) throws IOException{
        if (message!=null) {
            if( log.isDebugEnabled() ) { log.debug( "Writing: " + message ); }
            smtpSSH.print(message);
        }
    }

}