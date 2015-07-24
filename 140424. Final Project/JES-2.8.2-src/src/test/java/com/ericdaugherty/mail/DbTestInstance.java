/******************************************************************************
 * This program is a 100% Java Email Server.
 ******************************************************************************
 * Copyright (c) 2001-2013, Eric Daugherty (http://www.ericdaugherty.com)
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
 * $Rev: $
 * $Date: $
 *
 ******************************************************************************/

package com.ericdaugherty.mail;

import com.ericdaugherty.mail.server.configuration.cbc.CBCRequest;
import com.ericdaugherty.mail.server.utils.IOUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class DbTestInstance extends AbstractTestInstance{
   
   private final String realm;
   private final String dbServer;
   private final int dbPort;
   
   DbTestInstance(TestParameters parameters, Properties userInfo, PasswordAuthenticator senderCredentials) {
      super(parameters, userInfo, senderCredentials);
      
      domainAndPort = server + ":" + parameters.getCbcPort();
      realm = parameters.getRealm();
      dbServer = parameters.getDbServer();
      dbPort = parameters.getDbPort();
   }
   
   @Override
   public void setup() throws Exception {
      super.setup();
      //Domain, user and realm entries in the configuration files are ignored.
      //Need to pass them to jes via the CBC

         if (profile == null) {
         //Domain first
         addDomain(server);
         //Now a realm
         List<String> realms = new ArrayList<String>(2);
         if (realm != null) {
            addRealm(1, realm);
            realms.add(realm);
         }
         //And finally the users
         String username = senderCredentials.getPasswordAuthentication().getUserName();
         if (username.indexOf('@') != -1) {
            username = username.substring(0, username.indexOf('@'));
         }
         addUser(username, senderCredentials.getPasswordAuthentication().getPassword().toCharArray(), 1, realms);
         Iterator iterator = userInfo.keySet().iterator();
         String user;
         while (iterator.hasNext()) {
            user = (String) iterator.next();

            addUser(user, userInfo.getProperty(user).toCharArray(), 1, realms);
         }
      } else if (profile == Profile.CBCONLY) {
         
         addDomain("test1.example.com");
         addDomain("test2.example.com");
         deleteDomain(2);
         setDefaultDomain(1);
         addRealm(1, "test.realm1");
         addRealm(1, "test.realm2");
         addRealm(1, "test.realm3");
         addRealm(1, "test.realm4");
         deleteRealm(6);
         List<String> realms = new ArrayList<String>(2);
         realms.add("test.realm1");
         realms.add("test.realm2");
         addUser("test.user1", "password1".toCharArray(), 1, realms);
         addUser("test.user2", "password2".toCharArray(), 1, realms);
         deleteUser(2);
         setDefaultMailbox(1, 1);
         List<String> forwardAddresses = new ArrayList<String>(2);
         forwardAddresses.add("forward1@another.com");
         forwardAddresses.add("forward2@another.com");
         addForwardAddress(1, forwardAddresses);
         forwardAddresses.clear();
         List<Integer> forwardAddressIds = new ArrayList<Integer>(1);
         forwardAddressIds.add(2);
         removedForwardAddress(1, forwardAddressIds);
         realms.clear();
         realms.add("test.realm3");
         addUserToRealms("test.user1", 1, 1, "password11".toCharArray(), realms);
         List<Integer> realmIds = new ArrayList<Integer>(1);
         realmIds.add(5);
         removeUserFromRealms(1, realmIds);
      }

      //Allow JES to update the database
      try {
         Thread.sleep(5 * 1000);
      } catch (InterruptedException ex) {
      }
   }
   
   @Override
   public void finish(boolean aborted) throws IOException{

      System.setSecurityManager(null);
      
      Connection conn = null;
      Statement st = null;
      try {
         conn = java.sql.DriverManager.getConnection("jdbc:derby://"+dbServer+":"+dbPort+"/JES", "test", "test");
         conn.setAutoCommit(false);
         st = conn.createStatement();
         st.executeUpdate("DELETE FROM jes_realm_users");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("DELETE FROM jes_realms");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("ALTER TABLE jes_realms ALTER COLUMN realm_id RESTART WITH 0");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("insert into jes_realms (realm_name, realm_name_lower_case) VALUES ('null', 'null')");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("DELETE FROM jes_user_forwards");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("DELETE FROM jes_default_mailbox");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("DELETE FROM jes_users");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("DELETE FROM jes_domains");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("ALTER TABLE jes_domains ALTER COLUMN domain_id RESTART WITH 1");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("ALTER TABLE jes_users ALTER COLUMN user_id RESTART WITH 1");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("ALTER TABLE jes_realms ALTER COLUMN realm_id RESTART WITH 1");
         st.close();
         st = conn.createStatement();
         st.executeUpdate("ALTER TABLE jes_user_forwards ALTER COLUMN user_forward_id RESTART WITH 1");
         st.close();
         conn.commit();
      }
      catch (SQLException e) {
         IOUtils.rollback(conn);
      }
      finally {
         if (st!=null) {
            try {
               st.close();
            }
            catch (SQLException sqle) {}
         }
         IOUtils.close(conn);
      }

      if (!aborted) {
         com.ericdaugherty.mail.server.Mail.getInstance().shutdown();

         try {
            Thread.sleep(5 * 1000);
         } catch (InterruptedException ex) {
         }
      }
   }

   public void addDomain(String domain) throws Exception {
      
      String result = transmitData(CBCRequest.addDomain(domain));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_INSERT_DOMAIN + " was unsuccessful");
      }
   }

   public void setDefaultDomain(int domainId) throws Exception {
      
      String result = transmitData(CBCRequest.setDefaultDomain(domainId));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_SET_DEFAULT_DOMAIN + " was unsuccessful");
      }
   }

   public void deleteDomain(int domainId) throws Exception{

      String result = transmitData(CBCRequest.deleteDomain(domainId));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_DELETE_DOMAIN + " was unsuccessful");
      }
   }

   public void addUser(String username, char[] password, int domainId, List<String> realmsToAdd) throws Exception{

      CBCRequest.CBCUser user = new CBCRequest.CBCUser(username, password, domainId, realmsToAdd);
      String result = transmitData(CBCRequest.addUser(user));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_INSERT_USER + " was unsuccessful");
      }
   }

   public void deleteUser(int userId) throws Exception{

      String result = transmitData(CBCRequest.deleteUser(userId));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_DELETE_USER + " was unsuccessful");
      }
   }

   public void setUserPassword(int userId, char[] password) throws Exception{

      String result = transmitData(CBCRequest.setUserPassword(userId, password));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_SET_USER_PASSWORD + " was unsuccessful");
      }
   }

   public void setDefaultMailbox(int domainId, int userId) throws Exception {
      
      String result = transmitData(CBCRequest.setDefaultMailbox(domainId, userId));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_SET_DEFAULT_MAILBOX + " was unsuccessful");
      }
   }

   public void addForwardAddress(int userId, List<String> forwardAddress) throws Exception{
      
      String result = transmitData(CBCRequest.addForwardAddress(userId, forwardAddress));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_ADD_FORWARD_ADDRESS + " was unsuccessful");
      }
   }

   public void removedForwardAddress(int userId, List<Integer> forwardAddress) throws Exception{
      
      String result = transmitData(CBCRequest.removeForwardAddress(userId, forwardAddress));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_REMOVE_FORWARD_ADDRESS + " was unsuccessful");
      }
   }

   public void addRealm(int domainId, String realmName) throws Exception{
      
      List<String> realmNames = new ArrayList<String>(1);
      realmNames.add(realmName);
      String result = transmitData(CBCRequest.addRealm(domainId, realmNames));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_INSERT_REALM + " was unsuccessful");
      }
   }

   public void deleteRealm(int realmId) throws Exception{

      String result = transmitData(CBCRequest.deleteRealm(realmId));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_DELETE_REALM + " was unsuccessful");
      }
   }

   public void addUserToRealms(String username, int userId, int domainId, char[] password, List<String> realms) throws Exception{

      CBCRequest.CBCRealmUser realmUser = new CBCRequest.CBCRealmUser(userId, username, password, domainId, realms);
      String result = transmitData(CBCRequest.addUserToRealms(realmUser));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_ADD_USER_TO_REALM + " was unsuccessful");
      }
   }

   public void removeUserFromRealms(int userId, List<Integer> realmIds) throws Exception{

      String result = transmitData(CBCRequest.removeUserFromRealms(userId, realmIds));
      if (result == null || !result.equals("success")) {
         throw new Exception("The requested operation: " + CBCRequest.COMMAND_REMOVE_USER_FROM_REALM + " was unsuccessful");
      }
   }
   
   private static final String CRLF_STRING = "\r\n";
   private static final Charset UTF_8 = Charset.availableCharsets().get("UTF-8");
   private final String domainAndPort;
   /** Reader to read data from the client */
   private BufferedReader in;
   /** Writer to sent data to the client */
   private PrintWriter out;
   /** Socket connection to the client */
   private Socket socket;

   private void Waitforconnection() throws SocketException {
      int count = 0;
      while (!socket.isConnected()) {
         try {
            Thread.sleep(500);
         } catch (InterruptedException ex) {
            throw new SocketException("Unable to complete the transaction due to internal error.");
         }
         if (count++ > 120) {
            throw new SocketException("No connection established. Please try again later.");
         }
      }
      socket.setSoTimeout(5 * 60 * 1000);
   }

   /**
    * Writes the specified output message to the client.
    */
   private void write(String message) {
      if (message != null) {
         out.print(message + CRLF_STRING);
         out.flush();
      }
   }

   private String transmitData(List<String> lineCommands) throws IOException {

      try {

         String domain = domainAndPort.substring(0, domainAndPort.indexOf(':'));
         if (domain.equalsIgnoreCase("localhost") || domain.equals("127.0.0.1")) {
            domain = null;
         }
         final String finalDomain = domain;

         socket = new Socket(InetAddress.getByName(finalDomain), Integer.valueOf(domainAndPort.substring(domainAndPort.indexOf(':') + 1)));

         Waitforconnection();
         
         out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8)));

         for (String lineCommand : lineCommands) {
            write(lineCommand);
         }
         
         in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
         
         String result = in.readLine();
         return result;
      } finally {
         IOUtils.close(socket);
      }
   }
}
