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
import java.security.AccessController;
import java.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

//Log imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.backEnd.PersistException;
import com.ericdaugherty.mail.server.configuration.cbc.CBCExecutor;
import com.ericdaugherty.mail.server.info.*;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 *
 * @author Andreas Kyrmegalos
 */
final class ConfigurationManagerBackEndControl implements ConfigurationParameterConstants {

   /** Logger */
   private static final Log log = LogFactory.getLog(ConfigurationManager.class);
   
   private final ConfigurationManager cm = ConfigurationManager.getInstance();
   
   void shutdown() {
      
      if (backEnd != null) {
         backEnd.shutdown();
         backEnd = null;
      }
   }
   
   Map<String,String> getConfiguration() {
      
      Map<String,String> configuration = new HashMap<String,String>();
      
      configuration.put("selectedBackEndType", backEndType.toString());
      configuration.put("backendSecure", Boolean.valueOf(backendSecure)+RESTART);
      configuration.put("backendMinimum", backendMinimum+RESTART);
      configuration.put("backendMinimumMin", "15");
      configuration.put("backendMinimumMax", "50");
      configuration.put("backendMaximum", backendMaximum+RESTART);
      configuration.put("backendMaximumMin", "100");
      configuration.put("backendMaximumMax", "1000");
      configuration.put("allowRemoteRestart", Boolean.toString(cm.isAllowRemoteRestart()));

      return configuration;
   }
   
   private JESVaultControl jesVaultControl;
   private char[] guiDbPassword;
   private ConfigurationManagerBackEnd backEnd;
   private BackEndType backEndType;
   private boolean backendSecure;
   private int backendMinimum;
   private int backendMaximum;

   char[] getGUIDbPassword() {

      AccessController.checkPermission(new PropertyPermission("jes.guiDBPassword", "read"));
      return guiDbPassword;
   }

   public ConfigurationManagerBackEnd getBackEnd() {
      return backEnd;
   }

   /**
    * The type of backend used to store domains, users, realms
    *
    * @return backEndType BackEndType
    */
   public BackEndType getBackEndType() {
      return backEndType;
   }

   public boolean isBackEndSecure() {
      return backendSecure;
   }

   /**
    * Checks the local domains to see if the specified parameter matches.
    *
    * @param domain a domain to check.
    * @return true if and only if it matches exactly an existing domain.
    */
   public boolean isLocalDomain(String domain) {
      return backEnd.isLocalDomain(domain);
   }
   
   public boolean isSingleDomainMode() {
      return backEnd.isSingleDomainMode();
   }
   
   public Domain getSingleDomain() {
      return backEnd.getSingleDomain();
   }

   public EmailAddress getDefaultMailbox(String domain) {
      if (domain == null) {
         return null;
      }
      return backEnd.getDefaultMailbox(domain);
   }

   public void updateDefaultDomain() {
      backEnd.updateDefaultDomain();
   }

   /**
    * Returns the specified user, or null if the user
    * does not exist.
    *
    * @param address the user's full email address.
    * @return null if the user does not exist.
    */
   public User getUser(EmailAddress address) {
      User user = backEnd.getUser(address);
      if (user == null) {
         log.info("Tried to load non-existent user: " + address);
      }

      return user;
   }

   /**
    * Returns the specified realm, or null if the realm
    * does not exist.
    *
    * @param realmName the realm's full name.
    * @return null if the realm does not exist.
    */
   public Realm getRealm(String realmName) {
      Realm realm = backEnd.getRealm(realmName);
      if (realm == null) {
         log.info("Tried to load non-existent realm: " + realmName);
      }
      return realm;
   }

   public Set<? extends Realm> getRealms() {
      return backEnd.getRealms();
   }

   public char[] getRealmPassword(Realm realm, EmailAddress emailAddress) {
      return backEnd.getRealmPassword(realm, emailAddress);
   }

   public List<String> updateThroughConnection(List<CBCExecutor> cbcExecutors) throws PersistException {
      return backEnd.updateThroughConnection(cbcExecutors);
   }

