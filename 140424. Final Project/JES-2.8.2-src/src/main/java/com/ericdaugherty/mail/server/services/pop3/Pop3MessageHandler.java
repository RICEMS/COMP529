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

package com.ericdaugherty.mail.server.services.pop3;

import com.ericdaugherty.mail.server.errors.TooManyErrorsException;
import com.ericdaugherty.mail.server.info.User;
import com.ericdaugherty.mail.server.persistence.pop3.POP3MessagePersistenceFactory;
import com.ericdaugherty.mail.server.persistence.pop3.POP3MessagePersistenceProcessor;
import com.ericdaugherty.mail.server.services.ProcessorStreamHandler;
import com.xlat4cast.jes.dns.internal.Domain;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class Pop3MessageHandler {

   private final User user;
   
   private List<Pop3Message> messages;
   
   private final POP3MessagePersistenceProcessor pOP3MessagePersistenceProccessor;
   
   private volatile boolean invalidate;
   
   public Pop3MessageHandler(@NonNull User user) {
      this.user = user;

      if (!(user.getEmailAddress().isNULL() || user.getEmailAddress().isMailerDaemon())) {
         pOP3MessagePersistenceProccessor = POP3MessagePersistenceFactory.getInstance().getPOP3PersistenceProccessor(user);
      } else {
         pOP3MessagePersistenceProccessor = POP3MessagePersistenceFactory.getInstance().getNullPeristenceProccessor();
      }
   }
   
   void invalidate(@NonNull Domain domain) {
      if (domain.equals(user.getEmailAddress().getDomain())) {
         invalidate = true;
         pOP3MessagePersistenceProccessor.invalidate();
      }
   }

   /**
    * Returns an array of Pop3Message objects that represents all messages
    * stored for this user.
    *
    * @return
    * @throws java.io.IOException
    */
   @NonNull
   public List<Pop3Message> getMessages() throws IOException {

      if (invalidate)
         throw new IOException("User " + user.getEmailAddress() + " was deleted.");
      
      if (messages == null) {
         messages = pOP3MessagePersistenceProccessor.getMessages();
         if (messages == null) {
            if (invalidate) {
               throw new IOException("Local error in processing.");
            } else {
               return Collections.emptyList();
            }
         }
      }
      return messages;
   }
   
   @CheckForNull
   public List<String> deleteMessages() throws IOException {
      return pOP3MessagePersistenceProccessor.deleteMessages(getMessages());
   }

   public void retreiveMessage(ProcessorStreamHandler pop3CH, int messageNumber)
         throws TooManyErrorsException, FileNotFoundException, IOException {
      pOP3MessagePersistenceProccessor.
            retreiveMessage(pop3CH, getMessage(messageNumber).getMessageLocation());
   }
   
   public void retreiveMessageTop(ProcessorStreamHandler pop3CH, int messageNumber, long numLines)
         throws TooManyErrorsException, FileNotFoundException, IOException {
      pOP3MessagePersistenceProccessor.
            retreiveMessageTop(pop3CH, getMessage(messageNumber).getMessageLocation(), numLines);
   }

   /**
    * Returns an array of Pop3Message objects that represents all messaged
    * stored for this user not marked for deletion.
    *
    * @return
    * @throws java.io.IOException
    */
   @NonNull
   public List<Pop3Message> getNonDeletedMessages() throws IOException {
      if (invalidate)
               throw new IOException("Local error in processing.");
      List<Pop3Message> messages = getMessages();
      List<Pop3Message> nonDeletedMessages = new ArrayList<Pop3Message>(messages.size());
      for (Pop3Message message : messages) {
         if (invalidate)
               throw new IOException("Local error in processing.");
         if (!message.isDeleted())
            nonDeletedMessages.add(message);
      }
      if (!nonDeletedMessages.isEmpty()) {
         return nonDeletedMessages;
      } else {
         return Collections.emptyList();
      }
   }

   /**
    * Gets the specified message. Pop3Message numbers are 1 based. This method
    * counts on the calling method to verify that the messageNumber actually
    * exists.
    *
    * @param messageNumber
    * @return
    * @throws java.io.IOException
    */
   @NonNull
   public Pop3Message getMessage(int messageNumber) throws IOException {
      List<Pop3Message> messages = getMessages();
      return messages.get(messageNumber - 1);
   }

   /**
    * Gets the total number of messages currently stored for this user.
    *
    * @return
    * @throws java.io.IOException
    */
   public long getMessageCount() throws IOException {
      return getMessages().size();
   }

   /**
    * Gets the total number of non deleted messages currently stored for this
    * user.
    *
    * @return
    * @throws java.io.IOException
    */
   public long getNumberOfNonDeletedMessages() throws IOException {
      return getNonDeletedMessages().size();
   }

   /**
    * Gets the total size of the non deleted messages currently stored for this
    * user.
    *
    * @return
    * @throws java.io.IOException
    */
   public long getSizeOfAllNonDeletedMessages() throws IOException {

      List<Pop3Message> messages = getNonDeletedMessages();
      long totalSize = 0;
      for (Pop3Message message : messages) {
         if (invalidate)
               throw new IOException("Local error in processing.");
         totalSize += message.getMessageSize(user);
      }
      return totalSize;
   }
}
