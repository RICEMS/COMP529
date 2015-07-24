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
import java.io.*;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.*;
import javax.security.auth.callback.*;
import org.w3c.dom.Element;

//Log imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.utils.IOUtils;

/**
 *
 * @author Andreas Kyrmegalos
 */
final class ConfigurationManagerGeneral implements ConfigurationParameterConstants {

   /** Logger */
   private static final Log log = LogFactory.getLog(ConfigurationManager.class);
   private final ConfigurationManager cm = ConfigurationManager.getInstance();

   void shutdown() {
      securityManagerEnabled = false;
   }
   
   Map<String,String> getConfiguration() {
      
      Map<String,String> configuration = new HashMap<String,String>();
      
      configuration.put("externalDelegated", Boolean.toString(externalDelegated));
      configuration.put("notifyDefault", Boolean.toString(notifyDefault));
      configuration.put("warningDisabled", Boolean.toString(warningDisabled));
      configuration.put("throttlingStep", throttlingStep + "");
      configuration.put("throttlingStepMin", "0");
      configuration.put("throttlingStepMax", "7");
      configuration.put("securityManagerEnabled", Boolean.toString(securityManagerEnabled)+RESTART);
      configuration.put("persistMaster", Boolean.toString(persistMaster)+RESTART);
      configuration.put("selectedCryptography", (limitedCryptography?"limited":"unlimited")+RESTART);
      configuration.put("cryptography", "limited,unlimited");
      configuration.put("allowRemoteRestart", Boolean.toString(allowRemoteRestart));
      configuration.put("maxPassAttempts", maxPassAttempts+RESTART);
      configuration.put("maxPassAttemptsMin", "3");
      configuration.put("maxPassAttemptsMax", "10");
      configuration.put("maxErrorCount", maxErrorCount+RESTART);
      configuration.put("maxErrorCountMin", "10");
      configuration.put("maxErrorCountMax", "50");
      configuration.put("keystoreLocation", keystoreLocation+RESTART);
      configuration.put("selectedKeystoreProvider", keystoreProvider+RESTART);
      configuration.put("keystoreProvider", "SUN,SunJCE");
      configuration.put("selectedKeystoreType", keystoreType+RESTART);
      configuration.put("keystoreType", "JKS,JCEKS");
      configuration.put("truststoreLocation", truststoreLocation+RESTART);
      configuration.put("selectedTruststoreProvider", truststoreProvider+RESTART);
      configuration.put("truststoreProvider", "SUN,SunJCE");
      configuration.put("selectedTruststoreType", truststoreType+RESTART);
      configuration.put("truststoreType", "JKS,JCEKS");
      
      return configuration;
   }
   private boolean externalDelegated;
   private boolean notifyDefault;
   private boolean warningDisabled;
   
   boolean isExternalDelegated() {
      return externalDelegated;
   }
   
   boolean isNotifyDefault() {
      return notifyDefault;
   }
   
   boolean isWarningDisabled() {
      return warningDisabled;
   }
   
   public int getThrottlingStep() {
      return throttlingStep;
   }

   /** The time period to force message processors to wait before executing a task */
   public int getThrottlingDelay() {
      switch (throttlingStep) {
         case 1: return 50;
         case 2: return 100;
         case 3: return 233;
         case 4: return 367;
         case 5: return 500;
         case 6: return 750;
         case 7: return 1000;
         default: return 0;
      }
   }

   void generalConfiguration(Element element) {

      externalDelegated = Boolean.parseBoolean(element.getAttribute("externalDelegated"));
      notifyDefault = Boolean.parseBoolean(element.getAttribute("notifyDefault"));
      warningDisabled = Boolean.parseBoolean(element.getAttribute("warningDisabled"));
      throttlingStep = Integer.parseInt(element.getAttribute("throttlingStep"));
      if (throttlingStep < 0)
         throttlingStep = 0;
      else if (throttlingStep > 7)
         throttlingStep = 7;
      
      generalSecurityConfiguration((Element) element.getElementsByTagName("security").item(0));
   }
   private boolean securityManagerEnabled;
   private boolean persistMaster;
   /** The time period to force message processors to wait before executing a task */
   private int throttlingStep;
   private boolean limitedCryptography = true;
   private boolean allowRemoteRestart;
   private boolean legacyFileIOMode = false;
   private int maxPassAttempts;
   private int maxErrorCount;
   /** Controls whether or not debugging output for SSL is desired */
   private boolean debugSSL;
   /** The keystore complete path */
   private String keystoreLocation;
   /** The keystore provider */
   private String keystoreProvider;
   /** The keystore type */
   private String keystoreType;
   /** The truststore complete path */
   private String truststoreLocation;
   /** The truststore provider */
   private String truststoreProvider;
   /** The truststore type */
   private String truststoreType;
   private String[] enabledCiphers;
   private String[] enabledProtocols;
   private SSLContext sslContext;

