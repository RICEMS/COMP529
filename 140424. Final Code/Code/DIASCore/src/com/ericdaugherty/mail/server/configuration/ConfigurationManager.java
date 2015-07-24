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

import com.ericdaugherty.mail.server.JSON.JSONObject;
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.backEnd.PersistException;
import com.ericdaugherty.mail.server.configuration.cbc.CBCExecutor;
import com.ericdaugherty.mail.server.errors.ConfigurationUpdateException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.info.Realm;
import com.ericdaugherty.mail.server.info.User;
import com.ericdaugherty.mail.server.persistence.smtp.SMTPMessagePersistenceFactory;
import com.ericdaugherty.mail.server.security.JESSecurityManager;
import com.ericdaugherty.mail.server.security.PolicyHandler.PolicyEntryWrapper;
import com.ericdaugherty.mail.server.services.DeliveryService;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessageFactory;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.ClientRequest;
import com.xlat4cast.jes.dns.Resolver;
import com.xlat4cast.jes.dns.client.MXReply;
import com.xlat4cast.jes.dns.internal.DNSException;
import com.xlat4cast.jes.dns.internal.Domain;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.net.ssl.*;
import javax.swing.JOptionPane;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides a centralized repository for all configuration information.
 * <p>
 * All configuration information should be retrieved here for everyday
 * use. The ConfigurationManager will reload some configuration changes
 * dynamically.
 * <p>
 * Classes may cache the reference to the ConfigurationManager instance,
 * as only one will ever be created.
 *
 * @author Eric Daugherty
 * @author Andreas Kyrmegalos (2.x branch)
 */
public final class ConfigurationManager implements ConfigurationParameterConstants {

   /** Logger */
   private static final Log log = LogFactory.getLog(ConfigurationManager.class);
   static {
      if (!Charset.isSupported("US-ASCII"))
         throw new RuntimeException("US-ASCII must be a supported Charset.");
      if (!Charset.isSupported("UTF-8"))
         throw new RuntimeException("UTF-8 must be a supported Charset.");
      if (!Charset.isSupported("ISO-8859-1"))
         throw new RuntimeException("ISO-8859-1 must be a supported Charset.");
      
   }
   
   public static final Locale LOCALE = Locale.US;
   private static final boolean win = System.getProperty("os.name").toLowerCase().indexOf("win")!=-1;
   private static ConfigurationManager instance;
   private static volatile boolean running;
   static boolean complete;
   static boolean abort;
   
   private ConfigurationFileWatcher watcher;
   private boolean restart;
   
   private List<Domain> domainMXs;

   /**
    * @return A boolean flag indicating whether the underlying OS is a version of Windows or not
    */
   public static boolean isWin() {
      return win;
   }
   
   //Required to be supported by all JVM implementations
   private static final Charset[] defaultCharset = {
      Charset.forName("US-ASCII"),
      Charset.forName("UTF-8"),
      Charset.forName("ISO-8859-1")
   };
   
   public static Charset getUsAsciiCharset() {
      return defaultCharset[0];
   }
   
   public static Charset getUtf8Charset() {
      return defaultCharset[1];
   }
   
   public static Charset getIso88591Charset() {
      return defaultCharset[2];
   }
   
   public boolean isRestart() {
      return restart;
   }
   private ConfigurationManagerDirectories cmd;
   private ConfigurationManagerGeneral cmg;
   private ConfigurationManagerBackEndControl cmb;
   private ConfigurationManagerMail cmm;
   private ConfigurationManagerAmavis cma;
   private ConfigurationManagerCBC cmc;

   public enum TransferMode {

      FULL, LOCAL, REMOTE, TESTING;

      @Override
      public String toString() {
         switch (this) {
            case FULL:
               return "Full";
            case LOCAL:
               return "Local";
            case REMOTE:
               return "Remote";
            case TESTING:
               return "Testing";
         }
         return "";
      }
   }

   public enum RetrievalMode {

      POP3, NONE;

      @Override
      public String toString() {
         switch (this) {
            case POP3:
               return "POP3";
            case NONE:
               return "None";
         }
         return "";
      }
   }

   /**
    * Provides access to the singleton instance.
    *
    * @return the singleton instance.
    */
   public static ConfigurationManager getInstance() {
      if (instance == null) {
         throw new RuntimeException("ConfigurationManager can not be accessed before it is initialized!");
      }
      return instance;
   }

   public static void shutdown() {

      //Currently the CBC is not administered remotely, therefore
      //the simplest approach is to just keep the instance around
      if (!Mail.getInstance().isRestart()) {
         instance.cmc.shutdown();
      }
      instance.cmb.shutdown();
      if (!Mail.getInstance().isRestart()) {
         PasswordFactory.shutdown();
         JESVaultControl.shutdown();
      }
      running = false;
      if (instance.watcher != null) {
         instance.watcher.interrupt();
         instance.watcher = null;
      }
      instance.cmg.shutdown();
      DeliveryService.shutdown();
      instance = null;
      abort = false;
   }

   /**
    * The ConfigurationManager is initialized using the specified
    * directory.  This method should only be called once during
    * startup, and then never again.  The file path can not
    * be re-initialized!
    *
    * @param rootDirectory the directory JES was installed into.
    * @return returns the singleton instance of the ConfigurationManager.
    */
   public static synchronized ConfigurationManager initialize(String rootDirectory) {
      
      //Make sure we are not already configured.
      //Never called from more than one Thread. No worries.
      if (instance != null) {
         throw new RuntimeException("ConfigurationManager already initialized.");
      }

      String generalConfigFilename = "mail.xml";
      String reverseDNSFilename = "reverseDNS.conf";
      String rcptPolicyFilename = "rcptPolicy.conf";

      // Verify the General config file existance.
      File generalConfigFile = new File(rootDirectory + File.separator + "conf", generalConfigFilename);
      if (!generalConfigFile.isFile() || !generalConfigFile.exists()) {
         throw new RuntimeException("Invalid/Missing mail.xml configuration file! " + generalConfigFile.getAbsolutePath());
      }

      // Verify the ReverseDNS config file existance.
      File userreverseDNSFile = new File(rootDirectory + File.separator + "conf", reverseDNSFilename);
      if (!userreverseDNSFile.isFile() || !userreverseDNSFile.exists()) {
         log.error("Invalid reverseDNS.conf ConfigurationFile! " + userreverseDNSFile.getAbsolutePath());
         throw new RuntimeException("Invalid reverseDNS.conf ConfigurationFile! " + userreverseDNSFile.getAbsolutePath());
      }

      // Verify the Recipient Policy config file exists.
      File rcptPolicyFile = new File(rootDirectory + File.separator + "conf", rcptPolicyFilename);
      if (!rcptPolicyFile.isFile() || !rcptPolicyFile.exists()) {
         log.error("Invalid rcptPolicy.conf ConfigurationFile! " + rcptPolicyFile.getAbsolutePath());
         throw new RuntimeException("Invalid rcptPolicy.conf ConfigurationFile! " + rcptPolicyFile.getAbsolutePath());
      }
      log.warn(rootDirectory);
      // Go ahead and create the singleton instance.
      instance = new ConfigurationManager(generalConfigFile, userreverseDNSFile);
      
      instance.init(rootDirectory);

      return instance;
   }

