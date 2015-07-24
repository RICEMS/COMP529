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
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.security.auth.Subject;
import javax.security.auth.login.*;
import org.w3c.dom.Element;

//Log imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.auth.AuthContext;
import com.ericdaugherty.mail.server.auth.LoginConfig;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager.*;
import com.ericdaugherty.mail.server.security.JESSecurityManager;
import com.ericdaugherty.mail.server.utils.DelimitedInputStream;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.ericdaugherty.mail.server.utils.JESProperties;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 *
 * @author Andreas Kyrmegalos
 */
final class ConfigurationManagerMail implements ConfigurationParameterConstants {

   /** Logger */
   private static final Log log = LogFactory.getLog(ConfigurationManager.class);
   private final ConfigurationManager cm;

   Map<String, String> getConfiguration() {

      //TODO An issue has arisen with the setEUID/setUID native methods
      int base = (!ConfigurationManager.isWin()&&com.xlat4cast.mail.server.UnixUID.getUID()!=0)?1025:1;
      Map<String, String> configuration = new HashMap<String, String>();

      configuration.put("externalDelegated", Boolean.toString(cm.isExternalDelegated()));
      configuration.put("listenAddress", listenAddress.getHostAddress());
      configuration.put("selectedTransferMode", transferMode.toString() + RESTART);
      configuration.put("transferMode", "Full,Local,Remote");
      configuration.put("selectedRetrievalMode", retrievalMode.toString() + RESTART);
      configuration.put("retrievalMode", "POP3,None");
      configuration.put("smtpPort", smtpPort + "");
      configuration.put("smtpPortMin", String.valueOf(base));
      configuration.put("smtpPortMax", "65535");
      configuration.put("rejectNonExistentLocal", Boolean.toString(nonExistentLocalRejected));
      configuration.put("deliveryAttemptThreshold", deliveryAttemptThreshold + "");
      configuration.put("deliveryAttemptThresholdMin", "1");
      configuration.put("deliveryAttemptThresholdMax", "60");
      configuration.put("standardSMTPsecure", Boolean.toString(standardSMTPsecure) + RESTART);
      configuration.put("selectedClientAuthSMTP", clientAuthSMTP + RESTART);
      configuration.put("clientAuthSMTP", "no,requested,required");
      configuration.put("selectedAllowClearTextSMTP", (allowClearTextSMTP.getLiteral()) + RESTART);
      configuration.put("allowClearTextSMTP", "never,encryptedOnly,always");
      configuration.put("verifyIP", Boolean.toString(verifyIP) + RESTART);
      configuration.put("relayApprovedIPAddresses", Utils.stringify(relayApprovedIPAddresses, ",") + RESTART);
      configuration.put("relayApprovedEmailAddresses", Utils.stringify(relayApprovedEmailAddresses, ",") + RESTART);
      configuration.put("enablePOPBeforeSMTP", Boolean.toString(enablePOPBeforeSMTP) + RESTART);
      configuration.put("authenticationTimeoutMilliseconds", (authenticationTimeoutMilliseconds / (60 * 1000)) + RESTART);
      configuration.put("authenticationTimeoutMillisecondsMin", "1");
      configuration.put("authenticationTimeoutMillisecondsMax", "30");
      configuration.put("HELO", Boolean.toString(enableHELO));
      configuration.put("8BITMIME", Boolean.toString(mime8bit));
      configuration.put("PIPELINING", Boolean.toString(pipelining));
      configuration.put("SIZE", maximumMessageSize + "");
      configuration.put("SIZEMin", "1");
      configuration.put("SIZEMax", "1024");
      configuration.put("maxValidRCPT", maxValidRCPT + "");
      configuration.put("maxValidRCPTMin", "100");
      configuration.put("addPctRCPT", addPctRCPT + "");
      configuration.put("addPctRCPTMin", "5");
      configuration.put("minTotFailRCPT", minTotFailRCPT + "");
      configuration.put("minTotFailRCPTMin", "20");
      configuration.put("minPctFailRCPT", minPctFailRCPT + "");
      configuration.put("minPctFailRCPTMin", "70");
      configuration.put("pop3Port", pop3Port + "");
      configuration.put("pop3PortMin", String.valueOf(base));
      configuration.put("pop3PortMax", "65535");
      configuration.put("standardPOP3secure", Boolean.toString(standardPOP3secure) + RESTART);
      configuration.put("selectedClientAuthPOP3", clientAuthPOP3 + RESTART);
      configuration.put("clientAuthPOP3", "no,requested,required");
      configuration.put("selectedAllowClearTextPOP3", (allowClearTextPOP3.getLiteral()) + RESTART);
      configuration.put("allowClearTextPOP3", "never,encryptedOnly,always");
      configuration.put("selectedSaslQOP", saslQOP + RESTART);
      configuration.put("saslQOP", "auth,auth-int,auth-conf");
      configuration.put("selectedCramMembers", Utils.stringifyString(cramMembers, ",") + RESTART);
      configuration.put("cramMembers", "CRAM-SHA-512,CRAM-SHA-384,CRAM-SHA-256,CRAM-SHA-1,CRAM-MD5");
      configuration.put("digestMD5Enabled", Boolean.toString(digestMD5Enabled) + RESTART);
      configuration.put("selectedDigestMD5Ciphers", digestMD5Ciphers + RESTART);
      configuration.put("digestMD5Ciphers", "3des,des,rc4,rc4-56,rc4-40");
      configuration.put("selectedScramMembers", Utils.stringifyString(scramMembers, ",") + RESTART);
      configuration.put("scramMembers", "SCRAM-SHA-512,SCRAM-SHA-384,SCRAM-SHA-256,SCRAM-SHA-1");
      configuration.put("gssEnabled", Boolean.toString(gssEnabled) + RESTART);
      configuration.put("gssRealm", gssRealm + RESTART);
      configuration.put("gssKDC", gssKDC + RESTART);
      configuration.put("gssPrincipal", gssOptions.get("principal") + RESTART);
      configuration.put("gssStoreKey", gssOptions.get("storeKey") + RESTART);
      configuration.put("gssUseKeyTab", gssOptions.get("useKeyTab") + RESTART);
      configuration.put("gssKeyTab", gssOptions.get("keyTab") + RESTART);
      configuration.put("executeThreadCount", executeThreadCount + RESTART);
      configuration.put("executeThreadCountMin", "2");
      configuration.put("executeThreadCountMax", "50");
      configuration.put("secureActive", Boolean.toString(secureActive) + RESTART);
      configuration.put("secureExecuteThreadCount", secureExecuteThreadCount + RESTART);
      configuration.put("secureExecuteThreadCountMin", "2");
      configuration.put("secureExecuteThreadCountMax", "50");
      configuration.put("secureSMTPPort", secureSMTPPort + RESTART);
      configuration.put("secureSMTPPortMin", String.valueOf(base));
      configuration.put("secureSMTPPortMax", "65535");
      configuration.put("securePOP3Port", securePOP3Port + RESTART);
      configuration.put("securePOP3PortMin", String.valueOf(base));
      configuration.put("securePOP3PortMax", "65535");
      configuration.put("outgoingSecure", Boolean.toString(outgoingSecure) + RESTART);
      configuration.put("defaultSMTPServers", Utils.stringifyDefaultSMTPServer(defaultSMTPServers, ","));
      configuration.put("allowRemoteRestart", Boolean.toString(cm.isAllowRemoteRestart()));

      return configuration;
   }
   
