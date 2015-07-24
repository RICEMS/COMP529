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

import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.xlat4cast.jes.dns.internal.Domain;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

/**
 *
 * @author Andreas Kyrmegalos
 */
final class ConfigurationManagerDirectories implements ConfigurationParameterConstants {
   
   /** Logger */
   private static final Log log = LogFactory.getLog(ConfigurationManager.class);
   
   private final ConfigurationManager cm;

   private final Set<String> dirRequests;
   
   private final ReentrantLock dirRequestLock = new ReentrantLock();
   
   //Used for testing purposes only
   ConfigurationManagerDirectories(){
      cm = null;
      dirRequests = null;
   }

   ConfigurationManagerDirectories(final String rootDirectory) {
      cm = ConfigurationManager.getInstance();
      dirRequests = new LinkedHashSet<String>();
      
      setRootDirectory(rootDirectory);
      setSecurityDirectory(rootDirectory + File.separator + "security");
      setBackupDirectory(rootDirectory + File.separator + "backup");
      setSMTPDirectory(rootDirectory + File.separator + "smtp");
      setUsersDirectory(rootDirectory + File.separator + "users");
      setFailedDirectory(rootDirectory + File.separator + "failed");
      System.setProperty("jes.install.directory", rootDirectory);
   }

   Map<String, String> getConfiguration() {

      Map<String, String> configuration = new HashMap<String, String>();

      configuration.put("dirRoot", getRootDirectory());
      
      String dir = getSMTPDirectory();
      if(dir.equals(getRootDirectory() + File.separator + "smtp")) {
         dir = "using default SMTP directory";
      }
      configuration.put("dirSMTP", dir + RESTART);
      
      dir = getUsersDirectory();
      if(dir.equals(getRootDirectory() + File.separator + "users")) {
         dir = "using default users directory";
      }
      configuration.put("dirUsers", dir + RESTART);
      
      dir = getFailedDirectory();
      if(dir.equals(getRootDirectory() + File.separator + "failed")) {
         dir = "using default failed directory";
      }
      configuration.put("dirFailed", dir + RESTART);
      
      dir = getTestingDirectory();
      if (dir == null) {
         dir = "";
      }
      configuration.put("dirTesting", dir + RESTART);
      
      configuration.put("fileSeparator", File.separator);
      configuration.put("allowRemoteRestart", Boolean.toString(cm.isAllowRemoteRestart()));

      return configuration;
   }
   
   public BackEndCreationResponse createDomainDirectory(@NonNull String domain) {
      try {
         EmailAddress.parseDomain(domain);
      } catch (InvalidAddressException iae) {
         log.error(domain + " is an invalid domain name.", iae);
         return BackEndCreationResponse.INVALID;
      }
      File users = new File(cm.getUsersDirectory());
      if (!users.exists() && !users.mkdir()) {
         log.error("Unable to create users directory: " + users.getPath());
         return BackEndCreationResponse.NOT_CREATED;
      }
      File domainDirectory = new File(users, domain);
      if (domainDirectory.exists())
         return BackEndCreationResponse.EXISTS;
      if (domainDirectory.mkdir()) {
         log.info("Created domain directory: " + domain);
         return BackEndCreationResponse.CREATED;
      } else {
         log.error("Error creating domain directory: \"" + domainDirectory.getPath() + "\".");
         return BackEndCreationResponse.NOT_CREATED;
      }
   }
   
   public void revertDomainDirectory(String domain) {
      File domainDirectory = new File(cm.getUsersDirectory(), domain);
      if (domainDirectory.isDirectory()) {
         boolean deleted = domainDirectory.delete();
         if (!deleted) {
            if (log.isDebugEnabled())
                  log.debug("Unable to deleted " + domainDirectory.getPath());
         }
      }
   }
   
   public void revertUserDirectory(String userRepository) {
      File userDirectory = new File(userRepository);
      if (userDirectory.isDirectory()) {
         boolean deleted = userDirectory.delete();
         if (!deleted) {
            if (log.isDebugEnabled())
                  log.debug("Unable to deleted " + userDirectory.getPath());
         }
      }
   }

   public void requestDirCreation(String directory) {
      
      dirRequestLock.lock();
      try {
         boolean added = dirRequests.add(directory);
         if (added && log.isDebugEnabled())
            log.debug("added request to create directory " + directory.substring(directory.lastIndexOf(File.separatorChar) + 1));
      } finally {
         dirRequestLock.unlock();
      }
   }
   
