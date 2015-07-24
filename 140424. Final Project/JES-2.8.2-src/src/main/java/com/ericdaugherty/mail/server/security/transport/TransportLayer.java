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

package com.ericdaugherty.mail.server.security.transport;

//Java Imports
import java.io.IOException;
import java.net.Socket;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local Imports
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;

/**
 * Responsibility for setting up TLS/SSL sessions and verifying credentials is handed
 * to this class.
 *
 * @author Andreas Kyrmegalos
 */
public class TransportLayer {

   /** Logger */
   private static final Log log = LogFactory.getLog( TransportLayer.class );

   private static SSLSocketFactory sslSocketFactory = ConfigurationManager.getInstance().getSSLSocketFactory();
   
   private SSLSocket socket;

   public TransportLayer() {}
   
   public TransportLayer(SSLSocket socket) {
      this.socket = socket;
   }

   public void init(Socket socket, boolean serverMode, boolean autoClose, boolean smtp) throws IOException{
      ConfigurationManager cm = ConfigurationManager.getInstance();
      this.socket = (SSLSocket)sslSocketFactory.createSocket(socket, socket.getInetAddress().getHostName(),
             socket.getPort(), autoClose);
      if (serverMode) {
         if ("required".equals(smtp?cm.getClientAuthSMTP():cm.getClientAuthPOP3())) {
           this.socket.setNeedClientAuth(true);
         }
         else if ("requested".equals(smtp?cm.getClientAuthSMTP():cm.getClientAuthPOP3())) {
            this.socket.setWantClientAuth(true);
         }
         this.socket.setUseClientMode(false);
      }
      this.socket.setEnabledCipherSuites(cm.getEnabledCiphers());
      this.socket.setEnabledProtocols(ConfigurationManager.getInstance().getEnabledProtocols());
      if (!serverMode) {
         this.socket.startHandshake();
         verifyPeer(false, smtp);
      }
   }

   public final void verifyPeer(boolean serverMode, boolean smtp) throws SSLPeerUnverifiedException {
      
      ConfigurationManager cm = ConfigurationManager.getInstance();
      //In client mode verify the server domain
      //In server mode decide based on the getClientAuth.... parameter wether to verify the client domain
      if (!serverMode || !"no".equals(smtp?cm.getClientAuthSMTP():cm.getClientAuthPOP3())) {
         SSLSession sSLSession = socket.getSession();
         try {
            validateHostName((X509Certificate)sSLSession.getPeerCertificates()[0]);
         }
         catch (SSLPeerUnverifiedException sslpue) {
            log.debug(sslpue.getMessage());
            if (!"requested".equals(smtp?cm.getClientAuthSMTP():cm.getClientAuthPOP3())) {
               String cipherSuite = sSLSession.getCipherSuite();
               if (cipherSuite.indexOf("KRB5")==-1) throw sslpue;
               //else kerberos stuff if needed
            }
         }
      }
   }

   private void validateHostName(X509Certificate certificate) throws SSLPeerUnverifiedException {

      Collection<List<?>> san = null;
      List<String> declaredHostNames = new ArrayList<String>();
      try {
         san = certificate.getSubjectAlternativeNames();

      }
      catch (CertificateParsingException cpe) {
         log.debug(cpe.getMessage());
      }
      if (san!=null) {
         Iterator<List<?>> iter = san.iterator();
         List<?> item;
         while(iter.hasNext()) {
            item = iter.next();
            if ((Integer)item.get(0)==2) {
               declaredHostNames.add(((String)item.get(1)).toLowerCase(ConfigurationManager.LOCALE));
               if (log.isDebugEnabled()) {
                  log.debug(item.get(1));
               }
            }
         }
      }
      else {
         log.debug("No subjectAlternativeNames available");
      }
      String dn = certificate.getSubjectX500Principal().getName();
      if (!dn.equals("")) {
         if (dn.indexOf(',')==-1) {
            declaredHostNames.add(dn.substring(dn.indexOf('=')+1));
         }
         else {
            declaredHostNames.add(dn.substring(dn.indexOf('=')+1,dn.indexOf(',')));
         }
      }
      if (declaredHostNames.isEmpty()) {
         throw new SSLPeerUnverifiedException("no hostnames supplied");
      }
      doHostNameMatching(declaredHostNames);
   }

   private void doHostNameMatching(List<String> declaredHosts) throws SSLPeerUnverifiedException {

      String hostname = socket.getInetAddress().getCanonicalHostName();
      if (Mail.isTesting()&&declaredHosts.get(0).equals("localhost")) {
         log.debug(hostname+" hostname connected, "+declaredHosts.get(0)+" supplied by certificate");
         hostname = "localhost";
      }
      if (hostname.endsWith(".")) hostname = hostname.substring(0,hostname.length()-1);
      String[] hostNameBreakDown = hostname.split("\\.");
      String[] candidateBreakDown;
      int fieldCount = hostNameBreakDown.length, index, count;
      boolean verified;
      for (String host : declaredHosts) {
         candidateBreakDown = host.split("\\.");
         if (candidateBreakDown.length!=fieldCount) continue;
         verified = true;
         count = 1;
         for (index = fieldCount-1;index>=0;index--) {
            if (verified) {
               verified = candidateBreakDown[index].equals(hostNameBreakDown[index]) || (count>2&&candidateBreakDown[index].equals("*"));
            }
            if (!verified) break;
            count++;
         }
         if (verified) return;
      }
      throw new SSLPeerUnverifiedException("no supplied hostnames matched the lookup host name");

   }

   public Socket getSocket() {
      return socket;
   }

   public void conclude() {
      if (socket!=null) socket.getSession().invalidate();
      socket = null;
   }
}