   public boolean isSecurityManagerEnabled() {
      return securityManagerEnabled;
   }

   public boolean isPersistMaster() {
      return persistMaster;
   }

   boolean isLimitedCryptography() {
      return limitedCryptography;
   }
   
   public boolean isAllowRemoteRestart() {
      return allowRemoteRestart;
   }
   
   public boolean isLegacyFileIOMode() {
      return legacyFileIOMode;
   }

   public int getMaxPassAttempts() {
      return maxPassAttempts;
   }

   /**
    * The maximum number of errors during a POP3 or SMTP server session.
    * 
    * @return int maxErrorCount
    */
   public int getMaxErrorCount() {
      return maxErrorCount;
   }

   /**
    * The location of the keystore containing the server's certificates and
    * private key.
    *
    * @return String keystoreLocation
    */
   public String getKeystoreLocation() {
      return keystoreLocation;
   }

   /**
    * The provider of the keystore containing the server's certificates and
    * private key.
    *
    * @return String keystoreProvider
    */
   public String getKeystoreProvider() {
      return keystoreProvider;
   }

   /**
    * The type of the keystore containing the server's certificates and
    * private key.
    *
    * @return String keystoreType
    */
   public String getKeystoreType() {
      return keystoreType;
   }

   /**
    * The location of the truststore containing the server's certificates and
    * private key.
    *
    * @return String truststoreLocation
    */
   public String getTruststoreLocation() {
      return truststoreLocation;
   }

   /**
    * The provider of the truststore containing the server's certificates and
    * private key.
    *
    * @return String truststoreProvider
    */
   public String getTruststoreProvider() {
      return truststoreProvider;
   }

   /**
    * The type of the truststore containing the server's certificates and
    * private key.
    *
    * @return String truststoreType
    */
   public String getTruststoreType() {
      return truststoreType;
   }

   public String[] getEnabledCiphers() {
      return enabledCiphers;
   }

   public String[] getEnabledProtocols() {
      return enabledProtocols;
   }

   public SSLServerSocketFactory getSSLServerSocketFactory() {
      if(sslContext == null) {
         return null;
      }
      return sslContext.getServerSocketFactory();
   }

   public SSLSocketFactory getSSLSocketFactory() {
      if(sslContext==null) {
         return null;
      }
      return sslContext.getSocketFactory();
   }

   private void generalSecurityConfiguration(Element element) {

      if (!cm.isFixed()) {
         securityManagerEnabled = Boolean.parseBoolean(element.getAttribute("securityManagerEnabled"));
         persistMaster = Boolean.parseBoolean(element.getAttribute("persistMaster"));
         try {
            limitedCryptography = element.getAttribute("cryptography").equals("limited");
         } catch (Exception ex) {
            limitedCryptography = true;
         }
         allowRemoteRestart = Boolean.parseBoolean(element.getAttribute("allowRemoteRestart"));
         legacyFileIOMode = Boolean.parseBoolean(element.getAttribute("legacyFileIOMode"));
         maxPassAttempts = Integer.parseInt(element.getAttribute("maxPassAttempts"));
         maxErrorCount = Integer.parseInt(element.getAttribute("maxErrorCount"));
      }
      Element certificateStore = (Element) element.getElementsByTagName("certificateStore").item(0);
      debugSSL = Boolean.parseBoolean(certificateStore.getAttribute("debugSSL"));
      if (!cm.isFixed()) {

         Element keystore = (Element) certificateStore.getElementsByTagName("keystore").item(0);
         keystoreLocation = keystore.getElementsByTagName("location").item(0).getTextContent();
         if (keystoreLocation==null||keystoreLocation.trim().length() == 0) {
            keystoreLocation = cm.getSecurityDirectory() + File.separator + "keystore.jceks";
         }
         else {
            keystoreLocation = keystoreLocation.trim();
         }
         keystoreProvider = keystore.getAttribute("provider");
         keystoreType = keystore.getAttribute("type");
         Element truststore = (Element) certificateStore.getElementsByTagName("truststore").item(0);
         truststoreLocation = truststore.getElementsByTagName("location").item(0).getTextContent();
         if (truststoreLocation==null||truststoreLocation.length() == 0) {
            truststoreLocation = cm.getSecurityDirectory() + File.separator + "truststore.jks";
         }
         else {
            truststoreLocation = truststoreLocation.trim();
         }
         truststoreProvider = truststore.getAttribute("provider");
         truststoreType = truststore.getAttribute("type");
         enabledCiphers = limitedCryptography ? JSSEConfigurator.getVersionConstrainedLimitedEnabledCiphers() : JSSEConfigurator.getVersionConstrainedEnabledCiphers();
         enabledProtocols = JSSEConfigurator.getVersionConstrainedEnabledProtocols();
      }
   }

