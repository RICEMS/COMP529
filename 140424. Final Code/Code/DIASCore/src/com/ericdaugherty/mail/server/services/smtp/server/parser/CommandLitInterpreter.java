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

package com.ericdaugherty.mail.server.services.smtp.server.parser;

//Java imports
import java.util.regex.Matcher;

//Local imports
import com.ericdaugherty.mail.server.errors.SMTPReplyException;
import com.ericdaugherty.mail.server.services.smtp.server.SMTPServerSessionControl;
import com.ericdaugherty.mail.server.services.smtp.support.Utils;

/**
 *
 * @author Andreas Kyrmegalos
 */
public abstract class CommandLitInterpreter extends AbstractInterpreter {
   
   public CommandLitInterpreter(String regex) {
      super(regex);
   }

   @Override
   public String[] parseCommand(String line) throws SMTPReplyException {
      Matcher matcher = pattern.matcher(line);
      if (!matcher.find()) {
         throw new SMTPReplyException(SMTPServerSessionControl.MESSAGE_SYNTAX_ERROR_PARAMETER);
      }
      String[] argPar;
      if (matcher.groupCount() > 2) {
         argPar = new String[2];
         argPar[0] = matcher.group(2);
         argPar[1] = matcher.group(3);
      } else if (matcher.groupCount() > 1) {
         argPar = new String[1];
         argPar[0] = matcher.group(2);
      } else {
         return Utils.EMPTY_STRING_ARRAY;
      }
      return argPar;
   }
}
