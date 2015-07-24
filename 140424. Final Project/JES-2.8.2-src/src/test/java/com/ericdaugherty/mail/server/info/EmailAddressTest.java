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

//Local import
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import java.io.IOException;
import java.util.Locale;

//JUnit imports
import junit.framework.*;

//Local Imports
import com.ericdaugherty.mail.server.errors.InvalidAddressException;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class EmailAddressTest extends TestCase {
   
   /**
    * The primary class responsible for executing the available tests
    *
    * @param testName
    * @throws java.io.IOException
    */
   public EmailAddressTest( String testName ) throws IOException{
      super( testName );
   }

   public static Test suite() {
      return new TestSuite( EmailAddressTest.class );
   }

   /**
    * Tests the local-part and domain parsing
    *
    * @throws java.lang.Exception
    */
   public void test1() throws Exception {

      EmailAddress address = new EmailAddress();

      String[] usernameFailTests = new String[]{
         "\"cd32dd2", "\"vfecw\\\"","\"\\a\\b\\c\\\u0000\"","\"\\\u0019\"",
         "\"\u0019\"","\"f\\\u0019\"", "\"\\\"",
         ".c3dx22x2", "abc..","asdcd\\dvcd", "wdcv\"wecw", "cc(", "fgsfc\u0019g"
      };
      for (int i=0;i<usernameFailTests.length;i++) {
         try {
            address.parseLocalPartRFC5321(usernameFailTests[i]);
            System.out.println("test "+i+" failed "+usernameFailTests[i]);
            assertTrue(false);
         }
         catch (InvalidAddressException iae) {
            System.out.println("test "+i+" passed "+usernameFailTests[i]+" "+iae.getMessage());
         }
      }
      String[] usernamePassTests = new String[]{
         "\"f\\y\"","\"\\ghdbds\"","\"ASDSW34dwcd!#$%^&*-+=_`{|}~.\"", "ASDSW34dwcd.!#$%^&*-+=_`{|}~", "\"abc\\\"def\"", "\"abc\\\\def\""
      };
      for (int i=0;i<usernamePassTests.length;i++) {
         try {
            System.out.println("test "+i+" passed "+usernamePassTests[i]+" -> "+address.parseLocalPartRFC5321(usernamePassTests[i]));
         }
         catch (InvalidAddressException iae) {
            System.out.println("test "+i+" failed "+usernamePassTests[i]);
            assertTrue(false);
         }
      }
      String[] domainFailTests = new String[]{
         "com", "efc;ln", "e_xample.com", "-example.com", "example-.com", "$.example.com", "ex.exa@mple.com"
      };
      for (int i=0;i<domainFailTests.length;i++) {
         try {
            address.parseDomainRFC5321(domainFailTests[i]);
            System.out.println("test "+i+" failed "+domainFailTests[i]);
            assertTrue(false);
         }
         catch (InvalidAddressException iae) {
            System.out.println("test "+i+" passed "+domainFailTests[i]+" "+iae.getMessage());
         }
      }
      String[] domainPassTests = new String[]{
         "LOCALhost", "example.com", "mo-re.exam-ple.com"
      };
      for (int i=0;i<domainPassTests.length;i++) {
         try {
            address.parseDomainRFC5321(domainPassTests[i]);
            System.out.println("test "+i+" passed "+domainPassTests[i]);
         }
         catch (InvalidAddressException iae) {
            System.out.println("test "+i+" failed "+domainPassTests[i]);
            assertTrue(false);
         }
      }
   }

   /**
    * Tests the letter casing
    *
    * @throws java.lang.Exception
    */
   public void test2() throws Exception {
      
      String localPart = "testMe";
      String domain = "exaMPle.com";
      EmailAddress emailAddress = EmailAddress.getEmailAddress(localPart+'@'+domain);
      
      //Local part letter casing. Case-sensitive. Positive case
      assertTrue(emailAddress.equals(new EmailAddress(localPart+'@'+domain)));
      
      //Local part letter casing. Case-sensitive. Negative cases
      assertFalse(emailAddress.equals(new EmailAddress(localPart.toUpperCase(ConfigurationManager.LOCALE)+'@'+domain)));
      assertFalse(emailAddress.equals(new EmailAddress(localPart.toLowerCase(ConfigurationManager.LOCALE)+'@'+domain)));
      
      //Domain letter casing. Case-insensitive. Positive cases
      assertTrue(emailAddress.equals(new EmailAddress(localPart+'@'+domain.toUpperCase(ConfigurationManager.LOCALE))));
      assertTrue(emailAddress.equals(new EmailAddress(localPart+'@'+domain.toLowerCase(ConfigurationManager.LOCALE))));
      
      //Combined letter casing. Mixed casing. Negative cases
      assertFalse(emailAddress.equals(new EmailAddress(localPart.toUpperCase(ConfigurationManager.LOCALE)+'@'+domain.toUpperCase(ConfigurationManager.LOCALE))));
      assertFalse(emailAddress.equals(new EmailAddress(localPart.toUpperCase(ConfigurationManager.LOCALE)+'@'+domain.toLowerCase(ConfigurationManager.LOCALE))));
      assertFalse(emailAddress.equals(new EmailAddress(localPart.toLowerCase(ConfigurationManager.LOCALE)+'@'+domain.toUpperCase(ConfigurationManager.LOCALE))));
      assertFalse(emailAddress.equals(new EmailAddress(localPart.toLowerCase(ConfigurationManager.LOCALE)+'@'+domain.toLowerCase(ConfigurationManager.LOCALE))));
      
   }

}
