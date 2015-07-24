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

package com.ericdaugherty.mail.server.services.smtp;

//Java Imports
import com.ericdaugherty.mail.server.persistence.smtp.SMTPMessagePersistenceProcessor;
import java.io.IOException;
import java.util.List;

/**
 * A SMTP persistence processor interface.
 *
 * @author Andreas Kyrmegalos
 */
public interface PersistableSMTPMessage {
   
   public void setSMTPMessagePersistenceProcessor(SMTPMessagePersistenceProcessor processor);

   public void initializePersistedMessage(String filename, boolean headersOnly) throws IOException;
   
   public String getMessageLocation();

   public long getSize();

   public void addDataLine( byte[] line );

   public void save(boolean useAmavisSMTPDirectory) throws IOException;

   public boolean saveBegin(boolean useAmavisSMTPDirectory);

   public void saveIncrement(List<byte[]> dataLines, boolean writeHeaders, boolean append) throws IOException;

   public boolean saveFinish();

   public List<byte[]> loadIncrementally(int start) throws IOException;

   public List<byte[]> loadIncrementally(int start, String messageName) throws IOException;

   public void moveToFailedFolder() throws IOException;

   public boolean isNotSavedInAmavis();

   public long getPersistedSize();

   public Object getPersistedID();

   public boolean deleteMessage();

   // Handling of a special case where there is only one recipient, it belongs to a local domain and the user does
   // not exist. Message is resaved in the amavis.incoming.directory to be delivered immediatelly to the postmaster.
   public void redirectToPostmaster() throws IOException;

}
