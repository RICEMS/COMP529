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

package com.ericdaugherty.mail.server.persistence.localDelivery;

import com.ericdaugherty.mail.server.configuration.BackEndCreationResponse;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.UserCreationException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.info.User;
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;
import com.ericdaugherty.mail.server.utils.FileUtils;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.internal.Domain;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class offers the means of persisting a message to a local user's
 * mailbox.
 *
 * @author Andreas Kyrmegalos
 */
public class SimpleFileIOProcessor implements LocalDeliveryProcessor {

   private static final byte[] EOL = System.getProperty("line.separator").getBytes(ConfigurationManager.getUsAsciiCharset());

   /** Logger */
   protected Log log = LogFactory.getLog( SimpleFileIOProcessor.class );

   /** The ConfigurationManager */
   protected final ConfigurationManager configurationManager = ConfigurationManager.getInstance();
   
   private final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();
   
   private final int hashCode = UUID.randomUUID().toString().hashCode();
   
   private volatile boolean inUse;
   private volatile boolean invalidate;
   
   protected final User user;
   
   SimpleFileIOProcessor() {
      this.user = null;
   }
   
   SimpleFileIOProcessor(User user) {
      this.user = user;
   }
   
   @Override
   public BackEndCreationResponse createUserRepository(String userRepository) throws UserCreationException {

      File directory = new File( userRepository );

      if ( !directory.exists() ) {
         log.info( "Directory " + directory.getName() + " does not exist, creating..." );
         if (directory.mkdir()) {
            log.info("Successfully created directory " + directory.getName());
            return BackEndCreationResponse.CREATED;
         } else {
            throw new UserCreationException("Unable to create folder for user: " + directory.getName());
         }
      }
      if( !directory.isDirectory() ) {
         String message = "User Directory: " + userRepository
               + " already exists and is not a directory!" ;
         log.error(message);
         throw new UserCreationException(message);
      }
      return BackEndCreationResponse.EXISTS;
   }

   /**
    * Gets the user's directory as a {@link String}. This method also verifies
    * that the directory exists. The user directory is located under the domain
    * directory.
    *
    * @return The full path denoting the user's directory.
    */
   @Override
    public String getUserRepository() {

        File directory = new File(configurationManager.getUsersDirectory(), user.getUserAdress());
        return directory.getPath();
    }

    /**
     * Returns a message's size on disk
     *
     * @param messageLocation The message filename (this is not a full path, since it is generated by a list() file method).
     * @return The message size on disk is returned.
     */
   @Override
    public long getMessagePersistedSize(String messageLocation) {
       return new File(getUserRepository(), messageLocation).length();
    }
   
   private static final Set<String> locUids = new ConcurrentSkipListSet<String>();
   
   private static File getSafeToUseFile(@NonNull File parent, @NonNull String locUid) {
      
      File messageFile = new File(parent, locUid + ".tmp");
      if (messageFile.exists() || new File(parent, locUid + ".loc").exists())
            return null;
      return locUids.add(locUid) ? messageFile : null;
   }
   
   public static void removeUniqueName(@NonNull String locUid) {
      locUids.remove(locUid);
   }

    /**
     * Saves a message to the user's directory and returns the full path/filename of the file where it was persisted.
     *
     * @param message The message itself.
     * @param address The user's email address.
     * @return In the case of a file-system back-end the full path/filename is returned.
     * @throws java.io.IOException
     */
   @Override
   public Object persistLocalMessage(SMTPMessage message, EmailAddress address) throws IOException {

      inUse = true;
      try {
           //Get the directory and create a new file.
           File userDirectory = new File(getUserRepository());
           //The temporary file to write to. Using a tmp extension to avoid having the file picked up by
           //the user's pop3 file lister, before delivery is complete.
           String locUid;
           File messageFile;
           do {
               locUid = message.getSmtpUid() + "_" + String.valueOf(Long.valueOf(System.currentTimeMillis()).intValue());
               messageFile = getSafeToUseFile(userDirectory, locUid);
           } while (messageFile == null);
           try {

               File messageLocation = new File(userDirectory, locUid + ".loc");

               if(log.isDebugEnabled())
                  log.debug( "Delivering to: " + userDirectory.getName() + File.separator + messageFile.getName() );

               FileOutputStream fos = null;
               BufferedOutputStream bos = null;
               try {
                  bos = new BufferedOutputStream(fos = new FileOutputStream(messageFile), 4096);

                  String outLine = "Return-Path: <" + message.getFromAddress().getAddress() + ">";

                  //Write the Return-Path: header
                  bos.write(outLine.getBytes(usAsciiCharset));
                  bos.write(EOL);
                  bos.flush();

                  outLine = "Delivered-To: " + user.getEmailAddress().getAddress();

                  //Write the Delivered-To: header
                  bos.write(outLine.getBytes(usAsciiCharset));
                  bos.write(EOL);
                  bos.flush();

                  if (invalidate)
                     throw new IOException("Domain invalidated, cannot perform local delivery.");
                  //Get the data to write incrementally.
                  int count = 8;
                  List<byte[]> dataLines = message.loadIncrementally(count);
                  while (dataLines.size() > 0) {
                     //Write the data.
                     for(byte[] singleLine : dataLines) {
                        //Provision for transparency according to RFC 2821/4.5.2
                        if (singleLine.length > 0 && singleLine[0] == 0x2e) {
                           bos.write(new byte[]{0x02e});
                        }
                        bos.write(singleLine);
                        bos.write(EOL);
                        bos.flush();
                     }
                     count += 250;
                     dataLines.clear();
                     if (invalidate)
                        throw new IOException("Domain invalidated, cannot perform local delivery.");
                     dataLines = message.loadIncrementally(count);
                  }
               } finally {
                  IOUtils.close(bos);
                  IOUtils.close(fos);
               }

               FileUtils.copyFile(messageFile, messageLocation);
               if (!messageFile.delete() && messageFile.exists()) {
                  throw new IOException("Failed to rename " + messageFile.getPath() + " to " + messageLocation.getPath());
               }
               return messageLocation.getPath();
           } catch(IOException ioe) {
               log.error("Error performing local delivery.", ioe);
               throw ioe;
           } finally {
              removeUniqueName(locUid);
           }
      } finally {
         inUse = false;
      }
    }

   @Override
   public int compareTo(LocalDeliveryProcessor o) {
      return this.hashCode - o.hashCode();
   }
   
   @Override
   public int hashCode() {
      return hashCode;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof LocalDeliveryProcessor)) {
         return false;
      }
      final LocalDeliveryProcessor other = (LocalDeliveryProcessor) obj;
      if (this.hashCode() != other.hashCode()) {
         return false;
      }
      return true;
   }
   
   @Override
   public void invalidate(Domain domain) {
      if (user == null)
         return;
      if (domain.equals(user.getDomain())) {
         invalidate = true;
         while (inUse) {
            try {
               Thread.sleep(100);
            } catch (InterruptedException ie) {
               //let the loop decide
            }
         }
      }
   }
}
