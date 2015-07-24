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

package com.ericdaugherty.mail.server.configuration;

//Java imports
import java.net.*;
import org.w3c.dom.Element;

//Log imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local import
import com.ericdaugherty.mail.server.Mail;

/**
 *
 * @author Andreas Kyrmegalos
 */
public final class ConfigurationManagerCBC {

   /** Logger */
   private static final Log log = LogFactory.getLog(ConfigurationManager.class);
   private final ConfigurationManager cm = ConfigurationManager.getInstance();
   
   private ConnectionBasedConfigurator connectionBasedConfigurator;

   private boolean configurationEnabled;
   /** If true uses SSL/TLS to secure the connection to the CBC */
   private boolean configurationSecure;
   /** The local IP address to receive commands on. */
   private InetAddress configurationAddress;
   /** The port the configuration address will use. */
   private int configurationPort;
   
   public static final class RetainConfigurator {
      
      private final ConnectionBasedConfigurator configurator;
      
      private RetainConfigurator(ConnectionBasedConfigurator configurator) {
         this.configurator = configurator;
      }
      
      private ConnectionBasedConfigurator getConfigurator() {
         return configurator;
      }
   }
   
   RetainConfigurator getConfigurator() {
      return new RetainConfigurator(connectionBasedConfigurator);
   }
   
   public boolean isConfigurationEnabled() {
      return configurationEnabled;
   }

   public boolean isConfigurationSecure() {
      return configurationSecure;
   }

   public InetAddress getConfigurationAddress() {
      return configurationAddress;
   }

   public int getConfigurationPort() {
      return configurationPort;
   }
   
   public void shutdown() {
      
      if (connectionBasedConfigurator!=null) {
         if (Mail.getInstance().isRestart()) {
            
         }
         else {
            connectionBasedConfigurator.shutdown();
         }
      }
      connectionBasedConfigurator = null;
   }

   void cbcConfiguration(Element element) {

      if (!cm.isFixed()) {

         if (configurationEnabled = Boolean.parseBoolean(element.getAttribute("enable"))) {

            configurationSecure = Boolean.parseBoolean(element.getAttribute("secure"));
            
            if (Boolean.parseBoolean(((Element) element.getOwnerDocument().getElementsByTagName("backend").item(0)).getAttribute("secure"))) {
               configurationSecure = true;
            }

            String checkSuppliedStringAddress = element.getAttribute("listenAddress");
            
            if (!checkSuppliedStringAddress.toLowerCase(cm.LOCALE).equals("localhost")) {
               String[] checkForDomainName;
               if (checkSuppliedStringAddress.indexOf('.') != -1) {

                  checkForDomainName = checkSuppliedStringAddress.split("\\.");
               } else {

                  checkForDomainName = checkSuppliedStringAddress.split(":");
               }
               for (int i = checkForDomainName.length - 1; i >= 0; i--) {

                  if (checkForDomainName.length == 0) {

                     continue;
                  }
                  try {
                     Integer.parseInt(checkForDomainName[i]);
                  } catch (NumberFormatException nfe) {
                     throw new RuntimeException("No domain names allowed for configurator address: " + checkSuppliedStringAddress);
                  }
               }
            }
            
            configurationAddress = Utils.getAllowedAddress("cbc.listenAddress", checkSuppliedStringAddress, true, false);
            
            configurationPort = Utils.parsePort(element.getAttribute("port"), 41001);
            if (configurationPort < 0 || configurationPort > 65535) {
               configurationPort = 41001;
            }

            if (!Mail.getInstance().isRestarting()) {
               if(!configurationSecure) {
                  new Thread(connectionBasedConfigurator = new ConnectionBasedConfigurator(cm.isConfigurationSecure()),"Configuration").start();
               }
            }
            else {
               connectionBasedConfigurator = Mail.getInstance().getRetainConfigurator().getConfigurator();
            }
         }
      }
   }

   //This must only be called once, at application start up
   void deferredSecurityConfiguration() {
      if (!Mail.getInstance().isRestarting()) {
         if(configurationSecure) {
            new Thread(connectionBasedConfigurator = new ConnectionBasedConfigurator(cm.isConfigurationSecure()),"Configuration").start();
         }
      }
      else {
         connectionBasedConfigurator = Mail.getInstance().getRetainConfigurator().getConfigurator();
      }
   }
}
