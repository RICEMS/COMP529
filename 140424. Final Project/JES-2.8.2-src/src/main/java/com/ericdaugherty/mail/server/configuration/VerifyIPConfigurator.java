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

//Java Imports
import com.ericdaugherty.mail.server.utils.IOUtils;
import java.io.*;
import java.util.*;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * As part of the IP verification system this class is used to configure the
 * system for IP evalation and subsequent actions.
 *
 * @author Andreas Kyrmegalos
 */
public class VerifyIPConfigurator {

   /** Logger */
   private static final Log log = LogFactory.getLog( VerifyIPConfigurator.class );

   private static VerifyIPConfigurator vIPC;

   private File dnsBWListsFile;

   private DnsSubList whiteList, mixedList, blackList;

   private boolean strictUsed, blockingIP, amavisRelayed;

   private VerifyIPConfigurator() {}

   private boolean init(String mailDirectory) {
      
      dnsBWListsFile = new File(mailDirectory, "conf"+File.separator+"dnsBWLists.conf");
      if (!dnsBWListsFile.exists()) {
         log.error("VerifyIP configuration file not found! "+dnsBWListsFile.getPath());
         return false;
      }
      Properties properties = new Properties();

      FileInputStream is = null;
      try {
         is = new FileInputStream(dnsBWListsFile);
         properties.load(is);
      }
      catch (IOException ioe) {
         log.error("Error parsing VerifyIP configuration file", ioe);
         return false;
      }
      finally {
         IOUtils.close(is);
      }
      Map<String,DnsList> lists = new HashMap<String,DnsList>();
      int listCount = 1, subListCount, replyCount;
      String listName, hostName, subListName, reply, severity;
      DnsList dnsList;
      DnsSubList dnsSubList;
      String replies[];

      do {
         listName = properties.getProperty("list.name."+listCount);
         if (listName == null) {
            if (listCount == 1) {
               log.error("no list declared");
               return false;
            }
            break;
         }
         hostName = properties.getProperty(listName+".hostname");
         if (hostName==null) {
            log.error(listName+"'s hostname not set");
            return false;
         }
         subListCount = 1;
         dnsList = new DnsList(hostName);
         do {
            subListName = properties.getProperty(listName+".sublist."+subListCount);
            if (subListName==null) {
               if (subListCount == 1) {
                  log.error("no sublist declared for list "+listName);
                  return false;
               }
               break;
            }
            reply = properties.getProperty(listName+'.'+subListName+".general");
            if (reply == null) {
               log.error(listName+"'s sublist "+subListName+" entry has no general property set");
               return false;
            }
            dnsSubList = new DnsSubList();
            replies = reply.split("\\,");
            replyCount = replies.length;
            for (int i=0;i<replyCount;i++) {
               reply = replies[i].substring(0, replies[i].indexOf(':'));
               if (!evaluateReply(reply)) {
                  log.error(listName+": sublist general entry of "+subListName+"'s reply has improper format. "+reply);
                  return false;
               }
               severity = replies[i].substring(replies[i].indexOf(':')+1);
               if (!evaluateSeverity(severity)) {
                  log.error(listName+": sublist general entry of "+subListName+"'s reply has improper format. "+severity);
                  return false;
               }
               dnsSubList.general.put(reply, severity);
            }
            reply = properties.getProperty(listName+'.'+subListName+".strict");
            if (reply != null) {
               dnsSubList.initStrict();
               replies = reply.split("\\,");
               replyCount = replies.length;
               for (int i=0;i<replyCount;i++) {
                  reply = replies[i].substring(0, replies[i].indexOf(':'));
                  if (!evaluateReply(reply)) {
                     log.error(listName+": sublist strict entry of "+subListName+"'s reply has improper format. "+reply);
                     return false;
                  }
                  severity = replies[i].substring(replies[i].indexOf(':')+1);
                  if (!evaluateSeverity(severity)) {
                     log.error(listName+": sublist strict entry of "+subListName+"'s severity has improper format. "+severity);
                     return false;
                  }
                  dnsSubList.strict.put(reply, severity);
               }
            }
            dnsList.getSubLists().put(subListName, dnsSubList);
            subListCount++;
            
         }while(true);
         lists.put(listName, dnsList);
         listCount++;

      }while(true);

      String listEntry = properties.getProperty("whitelist");
      if (listEntry==null) {
         log.error("No whitelist declared");
         return false;
      }
      listName = listEntry.substring(listEntry.indexOf('.')+1);
      subListName = listEntry.substring(0,listEntry.indexOf('.'));
      try {
         whiteList = (DnsSubList)((DnsList)lists.get(listName)).getSubLists().get(subListName);
      }
      catch (NullPointerException npe) {
         log.error("Check that "+subListName+'.'+listName+" selected for a whitelist corresponds to a declared list/sublist combo");
         return false;
      }
      whiteList.setHostName(subListName+'.'+((DnsList)lists.get(listName)).getHostName());
      

      listEntry = properties.getProperty("mixedlist");
      if (listEntry!=null) {
         listName = listEntry.substring(listEntry.indexOf('.')+1);
         subListName = listEntry.substring(0,listEntry.indexOf('.'));
         try {
            mixedList = (DnsSubList)((DnsList)lists.get(listName)).getSubLists().get(subListName);
         }
         catch (NullPointerException npe) {
            log.error("Check that "+subListName+'.'+listName+" selected for a mixedlist corresponds to a declared list/sublist combo");
            return false;
         }
         mixedList.setHostName(subListName+'.'+((DnsList)lists.get(listName)).getHostName());
      }

      listEntry = properties.getProperty("blacklist");
      if (listEntry==null) {
         log.error("No blacklist declared");
         return false;
      }
      listName = listEntry.substring(listEntry.indexOf('.')+1);
      subListName = listEntry.substring(0,listEntry.indexOf('.'));
      try {
         blackList = (DnsSubList)((DnsList)lists.get(listName)).getSubLists().get(subListName);
      }
      catch (NullPointerException npe) {
         log.error("Check that "+subListName+'.'+listName+" selected for a blacklist corresponds to a declared list/sublist combo");
         return false;
      }
      blackList.setHostName(subListName+'.'+((DnsList)lists.get(listName)).getHostName());

      strictUsed    = Boolean.valueOf(properties.getProperty("blacklist.use.strict", "false")).booleanValue();
      blockingIP    = Boolean.valueOf(properties.getProperty("max.severity.block", "false")).booleanValue();
      amavisRelayed = Boolean.valueOf(properties.getProperty("not.listed.amavis", "true")).booleanValue();
      if (!ConfigurationManager.getInstance().isAmavisSupportActive()) {
         amavisRelayed = false;
      }
      log.info("The verifyIP service has been successfully setup.");
      return true;
   }