   public static class MailErrorHandler implements ErrorHandler {

      @Override
      public void warning(SAXParseException ex) {
         log.info(ex.getLocalizedMessage());
      }

      @Override
      public void error(SAXParseException ex) {
         log.error(ex.getLocalizedMessage());
      }

      @Override
      public void fatalError(SAXParseException ex) throws SAXException {
         log.fatal(ex.getLocalizedMessage());
         throw ex;
      }
   }

   /**
    * Initialize the file path.  Enforces the Singleton pattern.
    *
    * @param generalConfigurationFile the file to load the general configuration from.
    * @param reverseDNSFile the file to load the reverse DNS servers from.
    */
   private ConfigurationManager(@NonNull File generalConfigurationFile, @NonNull File reverseDNSFile) {
      this.generalConfigurationFile = generalConfigurationFile;
      this.reverseDNSFile = reverseDNSFile;
   }

   private void init(String rootDirectory) {

      String instanceSchemaVersion = null;
      try {
         
         Document doc = getXMLDocument(generalConfigurationFile);
         instanceSchemaVersion = getSchemaVersion(doc, true);
      } catch (ConfigurationUpdateException cue) {
         throw new RuntimeException(cue.getMessage());
      }
      
      URL schemaURL = null;
      try {
         
         schemaURL = getSchemaURL(getFormattedSchemaVersion(instanceSchemaVersion));
      } catch (MalformedURLException ex) {
         log.info("", ex);
         throw new RuntimeException(ex.getMessage());
      }
      
      Schema schema = getXMLSchema(schemaURL, rootDirectory);
      try {
         
         Document doc = getXMLDocument(schemaURL);
         String validSchemaVersion = getSchemaVersion(doc, false);
         
         doc = getXMLDocument(new File(rootDirectory, "conf" + File.separator + "mail.xsd"));
         String persistedSchemaVersion = getSchemaVersion(doc, false);
      
         updateGeneralConfigFile(instanceSchemaVersion, validSchemaVersion, persistedSchemaVersion);
      } catch (ConfigurationUpdateException e) {
         //If there is no internet access, don't try to update
      }

      //Need to validate
      try {
         validateSource(new StreamSource(generalConfigurationFile), schema);
      } catch (SAXException e) {
         log.error("There was an error validating mail.xml", e);
         throw new RuntimeException("There was an error validating mail.xml");
      } catch (IOException e) {
         log.error("There was an error validating mail.xml", e);
         throw new RuntimeException("There was an error validating mail.xml");
      }

      DeliveryService.instantiate(this);


      cmd = new ConfigurationManagerDirectories(rootDirectory);
      cmg = new ConfigurationManagerGeneral();
      cmb = new ConfigurationManagerBackEndControl();
      cmm = new ConfigurationManagerMail(rootDirectory);
      cma = new ConfigurationManagerAmavis();
      cmc = new ConfigurationManagerCBC();

      // Load the properties from disk.
      loadProperties();
      
      if (Mail.isTesting()) {
         mappedPorts.put("testing", getSMTPPort() + 1);
         cma.initTesting();
         try {
            ((JESSecurityManager) System.getSecurityManager()).updateMappedPorts();
         } catch (NullPointerException npe) {
            //A security manager may not be active yet
         }
      }
   }
   
   /**
    * Handle the different schema version numbers.
    * 
    * @param instanceSchemaVersion refers to the schema version as defined in the
    * locally stored mail.xml file.
    * @param validSchemaVersion refers to the schema version as defined in the
    * mail.xsd file as retrieved over the internet. If no internet access
    * is available this version number will match the persistedSchemaVersion.
    * @param persistedSchemaVersion refers to the schema version as defined in the
    * mail.xsd file stored locally.
    */
   private void updateGeneralConfigFile(String instanceSchemaVersion,
         String validSchemaVersion, String persistedSchemaVersion) {
      
      //TODO Presently do nothing. The originally specified remote schema file used
      //by JES 2.5 has remained unchanged. A 2.5 instance will work as previously.
      //A 2.6 instance will use the new schema. Even replacing a version 2.5 jes.jar
      //with a version 2.6 will not prevent the server from starting.
      //Will not address issue now.
   }

   private void loadProperties() {

      loadGeneralProperties();
      loadreverseDNSservers();
      getBackEnd().loadUsersAndRealms();
   }
   /**
    * This is used to prevent reseting various settings
    * when mail.xml is updated
    *
    */
   private boolean fixed;

   boolean isFixed() {
      return fixed;
   }

   public boolean isLocalTestingMode() {
      return cmm.isLocalTestingMode();
   }

   void testingMode() {
      cmm.testingMode();
   }

   public SSLServerSocketFactory getSSLServerSocketFactory() {
      return cmg.getSSLServerSocketFactory();
   }

   public SSLSocketFactory getSSLSocketFactory() {
      return cmg.getSSLSocketFactory();
   }

   void addIdentityPassword(String principal, char[] pwd) {
      JESVaultControl.getInstance().addIdentityPassword(principal, pwd);
   }

   public char[] encryptUserPassword(char[] password) {
      return JESVaultControl.getInstance().encryptUserPassword(password);
   }

   public char[] getGUIDbPassword() {
      return cmb.getGUIDbPassword();
   }
   /** The file reference to the mail.xml configuration file */
   private final File generalConfigurationFile;
   /** The timestamp for the mail.xml file when it was last loaded */
   private long generalConfigurationFileTimestamp;