   public ConfigurationManagerMail() {
      
      cm = null;
   }
   
   public ConfigurationManagerMail(final String rootDirectory) {
      
      cm = ConfigurationManager.getInstance();
      rcptPolicyFile = new File(rootDirectory + File.separator + "conf", "rcptPolicy.conf");
   }
   private Map<String, Integer> portsToUpdate;
   private String gssRealm;
   private String gssKDC;
   private Map<String, String> gssOptions;
   private boolean localTestingMode;

   public boolean isLocalTestingMode() {
      return localTestingMode;
   }

   void testingMode() {
      String testingDirectory = cm.getTestingDirectory();
      if (testingDirectory!=null&&!testingDirectory.equals("")) {
         localTestingMode = true;
         transferMode = TransferMode.TESTING;
      }
   }
   
   private boolean iPv6Preferred;
   
   public boolean isIPv6Preferred() {
      return iPv6Preferred;
   }
   
   /** The local IP address to listen on.  Null for all addresses */
   private InetAddress listenAddress;
   public TransferMode transferMode;
   public RetrievalMode retrievalMode;

   /**
    * The local IP address to listen on.  Null for all addresses
    *
    * @return null for all addresses.
    */
   public InetAddress getListenAddress() {
      return listenAddress;
   }

   public TransferMode getTransferMode() {
      return transferMode;
   }

   public RetrievalMode getRetrievalMode() {
      return retrievalMode;
   }

