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

package com.ericdaugherty.mail.server.services.smtp.server.command;

//Java imports
import java.io.IOException;
import java.util.Arrays;

//Local imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.*;
import com.ericdaugherty.mail.server.services.smtp.server.CommandVerb;
import com.ericdaugherty.mail.server.services.smtp.server.SMTPServerSessionControl;
import com.ericdaugherty.mail.server.services.smtp.server.action.*;
import com.ericdaugherty.mail.server.services.smtp.server.parser.CommandInterpreter;

/**
 * This class will check the prerequisites
 *
 * @author Andreas Kyrmegalos
 */
public abstract class AbstractCommand implements Command {

   /**
    * The ConfigurationManager
    */
   protected final ConfigurationManager configurationManager = ConfigurationManager.getInstance();
   private int hashCode = -1;
   protected CommandVerb commandVerb;
   protected final CommandVerb requiredCommandVerb;
   protected CommandInterpreter commandInterpreter;
   protected PreCommandAction preCommandAction = PreCommandAction.getDefaultAction();
   protected CommandAction commandAction = CommandAction.getDefaultAction();
   protected PostCommandAction postCommandAction = PostCommandAction.getDefaultAction();

   protected AbstractCommand(CommandVerb commandVerb, CommandVerb requiredCommandVerb, CommandInterpreter commandInterpreter) {
      this.commandVerb = commandVerb;
      this.requiredCommandVerb = requiredCommandVerb;
      this.commandInterpreter = commandInterpreter;
      if (commandVerb != null) {
         CommandVerb[] values = CommandVerb.values();
         hashCode = Arrays.asList(values).indexOf(commandVerb) / (values.length / 4);
      }
   }

   @Override
   public void checkInitPrerequisites(SMTPServerSessionControl control) throws SMTPReplyException {
   }

   @Override
   public void checkMailPrerequisites(SMTPServerSessionControl control) throws SMTPReplyException {
   }

   @Override
   public void parseInput(String line) throws SMTPReplyException {
      if (commandInterpreter == null) {
         throw new IllegalStateException("CommandInterpreter not set.");
      }
      String[] parseCommand = commandInterpreter.parseCommand(line);
   }
   
   @Override
   public void reset(){
      preCommandAction = PreCommandAction.getDefaultAction();
      commandAction = CommandAction.getDefaultAction();
      postCommandAction = PostCommandAction.getDefaultAction();
   }

   @Override
   public void resetParser() {
      commandInterpreter.reset();
   }
   
   @Override
   public void setNoEncryptedCommandActions() {
      preCommandAction = PreCommandAction.getDefaultAction();
      commandAction = CommandAction.getNoEncryptionAction();
      postCommandAction = PostCommandAction.getDefaultAction();
   }

   @Override
   public void executeActions(SMTPServerSessionControl control) throws SMTPReplyException, TooManyErrorsException, IOException {

      preCommandAction.execute(control);
      commandAction.execute(control);
      postCommandAction.execute(control);
   }

   @Override
   public CommandVerb getCommandVerb() {
      return commandVerb;
   }

   @Override
   public boolean isQuit() {
      return false;
   }

   public CommandVerb getRequiredCommandVerb() {
      return requiredCommandVerb;
   }

   public CommandInterpreter getCommandInterpreter() {
      return commandInterpreter;
   }

   protected PreCommandAction getPreCommandAction() {
      return preCommandAction;
   }

   protected void setPreCommandAction(PreCommandAction preCommandAction) {
      this.preCommandAction = preCommandAction;
   }

   protected CommandAction getCommandAction() {
      return commandAction;
   }

   protected void setCommandAction(CommandAction commandAction) {
      this.commandAction = commandAction;
   }

   protected PostCommandAction getPostCommandAction() {
      return postCommandAction;
   }

   protected void setPostCommandAction(PostCommandAction postCommandAction) {
      this.postCommandAction = postCommandAction;
   }

   @Override
   public int hashCode() {
      return hashCode;
   }

   @Override
   public boolean equals(Object object) {
      if (object == null) {
         return false;
      }
      if (!(object instanceof AbstractCommand)) {
         return false;
      }
      AbstractCommand that = (AbstractCommand) object;
      if ((this.commandVerb == null && that.commandVerb != null) || this.commandVerb != that.commandVerb) {
         return false;
      }
      return true;
   }
}
