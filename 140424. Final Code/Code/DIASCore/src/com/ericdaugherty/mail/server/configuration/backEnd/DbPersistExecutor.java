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
 * $Rev: 361 $
 * $Date: 2014-04-06 16:51:16 +0200 (Sun, 06 Apr 2014) $
 *
 ******************************************************************************/

package com.ericdaugherty.mail.server.configuration.backEnd;

import com.ericdaugherty.mail.server.configuration.BackEndCreationResponse;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.ConfigurationManagerBackEndDb;
import com.ericdaugherty.mail.server.configuration.MailServicesControl;
import com.ericdaugherty.mail.server.configuration.PasswordFactory;
import com.ericdaugherty.mail.server.configuration.cbc.NewRealm;
import com.ericdaugherty.mail.server.configuration.cbc.NewUser;
import com.ericdaugherty.mail.server.dbAccess.ExecuteProcessAbstractImpl;
import com.ericdaugherty.mail.server.dbAccess.ProcessEnvelope;
import com.ericdaugherty.mail.server.errors.UserCreationException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.info.db.DomainDb;
import com.ericdaugherty.mail.server.info.db.RealmDb;
import com.ericdaugherty.mail.server.info.db.UserDb;
import com.ericdaugherty.mail.server.persistence.localDelivery.LocalDeliveryFactory;
import com.ericdaugherty.mail.server.persistence.localDelivery.LocalDeliveryProcessor;
import com.ericdaugherty.mail.server.services.DeliveryService;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.internal.Domain;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class DbPersistExecutor implements PersistExecutor {

   /** Logger */
   private static final Log log = LogFactory.getLog(DbPersistExecutor.class);
   
   private final ConfigurationManager cm = ConfigurationManager.getInstance();
   private final ConfigurationManagerBackEndDb cmDB = (ConfigurationManagerBackEndDb) cm.getBackEnd();
   private final Connection connection;
   private final Properties sqlCommands;
   
   public DbPersistExecutor(@NonNull Connection connection, @NonNull Properties sqlCommands) {
      this.connection = connection;
      this.sqlCommands = sqlCommands;
   }
   
   private Set<String> createDomainDirectories(Set<String> domainAsDirSet) throws SQLException {
      Set<String> createdDomainDirectories = new LinkedHashSet<String>();
      for (String domain : domainAsDirSet) {
         BackEndCreationResponse response = cm.createDomainDirectory(domain);
         if (response == BackEndCreationResponse.CREATED) {
            createdDomainDirectories.add(domain);
         } else if (response != BackEndCreationResponse.EXISTS) {
            String message;
            if (response == BackEndCreationResponse.INVALID) {
               message = domain + " is an invalid domain name.";
            } else if (response == BackEndCreationResponse.NOT_CREATED) {
               message = "Namespace could not be assigned to " + domain + ".";
            } else {
               throw new UnsupportedOperationException(response + " not expected as BackEndDomainCreationResponse.");
            }
            revertDomainDirectories(createdDomainDirectories);
            throw new SQLException(message);
         }
      }
      return createdDomainDirectories;
   }
   
   private void revertDomainDirectories(Set<String> createdDirectories) {
      for (String domain : createdDirectories) {
         cm.revertDomainDirectory(domain);
      }
   }
   
   private Set<String> createUserDirectories(Set<UserDb> users) throws SQLException {
      Set<String> createdUserDirectories = new LinkedHashSet<String>();
      LocalDeliveryFactory ldf = LocalDeliveryFactory.getInstance();
      for (UserDb user : users) {
         LocalDeliveryProcessor ldp = ldf.getLocalDeliveryProccessor(user);
         String userRepository = ldp.getUserRepository();
         try {
            BackEndCreationResponse response = ldp.createUserRepository(userRepository);
            if (response == BackEndCreationResponse.CREATED)
               createdUserDirectories.add(userRepository);
         } catch (UserCreationException ex) {
            revertUserDirectories(createdUserDirectories);
            throw new SQLException("Namespace could not be assigned to " + user.getUserAdress() + ".");
         }
      }
      return createdUserDirectories;
   }
   
   private void revertUserDirectories(Set<String> createdDirectories) {
      for (String userRepository : createdDirectories) {
         cm.revertUserDirectory(userRepository);
      }
   }

   @Override
   public void insertDomain(final List<String> domains) throws PersistException {

      try {
         Set<DomainDb> existingDomains = cmDB.getDomains();
         if (!existingDomains.isEmpty()) {
            for(Iterator<String> iter = domains.iterator(); iter.hasNext();) {
               String domain = iter.next();
               if (existingDomains.contains(new DomainDb(domain, -1)))
                  iter.remove();
            }
         }

         connection.setAutoCommit(false);
         
         final Set<String> domainAsDirSet = new LinkedHashSet<String>();
         final Set<DomainDb> domainAsRealmSet = new LinkedHashSet<DomainDb>();

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domain.add"), Statement.RETURN_GENERATED_KEYS);
               for (String aDomain : domains) {
                  Domain domain = new Domain(aDomain);
                  psImpl.setString(1, domain.getDomainName());
                  psImpl.setString(2, domain.getUniqueName());
                  try {
                     psImpl.executeUpdate();
                  } catch (SQLException sqle) {
                     
                     throw sqle;
                  }
                  rsImpl = psImpl.getGeneratedKeys();
                  if (rsImpl.next()) {
                     domainAsDirSet.add(domain.getUniqueName());
                     domainAsRealmSet.add(new DomainDb(domain.getUniqueName(), rsImpl.getInt(1)));
                     rsImpl.close();
                  } else {
                     throw new SQLException("Need to have a domain id generated.");
                  }
               }
            }
         });

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.add"));
               for (DomainDb domain : domainAsRealmSet) {
                  psImpl.setString(1, domain.getDomainName());
                  psImpl.setString(2, domain.getUniqueName());
                  psImpl.setInt(3, domain.getDomainId());
                  psImpl.executeUpdate();
               }
            }
         });
         boolean fileIOMode27 = !cm.isLegacyFileIOMode();
         if (fileIOMode27) {
            Set<String> createdDomainDirectories = createDomainDirectories(domainAsDirSet);
            try {
               connection.commit();
            } catch (SQLException sqle) {
               revertDomainDirectories(createdDomainDirectories);
               throw sqle;
            }
         } else {
            connection.commit();
         }
         cmDB.updateDomains();
      } catch (SQLException sqle) {
         log.error("", sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle.getLocalizedMessage());
      }
   }

   @Override
   public void deleteDomain(final Set<Integer> domainIds) throws PersistException {

      try {

         connection.setAutoCommit(false);

         //Perhaps the default domain is getting deleted. Need to get the default id 
         //and update the jes_misc table if necessary
         DomainDb defaultDomain = (DomainDb)cmDB.getDefaultDomain();
         final int defaultDomainId = defaultDomain== null ? -1 : defaultDomain.getDomainId();

         boolean defaultDomainDeleted = (Boolean) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public Object executeProcessReturnObject() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domain.delete"));
               boolean defaultDomainDeleted = false;
               for(Integer domainId : domainIds) {
                  defaultDomainDeleted |= defaultDomainId == domainId;
                  psImpl.setInt(1, domainId);
                  psImpl.executeUpdate();
               }
               return defaultDomainDeleted;
            }
         });

         if (defaultDomainDeleted) {
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

               @Override
               public void executeProcessReturnNull() throws SQLException {
                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domain.setDefaultDomainId"));
                  psImpl.setInt(1, -1);
                  psImpl.executeUpdate();
               }
            });
         }
         connection.commit();
         Set<DomainDb> toRemove = new LinkedHashSet<DomainDb>(domainIds.size());
         for(Integer domainId : domainIds) {
            toRemove.add(cmDB.getDomain(domainId));
         }
         DeliveryService.getInstance().removeLocalDomains(toRemove);
         for(DomainDb domain : toRemove) {
            //Caution: SmtpSender is also being taken care of with the following code.
            //TODO Also handle SMTPProcessor?
            MailServicesControl msc = MailServicesControl.getInstance();
            if (msc != null)
               msc.notifyOnDomainDeleted(domain);
         }
         cmDB.acquireExtraLock(Thread.currentThread());
         try {
            cmDB.removeUsersFromCache(domainIds, true);
            Set<Integer> realmIds = new LinkedHashSet<Integer>();
            for (RealmDb realm : cmDB.getRealms()) {
               if (realm.isDomainRealm()) {
                  realmIds.add(realm.getRealmId());
               }
            }
            cmDB.removeRealmsFromCache(realmIds);
            cmDB.updateDomains(true);
         }finally {
            cmDB.releaseExtraLock();
         }
      } catch (SQLException sqle) {
         log.error("", sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle.getLocalizedMessage());
      }
   }

   @Override
   public void setDefaultDomain(final int domainId) throws PersistException {

      try {

         connection.setAutoCommit(false);

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domain.setDefaultDomainId"));
               psImpl.setInt(1, domainId);
               psImpl.executeUpdate();

            }
         });

         connection.commit();
         cmDB.updateDefaultDomain();
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   private static final class RealmDbWithHashedPass {

      private final RealmDb realm;
      private final char[] password;

      public RealmDbWithHashedPass(RealmDb realm, char[] password) {
         this.realm = realm;
         this.password = password;
      }
   }

   @Override
   public void insertUser(final List<NewUser> newUsers) throws PersistException {

      try {

         connection.setAutoCommit(false);
         
         //Need to encrypt all the passwords
         final Map<String, char[]> pass = new HashMap<String, char[]>();
         final Map<String, List<RealmDbWithHashedPass>> realmPass = new HashMap<String, List<RealmDbWithHashedPass>>();

         for (NewUser user : newUsers) {
            DomainDb domain = cmDB.getDomain(user.domainId);
            if (domain == null)
               throw new SQLException("The requested domain is not available. Cannot create user: " + user.username);
            pass.put(user.username, cm.encryptUserPassword(user.password));
            realmPass.put(user.username, new ArrayList<RealmDbWithHashedPass>());
            realmPass.get(user.username).add(new RealmDbWithHashedPass(RealmDb.getNullRealm(), PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(user.username, RealmDb.getNullRealm(), user.password)));
            RealmDb realm = cmDB.getRealm(domain.getUniqueName());
            if (realm == null)
               throw new SQLException("The requested realm " + domain.getDomainName() + " is not available. Cannot create user: " + user.username);
            realmPass.get(user.username).add(new RealmDbWithHashedPass(realm, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(user.username, realm, user.password)));            
            if (user.realms != null) {
               for (String realmName : user.realms) {
                  realm = cmDB.getRealm(realmName);
                  if (realm == null) {
                     throw new SQLException("The requested realm "+realmName+" is not available. Cannot create user: " + user.username);
                  }
                  realmPass.get(user.username).add(new RealmDbWithHashedPass(realm, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(user.username, realm, user.password)));
               }
               user.realms = null;
            }
         }

         final Set<UserDb> users = new LinkedHashSet<UserDb>();
         //Insert the users
         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("user.add"), Statement.RETURN_GENERATED_KEYS);
               
               DomainDb domain = null;
               
               for (NewUser user : newUsers) {
                  psImpl.setString(1, user.username);
                  psImpl.setString(2, user.username.toLowerCase(ConfigurationManager.LOCALE));
                  if (domain == null || (domain.getDomainId() != user.domainId))
                     domain = cmDB.getDomain(user.domainId);
                  psImpl.setInt(3, user.domainId);
                  char[] password = pass.get(user.username);
                  psImpl.setCharacterStream(4, new java.io.CharArrayReader(password), password.length);
                  psImpl.executeUpdate();
                  rsImpl = psImpl.getGeneratedKeys();
                  if (rsImpl.next()) {
                     user.userId = rsImpl.getInt(1);
                     users.add(new UserDb(EmailAddress.getEmailAddress(user.username, domain), user.userId));
                     rsImpl.close();
                  } else {
                     throw new SQLException("Need to have a user id generated.");
                  }
               }
            }
         });

         //Update the realm db entries
         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.addUser"));
               Iterator<NewUser> iter = newUsers.iterator();
               NewUser user;
               List<RealmDbWithHashedPass> list;
               RealmDbWithHashedPass rwep;
               RealmDb realm;
               while (iter.hasNext()) {
                  user = iter.next();
                  list = realmPass.get(user.username);
                  if (list != null) {
                     Iterator<RealmDbWithHashedPass> iter1 = list.iterator();
                     while (iter1.hasNext()) {
                        rwep = iter1.next();
                        realm = rwep.realm;
                        psImpl.setInt(1, realm.getRealmId());
                        psImpl.setInt(2, user.userId);
                        psImpl.setInt(3, user.domainId);
                        psImpl.setCharacterStream(4, new java.io.CharArrayReader(rwep.password), rwep.password.length);
                        psImpl.executeUpdate();
                     }
                  }
               }
            }
         });
         
         Set<String> createdUserDirectories = createUserDirectories(users);
         try {
            connection.commit();
         } catch (SQLException sqle) {
            revertUserDirectories(createdUserDirectories);
            throw sqle;
         }
      } catch (GeneralSecurityException e) {

         log.error(e.getMessage(), e);
         IOUtils.rollback(connection);
         throw new PersistException(e.getLocalizedMessage());
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   @Override
   public void deleteUser(final Set<Integer> userIds) throws PersistException {

      try {

         connection.setAutoCommit(false);

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("user.delete"));
               Iterator<Integer> iter = userIds.iterator();
               int userId;
               while (iter.hasNext()) {
                  userId = iter.next();
                  psImpl.setInt(1, userId);
                  psImpl.executeUpdate();
               }

            }
         });

         connection.commit();
         cmDB.acquireExtraLock(Thread.currentThread());
         try {
            cmDB.removeUsersFromCache(userIds, false);
            cmDB.checkIfDefaultMailboxDeleted(userIds);
         }
         finally {
            cmDB.releaseExtraLock();
         }
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   private static class JESRealmUser {

      private final int userId, domainId;
      private final String uniqueLocalPart, uniqueDomainName;
      private final RealmDb realm;
      private final char[] password;

      public JESRealmUser(String uniqueLocalPart, int userId, int domainId, String uniqueDomainName, char[] password, RealmDb realm) {
         this.uniqueLocalPart = uniqueLocalPart;
         this.userId = userId;
         this.domainId = domainId;
         this.uniqueDomainName = uniqueDomainName;
         this.password = password;
         this.realm = realm;
      }
   }

   @Override
   public void setUserPassword(final List<NewUser> users) throws PersistException {

      try {

         final Set<Integer> usersToRemoveFromCache = new LinkedHashSet<Integer>();
         connection.setAutoCommit(false);

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("user.updatePassword"));
               Iterator<NewUser> iter = users.iterator();
               NewUser user;
               char[] password;
               while (iter.hasNext()) {
                  user = iter.next();
                  password = cm.encryptUserPassword(user.password);
                  psImpl.setCharacterStream(1, new java.io.CharArrayReader(password), password.length);
                  psImpl.setInt(2, user.userId);
                  psImpl.executeUpdate();
                  usersToRemoveFromCache.add(user.userId);
               }

            }
         });

         List<JESRealmUser> list = (List<JESRealmUser>) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public Object executeProcessReturnObject() throws SQLException {

               List<JESRealmUser> list = new ArrayList<JESRealmUser>(users.size() + 10);
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.user.load"));
               Iterator<NewUser> iter = users.iterator();
               NewUser user;
               while (iter.hasNext()) {
                  user = iter.next();
                  psImpl.setInt(1, user.userId);
                  rsImpl = psImpl.executeQuery();
                  while (rsImpl.next()) {
                     list.add(new JESRealmUser(rsImpl.getString("username_lower_case"), user.userId, rsImpl.getInt("domain_id"), rsImpl.getString("domain_lower_case"), user.password, cmDB.getRealm(rsImpl.getString("realm_name_lower_case"))));
                  }
               }
               return list;
            }
         });

         final List<JESRealmUser> encrypted = new ArrayList<JESRealmUser>((int)(list.size()/0.75));
         Iterator<JESRealmUser> iter = list.iterator();
         JESRealmUser jesRealmUser;
         while (iter.hasNext()) {
            jesRealmUser = iter.next();
            
            encrypted.add(new JESRealmUser(null, jesRealmUser.userId, jesRealmUser.domainId, jesRealmUser.uniqueDomainName, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(jesRealmUser.uniqueLocalPart, jesRealmUser.realm, jesRealmUser.password), null));
         }

         //Update the realm passwords
         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {

               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.user.update"));
               Iterator<JESRealmUser> iter = encrypted.iterator();
               JESRealmUser jesRealmUser;
               while (iter.hasNext()) {
                  jesRealmUser = iter.next();
                  psImpl.setCharacterStream(1, new java.io.CharArrayReader(jesRealmUser.password), jesRealmUser.password.length);
                  psImpl.setInt(2, jesRealmUser.realm.getRealmId());
                  psImpl.setInt(3, jesRealmUser.userId);
                  psImpl.setInt(4, jesRealmUser.domainId);
                  psImpl.executeUpdate();
               }
            }
         });

         connection.commit();
         cmDB.acquireExtraLock(Thread.currentThread());
         try {
            cmDB.removeUsersFromCache(usersToRemoveFromCache, false);
         }
         finally{
            cmDB.releaseExtraLock();
         }
      } catch (GeneralSecurityException e) {

         log.error("", e);
         IOUtils.rollback(connection);
         throw new PersistException(e.getLocalizedMessage());
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   @Override
   public void addForwardAddress(final List<NewUser> forwardAddresses) throws PersistException {

      try {

         final Set<Integer> usersToRemoveFromCache = new LinkedHashSet<Integer>();
         connection.setAutoCommit(false);

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {

               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("userForwardAddresses.add"));
               Iterator<NewUser> iter = forwardAddresses.iterator();
               Iterator<String> iter2;
               NewUser newUser;
               while (iter.hasNext()) {
                  newUser = iter.next();
                  psImpl.setInt(1, newUser.userId);
                  iter2 = newUser.forwardAddresses.iterator();
                  while (iter2.hasNext()) {
                     psImpl.setString(2, iter2.next());
                     psImpl.executeUpdate();
                  }
                  usersToRemoveFromCache.add(newUser.userId);
               }
            }
         });

         connection.commit();
         cmDB.acquireExtraLock(Thread.currentThread());
         try {
            cmDB.removeUsersFromCache(usersToRemoveFromCache, false);
         }
         finally{
            cmDB.releaseExtraLock();
         }
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   @Override
   public void removeForwardAddress(final List<NewUser> forwardAddresses) throws PersistException {

      try {

         final Set<Integer> usersToRemoveFromCache = new LinkedHashSet<Integer>();
         connection.setAutoCommit(false);

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {

               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("userForwardAddresses.delete"));
               Iterator<NewUser> iter = forwardAddresses.iterator();
               Iterator<Integer> iter2;
               NewUser newUser;
               while (iter.hasNext()) {
                  newUser = iter.next();
                  iter2 = newUser.forwardAddressIds.iterator();
                  while (iter2.hasNext()) {
                     psImpl.setInt(1, iter2.next());
                     psImpl.executeUpdate();
                  }
                  usersToRemoveFromCache.add(newUser.userId);
               }
            }
         });

         connection.commit();
         cmDB.acquireExtraLock(Thread.currentThread());
         try {
            cmDB.removeUsersFromCache(usersToRemoveFromCache, false);
         }
         finally{
            cmDB.releaseExtraLock();
         }
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   @Override
   public void setDefaultMailBox(final Map<Integer, Integer> defaultMailBoxes) throws PersistException {

      try {

         connection.setAutoCommit(false);
     
         for (final Map.Entry<Integer, Integer> defaultMailBox : defaultMailBoxes.entrySet()) {
      
            final EmailAddress defaultMailbox = cmDB.getDefaultMailbox(defaultMailBox.getKey());

            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

               @Override
               public void executeProcessReturnNull() throws SQLException {
                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty(defaultMailbox.isNULL() ? "domain.setDefaultMailbox" : "domain.updateDefaultMailbox"));
                  psImpl.setInt(1, defaultMailBox.getValue());
                  psImpl.setInt(2, defaultMailBox.getKey());
                  psImpl.executeUpdate();
               }
            });
         }
         
         connection.commit();
         cmDB.updateDomains();
         
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   @Override
   public void insertRealm(final List<NewRealm> newRealms) throws PersistException {

      try {

         connection.setAutoCommit(false);

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.add"), Statement.RETURN_GENERATED_KEYS);
               Iterator<NewRealm> iter = newRealms.iterator();
               NewRealm newRealm;
               String realm;
               Iterator<String> iter2;
               while(iter.hasNext()) {
                  newRealm = iter.next();
                  psImpl.setInt(3, newRealm.domainId);
                  iter2 = newRealm.realms.iterator();
                  while(iter2.hasNext()) {
                     realm = iter2.next();
                     psImpl.setString(1, realm);
                     psImpl.setString(2, realm);
                     psImpl.executeUpdate();
                     rsImpl = psImpl.getGeneratedKeys();
                     if (rsImpl.next()) {
                        int id = rsImpl.getInt(1);
                     } else {
                        throw new SQLException("Need to have a realm id generated.");
                     }
                  }
               }
            }
         });

         connection.commit();
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   @Override
   public void removeRealm(final Set<Integer> realmIds) throws PersistException {

      try {

         connection.setAutoCommit(false);

         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.remove"));
               Iterator<Integer> iter = realmIds.iterator();
               int realmId;
               while(iter.hasNext()) {
                  realmId = iter.next();
                  psImpl.setInt(1, realmId);
                  psImpl.executeUpdate();
               }
            }
         });

         connection.commit();
         cmDB.acquireExtraLock(Thread.currentThread());
         try {
            cmDB.removeRealmsFromCache(realmIds);
         }
         finally{
            cmDB.releaseExtraLock();
         }
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   @Override
   public void addUserToRealm(final List<NewUser> users) throws PersistException {

      try {

         connection.setAutoCommit(false);

         final Set<Integer> usersToRemoveFromCache = new LinkedHashSet<Integer>();
         for (final NewUser user : users) {
            //A new password is passed, need to update the user's password as well
            final char[] password;
            final List<RealmDbWithHashedPass> realmPassToUpdate = new ArrayList<RealmDbWithHashedPass>();
            final List<RealmDbWithHashedPass> realmPassToAdd = new ArrayList<RealmDbWithHashedPass>();

            final DomainDb domain = cmDB.getDomain(user.domainId);
            if (domain == null)
               throw new SQLException("The requested domain is not available. Can not create user: "+user.username);
            password = cm.encryptUserPassword(user.password);
            realmPassToUpdate.add(new RealmDbWithHashedPass(RealmDb.getNullRealm(), PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(user.username, RealmDb.getNullRealm(), user.password)));
            final RealmDb domainRealm = cmDB.getRealm(domain.getUniqueName());
            if (domainRealm == null)
               throw new SQLException("The requested realm " + domain.getDomainName()
                     + " is not available. Can not add user: " + user.username);
            realmPassToUpdate.add(new RealmDbWithHashedPass(domainRealm, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(user.username, domainRealm, user.password)));
            
            final NewUser finalUser = user;
            final Set<String> uniqueRealmNames = new LinkedHashSet<String>();
            new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

               @Override
               public void executeProcessReturnNull() throws SQLException {

                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.load.nonStandard"));
                  psImpl.setInt(1, finalUser.userId);
                  psImpl.setString(2, domainRealm.getUniqueName());
                  psImpl.setString(3, RealmDb.getNullRealm().getUniqueName());
                  rsImpl = psImpl.executeQuery();
                  while (rsImpl.next()) {
                     //Always use the realm_name_lower_case as a key to the realms Map
                     //That way the old means of specifing uniqueness (lower casing the
                     //entire realm name as opposed to only the domain part) is still
                     //functional
                     uniqueRealmNames.add(rsImpl.getString("realm_name_lower_case"));
                  }
               }
            });
            for(String uniqueRealmName : uniqueRealmNames) {
               RealmDb realm = cmDB.getRealm(uniqueRealmName);
               realmPassToUpdate.add(new RealmDbWithHashedPass(realm, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(user.username, realm, user.password)));
            }
            
            if (user.realms != null) {
               for (String realmName : user.realms) {
                  RealmDb realmToAdd = cmDB.getRealm(realmName);
                  if (realmToAdd == null) {
                     throw new SQLException("The requested realm " + realmName + " is not available. Can not add user: " + user.username);
                  }
                  realmPassToAdd.add(new RealmDbWithHashedPass(realmToAdd, PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(user.username, realmToAdd, user.password)));
               }
               user.realms = null;
            }

            //update the user
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

               @Override
               public void executeProcessReturnNull() throws SQLException {
                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("user.updatePassword"));
                  psImpl.setCharacterStream(1, new java.io.CharArrayReader(password), password.length);
                  psImpl.setInt(2, user.userId);
                  psImpl.executeUpdate();

               }
            });

            //Update the realm_users db entries
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

               @Override
               public void executeProcessReturnNull() throws SQLException {
                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.user.update"));
                  RealmDbWithHashedPass rwep;
                  RealmDb realm;
                  Iterator<RealmDbWithHashedPass> iter = realmPassToUpdate.iterator();
                  while (iter.hasNext()) {
                     rwep = iter.next();
                     realm = (RealmDb) rwep.realm;
                     psImpl.setCharacterStream(1, new java.io.CharArrayReader(rwep.password), rwep.password.length);
                     psImpl.setInt(2, realm.getRealmId());
                     psImpl.setInt(3, user.userId);
                     psImpl.setInt(4, user.domainId);
                     psImpl.executeUpdate();
                  }
               }
            });
            
            //Add new realm_users db entries
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

               @Override
               public void executeProcessReturnNull() throws SQLException {
                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.addUser"));
                  RealmDbWithHashedPass rwep;
                  RealmDb realm;
                  Iterator<RealmDbWithHashedPass> iter = realmPassToAdd.iterator();
                  while (iter.hasNext()) {
                     rwep = iter.next();
                     realm = (RealmDb) rwep.realm;
                     psImpl.setInt(1, realm.getRealmId());
                     psImpl.setInt(2, user.userId);
                     psImpl.setInt(3, user.domainId);
                     psImpl.setCharacterStream(4, new java.io.CharArrayReader(rwep.password), rwep.password.length);
                     psImpl.executeUpdate();
                  }
               }
            });
            
            usersToRemoveFromCache.add(user.userId);
         }

         connection.commit();
         cmDB.acquireExtraLock(Thread.currentThread());
         try {
            cmDB.removeUsersFromCache(usersToRemoveFromCache, false);
         }
         finally {
            cmDB.releaseExtraLock();
         }
      } catch (GeneralSecurityException e) {

         log.error("", e);
         IOUtils.rollback(connection);
         throw new PersistException(e.getLocalizedMessage());
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }

   @Override
   public void removeUserFromRealm(final List<NewUser> users) throws PersistException {

      try {
         final Set<Integer> usersToRemoveFromCache = new LinkedHashSet<Integer>();
         connection.setAutoCommit(false);

         final List<Integer> removeFromNullRealm = (List<Integer>)new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

            @Override
            public Object executeProcessReturnObject() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.removeUser"));
               
               List<Integer> removeFromNullRealm = new ArrayList<Integer>();
               int defaultRealmId = 0;
               for(NewUser user : users) {
                  usersToRemoveFromCache.add(user.userId);
                  psImpl.setInt(1, user.userId);
                  for(int realmId : user.realmIds) {
                     if (realmId == defaultRealmId) {
                        removeFromNullRealm.add(user.userId);
                        continue;
                     }
                     psImpl.setInt(2, realmId);
                     psImpl.executeUpdate();
                  }
               }
               return removeFromNullRealm;
            }
         });

         if (!removeFromNullRealm.isEmpty()) {
            new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(connection, false, false, true, true) {

               @Override
               public void executeProcessReturnNull() throws SQLException {
                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.removeUserFromNullRealm"));
                  for(int realmId : removeFromNullRealm) {
                     psImpl.setInt(1, realmId);
                     psImpl.executeUpdate();
                  }
               }
            });
         }

         connection.commit();
         cmDB.acquireExtraLock(Thread.currentThread());
         try {
            cmDB.removeUsersFromCache(usersToRemoveFromCache, false);
         }
         finally{
            cmDB.releaseExtraLock();
         }
      } catch (SQLException sqle) {

         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(connection);
         throw new PersistException(sqle);
      }
   }
}