   void mailConfiguration(Element element) {

         //
         // Load the address JES will listen on for incoming mail traffic
         //
      listenAddress = Utils.getAllowedAddress("config.mail.listenAddress", element.getAttribute("listenAddress"), true, true);
      log.info("JES is listening on "+listenAddress.toString());
      
      if (!cm.isFixed() && !localTestingMode) {

         iPv6Preferred = Boolean.getBoolean("java.net.preferIPv6Addresses");
         
         String transferModeString = element.getAttribute("transferMode").toUpperCase(cm.LOCALE);
         transferMode = TransferMode.valueOf(transferModeString);

         String retrievalModeString = element.getAttribute("retrievalMode").toUpperCase(cm.LOCALE);
         retrievalMode = RetrievalMode.valueOf(retrievalModeString);
      }
      if (cm.isFixed()) {

         boolean usePOP3 = retrievalMode == RetrievalMode.POP3;
         Map<String,Integer> mappings = new HashMap<String,Integer>(cm.getMappedPorts());
         Map<String,Integer> unchanged = new HashMap<String,Integer>(cm.getMappedPorts());
         unchanged.put("smtp", Utils.parsePort(((Element) element.getElementsByTagName("SMTP").item(0)).getAttribute("port"),  25));
         if (usePOP3) {
            unchanged.put("pop3", Utils.parsePort(((Element) element.getElementsByTagName("POP3").item(0)).getAttribute("port"), 110));
         }    
         if (cm.isSecureActive()) {
            unchanged.put("secureSmtp", Utils.parsePort(((Element) element.getElementsByTagName("secure").item(0)).getAttribute("SMTP"), 465));
            if (usePOP3) {
               unchanged.put("securePop3", Utils.parsePort(((Element) element.getElementsByTagName("secure").item(0)).getAttribute("POP3"), 995));
            }
         }
         Map<String,Integer> toChange = new HashMap<String,Integer>(!cm.isSecureActive()?(usePOP3?2:1):(usePOP3?4:2),1);
         if (!unchanged.get("smtp").equals(mappings.get("smtp"))) {
            toChange.put("smtp", unchanged.get("smtp"));
         }
         else {
            unchanged.put("smtp", mappings.get("smtp"));
         }
         if (usePOP3) {
            if (!unchanged.get("pop3").equals(mappings.get("pop3"))) {
               toChange.put("pop3", unchanged.get("pop3"));
            }
            else {
               unchanged.put("pop3", mappings.get("pop3"));
            }
         }
         if (cm.isSecureActive()) {
            if (!unchanged.get("secureSmtp").equals(mappings.get("secureSmtp"))) {
               toChange.put("secureSmtp", unchanged.get("secureSmtp"));
            }
            else {
               unchanged.put("secureSmtp", mappings.get("secureSmtp"));
            }
            if (usePOP3) {
               if (!unchanged.get("securePop3").equals(mappings.get("securePop3"))) {
                  toChange.put("securePop3", unchanged.get("securePop3"));
               }
               else {
                  unchanged.put("securePop3", mappings.get("securePop3"));
               }
            }
         }
         if (!toChange.isEmpty()) {
               if(unchanged.get("config") != null && !cm.getConfigurationAddress().equals(cm.getListenAddress())) {
                  unchanged.remove("config");
               }
               if(unchanged.get("amavis") != null && !cm.getAmavisListenAddress().equals(cm.getListenAddress())) {
                  unchanged.remove("amavis");
               }
            unchanged.keySet().removeAll(toChange.keySet());
            portChecking(unchanged, toChange, "smtp");
            if (usePOP3) {
               portChecking(unchanged, toChange, "pop3");
            }
            if (cm.isSecureActive()) {
               portChecking(unchanged, toChange, "secureSmtp");
               if (usePOP3) {
                  portChecking(unchanged, toChange, "securePop3");
               }
            }
            portsToUpdate = toChange;
         } else {
            portsToUpdate = Collections.emptyMap();
         }
      }
      mailSMTPConfiguration((Element) element.getElementsByTagName("SMTP").item(0));
      mailPOP3Configuration((Element) element.getElementsByTagName("POP3").item(0));
      mailAuthMechsConfiguration((Element) element.getElementsByTagName("authMechs").item(0));
      mailThreadsConfiguration((Element) element.getElementsByTagName("threads").item(0));
      mailSecureConfiguration((Element) element.getElementsByTagName("secure").item(0));
      mailOutgoingSMTPServer((Element) element.getElementsByTagName("outgoingSMTPServer").item(0));
      List<String> principals;
      if (gssEnabled) {
         System.setProperty("java.security.krb5.realm", gssRealm);
         System.setProperty("java.security.krb5.kdc", gssKDC);
         Configuration.setConfiguration(new LoginConfig("com.ericdaugherty.mail.server.auth.GSSServerMode", gssOptions));
         String principal = gssOptions.get("principal");
         if (principal.substring(0, principal.indexOf(':')).equals(principal.substring(principal.indexOf(':') + 1, principal.indexOf('/')))) {
            principals = new ArrayList<String>(2);
            principals.add(principal.substring(0, principal.indexOf(':')));
            System.setProperty("sun.security.krb5.principal", principal.substring(0, principal.indexOf(':')) + principal.substring(principal.indexOf('/')));
            Subject subject = getSubject(principals.get(0));
            AuthContext.initialize(new Subject[]{subject});
            principals.add(principals.remove(0).toLowerCase());
         } else {
            principals = new ArrayList<String>(3);
            principals.add(principal.substring(0, principal.indexOf(':')));
            System.setProperty("sun.security.krb5.principal", principal.substring(0, principal.indexOf(':')) + principal.substring(principal.indexOf('/')));
            Subject subject1 = getSubject(principals.get(0));
            principals.add(principals.remove(0).toLowerCase());
            principals.add(principal.substring(principal.indexOf(':') + 1, principal.indexOf('/')));
            System.setProperty("sun.security.krb5.principal", principal.substring(principal.indexOf(':') + 1));
            Subject subject2 = getSubject(principals.get(1));
            principals.add(principals.remove(1).toLowerCase());
            AuthContext.initialize(new Subject[]{subject1, subject2});
         }
         System.setProperty("sun.security.krb5.principal", "");
      } else {
         principals = new ArrayList<String>(0);
      }
      JESVaultControl.getInstance().addPrincipals(principals);
      principals.clear();

   }
   
   private void portChecking(Map<String,Integer> unchanged, Map<String,Integer> toChange, String process) {
   
      //An already used port by another JES process, that doesn't also change its port, will not be handed over
      if (toChange.containsKey(process)) {
         Random random = new Random();   
         int base = (!ConfigurationManager.isWin()&&com.xlat4cast.mail.server.UnixUID.getUID()!=0)?1025:1;
         int port = toChange.get(process);
         int requestedPort = port;
         
         toChange.remove(process);
            
         if (unchanged.containsValue(port)||toChange.containsValue(port)) {
            
            java.net.ServerSocket serverSocket = null;
            do {
               port = base + random.nextInt(65535 - base);
               if (!(unchanged.containsValue(port)||toChange.containsValue(port))) {
                  //A port that is already bound by a process outside the JVM is also not acceptable
                  ppm = new ProcessPortMapping(process, port);
                  if (System.getSecurityManager()!=null) {
                     ((JESSecurityManager)System.getSecurityManager()).getTemporaryMappedPort();
                  }
                  try {
                     serverSocket = new java.net.ServerSocket(port,0,cm.getListenAddress());
                     break;
                  } catch (IOException e) {
                     if (log.isDebugEnabled())
                        log.debug("", e);
                  } finally {
                     IOUtils.close(serverSocket);
                     ppm = null;
                     if (System.getSecurityManager()!=null) {
                        ((JESSecurityManager)System.getSecurityManager()).removeTemporaryPortMapping();
                     }
                  }
               }
            } while(true);
         }
         
         toChange.put(process, port);
         if (requestedPort!=port) {
            process = process.toLowerCase(cm.LOCALE);
            if (process.contains("smtp")) {
               if (process.contains("secure")) {
                  process = "secureSMTPPort";
               }
               else {
                  process = "smtpPort";
               }
            }
            else if (process.contains("pop3")) {
               if (process.contains("secure")) {
                  process = "securePOP3Port";
               }
               else {
                  process = "pop3Port";
               }
            }
            cm.registerConfigDeviations(process, ""+port);
         }
      }
   }
   
   private ProcessPortMapping ppm;
   
   ProcessPortMapping getTemporaryMappedPort() {
      return ppm;
   } 