   private void loadGeneralProperties() {
   
      Document doc = null;
      try {
         doc = getXMLDocument(generalConfigurationFile);
      } catch (ConfigurationUpdateException e) {
         throw new RuntimeException(e.getLocalizedMessage());
      }
      
      Map<String, Element> configElements = new HashMap<String, Element>();
      NodeList nodeList = doc.getDocumentElement().getChildNodes();
      Node node;
      for (int i = 0; i < nodeList.getLength(); i++) {
         node = nodeList.item(i);
         if (node.getNodeType() == Node.ELEMENT_NODE) {
            configElements.put(node.getNodeName(), (Element) node);
         }
      }
      cmd.directoriesConfiguration(configElements.get(DIRECTORIES));
      cmg.generalConfiguration(configElements.get(GENERAL));
      cmb.backEndConfiguration(configElements.get(BACKEND));
      cmm.mailConfiguration(configElements.get(MAIL));
      cma.amavisdNewConfiguration(configElements.get(AMAVISDNEW));
      if (!fixed&&cma.isAmavisSupportActive()) {
         mappedPorts.put("amavis", cma.getAmavisFilteredSMTPPort());
      }
      cmc.cbcConfiguration(configElements.get(CBC));
      if (!fixed&&cmc.isConfigurationEnabled()) {
         mappedPorts.put("config", cmc.getConfigurationPort());
      }
      javaWrapperConfiguration(configElements.get(JAVAWRAPPER));

      //certain features have to be deffered as they are dependent on other options
      if (!fixed) {
         cmm.deferredSecurityConfiguration(Boolean.parseBoolean(((Element) ((Element) configElements.get("mail").getElementsByTagName("SMTP").item(0)).getElementsByTagName("extensions").item(0)).getAttribute("HELO")));
         if (isConfigurationEnabled())
            cmc.deferredSecurityConfiguration();
      }
      
      if (fixed) {
         cmm.updatePorts();
      }
      
      updateMappedPorts();
      
      if (fixed) {
         MailServicesControl.getInstance().notifyChange();
      }
      
      // Update the 'last loaded' timestamp.
      generalConfigurationFileTimestamp = generalConfigurationFile.lastModified();
      fixed = true;
   }

   public enum ConfigSection {
      GENERAL;
   }

   public void deferredSecurityConfiguration(ConfigSection configSection) {

      switch (configSection) {
         case GENERAL:
            if (!running) {
               cmg.deferredSecurityConfiguration();
            }
      }
   }

   public Map<String, String> getGeneralConfiguration() {
      return cmg.getConfiguration();
   }

   public Map<String, String> getBackEndConfiguration() {
      return cmb.getConfiguration();
   }

   public Map<String, String> getMailConfiguration() {
      return cmm.getConfiguration();
   }

   public Map<String, String> getDirConfiguration() {
      return cmd.getConfiguration();
   }

   public Map<String, String> getAmavisConfiguration() {
      return cma.getConfiguration();
   }

   public BackEndCreationResponse createDomainDirectory(String domain) {
      return cmd.createDomainDirectory(domain);
   }

   public void revertDomainDirectory(String domain) {
      cmd.revertDomainDirectory(domain);
   }

   public void revertUserDirectory(String userRepository) {
      cmd.revertUserDirectory(userRepository);
   }

   public void createDirectories() {
      cmd.createDirectories();
   }

   public void requestDirCreation(String directory) {
      cmd.requestDirCreation(directory);
   }

   /**
    * The root directory used to store the incoming and outgoing messages.
    *
    * @return String
    */
   public String getRootDirectory() {
      return cmd.getRootDirectory();
   }

   /**
    * The directory used to store doBackup files.
    *
    * @return backupDirectory String
    */
   public String getBackupDirectory() {
      return cmd.getBackupDirectory();
   }

   /**
    * The directory used to hold security sensitive data.
    * 
    * @return securityDirectory String
    */
   public String getSecurityDirectory() {
      return cmd.getSecurityDirectory();
   }

   /**
    * The directory used to store incoming e-mails.
    * 
    * @return SMTPDirectory String
    */
   public String getSMTPDirectory() {
      return cmd.getSMTPDirectory();
   }

   /**
    * The directory used to store the user accounts.
    *
    * @return String
    */
   public String getUsersDirectory() {
      return cmd.getUsersDirectory();
   }

   /**
    * The directory used to store failed e-mails.
    *
    * @return failedDirectory String
    */
   public String getFailedDirectory() {
      return cmd.getFailedDirectory();
   }

   public String getTestingDirectory() {
      return cmd.getTestingDirectory();
   }

   public boolean isExternalDelegated() {
      return cmg.isExternalDelegated();
   }

   public boolean isSecurityManagerEnabled() {
      return cmg.isSecurityManagerEnabled();
   }

   public boolean isPersistMaster() {
      return cmg.isPersistMaster();
   }

   /**
    * The time period to force message processors to wait before executing a
    * task.
    */
   public int getThrottlingDelay() {
      return cmg.getThrottlingDelay();
   }

   boolean isLimitedCryptography() {
      return cmg.isLimitedCryptography();
   }

   public int getMaxPassAttempts() {
      return cmg.getMaxPassAttempts();
   }

   /**
    * The maximum number of errors during a POP3 or SMTP server session.
    * 
    * @return int maxErrorCount
    */
   public int getMaxErrorCount() {
      return cmg.getMaxErrorCount();
   }

   public String[] getEnabledCiphers() {
      return cmg.getEnabledCiphers();
   }
   
   public String[] getEnabledProtocols() {
      return cmg.getEnabledProtocols();
   }
   
   public boolean isAllowRemoteRestart() {
      return cmg.isAllowRemoteRestart();
   }
   
   public boolean isLegacyFileIOMode() {
      return cmg.isLegacyFileIOMode();
   }
   
   static final class JSSEStore {
      
      private final String location;
      private final String provider;
      private final String type;
      JSSEStore(String location, String provider, String type) {
         this.location = location;
         this.provider = provider;
         this.type = type;
      }

      String getLocation() {
         return location;
      }

      String getProvider() {
         return provider;
      }

      String getType() {
         return type;
      }  
   }
   
   JSSEStore getKeystore() {
      return new JSSEStore(cmg.getKeystoreLocation(), cmg.getKeystoreProvider(), cmg.getKeystoreType());
   }
   
   JSSEStore getTruststore() {
      return new JSSEStore(cmg.getTruststoreLocation(), cmg.getTruststoreProvider(), cmg.getTruststoreType());
   }
   
   public PolicyEntryWrapper getKeystoreLocation() {
      
      return new PolicyEntryWrapper(cmg.getKeystoreLocation());
   }
   