   public boolean checkLegacyFileIO(String usersDirectory, Set<? extends Domain> domains) {
      
      File usersDir = new File(usersDirectory);
      
      File[] files = usersDir.listFiles();
      
      Map<String, List<String>> domainDirs = new LinkedHashMap<String, List<String>>();
      String domain;
      List<String> userDirs;
      for (File file:files) {
         if (file.isDirectory()) {
            if (domains.contains(new Domain(file.getName()))) {
               throw new RuntimeException("Can not switch to new FileIO mode. The directory "
                     + file.getName() + " denotes an already registered local domain.");
            }
            domain = file.getName().substring(file.getName().indexOf('@')+1);
            userDirs = domainDirs.get(domain);
            if (userDirs == null) {
               userDirs = new LinkedList<String>();
               domainDirs.put(domain, userDirs);
            }
            userDirs.add(file.getName().substring(0, file.getName().indexOf('@')));
         }
      }
      if (domainDirs.isEmpty()) {
         return true;
      }
      
      //Need to check for domain names that have the same lower case value
      Iterator<String> iterator;
      iterator = domainDirs.keySet().iterator();
      String domainName, uniqueDomainName;
      Locale englishLocale = ConfigurationManager.LOCALE;
      Set<String> tempDirs = new LinkedHashSet<String>();
      Iterator<String> iter;
      String currentDomainName;
      while(iterator.hasNext()) {
         domainName = iterator.next();
         uniqueDomainName = domainName.toLowerCase(englishLocale);

         iter = tempDirs.iterator();
         while(iter.hasNext()) {
            currentDomainName = iter.next();
            if (!currentDomainName.equals(domainName)&&
                  currentDomainName.toLowerCase(englishLocale).equals(uniqueDomainName)) {
               throw new RuntimeException("Can not switch to new FileIO mode. There exist at "
                     + "least two registered domains("+domainName+","+currentDomainName+"), whose lower "
                     + "cased value is equal.");
            }
         }
         tempDirs.add(domainName);
      }
      tempDirs.clear();
      
      Map<File, List<String>> completed = new LinkedHashMap<File, List<String>>();
      String partialDomain = null;
      List<String> partialUsers = null;
      String partialUser = null;
      List<String> completedUsers;
      //Move the files
      boolean failed = false;
      File domainDir, oldUserDir;
      for(String aDomainDir:domainDirs.keySet()) {
         domainDir = new File(usersDir, aDomainDir);
         if (!domainDir.exists()) {
            if (!domainDir.mkdir()) {
               failed = true;
               break;
            }
         }
         completedUsers = new ArrayList<String>();
         for (String user:domainDirs.get(aDomainDir)) {
            File newUserDir = new File(domainDir, user);
            if (!newUserDir.exists()) {
               if (!newUserDir.mkdir()) {
                  partialDomain = aDomainDir;
                  partialUsers = completedUsers;
                  failed = true;
                  break;
               }
            }
            oldUserDir = new File(usersDir, user+'@'+aDomainDir);
            File[] userFiles = oldUserDir.listFiles();
            for (File aUserFile:userFiles) {
               if (!aUserFile.canRead()) {
                  partialDomain = aDomainDir;
                  partialUsers = completedUsers;
                  failed = true;
                  break;
               }
               if (!aUserFile.renameTo(new File(newUserDir, aUserFile.getName()))) {
                  log.warn("Move operation failed for file: "+aUserFile);
                  partialDomain = aDomainDir;
                  partialUsers = completedUsers;
                  partialUser = aUserFile.getName();
                  failed = true;
                  break;
               }
            }
            completedUsers.add(user);
         }
         completed.put(domainDir, completedUsers);
      }
      try {
         if (!new File(usersDir, ".switched").createNewFile())
            throw new IOException();
      }
      catch (IOException ioe) {
         failed = true;
      }
      //Check that the operation has successfully concluded
      if (failed) {
         log.warn("Can not switch to new FileIO mode. Could not successfully transfer all the "
               + "user messages to their enclosing domain folder. Reverting.");
         if (partialUser!=null) {
            File[] userFiles = new File(new File(usersDir, partialDomain), partialUser).listFiles();
            for (File aUserFile : userFiles) {
               if (!aUserFile.renameTo(new File(new File(usersDir, partialUser+'@'+partialDomain), aUserFile.getName()))) {
                  log.error("Renaming file "+aUserFile.getName()+" under "+partialDomain+" failed.");
               }
            }
         }
         if (partialDomain!=null) {
            for (String aPartialUser:partialUsers) {
               File[] userFiles = new File(new File(usersDir, partialDomain), aPartialUser).listFiles();
               for (File aUserFile:userFiles) {
                  if (!aUserFile.renameTo(new File(new File(usersDir, partialUser+'@'+partialDomain), aUserFile.getName()))) {
                     log.error("Renaming file "+aUserFile.getName()+" under "+partialDomain+" failed.");
                  }
               }
            }
         }
         File aFolder;
         boolean dirGone;
         for (File completedDomain:completed.keySet()) {
            for (String completedUser:completed.get(completedDomain)) {
               File[] userFiles = new File(new File(usersDir, completedDomain.getName()), completedUser).listFiles();
               for (File aUserFile:userFiles) {
                  if (!aUserFile.renameTo(new File(new File(usersDir, completedUser+'@'+completedDomain.getName()), aUserFile.getName()))) {
                     log.error("Renaming file "+aUserFile.getName()+" under "+completedUser+" failed.");
                  }
               }
               aFolder = new File(new File(usersDir, completedDomain.getName()), completedUser);
               dirGone = aFolder.delete();
               if (!dirGone) {
                  if (log.isDebugEnabled()) {
                     log.debug("Folder "+aFolder.getName()+" under "+completedDomain.getName()+" not deleted. Ignoring...");
                  }
               }
            }
            aFolder = new File(usersDir, completedDomain.getName());
            dirGone = aFolder.delete();
            if (!dirGone) {
               if (log.isDebugEnabled()) {
                  log.debug("Folder "+aFolder.getName()+" under "+usersDir.getName()+" not deleted. Ignoring...");
               }
            }
         }
         
         log.warn("Successfully reverted the user repository to its previous state.");
         return false;
      }
      else {
         File aFolder;
         boolean dirGone;
         for (File completedDomain:completed.keySet()) {
            for (String completedUser:completed.get(completedDomain)) {
               aFolder = new File(usersDir, completedUser+'@'+completedDomain.getName());
               dirGone = aFolder.delete();
               if (!dirGone) {
                  if (log.isDebugEnabled()) {
                     log.debug("Folder "+aFolder.getName()+" under folder users not deleted. Ignoring...");
                  }
               }
            }
         }
         log.warn("Successfully switched to the new FileIO mode.");
         return true;
      }
   }