   private Subject getSubject(String principalName) {
      LoginContext lc = null;
      try {
         LoginCallbackHandler cbh = null;
         //A password is needed, check if one exists in the passwords file
         if (JESVaultControl.getInstance().containsKey(principalName.toLowerCase())) {
            cbh = new LoginCallbackHandler();
            cbh.setPassword(JESVaultControl.getInstance().getPassword(principalName).clone());
         } //Not present in passwords file, will require a gui
         else if (!(gssOptions.containsKey("useKeyTab") && "true".equals(gssOptions.get("useKeyTab")))) {
            //If the jvm is headless throw exception
            if (java.awt.GraphicsEnvironment.isHeadless()) {
               log.error("The JVM is in headless mode but a GUI is required");
               throw new RuntimeException("A GUI is required");
            }
            cbh = new LoginCallbackHandlerGUI(principalName);
         }
         lc = new LoginContext("com.ericdaugherty.mail.server.auth.GSSServerMode", cbh);
      } catch (LoginException le) {
         throw new RuntimeException("Cannot create LoginContext. " + le.getMessage(), le);
      } catch (SecurityException se) {
         throw new RuntimeException("Cannot create LoginContext. " + se.getMessage(), se);
      }
      try {
         lc.login();
         log.info("successful kerberos login");
         Subject subject = lc.getSubject();
         subject.setReadOnly();
         return subject;

      } catch (AccountExpiredException aee) {
         throw new RuntimeException("Your account has expired. Please notify your administrator.", aee);
      } catch (CredentialExpiredException cee) {
         throw new RuntimeException("Your credentials have expired.", cee);
      } catch (LoginException fle) {
         throw new RuntimeException("Authentication Failed", fle);
      }
   }
   /** The port the SMTP server listens on. */
   private int smtpPort;
   private boolean nonExistentLocalRejected;
   /**
    * The max number of delivery attempts before message is considered
    * 'undeliverable' and moved to 'failed' folder
    */
   private int deliveryAttemptThreshold;
   /** Controls whether or not the standard server modules will use TLS/SSL */
   private boolean standardSMTPsecure;
   /** Defines the client-auth type */
   private String clientAuthSMTP;
   /** Controls whether or not clear text password are allowed for SMTP sessions */
   private ClearText allowClearTextSMTP;
   private boolean verifyIP;
   /** IP Addresses that are allowed to relay mail. */
   private String[] relayApprovedIPAddresses;
   /** Email Addresses that are allowed to relay mail. */
   private String[] relayApprovedEmailAddresses;
   /** True if POP Before SMTP is enabled */
   private boolean enablePOPBeforeSMTP;
   /** The timeout length for authenticated ip addresses */
   private long authenticationTimeoutMilliseconds;
   private boolean enableHELO;
   /** A flag to control the 8BITMIME feature **/
   private boolean mime8bit;
   /** A flag to control the PIPELINING feature **/
   private boolean pipelining;
   /** The maximum size (in megabytes) allowed for email attachments. */
   private int maximumMessageSize;
   private int maxValidRCPT;
   private int addPctRCPT;
   private int minTotFailRCPT;
   private int minPctFailRCPT;

   /**
    * The port the SMTP server listens on.
    *
    * @return port number
    */
   public int getSMTPPort() {
      return smtpPort;
   }

   public boolean isNonExistentLocalRejected() {
      return nonExistentLocalRejected;
   }

   /**
    * Get the max number of delivery attempts before message is considered
    * 'undeliverable' and moved to 'failed' folder
    * @return int deliveryAttemptThreshold
    */
   public int getDeliveryAttemptThreshold() {
      return deliveryAttemptThreshold;
   }

   /**
    * Check whether the standard SMTP modules are to use TLS/SSL security.
    *
    * @return boolean standardSMTPsecure
    */
   public boolean isStandardSMTPSecure() {
      return standardSMTPsecure;
   }

   /**
    * Check whether the standard SMTP/POP3 modules are to use TLS/SSL security.
    * 
    * @return boolean clientAuth
    */
   public String getClientAuthSMTP() {
      return clientAuthSMTP;
   }

   /**
    * Check whether or not clear text passwords are allowed in SMTP sessions.
    * 
    * @return int allowClearText
    */
   public ClearText allowClearTextSMTP() {
      return allowClearTextSMTP;
   }

   public boolean isVerifyIP() {
      return verifyIP;
   }

   /** IP Addresses that are allowed to relay mail. */
   public String[] getRelayApprovedIPAddresses() {
      return relayApprovedIPAddresses;
   }

   /** Email Addresses that are allowed to relay mail. */
   public String[] getRelayApprovedEmailAddresses() {
      return relayApprovedEmailAddresses;
   }

   /** True if POP Before SMTP is a valid relay option */
   public boolean isEnablePOPBeforeSMTP() {
      return enablePOPBeforeSMTP;
   }

   /** The timeout length for authenticated ip addresses */
   public long getAuthenticationTimeoutMilliseconds() {
      return authenticationTimeoutMilliseconds;
   }

   public boolean isHELOEnabled() {
      return enableHELO;
   }

   /**
    * A flag to indicate if 8BITMIME is to be used
    *
    * @return boolean mime8bit
    */
   public boolean is8bitMIME() {
      return mime8bit;
   }

   /**
    * A flag to indicate if PIPELINING is to be used
    *
    * @return boolean pipelining
    */
   public boolean isPipelining() {
      return pipelining;
   }

   /** The maximum size (in megabytes) allowed for email attachments. */
   public int getMaximumMessageSize() {
      return maximumMessageSize;
   }

   public int getMaxValidRCPT() {
      return maxValidRCPT;
   }

   public int getAddPctRCPT() {
      return addPctRCPT;
   }

   public int getMinTotFailRCPT() {
      return minTotFailRCPT;
   }

   public int getMinPctFailRCPT() {
      return minPctFailRCPT;
   }