   void backEndConfiguration(Element element) {

      Element backendElement = null;
      if (!cm.isFixed()) {

         NodeList backendNodeList = element.getElementsByTagName("File");
         if (backendNodeList == null || backendNodeList.getLength() == 0) {
            backendNodeList = element.getElementsByTagName("Db");
         }
         if (backendNodeList == null || backendNodeList.getLength() == 0) {
            backendNodeList = element.getElementsByTagName("LDAP");
         }
         backendElement = (Element) backendNodeList.item(0);
         backEndType = BackEndType.getBackEndType(backendElement.getNodeName().toUpperCase(cm.LOCALE));
         
         backendSecure = Boolean.parseBoolean(element.getAttribute("secure"));
         
         Element cbcElement = (Element) element.getOwnerDocument().getElementsByTagName(CBC).item(0);
         
         if (!backendSecure
               && Boolean.parseBoolean(cbcElement.getAttribute("enable"))
               && Boolean.parseBoolean(cbcElement.getAttribute("secure"))) {
            cm.registerConfigDeviations("backendSecure", "true");
            backendSecure = true;
         }
         backendMinimum = Integer.parseInt(element.getAttribute("minimum"));
         backendMaximum = Integer.parseInt(element.getAttribute("maximum"));

         //if (!Mail.getInstance().isRestarting()) {
            try {
               JESVaultControl.initialize();
               jesVaultControl = JESVaultControl.getInstance();
               jesVaultControl.loadPasswords();
            }
            catch (java.security.GeneralSecurityException e) {
               log.error("Could not initialize the Vault's Cipher mechanism.", e);
               throw new RuntimeException("Could not initialize the Vault's Cipher mechanism.");
            }
         //}
         //else {
            jesVaultControl = JESVaultControl.getInstance();
         //}

         switch (backEndType) {
            case FILE: {
               backEnd = new ConfigurationManagerBackEndFile(cm);
            }
            break;
            case RDBM: {
               char[] username, password = new char[0], guiUsername, guiPassword = new char[0];
               username = jesVaultControl.getPassword(BACKEND_DB_USERNAME);
               guiUsername = jesVaultControl.getPassword(GUI_DB_USERNAME);
               
               if (username == null || guiUsername == null) {
                  log.fatal("Need to specify username and password for both the backend (full access) and the gui (read only access) to gain access to the database.");
                  throw new RuntimeException("Need to specify username and password for both the backend (full access) and the gui (read only access) to gain access to the database.");
               }
               
               int pos;
               StringBuilder sb;
               
               sb = new StringBuilder();
               sb.append(username);
               if (sb.length()>0) {
                  username = sb.substring(0, sb.indexOf("\u0000")).toCharArray();
                  pos = sb.indexOf("\u0000");
                  password = new char[sb.length()-pos-1];
                  sb.getChars(pos+1, sb.length(), password, 0);
               }
               if (sb.length()>0) {
                  sb.delete(0, sb.length());
               }
               
               sb = new StringBuilder();
               sb.append(guiUsername);
               if (sb.length()>0) {
                  guiUsername = sb.substring(0, sb.indexOf("\u0000")).toCharArray();
                  pos = sb.indexOf("\u0000");
                  guiPassword = new char[sb.length()-pos-1];
                  sb.getChars(pos+1, sb.length(), guiPassword, 0);
               }
               if (sb.length()>0) {
                  sb.delete(0, sb.length());
               }
               
               if (username.length == 0 || password.length == 0 || guiUsername.length == 0 || guiPassword.length == 0) {

                  throw new RuntimeException("Need to specify username and password for both the backend (full access) and the gui (read only access) to gain access to the database.");
               } else if (new String(username).equalsIgnoreCase(new String(guiUsername))) {

                  throw new RuntimeException("Need to specify distinct usernames for the backend and the gui.");
               }
               
               guiDbPassword = new char[guiUsername.length+1+guiPassword.length];
               System.arraycopy(guiUsername, 0, guiDbPassword, 0, guiUsername.length);
               guiDbPassword[guiUsername.length] = '\u0000';
               System.arraycopy(guiPassword, 0, guiDbPassword, guiUsername.length+1, guiPassword.length);
               
               //Since no option is given to a remote admin session to alter the Backend settings
               //and the derby server instance is not shutdown, there is really no need to reset
               //the values
               if (!Mail.getInstance().isRestarting()) {
                  String attribute = backendElement.getAttribute("directory");
                  if (attribute.length() == 0) {
                     attribute = cm.getRootDirectory();
                  }
                  System.setProperty("derby.system.home", attribute);

                  attribute = backendElement.getAttribute("host");
                  if (attribute.length() == 0) {
                     attribute = "localhost";
                  }
                  
                  java.net.InetAddress derbyListenAddress = Utils.getAllowedAddress("config.backend.db.host", attribute, backendSecure, true);
                  System.setProperty("derby.drda.host", derbyListenAddress==null?"localhost":derbyListenAddress.getHostAddress());

                  attribute = backendElement.getAttribute("port");
                  if (attribute.length() == 0) {
                     attribute = "1527";
                  }
                  System.setProperty("derby.drda.portNumber", attribute);
                  
                  log.info("JES db backend will be listening on " + derbyListenAddress + ":" + attribute);

                  System.setProperty("derby.connection.requireAuthentication", "true");
                  if (backendSecure) {
                     System.setProperty("derby.drda.sslMode", "peerAuthentication");
                  }
                  System.setProperty("derby.authentication.provider", "com.ericdaugherty.mail.server.configuration.DbAuthentication");
               }
               
               backEnd = new ConfigurationManagerBackEndDb(cm, username, password, backendMinimum, backendMaximum);
            }
            break;
            case LDAP: {
               throw new RuntimeException("The LDAP backend has not yet been implemented.");
            }
            default: {
               throw new AssertionError();
            }
         }
      }
      
      switch(backEndType) {
         case FILE: {
            if (backendElement == null)
               backendElement = (Element)element.getElementsByTagName("File").item(0);
         };break;
         case RDBM: {
            if (backendElement == null)
               backendElement = (Element)element.getElementsByTagName("Db").item(0);
         }break;
         case LDAP: {
            throw new RuntimeException("The LDAP backend has not yet been implemented.");
         }
         default: {
            throw new AssertionError();
         }
      }
      backEnd.init(backendElement);
      
   //   if (!Mail.getInstance().isRestarting()) {
         PasswordFactory.instantiate(backEndType);
     // }
   }
}
