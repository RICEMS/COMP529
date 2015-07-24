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

package com.ericdaugherty.mail.server.configuration;

import com.ericdaugherty.mail.server.Mail;
import static com.ericdaugherty.mail.server.configuration.Utils.SHA_C;
import com.ericdaugherty.mail.server.configuration.backEnd.DbPersistExecutor;
import com.ericdaugherty.mail.server.configuration.backEnd.PersistException;
import com.ericdaugherty.mail.server.configuration.cbc.CBCExecutor;
import com.ericdaugherty.mail.server.configuration.cbc.CBCResponseException;
import com.ericdaugherty.mail.server.dbAccess.ExecuteProcessAbstractImpl;
import com.ericdaugherty.mail.server.dbAccess.ProcessEnvelope;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.info.Realm;
import com.ericdaugherty.mail.server.info.db.DomainDb;
import com.ericdaugherty.mail.server.info.db.RealmDb;
import com.ericdaugherty.mail.server.info.db.UserDb;
import com.ericdaugherty.mail.server.services.DeliveryService;
import com.ericdaugherty.mail.server.utils.DelimitedInputStream;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.ericdaugherty.mail.server.utils.JESProperties;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.drda.NetworkServerControl;
import org.w3c.dom.Element;

/**
 *
 * @author Andreas Kyrmegalos
 */
public final class ConfigurationManagerBackEndDb implements ConfigurationManagerBackEnd, ConfigurationParameterConstants {

   /** Logger */
   private static final Log log = LogFactory.getLog(ConfigurationManagerBackEndDb.class);
   private ConfigurationManager cm;
   private final Properties sqlCommands;
   private BasicDataSource connectionPool;
   private char[] username;
   private char[] password;
   /** A Map of Users keyed by their uniqueUsername String */
   private ConcurrentMap<String, UserDb> users;
   /** A Map of Realms keyed by their uniqueRealmName String */
   private ConcurrentMap<String, RealmDb> realms;
   private Set<RealmDb> realmsForResponse;
   private final int minimum, maximum;
   private volatile int userCount;
   /**
    * Array of domains with the default usernameDb that the SMTP server
    * should accept mail for local delivery
    */
   private ConcurrentMap<DomainDb, UserDb> localDomainsWithDefaultMailbox;
   private DomainDb defaultDomain;
   private boolean singleDomainMode;
   private DomainDb singleDomain;
   private NetworkServerControl server;
   /** The default realmId for all domains **/
   private int defaultRealmId = -1;
   
   private final Object cbcLock = new Object();
   
   private boolean interLock = false;
   
   private volatile boolean extraLock = false;
   
   private volatile Thread lockingThread;
   
   public void acquireExtraLock(Thread thread) {
      extraLock = true;
      this.lockingThread = thread;
   }
   
   public void releaseExtraLock() {
      extraLock = false;
      this.lockingThread = null;
      synchronized(cbcLock) {
         cbcLock.notifyAll();
      }
   }
   
   private volatile boolean shuttingDown;
   
   public static final class RetainServer {
      
      private final NetworkServerControl server;
      
      private RetainServer(NetworkServerControl server) {
         this.server = server;
      }
      
      private NetworkServerControl getServer() {
         return server;
      }
   }
   
   RetainServer getServer() {
      return new RetainServer(server);
   }

   private Connection getConnection() throws SQLException {
      return connectionPool.getConnection();
   }

   private void initSecure() {

      ConfigurationManager.JSSEStore store = cm.getKeystore();
      try {
         System.setProperty("javax.net.ssl.keyStore", store.getLocation());
      } catch (Exception ex) {
         //ignore
      }
      try {
         System.setProperty("javax.net.ssl.keyStorePassword", new String(JESVaultControl.getInstance().getPassword("keystore")));
      } catch (Exception ex) {
         //ignore
      }
      try {
         System.setProperty("javax.net.ssl.keyStoreProvider", store.getProvider());
      } catch (Exception ex) {
         //ignore
      }
      try {
         System.setProperty("javax.net.ssl.keyStoreType", store.getType());
      } catch (Exception ex) {
         //ignore
      }
      
      store = cm.getTruststore();
      String truststoreLocation = store.getLocation();
      if (new File(truststoreLocation).exists()) {
         try {
            System.setProperty("javax.net.ssl.trustStore", truststoreLocation);
         } catch (Exception ex) {
         }
         try {
            System.setProperty("javax.net.ssl.trustStoreProvider", store.getProvider());
         } catch (Exception ex) {
         }
         try {
            System.setProperty("javax.net.ssl.trustStoreType", store.getType());
         } catch (Exception ex) {
         }
      }
   }