   private void mailSMTPConfiguration(Element element) {

      if (!cm.isFixed()){
         smtpPort = Utils.parsePort(element.getAttribute("port"), 25);
      }
      nonExistentLocalRejected = Boolean.valueOf(element.getAttribute("rejectNonExistentLocal"));
      Element delivery = (Element) element.getElementsByTagName("delivery").item(0);
      deliveryAttemptThreshold = Integer.parseInt(delivery.getAttribute("threshold"));
      Element secureChannel = (Element) element.getElementsByTagName("secureChannel").item(0);
      if (!cm.isFixed()) {

         standardSMTPsecure = Boolean.parseBoolean(secureChannel.getAttribute("enable"));
         clientAuthSMTP = secureChannel.getAttribute("clientAuth");
      }
      Element authentication = (Element) element.getElementsByTagName("authentication").item(0);
      if (!cm.isFixed()) {

         String smtpOption = authentication.getAttribute("allowClearText");
         if (smtpOption.equals("always")) {
            allowClearTextSMTP = ClearText.ALWAYS;
         } else if (smtpOption.equals("never")) {
            allowClearTextSMTP = ClearText.NEVER;
         } else {
            allowClearTextSMTP = ClearText.ENCRYPTED_ONLY;
         }
         if (verifyIP = Boolean.parseBoolean(authentication.getAttribute("verifyIP"))) {
            verifyIP = VerifyIPConfigurator.initializeVerifyIPConfigurator(cm.getRootDirectory());
         }
         // Relay approved IP Addresses
         String ipAddresses = authentication.getElementsByTagName("IPOverride").item(0).getTextContent();
         if (ipAddresses != null) {
            relayApprovedIPAddresses = Utils.tokenize(ipAddresses.trim(), true, " ");
         }
         // Relay approved email Addresses
         String emailAddresses = authentication.getElementsByTagName("MailFromOverride").item(0).getTextContent();
         if (emailAddresses != null) {
            relayApprovedEmailAddresses = Utils.tokenize(emailAddresses.trim(), true, " ");
         }
         Element popBeforeSMTP = (Element) authentication.getElementsByTagName("POPBeforeSMTP").item(0);
         enablePOPBeforeSMTP = Boolean.parseBoolean(popBeforeSMTP.getAttribute("enable"));
         // Initialize the timeout Minutes parameter
         authenticationTimeoutMilliseconds = Integer.parseInt(popBeforeSMTP.getAttribute("timeout")) * 60L * 1000L;
      }
      Element extensions = (Element) element.getElementsByTagName("extensions").item(0);
      //enableHELO is deffered
      mime8bit = Boolean.parseBoolean(extensions.getAttribute("MIME8bit"));
      pipelining = Boolean.parseBoolean(extensions.getAttribute("pipelining"));
      maximumMessageSize = Integer.parseInt(extensions.getAttribute("size"));
      Element rcptPolicy = (Element) element.getElementsByTagName("rcptPolicy").item(0);
      maxValidRCPT = Integer.parseInt(((Element) rcptPolicy.getElementsByTagName("maxValidRcpt").item(0)).getFirstChild().getNodeValue());
      addPctRCPT = Integer.parseInt(((Element) rcptPolicy.getElementsByTagName("addPctRcpt").item(0)).getFirstChild().getNodeValue());
      minTotFailRCPT = Integer.parseInt(((Element) rcptPolicy.getElementsByTagName("minTotFailRcpt").item(0)).getFirstChild().getNodeValue());
      minPctFailRCPT = Integer.parseInt(((Element) rcptPolicy.getElementsByTagName("minPctFailRcpt").item(0)).getFirstChild().getNodeValue());
      rcptPolicyMap = getRcptPolicyMap(loadFile(rcptPolicyFile), cm.LOCALE, cm.getBackEnd());
   }
   /** The port the POP3 server listens on */
   private int pop3Port;
   /** Controls whether or not the standard server modules will use TLS/SSL */
   private boolean standardPOP3secure;
   /** Defines the client-auth type */
   private String clientAuthPOP3;
   /** Controls whether or not clear text password are allowed for POP3 sessions */
   private ClearText allowClearTextPOP3;

   /**
    * The port the POP3 server listens on.
    *
    * @return port number
    */
   public int getPOP3Port() {
      return pop3Port;
   }

   /**
    * Check whether the standard SMTP modules are to use TLS/SSL security.
    *
    * @return boolean standardSMTPsecure
    */
   public boolean isStandardPOP3Secure() {
      return standardPOP3secure;
   }

   /**
    * Check whether the standard SMTP/POP3 modules are to use TLS/SSL security.
    * 
    * @return boolean clientAuth
    */
   public String getClientAuthPOP3() {
      return clientAuthPOP3;
   }

   /**
    * Check whether or not clear text passwords are allowed in POP3 sessions.
    * 
    * @return int allowClearText
    */
   public ClearText allowClearTextPOP3() {
      return allowClearTextPOP3;
   }

   private void mailPOP3Configuration(Element element) {
 
      if (!cm.isFixed()){
        pop3Port = Utils.parsePort(element.getAttribute("port"), 110);
      }
      Element secureChannel = (Element) element.getElementsByTagName("secureChannel").item(0);
      if (!cm.isFixed()) {

         standardPOP3secure = Boolean.parseBoolean(secureChannel.getAttribute("enable"));
         clientAuthPOP3 = secureChannel.getAttribute("clientAuth");
      }
      Element authentication = (Element) element.getElementsByTagName("authentication").item(0);
      if (!cm.isFixed()) {

         String pop3Option = authentication.getAttribute("allowClearText");
         if (pop3Option.equals("always")) {
            allowClearTextPOP3 = ClearText.ALWAYS;
         } else if (pop3Option.equals("never")) {
            allowClearTextPOP3 = ClearText.NEVER;
         } else {
            allowClearTextPOP3 = ClearText.ENCRYPTED_ONLY;
         }
      }
   }
   private String saslQOP;
   private List<String> cramMembers;
   private boolean digestMD5Enabled;
   private String digestMD5Ciphers;
   private List<String> scramMembers;
   private boolean gssEnabled;

