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

package com.ericdaugherty.mail.server.services.smtp.server.transaction;

//Java imports
import java.io.IOException;

//Local imports
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.errors.TooManyErrorsException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.services.smtp.server.SMTPServerSessionControl.ReplyWriter;

/**
 *
 * @author Andreas Kyrmegalos
 */
public interface TransferModeAction {

   void init(ReplyWriter replyWriter);

   /**
    * Handle the "MAIL FROM:" command, which defines the sending address for
    * this message. No checking for the validity of the sender's identity is
    * performed but should the domain be a registered local one then the
    * credentials are checked after the RCPT TO command(s) is (are) received.
    *
    * @param fromAddress
    * @param parameters
    * @return
    * @throws com.ericdaugherty.mail.server.errors.TooManyErrorsException
    * @throws com.ericdaugherty.mail.server.errors.InvalidAddressException
    * @throws java.io.IOException
    */
   boolean handleMailFrom(String fromAddress, String[] parameters)
         throws TooManyErrorsException, IOException, InvalidAddressException;

   /**
    * Handle the "RCPT TO:" command, which defines one of the receiving
    * addresses.
    *
    * @param address
    * @param parameters
    * @throws com.ericdaugherty.mail.server.errors.TooManyErrorsException
    * @throws java.io.IOException
    * @throws com.ericdaugherty.mail.server.errors.InvalidAddressException
    */
   void handleRcptTo(EmailAddress address, String[] parameters)
         throws TooManyErrorsException, IOException, InvalidAddressException;
}