   public PolicyEntryWrapper getTruststoreLocation() {
      
      return new PolicyEntryWrapper(cmg.getTruststoreLocation());
   }

   public ConfigurationManagerBackEnd getBackEnd() {
      return cmb.getBackEnd();
   }

   /**
    * The type of backEnd used to store domains, users, realms
    *
    * @return backEndType BackEndType
    */
   public BackEndType getBackEndType() {
      return cmb.getBackEndType();
   }

   public boolean isBackEndSecure() {
      return cmb.isBackEndSecure();
   }

   /**
    * Checks the local domains to see if the specified domain matches.
    *
    * @param domain a domain to check.
    * @return true if and only if it matches exactly an existing domain.
    */
   public boolean isLocalDomain(String domain) {
      return cmb.isLocalDomain(domain);
   }
   
   public boolean isLocalMXDomain(String domainMX) {
      
      Set<? extends Domain> domains = cmb.getBackEnd().getDomains();
      if (domainMXs == null || domainMXs.isEmpty()) {
         Resolver dnsResolver = Mail.getInstance().getDNSResolver();
         domainMXs = new LinkedList<Domain>();
         for (Domain domain:domains) {
            try {
               MXReply reply = dnsResolver.submitRequest(new ClientRequest.MXRequest(domain));
               for (Set<InetAddress> addresses : reply.getMXs().values()) {
                  for (InetAddress address : addresses)
                     domainMXs.add(new Domain(address.getHostAddress()));
               }
               //If no records are found it would be unreasonable to reject the connection
               if (reply.getMXs().isEmpty()) {
                  domainMXs.add(domain);
               }
            } catch (DNSException ex) {
               log.error(ex.getLocalizedMessage());
            }
         }
      }
      return domainMXs.contains(new Domain(domainMX));
   }
   
   public boolean isSingleDomainMode() {
      return cmb.isSingleDomainMode();
   }
   
   public Domain getSingleDomain() {
      return cmb.getSingleDomain();
   }

   public EmailAddress getDefaultMailbox(String domain) {
      return cmb.getDefaultMailbox(domain);
   }

   public void updateDefaultDomain() {
      getBackEnd().updateDefaultDomain();
   }

   /**
    * Returns the specified user, or null if the user
    * does not exist.
    *
    * @param address the user's full email address.
    * @return null if the user does not exist.
    */
   public User getUser(EmailAddress address) {
      return cmb.getUser(address);
   }

   /**
    * Returns the specified realm, or null if the realm
    * does not exist.
    *
    * @param realmName the realm's full name.
    * @return null if the realm does not exist.
    */
   public Realm getRealm(String realmName) {
      return cmb.getRealm(realmName);
   }

   public Set<? extends Realm> getRealms() {
      return cmb.getRealms();
   }

   public char[] getRealmPassword(Realm realm, EmailAddress emailAddress) {
      if (realm == null || emailAddress == null) return null;
      return cmb.getRealmPassword(realm, emailAddress);
   }

   public List<String> updateBackEndThroughConnection(List<CBCExecutor> cbcExecutors) throws PersistException {
      return cmb.updateThroughConnection(cbcExecutors);
   }
   
   public ConfigurationManagerBackEndDb.RetainServer getRetainServer() {
      if (cmb.getBackEndType() == BackEndType.RDBM) {
         return ((ConfigurationManagerBackEndDb)cmb.getBackEnd()).getServer();
      }
      return null;
   }
   
   public boolean isIPv6Preferred() {
      return cmm.isIPv6Preferred();
   }

   /**
    * The local IP address to listen on.  Null for all addresses
    *
    * @return null for all addresses.
    */
   public InetAddress getListenAddress() {
      return cmm.getListenAddress();
   }

   public TransferMode getTransferMode() {
      return cmm.getTransferMode();
   }

   public RetrievalMode getRetrievalMode() {
      return cmm.getRetrievalMode();
   }

   /**
    * The port the SMTP server listens on.
    *
    * @return port number
    */
   public int getSMTPPort() {
      return cmm.getSMTPPort();
   }

   public boolean isNonExistentLocalRejected() {
      return cmm.isNonExistentLocalRejected();
   }

   /**
    * Get the max number of delivery attempts before message is considered
    * 'undeliverable' and moved to 'failed' folder
    * @return
    */
   public int getDeliveryAttemptThreshold() {
      return cmm.getDeliveryAttemptThreshold();
   }

   /**
    * Check whether the standard SMTP modules are to use TLS/SSL security.
    *
    * @return
    */
   public boolean isStandardSMTPSecure() {
      return cmm.isStandardSMTPSecure();
   }

   /**
    * Check whether the standard SMTP/POP3 modules are to use TLS/SSL security.
    * 
    * @return
    */
   public String getClientAuthSMTP() {
      return cmm.getClientAuthSMTP();
   }

   /**
    * Check whether or not clear text passwords are allowed in SMTP sessions.
    * 
    * @return
    */
   public ClearText allowClearTextSMTP() {
      return cmm.allowClearTextSMTP();
   }

   public boolean isVerifyIP() {
      return cmm.isVerifyIP();
   }

   /** IP Addresses that are allowed to relay mail.
    * @return
    */
   public String[] getRelayApprovedIPAddresses() {
      return cmm.getRelayApprovedIPAddresses();
   }

   /** Email Addresses that are allowed to relay mail.
    * @return
    */
   public String[] getRelayApprovedEmailAddresses() {
      return cmm.getRelayApprovedEmailAddresses();
   }

   /** True if POP Before SMTP is a valid relay option
    * @return
    */
   public boolean isEnablePOPBeforeSMTP() {
      return cmm.isEnablePOPBeforeSMTP();
   }

   /** The timeout length for authenticated ip addresses
    * @return
    */
   public long getAuthenticationTimeoutMilliseconds() {
      return cmm.getAuthenticationTimeoutMilliseconds();
   }

   public boolean isHELOEnabled() {
      return cmm.isHELOEnabled();
   }

   /**
    * A flag to indicate if 8BITMIME is to be used
    *
    * @return
    */
   public boolean is8bitMIME() {
      return cmm.is8bitMIME();
   }

   /**
    * A flag to indicate if PIPELINING is to be used
    *
    * @return
    */
   public boolean isPipelining() {
      return cmm.isPipelining();
   }

   /** The maximum size (in megabytes) allowed for email attachments.
    * @return
    */
   public int getMaximumMessageSize() {
      return cmm.getMaximumMessageSize();
   }