   public void createDirectories() {
      
      dirRequestLock.lock();
      try {
         if (dirRequests.isEmpty())
            return;
         if (log.isDebugEnabled())
            log.debug("Creating Directories");
         if (dirRequests.remove("users")) {
            File users = new File(getUsersDirectory());
            if (!users.exists()) {
               log.info("Users directory does not exist.  Creating: " + users.getAbsolutePath());
               if (!users.mkdir()) {
                  log.error("Error creating users directory: " + users.getAbsolutePath() + ".");
                  throw new RuntimeException("Unable to create users Directory.");
               }
            }
            try {
               if (!new File(users, ".switched").createNewFile())
                  throw new IOException();
            }
            catch (IOException ioe) {
               log.error("Error while marking the users directory as switched.");
               throw new RuntimeException("Error while marking the users directory as switched.");
            }
         }

         boolean switched = cm.isLegacyFileIOMode()
               || new File(getUsersDirectory(), ".switched").exists();
         if(!switched)
            checkLegacyFileIO(getUsersDirectory(), cm.getBackEnd().getDomains());

         for (String dirRequest : dirRequests) {
            if (dirRequest.equals("smtp")) {
               File smtp = new File(getSMTPDirectory());
               if (smtp.exists()) {
                  continue;
               }
               log.info("SMTP Mail directory does not exist.  Creating: " + smtp.getAbsolutePath());
               if (!smtp.mkdir()) {
                  log.error("Error creating SMTP Mail directory: " + smtp.getAbsolutePath() + ". No incoming mail will be accepted!");
                  throw new RuntimeException("Unable to create SMTP Mail Directory.");
               }
            } else if (dirRequest.equals("amavis")) {
               File amavis = new File(cm.getAmavisSMTPDirectory());
               if (amavis.exists()) {
                  continue;
               }
               log.info("Amavis SMTP Mail directory does not exist.  Creating: " + amavis.getAbsolutePath());
               if (!amavis.mkdir()) {
                  log.error("Error creating Amavis SMTP Mail directory: " + amavis.getAbsolutePath() + ". No incoming mail will be accepted!");
                  throw new RuntimeException("Unable to create Amavis SMTP Mail Directory.");
               }
            } else if (dirRequest.equals("backup")) {
               File backup = new File(getBackupDirectory());
               if (backup.exists()) {
                  continue;
               }
               if (!backup.mkdir()) {
                  log.error("Error creating backup directory: " + backup.getAbsolutePath() + ".");
                  throw new RuntimeException("Unable to create backup Directory.");
               }
            } else if (dirRequest.equals("failed")) {
               File failed = new File(getFailedDirectory());
               if (failed.exists()) {
                  continue;
               }
               log.info("failed directory does not exist.  Creating: " + failed.getAbsolutePath());
               if (!failed.mkdir()) {
                  log.error("Error creating failed directory: " + failed.getAbsolutePath() + ".  No failed mail will be stored!");
                  throw new RuntimeException("Unable to create failed Directory.");
               }
            } else if (dirRequest.equals("external")) {
               File external = new File(getRootDirectory(), "external");
               if (external.exists()) {
                  continue;
               }
               if (!external.mkdir()) {
                  log.error("Error creating external directory: " + external.getAbsolutePath() + ".");
                  throw new RuntimeException("Unable to create external Directory.");
               }
            } else if (dirRequest.startsWith("testing:")) {
               String dirToCreate = dirRequest.substring(dirRequest.indexOf(':') + 1);
               File testing = new File(dirToCreate);
               if (!testing.exists()) {
                  if (!testing.mkdir()) {
                     log.error("Error creating testing directory: " + testing.getAbsolutePath() + ".");
                     throw new RuntimeException("Unable to create testing Directory.");
                  }
               }
            } else if (dirRequest.length() > 8 && dirRequest.substring(0,8).equals(".domain@")) {
               String dirToCreate = dirRequest.substring(dirRequest.indexOf('@') + 1);
               File domainDirectory = new File(cm.getUsersDirectory(), dirToCreate);
               if (!domainDirectory.exists()) {
                  if (!domainDirectory.mkdir()) {
                     log.error("Error creating domain directory: " + domainDirectory.getAbsolutePath() + ".");
                     throw new RuntimeException("Unable to create domain "+dirToCreate+" Directory.");
                  }
                  else {
                     log.info("Created domain directory: " + dirToCreate);
                  }
               }
            }
            else {
               throw new IllegalArgumentException("user directory creation not allowed.");
            }
         }
         dirRequests.clear();
      } finally {
         dirRequestLock.unlock();
      }
   }
   
