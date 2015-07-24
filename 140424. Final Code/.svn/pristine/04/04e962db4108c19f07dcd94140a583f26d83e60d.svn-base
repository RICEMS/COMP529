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

import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.AuthenticationException;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.info.User;
import java.nio.charset.Charset;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andreas Kyrmegalos
 */
public abstract class AbstractSaslServerMode implements AuthServerMode, SaslServer {
   
   /** Logger Category for this class. */
   private static final Log log = LogFactory.getLog( AbstractSaslServerMode.class );

   protected final Charset utf8Charset = ConfigurationManager.getUtf8Charset();
   protected final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();

   /** The ConfigurationManager */
   protected final ConfigurationManager configurationManager = ConfigurationManager.getInstance();
   
   /** The IP address of the client */
   protected String clientIp;
   
   protected boolean pop3;

   protected AbstractSaslServerMode.FinalizeAuthentication finalizeAuthentication;

   protected User user;
   
   protected String authorizationID;

   protected boolean completed;
   
   protected boolean failed;

   protected AbstractSaslServerMode(){}

   public void setClientIp(String clientIp) {
      this.clientIp = clientIp;
   }
   
   /**
    * A robust implementation of the handling of supplied authenticationIDs.
    * Offers the possibility of appending the domain name when it is missing from
    * the authenticationID and JES is in single domain mode.
    * 
    * @param authenticationID the SASL authenticationID as supplied by the client
    * @return String a valid authenticationID
    * @throws SaslException 
    */
   protected String getValidAuthenticationID(String authenticationID) throws SaslException{

      try {
         new EmailAddress(authenticationID);
      }
      catch (InvalidAddressException iae) {

         if (log.isDebugEnabled()) {
            log.debug("Attempting to login with invalid authenticationID: "+authenticationID);
         }
         //If in single domain mode, check for the @, append the domain if necessary and retry.
         if (configurationManager.isSingleDomainMode() && authenticationID.indexOf('@')==-1) {

            authenticationID = authenticationID + "@" + configurationManager.getSingleDomain().getDomainName();
            try {
               new EmailAddress(authenticationID);
            }
            catch (InvalidAddressException iae1) {
               if (log.isDebugEnabled()) {
                  log.debug("Unable to parse authenticationID: "+authenticationID);
               }
               throw new SaslException("User "+authenticationID+" not authenticated",
                     new AuthenticationException());
            }
         }
         //If the domain is missing, inform the client in a specific manner
         else {
            if (log.isDebugEnabled()) {
               log.debug("Unable to parse authenticationID: "+authenticationID);
            }
            throw new SaslException("User "+authenticationID+" not authenticated",
                  authenticationID.indexOf('@')==-1?
                  new AuthenticationException(DOMAIN_REQUIRED):new AuthenticationException());
         }
      }

      //return the authenticationID here as it might have been transformed
      return authenticationID;
   }

   protected abstract class FinalizeAuthentication {
      
      public abstract byte[] finalize(String authenticationID, String authorizationID, char[] password) throws SaslException;
   }

   @Override
   public User getUser() {
      return user;
   }

   @Override
   public void dispose() throws SaslException {
      clientIp = null;
      user = null;
      authorizationID = null;
   }

   /**
    * The venerable authorizationID. As specified in the SASL RFC, it is
    * "an identity to act as". If it is missing, or zero length, it is
    * replaced by the authenticationID. As per the RFC (page 5):
    *          "When this character string is omitted or empty, the client
    * is requesting to act as the identity associated with the credentials
    * (e.g., the user is requesting to act as the authentication identity)."
    * 
    * @return String the authorizationID that resulted from the SASL exchange
    */
   @Override
   public String getAuthorizationID() {
      if (!completed) {
         throw new IllegalStateException("Authentication still in progress");
      }
      return authorizationID;
   }

   @Override
   public boolean isComplete() {
      return completed;
   }
   
   protected static final String DOMAIN_REQUIRED = "domain required";
}