   public int getMaxValidRCPT() {
      return cmm.getMaxValidRCPT();
   }

   public int getAddPctRCPT() {
      return cmm.getAddPctRCPT();
   }

   public int getMinTotFailRCPT() {
      return cmm.getMinTotFailRCPT();
   }

   public int getMinPctFailRCPT() {
      return cmm.getMinPctFailRCPT();
   }

   /**
    * The port the POP3 server listens on.
    *
    * @return port number
    */
   public int getPOP3Port() {
      return cmm.getPOP3Port();
   }

   /**
    * Check whether the standard SMTP modules are to use TLS/SSL security.
    *
    * @return
    */
   public boolean isStandardPOP3Secure() {
      return cmm.isStandardPOP3Secure();
   }

   /**
    * Check whether the standard SMTP/POP3 modules are to use TLS/SSL security.
    * 
    * @return
    */
   public String getClientAuthPOP3() {
      return cmm.getClientAuthPOP3();
   }

   /**
    * Check whether or not clear text passwords are allowed in POP3 sessions.
    * 
    * @return
    */
   public ClearText allowClearTextPOP3() {
      return cmm.allowClearTextPOP3();
   }

   public String getSaslQOP() {
      return cmm.getSaslQOP();
   }

   public List<String> getCRAMMembers() {
      return cmm.getCRAMMembers();
   }

   public boolean isDigestMD5Enabled() {
      return cmm.isDigestMD5Enabled();
   }

   public String getDigestMD5Ciphers() {
      return cmm.getDigestMD5Ciphers();
   }

   public List<String> getSCRAMMembers() {
      return cmm.getSCRAMMembers();
   }

   public boolean isGSSEnabled() {
      return cmm.isGSSEnabled();
   }

   public String getGSSPrincipal() {
      return cmm.getGSSPrincipal();
   }

   public int getExecuteThreadCount() {
      return cmm.getExecuteThreadCount();
   }

   /**
    * Check whether the secure SMTP/POP3 modules are active/to be activated.
    *
    * @return
    */
   public boolean isSecureActive() {
      return cmm.isSecureActive();
   }

   /**
    * The number of threads to use for each secure listener.
    *
    * @return
    */
   public int getSecureExecuteThreadCount() {
      return cmm.getSecureExecuteThreadCount();
   }

   /**
    * The port the secure SMTP server listens on.
    *
    * @return port number
    */
   public int getSecureSMTPPort() {
      return cmm.getSecureSMTPPort();
   }

   /**
    * The port the secure POP3 server listens on.
    *
    * @return port number
    */
   public int getSecurePOP3Port() {
      return cmm.getSecurePOP3Port();
   }

   /**
    * Check whether to use TLS/SSL for an outbound SMTP message
    *
    * @return outgoingSecure
    */
   public boolean isOutgoingSecure() {
      return cmm.isOutgoingSecure();
   }

   /** True if all outgoing mail should go though the default server
    * @return
    */
   public boolean isDefaultSmtpServerEnabled() {
      return cmm.isDefaultSmtpServerEnabled();
   }

   /** The servers to send all outgoing mail through
    * @return
    */
   public List<DefaultSMTPServer> getDefaultSmtpServers() {
      return cmm.getDefaultSmtpServers();
   }

   public Map<Object, RcptPolicy<String>> getRcptPolicyMap() {
      return cmm.getRcptPolicyMap();
   }

   public static Map<Object, RcptPolicy<String>> getRcptPolicyMap(Properties rcptPolicyFileEntries, Locale locale, ConfigurationManagerBackEnd backEnd) {
      return ConfigurationManagerMail.getRcptPolicyMap(rcptPolicyFileEntries, locale, backEnd);
   }

   public boolean isAmavisSupportActive() {
      return cma.isAmavisSupportActive();
   }

   public InetAddress getAmavisListenAddress() {
      return cma.getAmavisListenAddress();
   }

   public int getAmavisSMTPPort() {
      return cma.getAmavisSMTPPort();
   }

   public int getAmavisFilteredSMTPPort() {
      return cma.getAmavisFilteredSMTPPort();
   }

   public String getAmavisSMTPDirectory() {
      return cma.getAmavisSMTPDirectory();
   }
   
   public ConfigurationManagerCBC.RetainConfigurator getRetainConfigurator() {
      return cmc.getConfigurator();
   }

   public boolean isConfigurationEnabled() {
      return cmc.isConfigurationEnabled();
   }

   public boolean isConfigurationSecure() {
      return cmc.isConfigurationSecure();
   }

   public InetAddress getConfigurationAddress() {
      return cmc.getConfigurationAddress();
   }

   public int getConfigurationPort() {
      return cmc.getConfigurationPort();
   }

   private void javaWrapperConfiguration(Element element) {

      //With the last expression reseting the values after a restart is avoided
	   System.out.println(Mail.getInstance());
	   
      if (!isFixed()&&!Mail.getInstance().isRestarting()&&System.getProperty("wrapper.key") != null) {
         System.setProperty("user.language",
               ((Element) element.getElementsByTagName("language").item(0)).getTextContent());
         System.setProperty("user.country",
               ((Element) element.getElementsByTagName("country").item(0)).getTextContent());
         System.setProperty("file.encoding",
               ((Element) element.getElementsByTagName("fileEncoding").item(0)).getTextContent());
         System.setProperty("os.name",
               ((Element) element.getElementsByTagName("OSName").item(0)).getTextContent());
      }
   }

   public void updateConfigurationThroughConnection(ParentTreeNode valuesToPersist) throws ConfigurationUpdateException {

      Document doc = getXMLDocument(generalConfigurationFile);
      
      Element base = null;
      NodeList nodeList = doc.getDocumentElement().getChildNodes();
      Node node;
      for (int i = 0; i < nodeList.getLength(); i++) {
         node = nodeList.item(i);
         if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (node.getNodeName().equals(valuesToPersist.getName())) {
               base = (Element) node;
               break;
            }
         }
      }
      if (base == null) {
         String message = "mail.xml contains no configuration section named " + valuesToPersist.getName();
         log.error(message);
         throw new ConfigurationUpdateException(message);
      }

      updateDOMElements(valuesToPersist, base);

      Source source = new DOMSource(doc);
      
