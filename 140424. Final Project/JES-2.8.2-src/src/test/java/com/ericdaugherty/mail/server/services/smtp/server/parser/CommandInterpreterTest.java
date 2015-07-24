/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ericdaugherty.mail.server.services.smtp.server.parser;

import com.ericdaugherty.mail.server.errors.SMTPReplyException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class CommandInterpreterTest {
   
   public CommandInterpreterTest() {
   }

   @Test
   public void testParseCommand() throws Exception {
      System.out.println("parseCommand");
      String line;
      CommandInterpreter instance;
      String[] parseCommand;
      
      instance = new CommandLitInterpreterImpl(CommandInterpreter.AUTHRegex);
      line = "AUTH DIGEST-MD5 ";
      parseCommand = instance.parseCommand(line);
      assertEquals("DIGEST-MD5", parseCommand[0]);
      line = "AUTH DIGEST-MD5 =";
      parseCommand = instance.parseCommand(line);
      assertEquals("DIGEST-MD5", parseCommand[0]);
      assertEquals("=", parseCommand[1]);
      line = "AUTH DIGEST-MD5 abcd=";
      parseCommand = instance.parseCommand(line);
      assertEquals("DIGEST-MD5", parseCommand[0]);
      assertEquals("abcd=", parseCommand[1]);
      
      instance = new CommandLitInterpreterImpl(CommandInterpreter.MAILRegex);
      line = "MAIL FROM:<>";
      parseCommand = instance.parseCommand(line);
      assertEquals("<>", parseCommand[0]);
      line = "MAIL FROM:<andreas@localhost> ";
      parseCommand = instance.parseCommand(line);
      assertEquals("<andreas@localhost>", parseCommand[0]);
      line = "MAIL FROM:<@example.com:andreas@localhost>";
      parseCommand = instance.parseCommand(line);
      assertEquals("<@example.com:andreas@localhost>", parseCommand[0]);
      line = "MAIL FROM:<@example.com,@example2.com:andreas@localhost>";
      parseCommand = instance.parseCommand(line);
      assertEquals("<@example.com,@example2.com:andreas@localhost>", parseCommand[0]);
   }
   
   public class CommandLitInterpreterImpl extends CommandLitInterpreter {

      public CommandLitInterpreterImpl(String regex) {
         super(regex);
      }
   }
}