   //This must only be called once, at application start up
   void deferredSecurityConfiguration() {

      if (!Mail.getInstance().isRestarting()&&debugSSL) {
         System.setProperty("javax.net.debug", "all");
         log.info("Javax.net.debug set.");
      }
      else if (System.getProperty("javax.net.debug")!=null) {
         log.info("Javax.net.debug set.");
      } else {
         log.info("SSL debugging not requested. Javax.net.debug not set.");
      }

      
      try {

         //KeyStore setup
         KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
         File keystore = new File(keystoreLocation);
         if (!keystore.exists()) {
            if (log.isDebugEnabled()) {
               log.debug("KeyStore file " + keystore.getPath() + " not found. Switching to default.");
            }
            keystoreLocation = cm.getSecurityDirectory() + File.separator + "keystore.jceks";
            keystore = new File(keystoreLocation);
         }
         if (keystore.exists()) {
            if (log.isDebugEnabled()) {
               log.debug("KeyStore file " + keystore.getPath() + " located.");
            }
            //A password is needed, check if one exists in the passwords map
            if (!JESVaultControl.getInstance().containsKey("keystore")) {
               //Not present in passwords file, will require a gui
               //If the jvm is headless throw exception
               if (java.awt.GraphicsEnvironment.isHeadless()) {
                  log.error("The JVM is in headless mode but a GUI is required");
                  throw new RuntimeException("A GUI is required");
               }

               final VaultPassword vp = new VaultPassword();
               CallbackHandler cbh = new CallbackHandler() {

                  @Override
                  public void handle(Callback[] callbacks) {

                     PasswordCallback pw = (PasswordCallback) callbacks[0];
                     passwordPopup(vp);
                     pw.setPassword(vp.getUserPass());
                     vp.clearUserPass();
                  }
               };
               PasswordCallback pcb = new PasswordCallback("keystore", false);
               cbh.handle(new Callback[]{pcb});

               if (pcb.getPassword() == null) {
                  throw new Exception("A password was not entered");
               }
               cm.addIdentityPassword("keystore", pcb.getPassword().clone());
               pcb.clearPassword();
            }
            KeyStore ks = KeyStore.getInstance(getKeystoreType(), getKeystoreProvider());
            FileInputStream fis = new FileInputStream(new File(getKeystoreLocation()));
            char[] password = null;
            try {
               password = JESVaultControl.getInstance().getPassword("keystore");
               ks.load(fis, password);
               kmf.init(ks, password);
            } finally {
               IOUtils.close(fis);
               PasswordFactory.clear(password);
            }
            log.info("KeyManagerFactory successfully initialized.");
         } else {
            if (log.isDebugEnabled()) {
               log.debug("KeyStore file " + keystore.getPath() + " again not found.");
            }
            kmf.init(null, null);
            log.info("No keystore file found.");
         }

         //TrustStore Setup
         TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
         File truststore = new File(truststoreLocation);
         if (truststore.exists()) {
            if (log.isDebugEnabled()) {
               log.debug("TrustStore file " + truststore.getPath() + " located.");
            }
            KeyStore ks = KeyStore.getInstance(getTruststoreType(), getTruststoreProvider());
            FileInputStream fis = new FileInputStream(new File(getTruststoreLocation()));
            try {
               ks.load(fis, null);
               tmf.init(ks);
            } finally {
               IOUtils.close(fis);
            }
         } else {
            if (log.isDebugEnabled()) {
               log.debug("Unable to locate trustStore file " + truststore.getPath() + ". Using default JSSE initiliazation method.");
            }
            tmf.init((KeyStore) null);
         }
         log.info("TrustManagerFactory successfully initialized.");

         sslContext = SSLContext.getInstance("TLS");
         sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
         log.info("SSLContext successfully initialized.");

      } catch (Exception e) {
         log.fatal(e.getLocalizedMessage(), e);
         throw new RuntimeException(e.getLocalizedMessage());
      }
   }

   private void passwordPopup(final VaultPassword vp) {

      final Object lock = new Object();
      
      PasswordPopup ppu = new PasswordPopup(vp, lock);

      synchronized (lock) {
         java.awt.EventQueue.invokeLater(new PasswordPopup.PasswordPopupRunnable(ppu));
         while(ppu.isWorking() && lock()) {
            try {
               lock.wait(5*1000L);
            } catch (InterruptedException ex) {
               log.error("There was an error while retrieving the keystore password");
               throw new RuntimeException("There was an error while retrieving the keystore password", ex);
            }
         }
      }
   }
   
   private boolean lock() {
      Mail mail = Mail.getInstance();
      if (mail == null)
         return false;
      return !mail.isShuttingDown();
   }
}