      //Need to validate
      try {
         validateSource(source, getXMLSchema(getSchemaURL(getFormattedSchemaVersion(getSchemaVersion(doc, true))), getRootDirectory()));
         log.info("Mail.xml validated.");
      } catch (SAXException e) {
         String message = "There was an error validating the in-memory DOM source for mail.xml.";
         log.error(message, e);
         throw new ConfigurationUpdateException(message);
      } catch (IOException e) {
         String message = "There was an error validating the in-memory DOM source for mail.xml.";
         log.error(message, e);
         throw new ConfigurationUpdateException(message);
      }

      //Persisting updates to mail.xml
      synchronized (getUpdateLock()) {
         setUpdatingFiles(true);

         FileOutputStream fos = null;
         String mailXml = getRootDirectory() + File.separator + "conf" + File.separator + "mail.xml";
         try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            StreamResult result = new StreamResult(fos = new FileOutputStream(mailXml));
            transformer.transform(source, result);
         } catch (FileNotFoundException e) {
            log.error(mailXml + " could not be found.", e);
            throw new ConfigurationUpdateException("Unable to persist updates to mail.xml");
         } catch (TransformerException e) {
            String message = "Unable to persist updates to mail.xml";
            log.error(message, e);
            throw new ConfigurationUpdateException(message);
         } finally {
            IOUtils.close(fos);
         }

         setUpdatingFiles(false);
         getUpdateLock().notifyAll();
      
