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

package com.ericdaugherty.mail.server.auth;

//Java Imports
import java.io.*;
import java.util.*;
import javax.security.auth.callback.*;
import javax.security.sasl.*;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Encoding Imports
import org.apache.commons.codec.binary.Base64;

//Local Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.MalformedBase64ContentException;
import com.ericdaugherty.mail.server.info.User;

/**
 * Verify client authentication using SASL GSS-API. Possibly protect
 * the data stream using integrity/privacy wrapping.
 *
 * @author Andreas Kyrmegalos
 */
public class GSSServerMode extends PlainServerMode{

   /** Logger */
   private static final Log log = LogFactory.getLog( GSSServerMode.class );

   private SaslServer gssSaslServer;
   private String authenticationID;
   private boolean integrity, privacy;

   public GSSServerMode(boolean smtp){
      super(smtp);
   }

   public void negotiateGSSAuthenticationContext() throws SaslException {

      String mechanism = "GSSAPI";
      Map<String,Object> properties = new HashMap<String,Object>();
      properties.put(Sasl.QOP, ConfigurationManager.getInstance().getSaslQOP());
      String principal = ConfigurationManager.getInstance().getGSSPrincipal();
      String protocol = !pop3?principal.substring(0,principal.indexOf(':')):principal.substring(principal.indexOf(':')+1,principal.indexOf('/'));
      String hostName = principal.substring(principal.indexOf('/')+1);

      gssSaslServer = Sasl.createSaslServer(mechanism,
                protocol, hostName, properties, new GSSServerMode.SASLCallbackHandler());

   }

   public byte[] evaluateResponse(byte[] responseBytes) throws SaslException {

      if (gssSaslServer.isComplete()) throw new SaslException("Authentication already completed.");
      try {
         if (!Base64.isArrayByteBase64(responseBytes)) {
            throw new SaslException("Can not decode Base64 Content",new MalformedBase64ContentException());
         }
         byte[] token = Base64.decodeBase64(responseBytes);
         token = gssSaslServer.evaluateResponse(token);
         if(gssSaslServer.isComplete()) {
            
            String qop = (String)gssSaslServer.getNegotiatedProperty(Sasl.QOP);
            if (qop.equals("auth-int")) {
               integrity = true;
            }
            else if (qop.equals("auth-conf")) {
               privacy = true;
            }

            finalizeAuthentication.finalize(authenticationID, authorizationID, null);
         }

         if (token != null) {
            return Base64.encodeBase64(token);
         }
         return new byte[0];
      }
      catch (IOException ioe) {
         throw new SaslException(ioe.getMessage(),ioe);
      }

   }

   public final class SASLCallbackHandler implements CallbackHandler {

       public void handle(Callback[] callbacks)
             throws IOException, UnsupportedCallbackException {
          if (callbacks == null || callbacks.length==0) {
             throw new IOException("Improper callback passed");
          }
          for (int i=0;i<callbacks.length;i++) {
             if (!(callbacks[i] instanceof AuthorizeCallback)) continue;
             AuthorizeCallback acb = (AuthorizeCallback)callbacks[i];
             String auth = acb.getAuthenticationID();
             String authz = acb.getAuthorizationID();
             if (auth.indexOf('@')!=-1&&authz.indexOf('@')!=-1) {
                String domain = auth.substring(auth.indexOf('@')+1).toLowerCase(ConfigurationManager.LOCALE);
                String domainz = authz.substring(authz.indexOf('@')+1).toLowerCase(ConfigurationManager.LOCALE);
                if (domain.indexOf(domainz)!=-1||domainz.indexOf(domain)!=-1) {
                   if (auth.substring(0,auth.indexOf('@')).equals(authz.substring(0,authz.indexOf('@')))) {
                      acb.setAuthorized(true);
                   }
                }
             }
             authenticationID = auth;
             authorizationID = authz;

          }
       }

   }
   
   public boolean isProtected() {
      return integrity||privacy;
   }

   public User getUser() {
      return user;
   }
   
   public String getMechanismName() {
      return gssSaslServer.getMechanismName();
   }

   public byte[] unwrap(byte[] incoming, int start, int len) throws SaslException {
      
      if (!isProtected()) {
         throw new IllegalStateException("Neither integrity nor privacy was negotiated");
      }
      if (!isComplete()) {
         throw new IllegalStateException("Authentication still in progress");
      }

      return gssSaslServer.unwrap(incoming, start, len);
   }

   public byte[] wrap(byte[] outgoing, int start, int len) throws SaslException {
      
      if (!isProtected()) {
         throw new IllegalStateException("Neither integrity nor privacy was negotiated");
      }
      if (!isComplete()) {
         throw new IllegalStateException("Authentication still in progress");
      }

      return gssSaslServer.wrap(outgoing, start, len);
   }

   public void dispose() throws SaslException {
      gssSaslServer.dispose();
      user = null;
      authenticationID = null;
      authorizationID = null;
   }

   public Object getNegotiatedProperty(String propName) {
      return gssSaslServer.getNegotiatedProperty(propName);
   }

   public String getAuthorizationID() {
      gssSaslServer.getAuthorizationID();
      return authorizationID;
   }

   public boolean isComplete() {
      return gssSaslServer.isComplete();
   }
}
