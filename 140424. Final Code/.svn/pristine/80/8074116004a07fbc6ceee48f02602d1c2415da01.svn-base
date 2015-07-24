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

package com.ericdaugherty.mail.server.persistence.pop3;

import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.InvalidateProcessorException;
import com.ericdaugherty.mail.server.errors.TooManyErrorsException;
import com.ericdaugherty.mail.server.info.*;
import com.ericdaugherty.mail.server.persistence.localDelivery.LocalDeliveryFactory;
import com.ericdaugherty.mail.server.services.ProcessorStreamHandler;
import com.ericdaugherty.mail.server.services.pop3.Pop3Message;
import com.ericdaugherty.mail.server.utils.DelimitedInputStream;
import com.ericdaugherty.mail.server.utils.IOUtils;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A file system based POP3 persistence engine.
 *
 * @author Andreas Kyrmegalos
 */
public class SimpleFileIOProcessor implements POP3MessagePersistenceProcessor {

   private final Charset usAsciiCharset = ConfigurationManager.getUsAsciiCharset();
   
   private final User user;
   private final String userRepository;
   private volatile boolean invalidate;
   private volatile boolean inUse;

   SimpleFileIOProcessor(@NonNull User user) {
      this.user = user;
      this.userRepository = LocalDeliveryFactory.getInstance().getLocalDeliveryProccessor(user).getUserRepository();
   }

   private String getUserRepository() {
      return userRepository;
   }
   
   @Override
   public void invalidate() {
      invalidate = true;
      while (inUse) {
         try {
            Thread.sleep(100);
         } catch (InterruptedException ie) {
            
         }
      }
   }

   @Override
   @CheckForNull
   public List<Pop3Message> getMessages() {
      
      if (invalidate)
         return null;
      inUse = true;
      try {
         final File directory = new File(getUserRepository());

         String[] messageNames = directory.list(new FilenameFilter() {

            @Override
            public boolean accept(File directoryFile, String file) {
               if (invalidate)
                  throw new InvalidateProcessorException();
               if (directoryFile.equals(directory)) {
                  if (file.toLowerCase().endsWith(".loc")) {
                     return true;
                  }
               }
               return false;
            }
         });
         if (messageNames == null)
            return null;
         List<Pop3Message> messages = new ArrayList<Pop3Message>(messageNames.length);
         for (String messageName : messageNames)
            messages.add(new Pop3Message(messageName));
         Collections.sort(messages);
         return Collections.unmodifiableList(messages);
      } catch (InvalidateProcessorException ipe) {
         return null;
      } finally {
         inUse = false;
      }
   }

   @Override
   @CheckForNull
   public List<String> deleteMessages(@NonNull List<Pop3Message> messages) {

      if (invalidate)
         return null;
      inUse = true;
      try {
         int numMessage = messages.size();
         if (numMessage == 0) {
            return Collections.emptyList();
         }
         File userDirectory = new File(getUserRepository());
         List<String> failedMessages = new ArrayList<String>(numMessage);
         for (Pop3Message currentMessage : messages) {
            if (invalidate)
               throw new InvalidateProcessorException();
            int attempts = 0;
            if (currentMessage.isDeleted()) {
               File aMessageFile = new File(userDirectory, currentMessage.getMessageLocation());
               do {
                  if (invalidate)
                     throw new InvalidateProcessorException();
                  if (!aMessageFile.delete() && aMessageFile.exists()) {
                     if (attempts == 5) {
                        failedMessages.add(currentMessage.getUniqueId());
                     }
                     try {
                        Thread.sleep(attempts * 5000L);
                     } catch (InterruptedException ex) {}
                  }
               }while (attempts++ < 5);
            }
         }
         if (failedMessages.isEmpty()) {
            return Collections.emptyList();
         }
         return failedMessages;
      } catch (InvalidateProcessorException ipe) {
         return null;
      } finally {
         inUse = false;
      }
   }