   private boolean evaluateReply(String reply) {
      if (!reply.startsWith("127")) return false;
      String[] replies = reply.split("\\.");
      String subReply;
      int number;
      int replyCount = replies.length;
      if (replyCount!=4) return false;
      for (int i=1;i<replyCount;i++) {
         subReply = replies[i];
         try {
            number = Integer.parseInt(subReply);
            if (number<0 || number>255) return false;
         }
         catch (NumberFormatException e) {
            if (!subReply.equals("*") || i ==3) {
               return false;
            }
         }
      }
      return true;
   }

   private boolean evaluateSeverity(String severity) {
      return severity.equals("B") || severity.equals("BB") || severity.equals("BBB")
            || severity.equals("WWW") || severity.equals("WW") || severity.equals("W");
   }

   public static synchronized boolean initializeVerifyIPConfigurator(String mailDirectory) {
      if (vIPC == null) {
         //Never called from more than one Threads. No worries.
         vIPC = new VerifyIPConfigurator();
         return vIPC.init(mailDirectory);
      }
      return false;
   }

   public static VerifyIPConfigurator getInstance() {
      return vIPC;
   }

   public DnsSubList getWhiteList() {
      return whiteList;
   }

   public DnsSubList getMixedList() {
      return mixedList;
   }

   public DnsSubList getBlackList() {
      return blackList;
   }

   public boolean isStrictUsed() {
      return strictUsed;
   }

   public boolean isBlockingIP() {
      return blockingIP;
   }

   public boolean isAmavisRelayed() {
      return amavisRelayed;
   }

   private static final class DnsList {

      private final String hostName;
      private final Map<String,DnsSubList> subLists;

      private DnsList(String hostName) {
         this.hostName = hostName;
         subLists = new HashMap<String,DnsSubList>();
      }

      String getHostName() {
         return hostName;
      }

      Map<String,DnsSubList> getSubLists() {
         return subLists;
      }
   }

   public static final class DnsSubList {

      private String hostName;
      private Map<String,String> general;
      private Map<String,String> strict;

      private DnsSubList() {
         general = new HashMap<String,String>();
      }

      void initStrict() {
         if (strict == null) {
            strict = new HashMap<String,String>();
         }
      }

      public String getHostName() {
         return hostName;
      }

      void setHostName(String hostName) {
         this.hostName = hostName;
      }

      public Map<String,String> getGeneral() {
         return java.util.Collections.unmodifiableMap(general);
      }

      public Map<String,String> getStrict() {
         return java.util.Collections.unmodifiableMap(strict);
      }

   }

}