   public String getSaslQOP() {
      return saslQOP;
   }

   public List<String> getCRAMMembers() {
      return cramMembers;
   }

   public boolean isDigestMD5Enabled() {
      return digestMD5Enabled;
   }

   public String getDigestMD5Ciphers() {
      return digestMD5Ciphers;
   }

   public List<String> getSCRAMMembers() {
      return scramMembers;
   }

   public boolean isGSSEnabled() {
      return gssEnabled;
   }

   public String getGSSPrincipal() {
      return gssOptions.get("principal");
   }

   private void mailAuthMechsConfiguration(Element element) {

      if (!cm.isFixed()) {

         saslQOP = element.getAttribute("qop");
         String cramMembers = ((Element) element.getElementsByTagName("CRAM").item(0)).getTextContent();
         if (!(cramMembers == null || cramMembers.trim().length() == 0)) {

            List<String> systemCRAMMembers = new ArrayList<String>();
            systemCRAMMembers.add("SHA-512");
            systemCRAMMembers.add("SHA-384");
            systemCRAMMembers.add("SHA-256");
            systemCRAMMembers.add("SHA-1");
            systemCRAMMembers.add("MD5");
            List<String> suppliedCRAMMembers = Arrays.asList(cramMembers.toUpperCase(cm.LOCALE).split(","));
            List<String> acceptedCRAMMembers = new ArrayList<String>();
            for (String cramMember : systemCRAMMembers) {
               if (suppliedCRAMMembers.contains(cramMember)) {
                  acceptedCRAMMembers.add("CRAM-" + cramMember);
               }
            }
            if (!acceptedCRAMMembers.isEmpty()) {

               this.cramMembers = acceptedCRAMMembers;
            } else {

               log.info("A number of CRAM authentication mechanisms were supplied but all were rejected: " + cramMembers);
            }
         }
         Element digestMD5 = (Element) element.getElementsByTagName("DIGEST-MD5").item(0);
         digestMD5Enabled = Boolean.parseBoolean(digestMD5.getAttribute("enable"));
         digestMD5Ciphers = ((Element) digestMD5.getElementsByTagName("ciphers").item(0)).getTextContent();
         String scramMembers = ((Element) element.getElementsByTagName("SCRAM").item(0)).getTextContent();
         if (!(scramMembers == null || scramMembers.trim().length() == 0)) {

            List<String> systemSCRAMMembers = new ArrayList<String>();
            systemSCRAMMembers.add("SHA-512");
            systemSCRAMMembers.add("SHA-384");
            systemSCRAMMembers.add("SHA-256");
            systemSCRAMMembers.add("SHA-1");
            List<String> suppliedSCRAMMembers = Arrays.asList(scramMembers.toUpperCase(cm.LOCALE).split(","));
            List<String> acceptedSCRAMMembers = new ArrayList<String>();
            for (String scramMember : systemSCRAMMembers) {
               if (suppliedSCRAMMembers.contains(scramMember)) {
                  acceptedSCRAMMembers.add("SCRAM-" + scramMember);
               }
            }
            if (!acceptedSCRAMMembers.isEmpty()) {
               this.scramMembers = acceptedSCRAMMembers;
            } else {
               log.info("A number of CRAM authentication mechanisms was supplied but all were rejected: " + cramMembers);
            }
         }
         Element gssapi = (Element) element.getElementsByTagName("GSS-API").item(0);
         gssEnabled = Boolean.parseBoolean(gssapi.getAttribute("enable"));
         gssRealm = gssapi.getAttribute("realm");
         if (gssEnabled && (gssRealm == null || gssRealm.trim().length() == 0)) {

            throw new RuntimeException("A Kerberos 5 realm has to be defined");
         }
         if (gssRealm == null) {
            gssRealm = ""; 
         }
         else {
            gssRealm = gssRealm.trim();
         }
         gssRealm = gssRealm.toUpperCase(cm.LOCALE);
         gssKDC = gssapi.getAttribute("kdc");
         if (gssEnabled && (gssKDC == null || gssKDC.trim().length() == 0)) {

            throw new RuntimeException("A Kerberos 5 kdc server hostname has to be defined");
         }
         if (gssKDC == null) {
            gssKDC = "";
         }
         else {
            gssKDC = gssKDC.trim();
         }
         gssOptions = new HashMap<String, String>(5, 0.75f);
         String principal = gssapi.getAttribute("principal");
         if (gssEnabled && (principal == null || principal.trim().length() == 0)) {

            throw new RuntimeException("A Kerberos 5 principal has to be defined");
         }
         if (principal != null && principal.trim().length() > 0) {
            if (principal.indexOf('/') == -1 || principal.charAt(0) == '/') {

               principal = "smtp:pop/" + principal.substring(principal.charAt(0) == '/' ? 1 : 0);
            } else {

               if (principal.indexOf(':') == -1) {

                  String protocol = principal.substring(0, principal.indexOf('/'));
                  principal = protocol + ":" + protocol + principal.substring(principal.indexOf('/'));
               } else {

                  String smtp, pop;
                  if (principal.charAt(0) == ':') {

                     smtp = "smtp";
                     pop = principal.substring(principal.indexOf(':') + 1, principal.indexOf('/'));
                  } else if (principal.charAt(principal.indexOf('/') - 1) == ':') {

                     smtp = principal.substring(0, principal.indexOf(':'));
                     pop = "pop";
                  } else {

                     smtp = principal.substring(0, principal.indexOf(':'));
                     pop = principal.substring(principal.indexOf(':') + 1, principal.indexOf('/'));
                  }
                  principal = smtp + ":" + pop + principal.substring(principal.indexOf('/'));

               }
            }
         }
         else {
            principal = "";
         }
         if (log.isDebugEnabled()) {

            log.debug(principal);
         }
         gssOptions.put("principal", principal);
         gssOptions.put("storeKey", Boolean.valueOf(gssapi.getAttribute("storeKey")).toString());
         Boolean useKeyTab = Boolean.valueOf(gssapi.getAttribute("useKeytab"));
         if (useKeyTab) {

            gssOptions.put("useKeyTab", "true");
            String keyTab = gssapi.getAttribute("keytab");
            if (gssEnabled && (keyTab == null || keyTab.length() == 0)) {

               throw new RuntimeException("A Kerberos 5 keyTab filename is required if useKeyTab is set to true");
            }
            if (keyTab == null) {
               keyTab = "";
            }
            else {
               keyTab = keyTab.trim();
            }
            gssOptions.put("keyTab", keyTab);
         } else {

            gssOptions.put("useKeyTab", "false");
            gssOptions.put("keyTab", "");
         }
      }
   }
   /** The number of threads to use for each non secure listener */
   private int executeThreadCount;

