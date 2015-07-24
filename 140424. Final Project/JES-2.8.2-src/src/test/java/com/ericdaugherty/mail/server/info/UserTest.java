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

package com.ericdaugherty.mail.server.info;

//Java imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import java.io.IOException;
import java.util.Locale;

//JUnit imports
import junit.framework.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class UserTest extends TestCase {
   
   /**
    * The primary class responsible for executing the available tests
    *
    * @param testName
    * @throws java.io.IOException
    */
   public UserTest( String testName ) throws IOException{
      super( testName );
   }

   public static Test suite() {
      return new TestSuite( UserTest.class );
   }
   
   private static class TestUser extends AbstractUser {

      public TestUser(EmailAddress emailAddress) {
         super(emailAddress);
      }

      @Override
      public boolean isPasswordValid(char[] plainTextPassword) {
         return false;
      }
   }

   public void test2() throws Exception {
      
      String localPart = "testMe";
      String domain = "exaMPle.com";
      EmailAddress emailAddress = EmailAddress.getEmailAddress(localPart+'@'+domain);
      
      TestUser testUser = new TestUser(emailAddress);
      
      //Unlike the EmailAddress class, the localPart is case-insesitive
      
      //Local part letter casing. Case-insensitive. Positive case
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart+'@'+domain))));
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart.toUpperCase(ConfigurationManager.LOCALE)+'@'+domain))));
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart.toLowerCase(ConfigurationManager.LOCALE)+'@'+domain))));
      
      //Domain letter casing. Case-insensitive. Positive cases
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart+'@'+domain.toUpperCase(ConfigurationManager.LOCALE)))));
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart+'@'+domain.toLowerCase(ConfigurationManager.LOCALE)))));
      
      //Combined letter casing. Case-insensitive. Positive cases
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart.toUpperCase(ConfigurationManager.LOCALE)+'@'+domain.toUpperCase(ConfigurationManager.LOCALE)))));
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart.toUpperCase(ConfigurationManager.LOCALE)+'@'+domain.toLowerCase(ConfigurationManager.LOCALE)))));
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart.toLowerCase(ConfigurationManager.LOCALE)+'@'+domain.toUpperCase(ConfigurationManager.LOCALE)))));
      assertTrue(testUser.equals(new TestUser(new EmailAddress(localPart.toLowerCase(ConfigurationManager.LOCALE)+'@'+domain.toLowerCase(ConfigurationManager.LOCALE)))));
      
   }
}