         if (valuesToPersist.isRestart()) {
            restart = true;
            Mail.getInstance().checkRestart();
         }
      }
   }
   
   private Document getXMLDocument(File xmlFile) throws ConfigurationUpdateException {
      
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db;
      try {
         db = dbf.newDocumentBuilder();
         db.setErrorHandler(new MailErrorHandler());
      } catch (ParserConfigurationException e) {
         String message = "There was an error constructing the DocumentBuilder for mail.xml.";
         log.error(message, e);
         throw new ConfigurationUpdateException(message);
      }
      FileInputStream fis = null;
      try {
         Document doc = db.parse(fis = new FileInputStream(xmlFile));
         doc.getDocumentElement().normalize();
         return doc;
      } catch (SAXException e) {
         String message = "There was an error parsing mail.xml.";
         log.error(message, e);
         throw new ConfigurationUpdateException(message);
      } catch (IOException e) {
         String message = "mail.xml was not found, or could not be read.";
         log.error(message, e);
         throw new ConfigurationUpdateException(message);
      } finally {
         IOUtils.close(fis);
      }
   }
   
   private Document getXMLDocument(URL schemaURL) throws ConfigurationUpdateException {
      
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = null;
      try {
         db = dbf.newDocumentBuilder();
         db.setErrorHandler(new MailErrorHandler());
      } catch (ParserConfigurationException pce) {
         log.error("There was an error constructing the DocumentBuilder for " + schemaURL.toString(), pce);
         throw new ConfigurationUpdateException(pce.getMessage());
      }
      Document doc;
      try {
         doc = db.parse(schemaURL.toExternalForm());
      } catch (IOException ioe) {
         log.error("There was an error parsing " + schemaURL.toString(), ioe);
         throw new ConfigurationUpdateException(ioe.getMessage());
      } catch (SAXException se) {
         log.error("There was an error parsing " + schemaURL.toString(), se);
         throw new ConfigurationUpdateException(se.getMessage());
      }
      doc.getDocumentElement().normalize();
      
      return doc;
   }
   
   /**
    * 
    * Retrieve the possibly null decimal value of the schema version
    * as defined in the supplied {@link Document}.
    * 
    * @param document the document whose version attribute we require
    * @param isXML if true the document is mail.xml, else mail.xsd
    * @return the value of the corresponding version attribute or null
    */
   private String getSchemaVersion(@CheckForNull Document document, boolean isXML){
      
      if (document == null)
         return null;
      
      Element root = document.getDocumentElement();
      if (root == null ||
            (isXML && !root.getNodeName().equals("config")) ||
            (!isXML && !root.getNodeName().equals("xsd:schema"))) {
         String message = isXML?
               "No config root element found, invalid mail.xml. Aborting...":
               "No xsd:schema element found, invalid XML schema. Aborting...";
         log.error(message);
         throw new RuntimeException(message);
      }
      return root.getAttribute(isXML?"schemaVersion":"version");
   }
   
   private String getFormattedSchemaVersion(@NonNull String schemaVersion) {
      
      if (schemaVersion.isEmpty()) {
         schemaVersion = null;
      }
      else {
         schemaVersion = "_"+schemaVersion.replaceAll("\\.", "_");
      }
      return schemaVersion;
   }
   
   private URL getSchemaURL(String schemaVersion) throws MalformedURLException {
      String urlFragment = schemaVersion != null ? ("schema/mail" + schemaVersion + ".xsd") : "mail.xsd";
      return new URL("http://javaemailserver.sourceforge.net/" + urlFragment);
   }
   
   private Schema getXMLSchema(@NonNull final URL schemaURL, @NonNull String rootDirectory) {
      
      final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = null;
      
      try {
         schema = AccessController.doPrivileged(
               new PrivilegedExceptionAction<Schema>(){

                  @Override
                  public Schema run() throws SAXException, MalformedURLException{
                     return factory.newSchema(schemaURL);
                  }
               });
         
      } catch (PrivilegedActionException pae) {
         Throwable throwable = pae.getCause();
         if (throwable instanceof SAXException) {
            SAXException e = (SAXException)throwable;
            if (e.getException()==null||(
                  e.getException().getClass().equals(FileNotFoundException.class)||
                  e.getException().getClass().equals(UnknownHostException.class))) {
               log.warn("Unable to download mail.xsd, using local copy instead. "+e.getLocalizedMessage());
               try {
                  schema = factory.newSchema(new File(rootDirectory, "conf" + File.separator + "mail.xsd"));
               } catch (SAXException ex) {
                  log.error("There was an error loading the schema", ex);
                  throw new RuntimeException("There was an error loading the schema");
               }
            } else {
               log.error("There was an error loading the schema", e);
               throw new RuntimeException("There was an error loading the schema");
            }
         }
         else {
            log.error("There was an error loading the schema", throwable);
            throw new RuntimeException("There was an error loading the schema");
         }
      }
      //This is an "interesting" outcome. Despite there being a permission in the policy.file
      //for the JES protection domain and no indication as to the involvement of other protection
      //domains in the permission evaluation process, an AccessControlException is thrown.
      catch (AccessControlException ace) {
         log.warn("Unable to download mail.xsd, using local copy instead. "+ace.getLocalizedMessage());
         try {
            schema = factory.newSchema(new File(rootDirectory + File.separator + "conf" + File.separator + "mail.xsd"));
         } catch (SAXException ex) {
            log.error("There was an error loading the schema", ex);
            throw new RuntimeException("There was an error loading the schema");
         }
      }
      return schema;
   }
   
   private static void validateSource(Source source, Schema schema) throws SAXException, IOException {
      
      Validator validator = schema.newValidator();
      validator.setErrorHandler(new MailErrorHandler());
      validator.validate(source);
   }

   private void updateDOMElements(TreeNode treeNode, Element base) {
      
      if (!treeNode.getAttributes().isEmpty()) {
         Iterator<TreeAttribute> iter = treeNode.getAttributes().iterator();
         TreeAttribute attribute;
         while (iter.hasNext()) {
            attribute = iter.next();
            base.getAttributeNode(attribute.getName()).setValue(attribute.getValue());
         }
      }
      if (treeNode.getValue() != null) {
         base.setTextContent(treeNode.getValue());
      } else if (!treeNode.getChildren().isEmpty()) {
         Iterator<TreeNode> iter = treeNode.getChildren().iterator();
         TreeNode currentNode;
         Element currentElement;
         while (iter.hasNext()) {
            currentNode = iter.next();
            currentElement = (Element) base.getElementsByTagName(currentNode.getName()).item(0);
            updateDOMElements(currentNode, currentElement);
         }
      }
   }
   
   private Map<String, String> configDeviations;
   
   public void resetDeviations() {
      configDeviations = null;
   }
   
   void registerConfigDeviations(String element, String value) {
      
      if (configDeviations==null) {
         configDeviations = new HashMap<String, String>();
      }
      configDeviations.put(element, value);
   }
   
   public String getConfigDeviations() {
      try {
         if (configDeviations==null||configDeviations.isEmpty()) {
            return "";
         }
         return new JSONObject(configDeviations).toString();
      }
      finally {
         configDeviations = null;
      }
   }
   
   /** Array of domains for which the SMTP server should alter the Hello Domain **/
   private String[] reverseDNSServers;
   /** The file reference to the reverseDNS.conf configuration file */
   private final File reverseDNSFile;
   /** The timestamp for the reverseDNS.conf file when it was last loaded */
   private long reverseDNSFileTimestamp;

   /**
    * Array of domains that the SMTP server should alter the initial greeting reported domain
    *
    * @return String array
    */
   public String[] getReverseDNSServers() {
      return reverseDNSServers.clone();
   }

   private void loadreverseDNSservers() {

      BufferedReader bufferedReader = null;
      try {
         
         Set<String> reverseDNSServers = new LinkedHashSet<String>();
         
         bufferedReader = new BufferedReader(new FileReader(reverseDNSFile));
         do {
            String server = bufferedReader.readLine();
            if (server == null)
               break;
            if (!(server.charAt(0) == 0x23)) {
               reverseDNSServers.add(server);
            }
         } while (true);
         
         this.reverseDNSServers = reverseDNSServers.toArray(new String[reverseDNSServers.size()]);
         
         log.info("Loaded " + reverseDNSServers.size() + " servers from reverseDNS.conf");

         // Update the 'last loaded' timestamp.
         reverseDNSFileTimestamp = reverseDNSFile.lastModified();
      } catch (IOException e) {
         // All checks should be done before we get here, so there better
         // not be any errors.  If so, throw a RuntimeException.
         throw new RuntimeException("Error Loading reverse DNS servers File!  Unable to continue Operation.");
      } finally {
         IOUtils.close(bufferedReader);
      }
   }
   
   private final Map<String, Integer> mappedPorts = new HashMap<String, Integer>();

   public final Map<String, Integer> getMappedPorts() {
      return Collections.unmodifiableMap(mappedPorts);
   }
   
   public static final class ProcessPortMapping {
   
      private final String process;
      private final int port;
      
      ProcessPortMapping(String process, int port) {
         this.process = process;
         this.port = port;
      }
      
      public String getProcess() {
         return process;
      }
      
      public int getPort() {
         return port;
      }
   }
   
   public final ProcessPortMapping getTemporaryMappedPort() {
      return cmm.getTemporaryMappedPort();
   } 

   private void updateMappedPorts() {

      if (mappedPorts.get("smtp")==null||mappedPorts.get("smtp")!=cmm.getSMTPPort()) {
         mappedPorts.put("smtp", cmm.getSMTPPort());
      }
      if (cmm.getRetrievalMode() == RetrievalMode.POP3) {
         if (mappedPorts.get("pop3")==null||mappedPorts.get("pop3")!=cmm.getPOP3Port()) {
            mappedPorts.put("pop3", cmm.getPOP3Port());
         }
      }
      if (cmm.isSecureActive()) {
         if (mappedPorts.get("secureSmtp")==null||mappedPorts.get("secureSmtp")!=cmm.getSecureSMTPPort()) {
            mappedPorts.put("secureSmtp", cmm.getSecureSMTPPort());
         }
         if (cmm.getRetrievalMode() == RetrievalMode.POP3) {
            if (mappedPorts.get("securePop3")==null||mappedPorts.get("securePop3")!=cmm.getSecurePOP3Port()) {
               mappedPorts.put("securePop3", cmm.getSecurePOP3Port());
            }
         }
      }

      try {
         ((JESSecurityManager) System.getSecurityManager()).updateMappedPorts();
      } catch (NullPointerException npe) {
         //A security manager may not be active yet
      }
   }

   //***************************************************************
   // Watchdog Inner Class
   //***************************************************************
   private final Object updateLock = new Object();
   private volatile boolean updatingFiles = false;
   private volatile boolean disableNotify;

   public Object getUpdateLock() {
      return updateLock;
   }

   public boolean isUpdatingFiles() {
      return updatingFiles;
   }

   public void setUpdatingFiles(boolean updatingFiles) {
      this.updatingFiles = updatingFiles;
   }

   public boolean isDisableNotify() {
      return disableNotify;
   }

   public void setDisableNotify(boolean disableNotify) {
      this.disableNotify = disableNotify;
   }

   /**
    * Checks the user configuration file and reloads it if it is new.
    */
   class ConfigurationFileWatcher extends Thread {

      /**
       * Initialize the thread.
       */
      public ConfigurationFileWatcher() {
         super("Config Watchdog");
         setDaemon(true);
      }

      /**
       * Check the timestamp on the file to see if it has been updated.
       */
      @Override
      public void run() {
         final long shortSleepTime = 3 * 1000L;
         final long longSleepTime =  8 * 1000L;
         boolean general = false, user = false, reverse = false;
         Calendar calendar;
         boolean doWeeklyBackup = false, doneWeeklyBackup = false;
         RUNNING_LOOP:
         while (running) {
            synchronized (getUpdateLock()) {
               boolean updatingFiles = isUpdatingFiles();
               if (!updatingFiles) {
                  try {
                     getUpdateLock().wait(shortSleepTime);
                  } catch (InterruptedException ie) {
                     //ok to ignore
                  }
                  if (!running)
                     break;
               }
               while (isUpdatingFiles()) {
                  try {
                     getUpdateLock().wait(longSleepTime);
                  } catch (InterruptedException ie) {
                     //ok to ignore
                  }
                  if (!running)
                     break RUNNING_LOOP;
               }
               setUpdatingFiles(true);
               if (generalConfigurationFile.lastModified() > generalConfigurationFileTimestamp) {
                  log.info("General Configuration File Changed, reloading...");
                  general = true;
               }
               ConfigurationManagerBackEnd backEnd = getBackEnd();
               if (!running || backEnd == null) {
                  setUpdatingFiles(false);
                  break;
               }
               user = backEnd.persistUserUpdate();
               if (reverseDNSFile.lastModified() > reverseDNSFileTimestamp) {
                  log.info("reverse DNS servers File Changed, reloading...");
                  reverse = true;
               }
               if (general || user || reverse) {
                  int select = 0;
                  if (!isDisableNotify() && !cmg.isWarningDisabled() && !java.awt.GraphicsEnvironment.isHeadless()) {
                     select = JOptionPane.showConfirmDialog(null, "Some configuration files have been updated, is it OK to reload?",
                           "Confirm Update", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                  }
                  setDisableNotify(false);

                  if (select == 0) {
                     if (log.isDebugEnabled()) {
                        log.debug("reloading settings");
                     }
                     if (general) {
                        loadGeneralProperties();
                     }
                     if (reverse) {
                        loadreverseDNSservers();
                     }
                     if (user) {
                        backEnd.updateUsersAndRealmPasswords();
                        persistUpdates();
                     }
                  }
                  if (cmg.isNotifyDefault()) {
                     notifyDefaultUser();
                  }
               }
               general = false;
               user = false;
               reverse = false;
               setUpdatingFiles(false);
            }

            calendar = Calendar.getInstance();
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
               doWeeklyBackup = true;
            } else if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) {
               doWeeklyBackup = false;
               doneWeeklyBackup = false;
            }
            if (doWeeklyBackup && !doneWeeklyBackup) {
               File userWeek = new File(cmd.getBackupDirectory(), "user.wek");
               if (!userWeek.exists()) {
                  doWeeklyBackup();
                  doneWeeklyBackup = true;
               } else if (!doneWeeklyBackup) {
                  Calendar backupCalendar = Calendar.getInstance();
                  backupCalendar.setTimeInMillis(userWeek.lastModified());
                  if (backupCalendar.get(Calendar.DATE) != calendar.get(Calendar.DATE)) {
                     doWeeklyBackup();
                  }
                  doneWeeklyBackup = true;
               }
            }
         }
         generalConfigurationFileTimestamp = Long.MAX_VALUE;
         reverseDNSFileTimestamp = Long.MAX_VALUE;
      }

      private void notifyDefaultUser() {
         log.info("Notifying the admin of request for updates");

         SMTPMessage bounceMessage = SMTPMessageFactory.getInstance().getNewMessage();

         Domain domain = getBackEnd().getDefaultDomain();
         
         if (domain == null) {
            log.warn("Changes were made to JES. No default mailbox set, can not notify.");
            return;
         }

         EmailAddress toAddress = cmb.getDefaultMailbox(domain.getUniqueName());
         if (toAddress.isNULL())
            return;

         //Set the from address as mailserver@ the first (default) local domain.
         EmailAddress fromAddress = EmailAddress.getEmailAddress("MAILER_DAEMON", domain);

         bounceMessage.setFromAddress(fromAddress);
         bounceMessage.addToAddress(toAddress);
         bounceMessage.addDataLine(string2Bytes("From: Mail Delivery Subsystem <MAILER-DAEMON@" + domain.getDomainName() + ">"));
         bounceMessage.addDataLine(string2Bytes("To: " + toAddress));
         bounceMessage.addDataLine(string2Bytes("Subject: Request for updates"));
         bounceMessage.addDataLine(string2Bytes("Date: " + new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", ConfigurationManager.LOCALE).format(new Date())));
         bounceMessage.addDataLine(string2Bytes("MIME-Version: 1.0"));
         bounceMessage.addDataLine(string2Bytes("Content-type: text/plain; charset=us-ascii"));
         bounceMessage.addDataLine(string2Bytes(""));
         bounceMessage.addDataLine(string2Bytes("mail.xml/user.conf has changed. Please take appropriate action."));

         //Save this message so it will be delivered.
         try {
            bounceMessage.saveBegin(ConfigurationManager.getInstance().isAmavisSupportActive());
            bounceMessage.saveIncrement(bounceMessage.getDataLines(), true, false);
            bounceMessage.saveFinish();
         } catch (IOException ioe) {
            log.error("Error storing outgoing 'bounce' email message", ioe);
         } finally {
            SMTPMessagePersistenceFactory.removeUniqueId(bounceMessage.getSmtpUid());
         }
      }

      private byte[] string2Bytes(String line) {
         return line.getBytes(ConfigurationManager.getUsAsciiCharset());
      }
   }

   public void persistUpdates() {

      if(!Mail.isStarted()&&!Mail.getInstance().isRestarting()) {
         JESVaultControl.getInstance().savePasswords();
      }
      createDirectories();
      getBackEnd().persistUsersAndRealms();
      doBackup();
      if (!running) {

         running = true;
         // Start the Watchdog Thread
         watcher = instance.new ConfigurationFileWatcher();
         watcher.start();
      }
   }

   private void doBackup() {

      try {
         getBackEnd().doBackup(cmd.getBackupDirectory());
      } catch (IOException e) {
         if (log.isDebugEnabled()) {
            log.debug(e.getMessage());
         }
      }
   }

   private void doWeeklyBackup() {

      try {
         getBackEnd().doWeeklyBackup(cmd.getBackupDirectory());
      } catch (IOException e) {
         if (log.isDebugEnabled()) {
            log.debug(e.getMessage());
         }
      }
   }
}