   ConfigurationManagerBackEndDb(ConfigurationManager cm,
         final char[] usernameDb, final char[] passwordDb,
         final int minimum, final int maximum) {

      this.cm = cm;
      this.username = usernameDb;
      this.password = passwordDb;
      this.minimum = minimum;
      this.maximum = maximum;
      
      localDomainsWithDefaultMailbox = new ConcurrentHashMap<DomainDb, UserDb>((int)((maximum+minimum)/2));
      sqlCommands = new Properties();

      if (!Mail.getInstance().isRestarting() && ConfigurationManager.getInstance().isBackEndSecure()) {
         initSecure();
      }

      initDbServer(usernameDb, passwordDb);
      if (shuttingDown) {
         return;
      }

      InputStream is = getClass().getResourceAsStream("/dbCommands.properties");
      try {
         sqlCommands.load(is);
      } catch (IOException ex) {
         log.error(ex);
         throw new RuntimeException("Failed to load the sql command set. Aborting startup...");
      } finally {
         IOUtils.close(is);
      }

      users = new java.util.concurrent.ConcurrentHashMap<String, UserDb>((int) (minimum / 0.75));
      realms = new java.util.concurrent.ConcurrentHashMap<String, RealmDb>((int) (minimum / 0.75));

      Connection conn = null;
      ResultSet rs = null;
      try {

         conn = getConnection();

         //The database has been created, need to verify that the tables exist
         rs = conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"});

         boolean tablesExist = rs.next();
         rs.close();

         List<String> updateCommands = new ArrayList<String>();
         boolean hasVersionColumn = false;
         String versionNumber = null;
         if (tablesExist) {
            Statement st = null;
            try {
               st = conn.createStatement();
               rs = st.executeQuery("SELECT * FROM jes_misc");
               ResultSetMetaData rsmd = rs.getMetaData();
               int columnCount = rsmd.getColumnCount();
               do {
                  if (rsmd.getColumnName(columnCount).toLowerCase(ConfigurationManager.LOCALE).equals("version")) {
                     hasVersionColumn = true;
                     break;
                  }
               } while (--columnCount > 0);
               if (rs.next()) {
                  if (hasVersionColumn) {
                     versionNumber = rs.getString(columnCount);
                  }
                  rs.close();
                  st.close();
               } else {
                  rs.close();
                  st.close();

                  st = conn.createStatement();
                  try {
                     rs = st.executeQuery("SELECT * FROM jes_users");
                     rsmd = rs.getMetaData();
                     columnCount = rsmd.getColumnCount();
                     String columnName;
                     boolean hasPassword25Column = false;
                     do {
                        columnName = rsmd.getColumnName(columnCount).toLowerCase(ConfigurationManager.LOCALE);
                        if (columnName.equals("password_25")) {
                           hasPassword25Column = true;
                        } else if (columnName.equals("password") || columnName.equals("salt")) {
                           if (rsmd.isNullable(columnCount) == ResultSetMetaData.columnNoNulls) {
                              updateCommands.add("jes_users.modifyColumn." + columnName);
                           }
                        }
                     } while (--columnCount > 0);
                     if (!hasPassword25Column) {
                        updateCommands.add("jes_users.addColumn.password_25");
                     }
                  } finally {
                     rs.close();
                     st.close();
                  }
               }
            } finally {
               if (st != null) {
                  try {
                     st.close();
                  } catch (SQLException ex) {
                  }
               }
            }
         }

         boolean hasCurrentVersion = hasVersionColumn && "2.8".equals(versionNumber);

         if (tablesExist) {
            if (!hasVersionColumn) {
               updateCommands.add("jes_misc.addColumn.version");
            }
            if (!hasCurrentVersion) {
               updateCommands.add("jes_misc.updateVersion");
            }
            boolean updateRealms28 = !hasVersionColumn || ("2.5".equals(versionNumber) || "2.6".equals(versionNumber));

            if (!updateCommands.isEmpty() || updateRealms28) {

               if (updateRealms28) {
                  updateCommands.add("jes_realms.dropConstraint.realm_name_domain_id_uk");
                  updateCommands.add("jes_realms.dropConstraint.anonymousUnique");
                  updateCommands.add("jes_realms.addConstraint.realm_name_lower_case_domain_id_uk");
               }
               
               Properties commands = loadProperties(getClass().getResourceAsStream("/updateJESDbTables28.properties"));
               conn.setAutoCommit(false);
               String replace = null;
               boolean tryReplace = true;
               for (int i = 0; i < updateCommands.size(); i++) {

                  String key = updateCommands.get(i);
                  String command = commands.getProperty(key);
                  if (command == null) {
                     continue;
                  }
                  if ("jes_realms.dropConstraint.anonymousUnique".equals(key)) {
                     if (replace == null) {
                        if (tryReplace){
                           tryReplace = false;
                           command = commands.getProperty("jes_realms.getConstraintName.anonymousUnique");
                           command = command.substring(0, command.length() - 1);
                           replace = executeQuery(conn, command);
                           i--;
                           continue;
                        } else {
                           throw new SQLException("Could not drop anonymous unique key for table jes_realm_users (for versions < 2.8).");
                        }
                     } else {
                        command = command.replaceFirst("REPLACE", replace);
                     }
                  }
                  command = command.substring(0, command.length() - 1);
                  if (log.isDebugEnabled()) {
                     log.debug("adding/updating table with command " + command);
                  }
                  executeStatement(conn, command);
               }
               conn.commit();
            }
         } else {
            String line;

            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/createJESDbTables.properties")));
            try {
               conn.setAutoCommit(false);

               line = br.readLine();
               if (line != null) {
                  
                  String command = line.substring(0, line.length() - 1);
                  if (log.isDebugEnabled())
                     log.debug("adding/updating table with command " + command);
                  executeStatement(conn, command);

                  while ((line = br.readLine()) != null && line.length() > 0) {

                     command = line.substring(0, line.length() - 1);
                     if (log.isDebugEnabled()) {
                        log.debug("adding/updating table with command " + command);
                     }
                     executeStatement(conn, command);
                  }

                  conn.commit();
               }
            } catch (IOException ioe) {
               throw new RuntimeException("Could not read from resource /createJESDbTables.properties.", ioe);
            } finally {
               IOUtils.close(br);
            }
         }

         //Set the security permissions
         CallableStatement cs = null;
         try {
            cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.sqlAuthorization','true')");
            cs.execute();
         } finally {
            if (cs != null) {
               cs.close();
            }
         }
         conn.commit();
         IOUtils.close(conn);
         connectionPool.close();

         try {
            initConnectionPool(usernameDb, passwordDb, "shutdown=true;" + (ConfigurationManager.getInstance().isBackEndSecure() ? "ssl=peerAuthentication;" : ""));
            conn = getConnection();
         } catch (SQLException e) {
            if (!((SQLException) e.getCause()).getSQLState().equals("08006")) {
               throw e;
            }
         } finally {
            IOUtils.close(conn);
            connectionPool.close();
            initConnectionPool(usernameDb, passwordDb, ConfigurationManager.getInstance().isBackEndSecure() ? "ssl=peerAuthentication" : "");
         }

         conn = getConnection();

         conn.setAutoCommit(false);

         String guiUser = new String(JESVaultControl.getInstance().getPassword(GUI_DB_USERNAME));
         guiUser = guiUser.substring(0, guiUser.indexOf("\u0000"));
         executeStatement(conn, "GRANT SELECT,REFERENCES ON TABLE jes_domains TO " + guiUser);
         executeStatement(conn, "GRANT SELECT(user_id,username,username_lower_case,domain_id),REFERENCES(user_id,username,username_lower_case,domain_id) ON TABLE jes_users TO " + guiUser);
         executeStatement(conn, "GRANT SELECT,REFERENCES ON TABLE jes_default_mailbox TO " + guiUser);
         executeStatement(conn, "GRANT SELECT,REFERENCES ON TABLE jes_user_forwards TO " + guiUser);
         executeStatement(conn, "GRANT SELECT,REFERENCES ON TABLE jes_realms TO " + guiUser);
         executeStatement(conn, "GRANT SELECT(realm_id,user_id,domain_id),REFERENCES(realm_id,user_id,domain_id) ON TABLE jes_realm_users TO " + guiUser);
         executeStatement(conn, "GRANT SELECT,REFERENCES ON TABLE jes_misc TO " + guiUser);
         conn.commit();

      } catch (SQLException sqle) {
         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(conn);
         throw new RuntimeException("Failed to start the db backend. Aborting startup...", sqle);
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException ex) {
            }
         }
         IOUtils.close(conn);
      }
   }
   
   private void executeStatement(Connection conn, final String sql) throws SQLException {
      Statement st = null;
      try {
         st = conn.createStatement();
         st.execute(sql);
      } finally {
         if (st != null) {
            try {
               st.close();
            } catch (SQLException sqle) {
               //nothing to see here, move along
            }
         }
      }
   }
   
   private <T> T executeQuery(Connection conn, final String sql) throws SQLException {
      Statement st = null;
      ResultSet rs = null;
      try {
         st = conn.createStatement();
         rs = st.executeQuery(sql);
         if (!rs.next())
            return null;
         return (T)rs.getObject(1);
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException sqle) {
               //nothing to see here, move along
            }
         }
         if (st != null) {
            try {
               st.close();
            } catch (SQLException sqle) {
               //nothing to see here, move along
            }
         }
      }
   }

   private Properties loadProperties(InputStream inputStream) {

      DelimitedInputStream dis = null;
      try {
         dis = new DelimitedInputStream(inputStream, 2048, true);
         JESProperties jesProperties = new JESProperties(dis);
         jesProperties.load();
         return jesProperties.getProperties();
      } catch (IOException ioe) {
         // All checks should be done before we get here, so there better
         // not be any errors.  If so, throw a RuntimeException.
         throw new RuntimeException("Error Loading Db update command properties!  Unable to continue Operation.");
      } finally {
         IOUtils.close(dis);
      }
   }

   private void initDbServer(final char[] usernameDb, final char[] passwordDb) {

      RetainServer rs = Mail.getInstance().getRetainServer();
      if (rs != null) {
         server = rs.getServer();
      }
      
      if (server == null) {
         //Start the Derby Network Server
         new Thread(new Runnable() {

            @Override
            public void run() {
               try {

                  String host = System.getProperty("derby.drda.host");
                  if (host.equalsIgnoreCase("localhost")) {
                     host = null;
                  }
                  int port;
                  try {
                     port = Integer.valueOf(System.getProperty("derby.drda.portNumber"));
                  } catch (NumberFormatException nfe) {
                     log.warn("Supplied Db port number " + System.getProperty("derby.drda.portNumber") + " is not a number.");
                     port = 1527;
                  }
                  server = new NetworkServerControl(InetAddress.getByName(host), port, new String(usernameDb), new String(passwordDb));
                  server.start(null);
               } catch (Exception ex) {
                  log.error("", ex);
                  throw new RuntimeException("Failed to start the database.");
               }
            }
         }).start();

         int inc = 1;
         do {
            try {
               //Increased the base time to accomodate apparent changes to
               //derby with version 10+
               Thread.sleep((long) Math.floor(Math.sqrt(inc)) * 500L);
               server.ping();
               break;
            } catch (InterruptedException e) {
               if (shuttingDown) {
                  return;
               }
            } catch (Exception e) {
               log.error("", e);
               if (e.getClass().equals(Exception.class)) {
                  //allow time for the server to start
                  log.warn("Derby Db not yet started. Waiting...");
               }
               else {
                  if (e instanceof CertificateException) {
                     log.warn("Check the mail.xml general.security.certificateStore.truststore entry. The location may be erronously specified.");
                  }
                  throw new RuntimeException(e.getLocalizedMessage());
               }
            }
         } while (++inc <= 10);
         if (inc == 11) {
            throw new RuntimeException("Timed out waiting for db server to start.");
         }
      }
      initConnectionPool(usernameDb, passwordDb, "create=true;" + (ConfigurationManager.getInstance().isBackEndSecure() ? "ssl=peerAuthentication;" : ""));
   }

   private void initConnectionPool(final char[] usernameDb, final char[] passwordDb, String connectionProperties) {

      connectionPool = new BasicDataSource();
      connectionPool.setDriverClassName("org.apache.derby.jdbc.ClientConnectionPoolDataSource");
      connectionPool.setUsername(new String(usernameDb));
      connectionPool.setPassword(new String(passwordDb));
      connectionPool.setUrl("jdbc:derby:JES");
      connectionPool.setConnectionProperties(connectionProperties);
      connectionPool.setInitialSize(2);
      connectionPool.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
   }
   
   @Override
   public void init(Element element) {
      
      Connection conn = null;
      try {
         conn = getConnection();

         conn.setAutoCommit(false);

         acquireExtraLock(null);
         try {
            updateDomains(conn, false);

            getDefaultRealmId(conn);
         }
         finally {
            releaseExtraLock();
         }

         conn.commit();
      } catch (SQLException sqle) {
         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(conn);
         throw new RuntimeException("Failed to initialize the db backend. Aborting startup...", sqle);
      } finally {
         IOUtils.close(conn);
      }
   }

   @Override
   public void shutdown() {

      shuttingDown = true;
      
      //It looks ugly, but this process to shut down the database actually works.
      //passing a shutdown connection parameter, simply does not work.
      try {
         if (connectionPool != null) {
            connectionPool.close();
         }
      } catch (SQLException sqle) {
         if (log.isDebugEnabled())
            log.debug(sqle.getMessage(), sqle);
      } finally {
         connectionPool = null;
      }
      initConnectionPool(username, password, "shutdown=true;" + (ConfigurationManager.getInstance().isBackEndSecure() ? "ssl=peerAuthentication;" : ""));
      try {
         getConnection().close();
      } catch (SQLException sqle) {
         if (log.isDebugEnabled()) {
            log.debug("This is purposeful. Disregard safely.\r\n\t\t\t\t"+sqle.getLocalizedMessage());
         }
      }
      try {
         if (connectionPool != null) {
            connectionPool.close();
         }
      } catch (SQLException sqle) {
         if (log.isDebugEnabled()) {
            log.debug(sqle.getMessage(), sqle);
         }
      } finally {
         connectionPool = null;
      }
      //Does not shutting down the server prevent the jvm from exiting normally?
      //if (true) return;
      if (!Mail.getInstance().isRestart()) {
         try {

            server.shutdown();
         } catch (Exception ex) {
            log.error(ex);
         } finally {
            server = null;
         }
      }
   }

   private void getDefaultRealmId(Connection conn) throws SQLException {

      //No need to synchronize
      
      defaultRealmId = (Integer) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

         @Override
         public Object executeProcessReturnObject() throws SQLException {
            psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.load.default"));
            rsImpl = psImpl.executeQuery();
            int defaultRealmId = -1;
            if (rsImpl.next()) {
               defaultRealmId = rsImpl.getInt("realm_id");
            }
            return defaultRealmId;
         }
      });
   }

   private void updateRealmsForResponse(Connection conn) throws SQLException {

      //No need to synchronize. Called only from synchronized code.
      realmsForResponse = (Set<RealmDb>) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

         @Override
         public Object executeProcessReturnObject() throws SQLException {

            psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.load.all"));
            rsImpl = psImpl.executeQuery();
            RealmDb realm;
            String fullRealmName;
            String uniqueRealmName;
            Set<RealmDb> realmsForResponse = new LinkedHashSet<RealmDb>();
            while (rsImpl.next()) {
               fullRealmName = rsImpl.getString("realm_name");
               if (fullRealmName==null||fullRealmName.equals("null")) {
                  continue;
               }
               uniqueRealmName = rsImpl.getString("realm_name_lower_case");
               realm = realms.get(uniqueRealmName);
               if (realm==null) {
                  realm = loadRealm(rsImpl);
               }
               realmsForResponse.add(realm);
            }
            return realmsForResponse;
         }
      });
   }

   /* Not implemented in a db scope */
   @Override
   public void restore(String backupDirectory) throws IOException {
   }

   /* Not implemented in a db scope */
   @Override
   public void doBackup(String backupDirectory) throws IOException {
   }

   /* Not implemented in a db scope */
   @Override
   public void doWeeklyBackup(String backupDirectory) throws IOException {
   }

   /* Not implemented in a db scope */
   @Override
   public void persistUsersAndRealms() {
   }

   @Override
   public List<String> updateThroughConnection(List<CBCExecutor> cbcExecutors) throws PersistException {

      Connection conn = null;
      try {
         conn = getConnection();
         
         DbPersistExecutor dbpe = new DbPersistExecutor(conn, sqlCommands);
         List<String> response = new LinkedList<String>();
         for(CBCExecutor cbcExecutor : cbcExecutors) {
            log.info(cbcExecutor.getClass().getName());
            cbcExecutor.processLines();
            cbcExecutor.execute(dbpe);
            response.add(cbcExecutor.getResponse());
         }
         return response;
      } catch (SQLException sqle) {
         log.error(sqle.getMessage(), sqle);
         throw new PersistException(sqle);
      } catch (CBCResponseException cre) {
         log.error(cre.getMessage(), cre);
         throw new PersistException(cre);
      } finally {
         IOUtils.close(conn);
      }
   }

   /**
    * This should always return false.
    *
    * @return always false
    */
   @Override
   public boolean persistUserUpdate() {
      return false;
   }

   /**
    * username may contain a uniqueDomainName.
    *
    * @param realm
    * @param emailAddress
    * @return 
    **/
   @Override
   public char[] getRealmPassword(Realm realm, EmailAddress emailAddress) {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return null;
                     }
                  }
               }
            }
         }
      }
      
      UserDb user = getUser(emailAddress);
      if (user==null) {
         log.warn("User "+emailAddress.getAddress()+" does not exist. Can not retrieve realm password.");
         return null;
      }
      return user.getRealmPass(realm).clone();
   }

   // Do not invoke getUser or get Realm
   @Override
   public void loadUsersAndRealms() {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return;
                     }
                  }
               }
            }
         }
      }
      
      //Load the minimum number of users
      Connection conn = null;
      try {

         conn = getConnection();
         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("users.load"));
               psImpl.setMaxRows(minimum);
               rsImpl = psImpl.executeQuery();
               String fullUsername;
               String uniqueUsername;
               UserDb user;
               char[] password = null, password25 = null;
               List<Domain> domains = new ArrayList<Domain>(localDomainsWithDefaultMailbox.keySet());
               while (rsImpl.next()) {
                  
                  fullUsername = rsImpl.getString("username") + '@' + rsImpl.getString("domain_name");
                  
                  try {
                     password = getPasswordFromResultSet(rsImpl, "password");
                  }
                  catch (IOException ioe) {
                  }
                  try {
                     password25 = getPasswordFromResultSet(rsImpl, "password_25");
                  }
                  catch (IOException ioe) {
                  }
                  if (password==null&&password25==null) {
                     log.error("Could not load password for user: "+fullUsername+". Skipping...");
                     continue;
                  }

                  //the username is stored sans the domain
                  uniqueUsername = rsImpl.getString("username_lower_case") + '@' + rsImpl.getString("domain_name_lower_case");
                  try {
                     user = loadUser(connImpl, rsImpl.getInt("user_id"), fullUsername, domains, password, rsImpl.getString("salt"), password25);
                     users.put(uniqueUsername, user);
                  } catch (InvalidAddressException iae) {
                     log.error("Unable to load the password for mailbox "+fullUsername+". Skipping.");
                  }
               }
            }
         });

         if (users.isEmpty() && !(Mail.isTesting() || cm.isLocalTestingMode())) {
            log.warn("No users registered!!!");
         }

         //Only load the realms that correspond to the loaded users
         new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

            @Override
            public void executeProcessReturnNull() throws SQLException {

               String fullRealmName;
               String uniqueRealmName;
               UserDb user;
               char[] password;
               RealmDb realm;
               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.load"));
               Iterator<UserDb> iter = users.values().iterator();

               while (iter.hasNext()) {

                  user = iter.next();
                  psImpl.setInt(1, user.getUserId());
                  rsImpl = psImpl.executeQuery();
                  while (rsImpl.next()) {
                     
                     try {
                        password = getPasswordFromResultSet(rsImpl, "password");
                     }
                     catch (IOException ioe) {
                        log.error("Could not load realm password for user: "+user.getUserAdress()+". Skipping...");
                        continue;
                     }
                     fullRealmName = rsImpl.getString("realm_name");
                     uniqueRealmName = rsImpl.getString("realm_name_lower_Case");
                     
                     if (fullRealmName.equals("null")) {
                        user.addRealmPass(RealmDb.getNullRealm(), password);
                     }
                     else {
                        realm = loadRealm(rsImpl);
                        user.addRealmPass(realm, password);
                        realms.put(uniqueRealmName, realm);
                     }
                  }
               }
               log.info("Loaded " + realms.size() + " realms from the database");
            }
         });
      } catch (SQLException sqle) {

         log.error(sqle);
      } finally {
         IOUtils.close(conn);
      }
   }

   /**
    * Creates a new User instance for the specified usernameDb
    * using the specified properties.
    *
    * @param fullAddress full usernameDb (me@mydomain.com)
    * @param properties the properties that contain the user parameters.
    * @return a new User instance.
    * @throws NullPointerException if both password and password25 are null
    */
   private UserDb loadUser(Connection conn, final int userId, String fullAddress, List<Domain> domains, char[] password, String salt, char[] password25) throws InvalidAddressException, SQLException {

      //No need to synchronize
      
      Domain domain = new Domain(fullAddress.substring(fullAddress.indexOf('@')+1));
      domain = domains.get(domains.indexOf(domain));
      EmailAddress address = new EmailAddress(fullAddress.substring(0, fullAddress.indexOf('@')), domain);
      final UserDb user = new UserDb(address, userId);
      // Load the passwordDb
      if (password25!=null&&password25.length > 0) {
         JESVaultControl.getInstance().setUserPassword(user, password25);
      } else {
         char[] toSet = new char[password.length+5];
         System.arraycopy(SHA_C, 0, toSet, 0, 5);
         System.arraycopy(password, 0, toSet, 5, password.length);
         user.setPassword(toSet);
         user.setSalt(salt);
      }

      new ProcessEnvelope().executeNull(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

         @Override
         public void executeProcessReturnNull() throws SQLException {
            psImpl = connImpl.prepareStatement(sqlCommands.getProperty("userForwardAddresses.load"));
            psImpl.setInt(1, user.getUserId());
            rsImpl = psImpl.executeQuery();
            List<EmailAddress> addressList = new ArrayList<EmailAddress>(10);
            String forwardAddress;
            while (rsImpl.next()) {
               forwardAddress = rsImpl.getString("forward_address");
               try {
                  addressList.add(new EmailAddress(forwardAddress));
               } catch (InvalidAddressException e) {
                  log.warn("Forward address: " + forwardAddress + " for user " + user.getUserAdress() + " is invalid and will be ignored.");
               }
            }
            EmailAddress[] emailAddresses = new EmailAddress[addressList.size()];
            emailAddresses = (EmailAddress[]) addressList.toArray(emailAddresses);

            if (log.isDebugEnabled()) {
               log.debug(emailAddresses.length + " forward addresses load for user: " + user.getUserAdress());
            }
            user.setForwardAddresses(emailAddresses);
         }
      });

      return user;
   }

   /**
    * Creates a new RealmDb instance. Users are not added since it
    * is not needed in a db context.
    * 
    * @param rsImpl The ResultSet to use to create the {@link RealmDb}
    * @return an instance of RealmDb
    * @throws SQLException 
    */
   public RealmDb loadRealm(ResultSet rsImpl) throws SQLException{
      
      RealmDb realm;
      String fullRealmName = rsImpl.getString("realm_name");
      int atPos = fullRealmName.indexOf('@');
      if (atPos==-1) {
         realm = new RealmDb(rsImpl.getInt("realm_id"), getDomain(rsImpl.getInt("domain_id")));
      }
      else {  
         realm = new RealmDb(rsImpl.getString("realm_name").substring(0, atPos),
               getDomain(rsImpl.getInt("domain_id")),rsImpl.getInt("realm_id"));
      }
      return realm;
   }

   /* Not implemented in a db scope */
   @Override
   public void updateUsersAndRealmPasswords() {
   }
   
   @Override
   public Set<DomainDb> getDomains() {
      return new LinkedHashSet<DomainDb>(localDomainsWithDefaultMailbox.keySet());
   }

   @Override
   public Set<RealmDb> getRealms() {
      return realmsForResponse;
   }
   
   public void updateDomains() {
      updateDomains(false);
   }

   public void updateDomains(boolean domainsDeleted) {

      Connection conn = null;
      try {

         conn = getConnection();
         conn.setAutoCommit(false);

         updateDomains(conn, domainsDeleted);

         conn.commit();

      } catch (SQLException sqle) {
         log.error(sqle.getMessage(), sqle);
         IOUtils.rollback(conn);
      } finally {
         IOUtils.close(conn);
      }
   }

   private void updateDomains(Connection conn, boolean domainsDeleted) throws SQLException {
      
      interLock = true;
      synchronized(cbcLock) {

         try {
            final int defaultDomainId = (Integer) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

               @Override
               public Object executeProcessReturnObject() throws SQLException {

                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domains.getDefaultDomainId"));
                  rsImpl = psImpl.executeQuery();
                  if (rsImpl.next()) {
                     return rsImpl.getInt(1);
                  }
                  return -1;
               }
            });

            //Load the domains
            Map<DomainDb, UserDb> newDomains = (Map<DomainDb, UserDb>) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

               @Override
               public Object executeProcessReturnObject() throws SQLException {

                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domains.load"));
                  rsImpl = psImpl.executeQuery();
                  String username;
                  DomainDb domain;
                  UserDb user;
                  Map<DomainDb, UserDb> newDomains = new LinkedHashMap<DomainDb, UserDb>();
                  while (rsImpl.next()) {

                     domain = new DomainDb(rsImpl.getString("domain_name"), rsImpl.getInt("domain_id"));
                     username = rsImpl.getString("username");
                     user = null;
                     try {
                        if (username != null) {
                           user = new UserDb(new EmailAddress(username, domain), rsImpl.getInt("user_id"));
                        }
                     } catch (InvalidAddressException iae) {
                        log.error("Invalid username " + username+". Default mailbox not set for domain: "+domain.getDomainName());
                     }
                     newDomains.put(domain, user == null ? new UserDb(new EmailAddress(), -1) : user);
                  }
                  return newDomains;

               }
            });
            if (shuttingDown) {
               interLock = false;
               cbcLock.notifyAll();
               return;
            }

            if (domainsDeleted)
               localDomainsWithDefaultMailbox.clear();
            localDomainsWithDefaultMailbox.putAll(newDomains);
            defaultDomain = null;
            if (defaultDomainId != -1) {
               for(DomainDb domain : localDomainsWithDefaultMailbox.keySet()) {
                  if (domain.getDomainId() == defaultDomainId) {
                     defaultDomain = domain;
                     break;
                  }
               }
            }
            singleDomainMode = newDomains.size() == 1;
            if (singleDomainMode) {
               if (defaultDomain != null) {
                  singleDomain = defaultDomain;
               } else {
                  singleDomain = newDomains.keySet().iterator().next();
               }
            } else {
               singleDomain = null;
            }
            updateRealmsForResponse(conn);
            
            DeliveryService.getInstance().setLocalDomains(localDomainsWithDefaultMailbox.keySet());

            if (localDomainsWithDefaultMailbox.isEmpty()) {
               log.warn("No local domains registered with JES.");
            }
         }
         finally {
            interLock = false;
            if (!extraLock) {
               cbcLock.notifyAll();
            }
         }
      }
   }

   /**
    * Returns the Domain with the supplied id.
    * Can return null. Should return null if the uniqueDomainName
    * does not exist.
    * 
    * @param domainId the id belonging to the Domain to be retrieved
    * @return the {@link Domain} with the supplied id
    */
   public DomainDb getDomain(int domainId) {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null&&Thread.currentThread()!=lockingThread) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return null;
                     }
                  }
               }
            }
         }
      }
      
      Iterator<DomainDb> iter = localDomainsWithDefaultMailbox.keySet().iterator();
      DomainDb domain;
      while (iter.hasNext()) {
         domain = iter.next();
         if (domain.getDomainId() == domainId) {
            return domain;
         }
      }
      return null;
   }

   @Override
   public boolean isLocalDomain(String domain) {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return false;
                     }
                  }
               }
            }
         }
      }
      
      return localDomainsWithDefaultMailbox.containsKey(new DomainDb(domain, -1));
   }
   
   @Override
   public boolean isSingleDomainMode() {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return false;
                     }
                  }
               }
            }
         }
      }
      
      return singleDomainMode;
   }
   
   @Override
   public DomainDb getSingleDomain() {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return null;
                     }
                  }
               }
            }
         }
      }
      
      return singleDomainMode?singleDomain:null;
   }

   @Override
   public DomainDb getDefaultDomain() {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return null;
                     }
                  }
               }
            }
         }
      }
      
      return defaultDomain;
   }
   
   @Override
   public void updateDefaultDomain() {
      
      Connection conn = null;
      try {

         conn = getConnection();
         updateDefaultDomain(conn);

      } catch (SQLException sqle) {
         log.error(sqle);
      } finally {
         IOUtils.close(conn);
      }
   }

   public void updateDefaultDomain(Connection conn) throws SQLException{

      interLock = true;
      
      synchronized(cbcLock) {
         try {
            DomainDb domain = (DomainDb) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

               @Override
               public Object executeProcessReturnObject() throws SQLException {

                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("domains.getDefaultDomain"));
                  rsImpl = psImpl.executeQuery();
                  if (rsImpl.next()) {

                     return new DomainDb(rsImpl.getString("domain_name"), rsImpl.getInt("domain_id"));
                  }
                  return null;

               }
            });
            if (domain!=null) {
               defaultDomain = domain;
            }
         }
         finally {
            interLock = false;
            cbcLock.notifyAll();
         }
      }
   }

   @Override
   public EmailAddress getDefaultMailbox(String domain) {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         synchronized(cbcLock) {
            while(interLock||extraLock) {
               try {
                  cbcLock.wait(150);
               }
               catch (InterruptedException ie) {
                  if (shuttingDown) {
                     return null;
                  }
               }
            }
         }
      }
      
      UserDb user = localDomainsWithDefaultMailbox.get(new DomainDb(domain, -1));
      if (user==null) {
         return null;
      }
      return user.getEmailAddress();
   }

   public EmailAddress getDefaultMailbox(int domainId) {
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         synchronized(cbcLock) {
            while(interLock||extraLock) {
               try {
                  cbcLock.wait(150);
               }
               catch (InterruptedException ie) {
                  if (shuttingDown) {
                     return null;
                  }
               }
            }
         }
      }
      
      Iterator<DomainDb> iter = localDomainsWithDefaultMailbox.keySet().iterator();
      DomainDb domain;
      while (iter.hasNext()) {

         domain = iter.next();
         if (domain.getDomainId() == domainId) {
            return localDomainsWithDefaultMailbox.get(domain).getEmailAddress();
         }
      }
      return null;
   }
   
   private char[] getPasswordFromResultSet(ResultSet rs, String columnName) throws SQLException, IOException{
      
      Reader reader = null;
      try {
         reader = rs.getCharacterStream(columnName);
         if (reader == null)
            return null;
         char[] output = new char[150];
         int pos = 0;
         for(;;) {
            int read = reader.read(output, pos, Math.min(50, output.length-pos));
            if (read==-1) break;
            pos+=read;
            if (output.length-pos==25) {
               char[] temp = new char[output.length+100];
               System.arraycopy(output, 0, temp, 0, pos);
               output = temp;
            }
         }
         char[] temp = new char[pos];
         System.arraycopy(output, 0, temp, 0, pos);
         return temp;
      } finally {
         IOUtils.close(reader);
      }
   }


   @Override
   public UserDb getUser(final EmailAddress address) {

      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return null;
                     }
                  }
               }
            }
         }
      }
      
      if (userCount == maximum) {

         realms.clear();
         users.clear();
         loadUsersAndRealms();
      }
      
      UserDb user = users.get(address.getAddress());

      //Compatibility mode with previous versions
      if (user == null) {
         user = users.get(address.getAddress().toLowerCase(ConfigurationManager.LOCALE));
      }
      if (user == null) {

         Connection conn = null;
         try {

            conn = getConnection();
            user = (UserDb) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

               @Override
               public Object executeProcessReturnObject() throws SQLException {

                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("users.load.single"));
                  psImpl.setString(1, address.getUsername().toLowerCase(cm.LOCALE));
                  psImpl.setString(2, address.getDomain().getUniqueName());
                  rsImpl = psImpl.executeQuery();
                  UserDb user = null;
                  char[] password = null, password25 = null;
                  if (rsImpl.next()) {

                     String fullUsername = rsImpl.getString("username") + '@' + rsImpl.getString("domain_name");
                     try {
                        password = getPasswordFromResultSet(rsImpl, "password");
                     }
                     catch (IOException ioe) {
                     }
                     try {
                        password25 = getPasswordFromResultSet(rsImpl, "password_25");
                     }
                     catch (IOException ioe) {
                     }
                     if (password==null&&password25==null) {
                        log.error("Could not load password for user: "+fullUsername+". Skipping...");
                        return null;
                     }

                     List<Domain> domains = new ArrayList<Domain>(localDomainsWithDefaultMailbox.keySet());
                     try {
                        user = loadUser(connImpl, rsImpl.getInt("user_id"), fullUsername, domains, password, rsImpl.getString("salt"), password25);
                        String uniqueUsername = rsImpl.getString("username_lower_case") + '@' + rsImpl.getString("domain_name_lower_case");
                        users.put(uniqueUsername, user);
                        userCount = users.size();
                     } catch (InvalidAddressException e) {
                        log.error("The supplied mailbox "+fullUsername+" is invalid. Skipping...");
                     }
                     return user;
                  }
                  return null;
               }
            });

            //Load all the realms associated with this user
            if (user != null) {

               final UserDb finalUser = user;

               new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

                  @Override
                  public void executeProcessReturnNull() throws SQLException {

                     String fullRealmName;
                     String uniqueRealmName;
                     char[] password;
                     RealmDb realm;
                     psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.load"));
                     psImpl.setInt(1, finalUser.getUserId());
                     rsImpl = psImpl.executeQuery();
                     while (rsImpl.next()) {
                        
                        try {
                           password = getPasswordFromResultSet(rsImpl, "password");
                        }
                        catch (IOException ioe) {
                           log.error("Could not load realm password for user: "+finalUser.getUserAdress()+". Skipping...");
                           continue;
                        }
                        //Always use the realm_name_lower_case as a key to the realms Map
                        //That way the old means of specifing uniqueness (lower casing the
                        //entire realm name as opposed to only the domain part) is still
                        //functional
                        uniqueRealmName = rsImpl.getString("realm_name_lower_case");
                        if (realms.containsKey(uniqueRealmName)) {
                           finalUser.addRealmPass(realms.get(uniqueRealmName), password);
                        }
                        else {
                           realm = loadRealm(rsImpl);
                           finalUser.addRealmPass(realm, password); 
                           realms.put(uniqueRealmName, realm);
                        }
                     }
                     log.info("Loaded " + realms.size() + " realms from the database");
                  }
               });
            }

         } catch (SQLException sqle) {

            log.error(sqle);
         } finally {
            IOUtils.close(conn);
         }

      }

      return user;
   }

   public void checkIfDefaultMailboxDeleted(Set<Integer> userIds) {

      Iterator<DomainDb> iter = localDomainsWithDefaultMailbox.keySet().iterator();
      UserDb defaultMailBox;
      while (iter.hasNext()) {
         defaultMailBox = (UserDb) localDomainsWithDefaultMailbox.get(iter.next());
         if (userIds.contains(defaultMailBox.getUserId())) {
            updateDomains(true);
            break;
         }
      }
   }

   @Override
   public RealmDb getRealm(final String realmName) {
      return getRealm(realmName, false);
   }

   public RealmDb getRealm(final String realmName, boolean cachedOnly) {

      if (realmName.equals("null"))
         return RealmDb.getNullRealm();
      
      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return null;
                     }
                  }
               }
            }
         }
      }
      
      RealmDb realm = realms.get(realmName);

      //Perhaps it has not been loaded. Load it now
      if (realm == null) {
         
         if (cachedOnly)
            return null;
         
         Connection conn = null;
         try {

            conn = getConnection();
            realm = (RealmDb) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

               @Override
               public Object executeProcessReturnObject() throws SQLException {

                  String uniqueRealmName;
                  psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realms.load.single"));
                  psImpl.setString(1, realmName);
                  rsImpl = psImpl.executeQuery();
                  if (rsImpl.next()) {

                     uniqueRealmName = rsImpl.getString("realm_name_lower_case");
                     
                     RealmDb realm = loadRealm(rsImpl);
                     realms.put(uniqueRealmName, realm);
                     return realm;
                  }
                  return null;
               }
            });


         } catch (SQLException sqle) {

            log.error("", sqle);
         } finally {
            IOUtils.close(conn);
         }
      }

      return realm;
   }

   public boolean isUserARealmMember(final Realm realm, final String uniqueUsername) {

      //System.out.println(extraLock+" "+interLock+" "+Arrays.asList(Thread.currentThread().getStackTrace()));
      if (interLock||extraLock) {
         if (lockingThread!=null) {
            synchronized(cbcLock) {
               while(interLock||extraLock) {
                  try {
                     cbcLock.wait(150);
                  }
                  catch (InterruptedException ie) {
                     if (shuttingDown) {
                        return false;
                     }
                  }
               }
            }
         }
      }
      
      Connection conn;
      try {

         conn = getConnection();
         return (Boolean) new ProcessEnvelope().executeObject(new ExecuteProcessAbstractImpl(conn, false, false, true, true) {

            @Override
            public Object executeProcessReturnObject() throws SQLException {

               psImpl = connImpl.prepareStatement(sqlCommands.getProperty("realm.isUserAMember"));
               psImpl.setInt(1, ((RealmDb) realm).getRealmId());
               psImpl.setString(2, uniqueUsername);
               rsImpl = psImpl.executeQuery();
               if (rsImpl.next()) {
                  return rsImpl.getInt(1) > 0;
               }
               return false;
            }
         });
      } catch (SQLException sqle) {
         log.error(sqle);
         return false;
      }
   }

   public void removeUserFromCache(int userId) {

      Iterator<UserDb> iter = users.values().iterator();
      UserDb user;
      while (iter.hasNext()) {
         user = iter.next();
         if (user.getUserId() == userId) {
            iter.remove();
            break;
         }
      }
   }

   public void removeUsersFromCache(Set<Integer> ids, boolean isDomainList) {

      if (!isDomainList) {
         Iterator<Integer> iter = ids.iterator();
         while (iter.hasNext()) {
            removeUserFromCache(iter.next());
         }
      }
      else {
         Iterator<UserDb> iter = users.values().iterator();
         UserDb user;
         while (iter.hasNext()) {
            user = iter.next();
            if (ids.contains(((DomainDb)user.getEmailAddress().getDomain()).getDomainId())) {
               iter.remove();
               break;
            }
         }
      }
   }
   
   public void removeRealmFromCache(int realmId) {

      Iterator<RealmDb> iter = realms.values().iterator();
      RealmDb realm;
      while (iter.hasNext()) {
         realm = iter.next();
         if (realm.getRealmId() == realmId) {
            realms.remove(realm.getFullRealmName().toLowerCase(ConfigurationManager.LOCALE));
            break;
         }
      }
   }
   
   public void removeRealmsFromCache(Set<Integer> realmIds) {

      Iterator<Integer> iter = realmIds.iterator();
      while (iter.hasNext()) {
         removeRealmFromCache(iter.next());
      }
   }

   public int getDefaultRealmId() {
      return defaultRealmId;
   }
}