   @Override
   public void retreiveMessage(ProcessorStreamHandler pop3CH, String messageLocation)
         throws TooManyErrorsException, FileNotFoundException, IOException {

     if (invalidate)
        return;
      inUse = true;
      try {
         FileInputStream fis = null;
         DelimitedInputStream dis = null;
         List<byte[]> dataLines = new ArrayList<byte[]>(250);
         try {
            //Open a reader to read the file.
            dis = new DelimitedInputStream(fis = new FileInputStream(new File(getUserRepository(), messageLocation)));

            //messageLocation is used as a placeholder for the SMTP uid
            messageLocation = messageLocation.substring(messageLocation.lastIndexOf(File.separator) + 1, messageLocation.lastIndexOf('.'));
            boolean foundRPLCRCPT = false, foundRPLCID = false;

            String singleLine;
            //Write the file to the client.
            byte[] currentLine = dis.readLine();
            while (currentLine != null) {
               if (invalidate)
                  throw new InvalidateProcessorException();
               dataLines.add(currentLine);
               singleLine = new String(currentLine, usAsciiCharset);
               if (singleLine.indexOf("<REPLACE-RCPT>") != -1) {
                  dataLines.set(dataLines.size() - 1, ("        for <" + user.getUserAdress() + ">" + singleLine.substring(singleLine.indexOf(';'))).getBytes(usAsciiCharset));
                  foundRPLCRCPT = true;
               } else if (singleLine.indexOf("<REPLACE-ID>") != -1) {
                  dataLines.set(dataLines.size() - 1, (singleLine.substring(0, singleLine.indexOf('<')) + messageLocation
                        + (singleLine.charAt(singleLine.length() - 1) == ';' ? ";" : "")).getBytes(usAsciiCharset));
                  foundRPLCID = true;
               }
               currentLine = dis.readLine();
               if (currentLine.length == 0 || (foundRPLCRCPT && foundRPLCID)) {
                  break;
               }
            }
            while (currentLine != null) {
               dataLines.add(currentLine);
               currentLine = dis.readLine();
               if (dataLines.size() == 250) {
                  for (byte[] readLine : dataLines) {
                     pop3CH.write(readLine);
                  }
                  dataLines.clear();
               }
            }
            int lineCount = dataLines.size();
            if (lineCount > 0) {
               for (byte[] readLine : dataLines) {
                  pop3CH.write(readLine);
               }
               dataLines.clear();
            }
            //Send the command end data transmission.
            pop3CH.write(new byte[]{0x2e});
         } finally {
            //Make sure the input stream gets closed.
            IOUtils.close(dis);
            IOUtils.close(fis);
            dataLines.clear();
         }
      } catch (InvalidateProcessorException ipe) {
         throw new IOException("Processor invalidated.");
      } finally {
         inUse = false;
      }
   }

   @Override
   public void retreiveMessageTop(ProcessorStreamHandler pop3CH, String messageLocation, long numLines)
         throws TooManyErrorsException, FileNotFoundException, IOException {

     if (invalidate)
        return;
      inUse = true;
      try {
         FileInputStream fis = null;
         DelimitedInputStream dis = null;
         try {
            //Open a reader to read the file.
            dis = new DelimitedInputStream(fis = new FileInputStream(new File(getUserRepository(), messageLocation)));

            //Write the Pop3Message Header.
            byte[] currentLine = dis.readLine();
            while (currentLine != null && currentLine.length != 0) {
               if (invalidate)
                  throw new InvalidateProcessorException();
               pop3CH.write(currentLine);
               currentLine = dis.readLine();
            }

            //Write an empty line to seperate header from body.
            pop3CH.write(currentLine);
            currentLine = dis.readLine();

            //Write the requested number of lines from the body of the
            //message, or until the entire message has been written.
            int index = 0;
            while (index < numLines && currentLine != null) {
               if (invalidate)
                  throw new InvalidateProcessorException();
               pop3CH.write(currentLine);
               currentLine = dis.readLine();
               index++;
            }
            //Send the command end data transmission.
            pop3CH.write(new byte[]{0x2e});
         } finally {
            IOUtils.close(dis);
            IOUtils.close(fis);
         }
      } catch (InvalidateProcessorException ipe) {
         throw new IOException("Processor invalidated.");
      } finally {
         inUse = false;
      }
   }
}
