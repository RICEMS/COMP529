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

package com.ericdaugherty.mail.server.services.smtp.server.support;

//Java Imports
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//Local Imports
import com.ericdaugherty.mail.server.services.smtp.SMTPMessage;

/**
 * The basic class that adds data to an incoming stream.
 *
 * @author Andreas Kyrmegalos
 */
public class AddDataLineDefault implements AddDataLine{

   protected SMTPMessage message;
   private List<byte[]> stringLines = new ArrayList<byte[]>(250);
   private boolean writeHeaders = true;

   public AddDataLineDefault(SMTPMessage message) {
      this.message = message;
   }

   public void addDataLine(byte[] line) throws IOException{
      stringLines.add(line);
      //The message size is defined as the number of octets, including CR-LF pairs
      message.incrementSize(line.length + 2);
      if (stringLines.size()==250) {
         message.saveIncrement(stringLines, writeHeaders, true);
         writeHeaders = false;
         stringLines.clear();
      }
   }

   public void flush() throws IOException{
      if (stringLines.size()>0) {
         message.saveIncrement(stringLines, writeHeaders, true);
      }
      stringLines.clear();
      stringLines = null;
   }
}