   /**
    * The number of threads to use for each listener.
    *
    * @return int
    */
   public int getExecuteThreadCount() {
      return executeThreadCount;
   }

   private void mailThreadsConfiguration(Element element) {

      executeThreadCount = Integer.parseInt(element.getAttribute("number"));
   }
   /** Controls whether or not the secure server modules will be activated */
   private boolean secureActive;
   /** The number of threads to use for each secure listener */
   private int secureExecuteThreadCount;
   /** The port the secure SMTP server listens on. */
   private int secureSMTPPort;
   /** The port the secure POP3 server listens on */
   private int securePOP3Port;

   /**
    * Check whether the secure SMTP/POP3 modules are active/to be activated.
    *
    * @return boolean secureActive
    */
   public boolean isSecureActive() {
      return secureActive;
   }

   /**
    * The number of threads to use for each secure listener.
    *
    * @return int
    */
   public int getSecureExecuteThreadCount() {
      return secureExecuteThreadCount;
   }

   /**
    * The port the secure SMTP server listens on.
    *
    * @return int secureSMTPPort
    */
   public int getSecureSMTPPort() {
      return secureSMTPPort;
   }

   /**
    * The port the secure POP3 server listens on.
    *
    * @return port number
    */
   public int getSecurePOP3Port() {
      return securePOP3Port;
   }

   private void mailSecureConfiguration(Element element) {

      if (!cm.isFixed()) {

         secureActive = Boolean.parseBoolean(element.getAttribute("enable"));
         secureExecuteThreadCount = Integer.parseInt(element.getAttribute("number")); 
         if (!cm.isFixed()){
            secureSMTPPort = Utils.parsePort(element.getAttribute("SMTP"), 465);
            securePOP3Port = Utils.parsePort(element.getAttribute("POP3"), 995);
         }
      }
   }
   private boolean outgoingSecure;
   /** True if all outgoing mail should go though the default server */
   private boolean defaultSMTPServerEnabled;
   /** The servers to send all outgoing mail through */
   private List<DefaultSMTPServer> defaultSMTPServers;

   /**
    * Check whether to use TLS/SSL for an outbound SMTP message
    *
    * @return boolean outgoingSecure
    */
   public boolean isOutgoingSecure() {
      return outgoingSecure;
   }

   /** True if all outgoing mail should go though the default server */
   public boolean isDefaultSmtpServerEnabled() {
      return defaultSMTPServerEnabled;
   }

   /** The servers to send all outgoing mail through */
   public List<DefaultSMTPServer> getDefaultSmtpServers() {
      return defaultSMTPServers;
   }

   private void mailOutgoingSMTPServer(Element element) {

      if (!cm.isFixed()) {
         outgoingSecure = Boolean.parseBoolean(element.getAttribute("secure"));
      }
      String smtpServers = ((Element) element.getElementsByTagName("server").item(0)).getTextContent();
      if (smtpServers != null && smtpServers.trim().length() > 0) {
         defaultSMTPServerEnabled = true;

         String[] raws = Utils.tokenize(smtpServers.trim(), true, " ");
         defaultSMTPServers = new java.util.concurrent.CopyOnWriteArrayList<DefaultSMTPServer>();
         for (String raw : raws) {
            defaultSMTPServers.add(getDefaultSMTPServer(raw));
         }
      } else {
         defaultSMTPServerEnabled = false;
         defaultSMTPServers = new java.util.concurrent.CopyOnWriteArrayList<DefaultSMTPServer>();
      }
   }
   
   DefaultSMTPServer getDefaultSMTPServer(String entry) {
      
      String realm, server, credentials;

      int slash = entry.indexOf('/');
      if (slash == -1) {
         credentials = null;
      } else {
         credentials = entry.substring(slash + 1);
         entry = entry.substring(0, slash);
      }

      DefaultSMTPServer defaultSMTPServer = new DefaultSMTPServer();

      int colon = entry.lastIndexOf(':');
      if (colon!=-1) {
         try {
            defaultSMTPServer.setPort(Integer.parseInt(entry.substring(colon + 1)));
            entry = entry.substring(0, colon);
         }
         catch (NumberFormatException nfe) {
            defaultSMTPServer.setPort(25);
         }
      }
      else {
         defaultSMTPServer.setPort(25);
      }

      colon = entry.indexOf(':');
      if (colon == -1) {
         server = entry;
         realm = null;
      } else {
         server = entry.substring(colon+1);
         if (credentials!=null) {
            realm = entry.substring(0,colon);
         }
         else {
            realm = null;
            log.warn("A realm was supplied for a server with no accompanying credentials. Ignoring the realm...");
         }
      }

      defaultSMTPServer.setHost(server);
      if (defaultSMTPServer.getHost().length() == 0) {
         defaultSMTPServer.setHost("localhost");
      }

      defaultSMTPServer.setRealm(realm);

      if (credentials != null) {
         colon = credentials.indexOf(':');
         defaultSMTPServer.setUsername(credentials.substring(0, colon));
         defaultSMTPServer.setPassword(credentials.substring(colon + 1));
      }

      return defaultSMTPServer;
   }
   