   /** The root directory of the application. */
   private String rootDirectory;
   /** A directory used for doBackup purposes. */
   private String backupDirectory;
   /** The root directory used to store the server certificate and private key. **/
   private String securityDirectory;

   /**
    * The root directory used to store the incoming and outgoing messages.
    *
    * @return String
    */
   public String getRootDirectory() {
      return rootDirectory;
   }

   /**
    * The root directory used to store the incoming and outgoing messages.
    *
    * @param rootDirectory String
    */
   private void setRootDirectory(String rootDirectory) {
      this.rootDirectory = rootDirectory;
   }

   /**
    * The directory used to store doBackup files.
    *
    * @return backupDirectory String
    */
   public String getBackupDirectory() {
      return backupDirectory;
   }

   /**
    * The directory used to store doBackup files.
    *
    * @param backupDirectory String
    */
   private void setBackupDirectory(String backupDirectory) {
      this.backupDirectory = backupDirectory;
      File backup = new File(backupDirectory);
      if (!backup.exists()) {
         requestDirCreation("backup");
      }
   }

   /**
    * The directory used to hold security sensitive data.
    * 
    * @return securityDirectory String
    */
   public String getSecurityDirectory() {
      return securityDirectory;
   }

   /**
    * The root directory used to store the server certificate and private key.
    *
    * @param securityDirectory String
    */
   private void setSecurityDirectory(String securityDirectory) {
      this.securityDirectory = securityDirectory;
   }
   /** The directory used to store the incoming e-mails. */
   private String smtpDirectory;
   /** The directory used to store the user accounts. */
   private String usersDirectory;
   /** The directory used to store the failed messages. */
   private String failedDirectory;
   /** A directory to direct all messages to when in testing mode */
   private String testingDirectory;

