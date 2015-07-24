/******************************************************************************
 * This program is a 100% Java Email Server.
 ******************************************************************************
 * Copyright (c) 2001-2013, Eric Daugherty (http://www.ericdaugherty.com)
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

//Java imports
import java.io.*;
import java.util.*;

//JUnit imports
import junit.framework.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class ConfigurationManagerMailTest extends TestCase{
   
   /**
    * The primary class responsible for executing the available tests
    *
    * @param testName
    * @throws java.io.IOException
    */
   public ConfigurationManagerMailTest( String testName ){
      super( testName );
   }

   public static Test suite() {
      return new TestSuite( ConfigurationManagerMailTest.class );
   }
   
   //Test the expected outcome
   public void testGetDefaultSMTPServer() {
      
      ConfigurationManagerMail cmm = new ConfigurationManagerMail();
      
      DefaultSMTPServer server;
      String test;
      
      //Only Server
      test = "mail.example.com";
      System.out.println("Testing a default SMTP server "+test);
      server = cmm.getDefaultSMTPServer(test);
      assertTrue(server.getHost().equals("mail.example.com"));
      assertTrue(server.getPort()==25);
      assertTrue(server.getRealm()==null);
      assertTrue(server.getUsername()==null);
      assertTrue(server.getPassword()==null);
      
      //Server & port
      test = "mail.example.com:45";
      System.out.println("Testing a default SMTP server "+test);
      server = cmm.getDefaultSMTPServer(test);
      assertTrue(server.getHost().equals("mail.example.com"));
      assertTrue(server.getPort()==45);
      assertTrue(server.getRealm()==null);
      assertTrue(server.getUsername()==null);
      assertTrue(server.getPassword()==null);
      
      //Server & realm
      test = "realm@example.com:mail.example.com";
      System.out.println("Testing a default SMTP server "+test);
      server = cmm.getDefaultSMTPServer(test);
      assertTrue(server.getHost().equals("mail.example.com"));
      assertTrue(server.getPort()==25);
      assertTrue(server.getRealm()==null);
      assertTrue(server.getUsername()==null);
      assertTrue(server.getPassword()==null);
      
      //Server & credentials
      test = "mail.example.com/user:pass";
      System.out.println("Testing a default SMTP server "+test);
      server = cmm.getDefaultSMTPServer(test);
      assertTrue(server.getHost().equals("mail.example.com"));
      assertTrue(server.getPort()==25);
      assertTrue(server.getRealm()==null);
      assertTrue(server.getUsername().equals("user"));
      assertTrue(server.getPassword().equals("pass"));
      
      //Server, realm & credentials
      test = "realm@example.com:mail.example.com/user:pass";
      System.out.println("Testing a default SMTP server "+test);
      server = cmm.getDefaultSMTPServer(test);
      assertTrue(server.getHost().equals("mail.example.com"));
      assertTrue(server.getPort()==25);
      assertTrue(server.getRealm().equals("realm@example.com"));
      assertTrue(server.getUsername().equals("user"));
      assertTrue(server.getPassword().equals("pass"));
      
      //Server, realm & port
      test = "realm@example.com:mail.example.com:45";
      System.out.println("Testing a default SMTP server "+test);
      server = cmm.getDefaultSMTPServer(test);
      assertTrue(server.getHost().equals("mail.example.com"));
      assertTrue(server.getPort()==45);
      assertTrue(server.getRealm()==null);
      assertTrue(server.getUsername()==null);
      assertTrue(server.getPassword()==null);
      
      //Server, realm, port & credentials
      test = "realm@example.com:mail.example.com:45/user:pass";
      System.out.println("Testing a default SMTP server "+test);
      server = cmm.getDefaultSMTPServer(test);
      assertTrue(server.getHost().equals("mail.example.com"));
      assertTrue(server.getPort()==45);
      assertTrue(server.getRealm().equals("realm@example.com"));
      assertTrue(server.getUsername().equals("user"));
      assertTrue(server.getPassword().equals("pass"));
      
   }

}
