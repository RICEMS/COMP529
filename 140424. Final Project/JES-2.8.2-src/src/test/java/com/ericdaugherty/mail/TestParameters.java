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
 * $Rev$
 * $Date$
 *
 ******************************************************************************/

package com.ericdaugherty.mail;

import com.ericdaugherty.mail.server.configuration.BackEndType;
import java.util.Properties;
import org.w3c.dom.Node;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class TestParameters {
   
   private String name;
   private String realm;
   private int messagesPerUser = 5;
   private int runsPerUser = 1;
   private boolean multithreaded;
   private int smtpPort, pop3Port;
   private String server = "localhost";
   private int cbcPort = 41001;
   private String dbServer = "localhost";
   private int dbPort = 17527;
   private final Properties javaMailSettings = new Properties();
   private Node settingsNode;
   private BackEndType backEndType = BackEndType.FILE;
   private Profile profile;

   public String getName() {
      return name;
   }

   public String getRealm() {
      return realm;
   }

   public int getMessagesPerUser() {
      return messagesPerUser;
   }

   public int getRunsPerUser() {
      return runsPerUser;
   }

   public boolean isMultithreaded() {
      return multithreaded;
   }

   public int getSmtpPort() {
      return smtpPort;
   }

   public int getPop3Port() {
      return pop3Port;
   }

   public String getServer() {
      return server;
   }

   public int getCbcPort() {
      return cbcPort;
   }

   public String getDbServer() {
      return dbServer;
   }

   public int getDbPort() {
      return dbPort;
   }

   public Properties getJavaMailSettings() {
      return javaMailSettings;
   }

   public Node getSettingsNode() {
      return settingsNode;
   }

   public BackEndType getBackEndType() {
      return backEndType;
   }

   void setName(String name) {
      this.name = name;
   }

   void setRealm(String realm) {
      this.realm = realm;
   }

   void setMessagesPerUser(int messagesPerUser) {
      this.messagesPerUser = messagesPerUser;
   }

   void setRunsPerUser(int runsPerUser) {
      this.runsPerUser = runsPerUser;
   }

   void setMultithreaded(boolean multithreaded) {
      this.multithreaded = multithreaded;
   }

   void setSmtpPort(int smtpPort) {
      this.smtpPort = smtpPort;
   }

   void setPop3Port(int pop3Port) {
      this.pop3Port = pop3Port;
   }

   void setServer(String server) {
      this.server = server;
   }

   void setCbcPort(int port) {
      this.cbcPort = port;
   }

   void setDbServer(String dbServer) {
      this.dbServer = dbServer;
   }

   void setDbPort(int dbPort) {
      this.dbPort = dbPort;
   }

   void setSettingsNode(Node settingsNode) {
      this.settingsNode = settingsNode;
   }

   void setBackEndType(BackEndType backEndType) {
      this.backEndType = backEndType;
   }

   public Profile getProfile() {
      return profile;
   }

   public void setProfile(Profile profile) {
      this.profile = profile;
   }

}