   /**
    * The directory used to store incoming e-mails.
    * 
    * @return SMTPDirectory String
    */
   public String getSMTPDirectory() {
      return smtpDirectory;
   }
   
   private static final class OnlyTempFileFilter implements FileFilter {

      @Override
      public boolean accept(File file) {
         return file.getName().toLowerCase().endsWith(".tmp");
      }
   };

   /**
    * The directory used to store incoming e-mails.
    *
    * @param smtpDirectory String
    */
   private void setSMTPDirectory(String smtpDirectory) {
      this.smtpDirectory = smtpDirectory;
      File smtpDir = new File(smtpDirectory);
      if (!smtpDir.exists()) {
         requestDirCreation("smtp");
         return;
      }
      //If any incomplete temporary messages are present at application initialiazation delete them.
      File[] files = new File(smtpDirectory).listFiles(new OnlyTempFileFilter());
      int numFiles = files.length;
      boolean fileGone;
      for (int i = 0; i < numFiles; i++) {
         fileGone = files[i].delete();
         if (!fileGone) {
            if (log.isDebugEnabled()) {
               log.debug("File "+files[i].getName()+" under "+smtpDirectory+" not deleted. Ignoring...");
            }
         }
      }
   }

   /**
    * The directory used to store the user accounts.
    *
    * @return String
    */
   public String getUsersDirectory() {
      return usersDirectory;
   }

   /**
    * The directory used to store the user accounts.
    *
    * @param mailDirectory String
    */
   private void setUsersDirectory(String usersDirectory) {
      this.usersDirectory = usersDirectory;
      File usersDir = new File(usersDirectory);
      // If the directory does not exist, create it.
      if (!usersDir.exists()) {
         requestDirCreation("users");
      }
   }

   /**
    * The directory used to store failed e-mails.
    *
    * @return failedDirectory String
    */
   public String getFailedDirectory() {
      return failedDirectory;
   }

   /**
    * The directory used to store failed e-mails.
    *
    * @param failedDirectory String
    */
   private void setFailedDirectory(String failedDirectory) {
      this.failedDirectory = failedDirectory;
      File failedDir = new File(failedDirectory);
      // If the directory does not exist, create it.
      if (!failedDir.exists()) {
         requestDirCreation("failed");
      }
   }

   public String getTestingDirectory() {
      return testingDirectory;
   }

   void directoriesConfiguration(Element element) {

      if (!cm.isFixed()) {
         String smtp = ((Element) element.getElementsByTagName("SMTP").item(0)).getTextContent();
         if (smtp == null || smtp.trim().isEmpty()) {
            smtp = getRootDirectory()+File.separator+"smtp";
         }
         else {
            smtp = smtp.trim();
         }
         setSMTPDirectory(smtp);
         log.info("JES incoming SMTP spool " + getSMTPDirectory());
         System.setProperty("jes.incoming.directory", getSMTPDirectory());

         String users = ((Element) element.getElementsByTagName("users").item(0)).getTextContent();
         if (users==null||users.trim().length()==0) {
            users = getRootDirectory()+File.separator+"users";
         }
         else {
            users = users.trim();
         }
         setUsersDirectory(users);
         System.setProperty("jes.users.directory", getUsersDirectory());

         String failed = ((Element) element.getElementsByTagName("failed").item(0)).getTextContent();
         if (failed==null||failed.trim().length()==0) {
            failed = getRootDirectory()+File.separator+"failed";
         }
         else {
            failed = failed.trim();
         }
         setFailedDirectory(failed);
         System.setProperty("jes.failed.directory", getFailedDirectory());

         testingDirectory = ((Element) element.getElementsByTagName("testing").item(0)).getTextContent();
         if (testingDirectory!=null&&testingDirectory.trim().length()!=0) {
            testingDirectory = testingDirectory.trim();
            if (testingDirectory.equals(smtp)) {
               throw new RuntimeException("The testing directory can not be the same as the SMTP directory");
            }
            cm.testingMode();
            requestDirCreation("testing:" + testingDirectory);
            System.setProperty("jes.testing.directory", getTestingDirectory());
         } else {
            System.setProperty("jes.testing.directory", getSMTPDirectory());
         }
      }
   }
}