   /** The file reference to the rcptPolicy.conf configuration file */
   private File rcptPolicyFile;
   //
   // recipient policy variables
   //
   private Map<Object, RcptPolicy<String>> rcptPolicyMap;

   public Map<Object, RcptPolicy<String>> getRcptPolicyMap() {
      return rcptPolicyMap;
   }

   public static Map<Object, RcptPolicy<String>> getRcptPolicyMap(Properties rcptPolicyFileEntries, Locale locale, ConfigurationManagerBackEnd backEnd) {

      Map<Object, RcptPolicy<String>> rcptPolicyMap = new ConcurrentHashMap<Object, RcptPolicy<String>>();

      Enumeration enumeration = rcptPolicyFileEntries.keys();
      String key;
      Properties lowerCased = new Properties();
      while (enumeration.hasMoreElements()) {
         key = (String) enumeration.nextElement();
         lowerCased.put(key.toLowerCase(locale), removeWSFromString(rcptPolicyFileEntries.getProperty(key)));
      }

      rcptPolicyFileEntries = lowerCased;

      boolean containsGlobalAllow = false;
      boolean containsGlobalBlock = false;
      if (rcptPolicyFileEntries.containsKey("allow")) {
         containsGlobalAllow = true;
      }
      if (rcptPolicyFileEntries.containsKey("block")) {
         containsGlobalBlock = true;
      }

      if (!containsGlobalAllow) {
         if (!containsGlobalBlock) {
            CopyOnWriteArrayList<String> single = new CopyOnWriteArrayList<String>();
            single.add("ALL");
            rcptPolicyMap.put("#####", new RcptPolicy<String>(single, true));
         } else {
            rcptPolicyMap.put("#####", new RcptPolicy<String>(new CopyOnWriteArrayList<String>(rcptPolicyFileEntries.getProperty("block").toLowerCase(locale).split(",")), false));
         }
      } else {
         if (!containsGlobalBlock) {
            rcptPolicyMap.put("#####", new RcptPolicy<String>(new CopyOnWriteArrayList<String>(rcptPolicyFileEntries.getProperty("allow").toLowerCase(locale).split(",")), true));
         } else {
            rcptPolicyMap.put("#####", new RcptPolicy<String>(new CopyOnWriteArrayList<String>(rcptPolicyFileEntries.getProperty("block").toLowerCase(locale).split(",")), false));
         }
      }

      Set rcptPolicyKeys = rcptPolicyFileEntries.keySet();
      Iterator iter = rcptPolicyKeys.iterator();
      Domain domainKey;
      while (iter.hasNext()) {
         key = (String) iter.next();
         if (key.equals("allow") || key.equals("block")) {
            continue;
         }
         domainKey = new Domain(key.substring(key.indexOf('.') + 1));
         if (backEnd != null && !backEnd.isLocalDomain(domainKey.getDomainName())) {
            continue;
         }
         if (key.startsWith("allow")) {
            if (!rcptPolicyKeys.contains("block." + domainKey)) {
               rcptPolicyMap.put(domainKey, new RcptPolicy<String>(new CopyOnWriteArrayList<String>(rcptPolicyFileEntries.getProperty(key).toLowerCase(locale).split(",")), true));
            }
         } else if (key.startsWith("block")) {
            rcptPolicyMap.put(domainKey, new RcptPolicy<String>(new CopyOnWriteArrayList<String>(rcptPolicyFileEntries.getProperty(key).toLowerCase(locale).split(",")), false));
         }
      }
      return rcptPolicyMap;
   }

   private static String removeWSFromString(String text) {
      StringBuilder sb = new StringBuilder(text.length());
      for (char c : text.toCharArray()) {
         if (c == 0x20) {
            continue;
         }
         sb.append(c);
      }
      return sb.toString();
   }

   private Properties loadFile(File fileToLoad) {

      DelimitedInputStream dis = null;
      try {
         dis = new DelimitedInputStream(new FileInputStream(fileToLoad), 2048);
         JESProperties jesProperties = new JESProperties(dis);
         jesProperties.load();
         return jesProperties.getProperties();
      } catch (IOException e) {
         // All checks should be done before we get here, so there better
         // not be any errors.  If so, throw a RuntimeException.
         throw new RuntimeException("Error Loading Recipient Policy File!  Unable to continue Operation.");
      } finally {
         IOUtils.close(dis);
      }
   }

   void deferredSecurityConfiguration(boolean enable) {
      enableHELO = enable;
      enableHELO = enableHELO && !(standardSMTPsecure || secureActive);
      enableHELO = enableHELO && !(cramMembers != null || digestMD5Enabled || scramMembers != null || gssEnabled);
      if (standardSMTPsecure || standardPOP3secure || secureActive || outgoingSecure || cm.isConfigurationSecure() || cm.isBackEndSecure()) {

         log.info("A secure data layer is required");
         cm.deferredSecurityConfiguration(ConfigurationManager.ConfigSection.GENERAL);
      }
      else {
         log.info("No secure data layer is required");
      }
   }
   
   void updatePorts() {
      
      if (portsToUpdate.containsKey("smtp")) {
         smtpPort = portsToUpdate.get("smtp");
      }
      if (portsToUpdate.containsKey("pop3")) {
         pop3Port = portsToUpdate.get("pop3");
      }
      if (cm.isSecureActive()) {
         if (portsToUpdate.containsKey("secureSmtp")) {
            secureSMTPPort = portsToUpdate.get("secureSmtp");
         }
         if (portsToUpdate.containsKey("securePop3")) {
            securePOP3Port = portsToUpdate.get("securePop3");
         }
      }
   }
}
