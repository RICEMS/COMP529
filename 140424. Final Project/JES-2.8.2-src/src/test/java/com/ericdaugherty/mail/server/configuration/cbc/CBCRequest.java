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

package com.ericdaugherty.mail.server.configuration.cbc;

import com.ericdaugherty.mail.server.JSON.JSONArray;
import com.ericdaugherty.mail.server.JSON.JSONException;
import com.ericdaugherty.mail.server.JSON.JSONObject;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class CBCRequest {
   
   public static final String DOMAIN = "domain:";
   public static final String DOMAIN_ID = "domainId:";
   public static final String USER_ID = "userId:";
   public static final String USERNAME = "username:";
   public static final String PASSWORD = "password:";
   public static final String REALM = "realm:";
   public static final String REALM_ID = "realmId:";
   public static final String FORWARD_ADDRESS = "forwardAddress:";
   public static final String FORWARD_ADDRESS_ID = "forwardAddressId:";
   
   public static final String COMMAND_INSERT_DOMAIN = "insertDomain";
   public static final String COMMAND_DELETE_DOMAIN = "deleteDomain";
   public static final String COMMAND_SET_DEFAULT_DOMAIN = "setDefaultDomain";
   public static final String COMMAND_INSERT_USER = "insertUser";
   public static final String COMMAND_DELETE_USER = "deleteUser";
   public static final String COMMAND_SET_USER_PASSWORD = "setUserPassword";
   public static final String COMMAND_SET_DEFAULT_MAILBOX = "setDefaultMailBox";
   public static final String COMMAND_ADD_FORWARD_ADDRESS = "addForwardAddress";
   public static final String COMMAND_REMOVE_FORWARD_ADDRESS = "removeForwardAddress";
   public static final String COMMAND_INSERT_REALM = "insertRealm";
   public static final String COMMAND_DELETE_REALM = "deleteRealm";
   public static final String COMMAND_ADD_USER_TO_REALM = "addUserToRealm";
   public static final String COMMAND_REMOVE_USER_FROM_REALM = "removeUserFromRealm";

   public static List<String> addDomain(String domain) {

      List<String> domains = new ArrayList<String>(1);
      domains.add(domain);
      return addDomains(domains);
   }
   
   public static List<String> addDomains(List<String> domains) {

      List<String> payload = new ArrayList<String>(3);
      payload.add(COMMAND_INSERT_DOMAIN);
      JSONArray domainArray = new JSONArray();
      for (String domain : domains)
         domainArray.put(domain);
      payload.add(DOMAIN + domainArray.toString());
      payload.add(".");
      
      return payload;
   }
   
   public static List<String> deleteDomain(int domainId) {

      List<Integer> domainIds = new ArrayList<Integer>(1);
      domainIds.add(domainId);
      return deleteDomains(domainIds);
   }
   
   public static List<String> deleteDomains(List<Integer> domainIds) {

      List<String> payload = new ArrayList<String>(3);
      payload.add(COMMAND_DELETE_DOMAIN);
      JSONArray domainIdArray = new JSONArray();
      for (int domainId : domainIds)
         domainIdArray.put(domainId);
      payload.add(DOMAIN_ID + domainIdArray.toString());
      payload.add(".");
      
      return payload;
   }
   
   public static List<String> setDefaultDomain(int domainId) {

      List<String> payload = new ArrayList<String>(3);
      payload.add(COMMAND_SET_DEFAULT_DOMAIN);
      payload.add(DOMAIN_ID + domainId);
      payload.add(".");
      
      return payload;
   }
   
   public static class CBCUser {
      
      private final String username;
      private final char[] password;
      private final int domainId;
      private final List<String> realmsToAdd;

      public CBCUser(@NonNull String username, @NonNull char[] password, @NonNull int domainId, @NonNull List<String> realmsToAdd) {
         this.username = username;
         this.password = password.clone();
         this.domainId = domainId;
         this.realmsToAdd = Collections.unmodifiableList(realmsToAdd);
      }

      public String getUsername() {
         return username;
      }

      public char[] getPassword() {
         return password.clone();
      }

      public int getDomainId() {
         return domainId;
      }

      public List<String> getRealmsToAdd() {
         return realmsToAdd;
      }
   }
   
   public static List<String> addUser(CBCUser user) {

      List<CBCUser> users = new ArrayList<CBCUser>(1);
      users.add(user);
      return addUsers(users);
   }
   
   public static List<String> addUsers(List<CBCUser> users) {
      
      List<String> payload = new ArrayList<String>(2 + users.size());
      payload.add(COMMAND_INSERT_USER);
      for (CBCUser user : users) {
         try {
            JSONObject jsonUser = new JSONObject(new LinkedHashMap());
            jsonUser.put(USERNAME, user.getUsername());
            jsonUser.put(PASSWORD, new String(user.getPassword()));
            jsonUser.put(DOMAIN_ID, user.getDomainId());
            if (!user.getRealmsToAdd().isEmpty()) {
               JSONArray realms = new JSONArray();
               for (String realm : user.getRealmsToAdd()) {
                  realms.put(realm);
               }
               jsonUser.put(REALM, realms);
            }
            payload.add(jsonUser.toString());
         } catch (JSONException je) {
            je.printStackTrace(System.err);
         }
      }
      payload.add(".");
      
      return payload;
   }
   
   public static List<String> deleteUser(int userId) {

      List<Integer> userIds = new ArrayList<Integer>(1);
      userIds.add(userId);
      return deleteUsers(userIds);
   }
   
   public static List<String> deleteUsers(List<Integer> userIds) {

      List<String> payload = new ArrayList<String>(3);
      payload.add(COMMAND_DELETE_USER);
      JSONArray userIdArray = new JSONArray();
      for (int userId : userIds)
         userIdArray.put(userId);
      payload.add(USER_ID + userIdArray.toString());
      payload.add(".");
      
      return payload;
   }

   public static List<String> setUserPassword(int userId, char[] password) {

      List<Integer> userIds = new ArrayList<Integer>(1);
      List<char[]> passwords = new ArrayList<char[]>(1);
      userIds.add(userId);
      passwords.add(password);
      return setUserPassword(userIds, passwords);
   }

   public static List<String> setUserPassword(List<Integer> userIds, List<char[]> passwords) {

      List<String> payload = new ArrayList<String>(4);
      payload.add(COMMAND_SET_USER_PASSWORD);
      JSONArray userIdArray = new JSONArray();
      for (Integer userId : userIds)
         userIdArray.put(userId);
      payload.add(USER_ID + userIdArray.toString());
      JSONArray passwordArray = new JSONArray();
      for (char[] password : passwords) {
         passwordArray.put(new String(password));
      }
      payload.add(PASSWORD + passwordArray.toString());
      payload.add(".");
      
      return payload;
   }

   public static List<String> setDefaultMailbox(int domainId, int userId){

      List<Integer> domainIds = new ArrayList<Integer>(1);
      List<Integer> userIds = new ArrayList<Integer>(1);
      domainIds.add(domainId);
      userIds.add(userId);
      return setDefaultMailboxes(domainIds, userIds);
   }

   public static List<String> setDefaultMailboxes(List<Integer> domainIds, List<Integer> userIds){

      List<String> payload = new ArrayList<String>(4);
      payload.add(COMMAND_SET_DEFAULT_MAILBOX);
      JSONArray domainIdArray = new JSONArray();
      for (Integer userId : domainIds)
         domainIdArray.put(userId);
      payload.add(DOMAIN_ID + domainIdArray.toString());
      JSONArray userIdArray = new JSONArray();
      for (int userId : userIds) {
         userIdArray.put(userId);
      }
      payload.add(USER_ID + userIdArray.toString());
      payload.add(".");
      
      return payload;
   }

   public static List<String> addForwardAddress(int userId, List<String> forwardAddresses){

      List<Integer> domainsIds = new ArrayList<Integer>(1);
      List<List<String>> forwardAddressList = new ArrayList<List<String>>(1);
      domainsIds.add(userId);
      forwardAddressList.add(forwardAddresses);
      return addForwardAddresses(domainsIds, forwardAddressList);
   }

   public static List<String> addForwardAddresses(List<Integer> userIds, List<List<String>> forwardAddressList){

      List<String> payload = new ArrayList<String>(4);
      payload.add(COMMAND_ADD_FORWARD_ADDRESS);
      JSONArray users = new JSONArray();
      for (Integer userId : userIds)
         users.put(userId);
      payload.add(USER_ID + users.toString());
      JSONArray forwardAddressesArray = new JSONArray();
      for (List<String> forwardAddresses : forwardAddressList) {
         JSONArray forwardAddressArray = new JSONArray();
         for (String forwardAddress : forwardAddresses) {
            forwardAddressArray.put(forwardAddress);
         }
         forwardAddressesArray.put(forwardAddressArray);
      }
      payload.add(FORWARD_ADDRESS + forwardAddressesArray.toString());
      payload.add(".");
      
      return payload;
   }

   public static List<String> removeForwardAddress(int userId, List<Integer> forwardAddressIds){

      List<Integer> userIds = new ArrayList<Integer>(1);
      List<List<Integer>> forwardAddressList = new ArrayList<List<Integer>>(1);
      userIds.add(userId);
      forwardAddressList.add(forwardAddressIds);
      return removeForwardAddresses(userIds, forwardAddressList);
   }

   public static List<String> removeForwardAddresses(List<Integer> userIds, List<List<Integer>> forwardAddressList){

      List<String> payload = new ArrayList<String>(4);
      payload.add(COMMAND_REMOVE_FORWARD_ADDRESS);
      JSONArray users = new JSONArray();
      for (Integer userId : userIds)
         users.put(userId);
      payload.add(USER_ID + users.toString());
      JSONArray forwardAddressesArray = new JSONArray();
      for (List<Integer> forwardAddresses : forwardAddressList) {
         JSONArray forwardAddressArray = new JSONArray();
         for (Integer forwardAddress : forwardAddresses) {
            forwardAddressArray.put(forwardAddress);
         }
         forwardAddressesArray.put(forwardAddressArray);
      }
      payload.add(FORWARD_ADDRESS_ID + forwardAddressesArray.toString());
      payload.add(".");
      
      return payload;
   }

   public static List<String> addRealm(int domainId, List<String> realmNames){

      List<Integer> domainsIds = new ArrayList<Integer>(1);
      List<List<String>> realmNamesList = new ArrayList<List<String>>(1);
      domainsIds.add(domainId);
      realmNamesList.add(realmNames);
      return addRealms(domainsIds, realmNamesList);
   }

   public static List<String> addRealms(List<Integer> domainIds, List<List<String>> realmNamesList){

      List<String> payload = new ArrayList<String>(4);
      payload.add(COMMAND_INSERT_REALM);
      JSONArray domains = new JSONArray();
      for (Integer domainId : domainIds)
         domains.put(domainId);
      payload.add(DOMAIN_ID + domains.toString());
      JSONArray realmNamesArray = new JSONArray();
      for (List<String> realmNames : realmNamesList) {
         JSONArray realmNameArray = new JSONArray();
         for (String realmName : realmNames) {
            realmNameArray.put(realmName);
         }
         realmNamesArray.put(realmNameArray);
      }
      payload.add(REALM + realmNamesArray.toString());
      payload.add(".");
      
      return payload;
   }
   
   public static List<String> deleteRealm(int realmId) {

      List<Integer> realmIds = new ArrayList<Integer>(1);
      realmIds.add(realmId);
      return deleteRealms(realmIds);
   }
   
   public static List<String> deleteRealms(List<Integer> realmIds) {

      List<String> payload = new ArrayList<String>(3);
      payload.add(COMMAND_DELETE_REALM);
      JSONArray realmIdArray = new JSONArray();
      for (int realmId : realmIds)
         realmIdArray.put(realmId);
      payload.add(REALM_ID + realmIdArray.toString());
      payload.add(".");
      
      return payload;
   }

   public static class CBCRealmUser extends CBCUser {
      
      private final int userId;
      
      public CBCRealmUser(@NonNull int userId, @NonNull String username, @NonNull char[] password,
            @NonNull int domainId, @NonNull List<String> realmsToAdd) {
         super(username, password, domainId, realmsToAdd);
         this.userId = userId;
      }

      public int getUserId() {
         return userId;
      }
   }
   
   public static List<String> addUserToRealms(CBCRealmUser realmUser){
      
      List<CBCRealmUser> realmUsers = new ArrayList<CBCRealmUser>(1);
      realmUsers.add(realmUser);
      return addUsersToRealms(realmUsers);
   }
   
   public static List<String> addUsersToRealms(List<CBCRealmUser> realmUsers){

      List<String> payload = new ArrayList<String>(2 + realmUsers.size());
      payload.add(COMMAND_ADD_USER_TO_REALM);
      for (CBCRealmUser realmUser : realmUsers) {
         try {
            JSONObject user = new JSONObject();
            user.put(USERNAME, realmUser.getUsername());
            user.put(USER_ID, realmUser.getUserId());
            user.put(DOMAIN_ID, realmUser.getDomainId());
            user.put(PASSWORD, new String(realmUser.getPassword()));
            JSONArray realms = new JSONArray();
            for (String realm : realmUser.getRealmsToAdd()) {
               realms.put(realm);
            }
            user.put(REALM, realms);
            payload.add(user.toString());
         } catch (JSONException je) {
            je.printStackTrace(System.err);
         }
      }
      payload.add(".");

      return payload;
   }

   public static List<String> removeUserFromRealms(int userId, List<Integer> realmIds){

      List<Integer> userIds = new ArrayList<Integer>(1);
      List<List<Integer>> realmIdList = new ArrayList<List<Integer>>(1);
      userIds.add(userId);
      realmIdList.add(realmIds);
      return removeUsersFromRealms(userIds, realmIdList);
   }

   public static List<String> removeUsersFromRealms(List<Integer> userIds, List<List<Integer>> realmIdList){

      List<String> payload = new ArrayList<String>(4);
      payload.add(COMMAND_REMOVE_USER_FROM_REALM);
      JSONArray users = new JSONArray();
      for (Integer userId : userIds)
         users.put(userId);
      payload.add(USER_ID + users.toString());
      JSONArray realmIdsArray = new JSONArray();
      for (List<Integer> realmsIds : realmIdList) {
         JSONArray realmIdArray = new JSONArray();
         for (Integer realmId : realmsIds) {
            realmIdArray.put(realmId);
         }
         realmIdsArray.put(realmIdArray);
      }
      payload.add(REALM_ID + realmIdsArray.toString());
      payload.add(".");
      
      return payload;
   }
}
