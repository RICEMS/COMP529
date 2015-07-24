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

package com.ericdaugherty.mail.server.configuration.backEnd;

import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.ConfigurationManagerBackEndFile;
import com.ericdaugherty.mail.server.configuration.cbc.NewRealm;
import com.ericdaugherty.mail.server.configuration.cbc.NewUser;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.*;
import com.ericdaugherty.mail.server.utils.JESProperties;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.IOException;
import java.util.*;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class FilePersistExecutor implements PersistExecutor {
   
   /** Logger */
   private static final Log log = LogFactory.getLog(FilePersistExecutor.class);
   private ConfigurationManager cm = ConfigurationManager.getInstance();
   
   /** A Map of Users keyed by their username */
   private Map<String, UserFile> users;
   /** A Map of Realms keyed by their uniqueRealmName */
   private Map<String, Realm> realms;
   /** List of available local domains */
   private final List<Domain> domains;
   
   public FilePersistExecutor(Map<String, UserFile> users, Map<String, Realm> realms, List<Domain> domains) {
      this.users = users;
      this.realms = realms;
      this.domains = domains;
   }
   
   @Override public void insertDomain(List<String> domains){}
   @Override public void deleteDomain(Set<Integer> domainIds){}

   @Override public void setDefaultDomain(int domainId){}

   @Override public void insertUser(List<NewUser> newUsers){
   
      Thread thread = Thread.currentThread();
      
      boolean updateUserFile = false;

      //First add new Users to the User Map
      for (Iterator<NewUser> iter = newUsers.iterator();iter.hasNext();) {
         NewUser newUser = iter.next();
         String username = newUser.username;
         int atPos = username.indexOf('@');
         if (atPos == -1) {
            log.error("User's " + username + " mailbox is malformed (no domain specified). Will not add.");
            iter.remove();
            if (thread.isInterrupted()) {
               return;
            }
            continue;
         }
         Domain domain = new Domain(username.substring(atPos+1));
         if (!cm.isLocalDomain(domain.getUniqueName())) {
            log.error("The user " + username + " doesn't belong to a local domain. Will not add.");
            iter.remove();
            if (thread.isInterrupted()) {
               return;
            }
            continue;
         }
         domain = domains.get(domains.indexOf(domain));
         if (!users.containsKey(username)) {
            username = username.substring(0, atPos+1)+domain.getDomainName();
            try {
               UserFile userToAdd = new UserFile(new EmailAddress(username.substring(0, atPos), domain));
               userToAdd.setPassword(newUser.password);
               if (log.isDebugEnabled()) {
                  log.debug("adding " + username + " to user map");
               }
               users.put(userToAdd.getUniqueName(), userToAdd);
               updateUserFile = true;
            } catch (InvalidAddressException e) {
               log.error("User's " + username + " mailbox is malformed. Will not add.");
               iter.remove();
            }
         } else {
            log.error("The user " + username + " already exists and was not added.");
            iter.remove();
         }
         //The executor may have issued a shutdownNow command. Always check the thread
         if (thread.isInterrupted()) {
            return;
         }
      }
      if (log.isDebugEnabled()) {
         log.debug("Added " + newUsers.size() + " new users to user map");
      }
      
      //Add new realms if neccessery
      for (NewUser newUser : newUsers) {
         String username = newUser.username;
         int atPos = username.indexOf('@');
         //Add to realms (creating one if neccessery)
         if (newUser.realm != null && newUser.realm.length() > 0) {
            
            //Already checked that the domain exists
            Domain domain = new Domain(username.substring(atPos+1));
            domain = domains.get(domains.indexOf(domain));
            
            username = username.substring(0, atPos +1 ) + domain.getDomainName();
            String fullRealmName = newUser.realm;
            atPos = fullRealmName.indexOf('@');
            String collection;
            Realm realm;
            if (atPos == -1) {
               
               if (fullRealmName.toLowerCase(ConfigurationManager.LOCALE).equals(domain.getUniqueName())) {
                  log.warn("A One-To-One domain to realm Map need not be specified. Ignoring...");
                  continue;
               }
               collection = fullRealmName;
               realm = new Realm(collection, domain);
            }
            else {
               collection = fullRealmName.substring(0, atPos+1);
               realm = new Realm(collection, domain);
            }
            if (!realms.containsKey(realm.getUniqueName())) {
               if (log.isDebugEnabled()) {
                  log.debug("adding realm " + fullRealmName + " to realm map");
               }
               realms.put(realm.getUniqueName(), ((ConfigurationManagerBackEndFile)cm.getBackEnd()).loadRealm(collection, domain, username));
            } else {
               realm = realms.get(realm.getUniqueName());
               UserFile userToAdd = new UserFile(EmailAddress.getEmailAddress(username.substring(0, atPos), domain));
               if (!realm.containsUser(userToAdd)) {
                  if (log.isDebugEnabled()) {
                     log.debug("adding new user " + username + " to realm " + realm.getFullRealmName());
                  }
                  realm.addUser(userToAdd);
               }
            }
         }
         //The executor may have issued a shutdownNow command. Always check the thread
         if (thread.isInterrupted()) {
            return;
         }
      }
      if (log.isDebugEnabled()) {
         log.debug("Completed adding new realms (if any) to realm map");
      }

      if (updateUserFile) {
         synchronized (cm.getUpdateLock()) {
            cm.setUpdatingFiles(true);
            Properties realmProperties = new Properties();
            StringBuilder userSB;
            Iterator<String> iter3 = realms.keySet().iterator();
            Iterator<User> iter2;
            String userList;
            while (iter3.hasNext()) {
               userSB = new StringBuilder(30);
               Realm realm = realms.get(iter3.next());
               iter2 = realm.userIterator();
               while (iter2.hasNext()) {
                  userSB.append(iter2.next().getUserAdress()).append(',');
               }
               userList = userSB.toString();
               if (userList.length() > 0) {
                  userList = userList.substring(0, userList.length() - 1);
                  if (log.isDebugEnabled()) {
                     log.debug("setting entry " + userList + " for realm " + realm.getFullRealmName());
                  }
                  realmProperties.put("realm." + realm.getFullRealmName(), userList);
               }
            }

            cm.setDisableNotify(true);

            UserFile aUser;
            Properties userProperties = new Properties();
            iter3 = users.keySet().iterator();
            while (iter3.hasNext()) {
               aUser = users.get(iter3.next());
               if (log.isDebugEnabled()) {
                  log.debug("adding user " + aUser.getUserAdress() + " for persistence");
               }
               userProperties.put("user." + aUser.getUserAdress(), aUser.getEncryptedPassword());
            }

            //Last chance to abort in case of a shutdown
            if (Mail.getInstance().isShuttingDown() || thread.isInterrupted()) {
               return;
            }

            try {
               JESProperties.store(realmProperties, ((ConfigurationManagerBackEndFile)cm.getBackEnd()).getRealmsConfigurationFile(), ConfigurationManager.REALMS_PROPERTIES_HEADER);
               log.info("Changes to realms.conf persisted to disk.");
            } catch (IOException e) {
               restore();
               log.error("Unable to store changes to realms.conf!");
               return;
            }

            try {
               JESProperties.store(userProperties, ((ConfigurationManagerBackEndFile)cm.getBackEnd()).getUserConfigurationFile(), ConfigurationManager.USER_PROPERTIES_HEADER);
               log.info("Changes to user.conf persisted to disk.");
            } catch (IOException e) {
               restore();
               log.error("Unable to store changes to user.conf!  Plain text passwords were not hashed!");
            }
            cm.setUpdatingFiles(false);
            cm.getUpdateLock().notifyAll();
         }
      }
      this.users = null;
      this.realms = null;
      this.cm = null;
   }

   private void restore() {
      try {
         ((ConfigurationManagerBackEndFile)cm.getBackEnd()).restore(cm.getBackupDirectory());
      } catch (IOException e) {
      }
   }
   @Override public void deleteUser(Set<Integer> userIds){}
   
   @Override public void setUserPassword(List<NewUser> users){}

   @Override public void addForwardAddress(List<NewUser> forwardAddresses){}
   @Override public void removeForwardAddress(List<NewUser> forwardAddresses){}

   @Override public void setDefaultMailBox(Map<Integer, Integer> defaultMailBoxes){}

   @Override public void insertRealm(List<NewRealm> newRealms){}
   @Override public void removeRealm(Set<Integer> realmIds){}

   @Override public void addUserToRealm(List<NewUser> users){}
   @Override public void removeUserFromRealm(List<NewUser> users){}

}
