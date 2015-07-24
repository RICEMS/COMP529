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

//Java import
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import java.io.IOException;
import java.util.Locale;

//Local import
import com.xlat4cast.jes.dns.internal.Domain;

//JUnit imports
import junit.framework.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class RealmTest extends TestCase {
   
   /**
    * The primary class responsible for executing the available tests
    *
    * @param testName
    * @throws java.io.IOException
    */
   public RealmTest( String testName ) throws IOException{
      super( testName );
   }

   public static Test suite() {
      return new TestSuite( RealmTest.class );
   }
   
   public void test2() throws Exception {
            
      String domainName = "exaMPle.com";
      Domain domain = new Domain(domainName);
      
      String collection = "usERs";
      Realm realm = new Realm(collection, domain);
      
      //collection letter casing. Case-sensitive. Positive case
      assertTrue(realm.equals(new Realm(collection, domain)));
      
      //collection letter casing. Case-sensitive. Negative cases
      assertFalse(realm.equals(new Realm(collection.toUpperCase(ConfigurationManager.LOCALE), domain)));
      assertFalse(realm.equals(new Realm(collection.toLowerCase(ConfigurationManager.LOCALE), domain)));
      
      //Domain letter casing. Case-insensitive. Positive cases
      assertTrue(realm.equals(new Realm(collection, new Domain(domainName.toUpperCase(ConfigurationManager.LOCALE)))));
      assertTrue(realm.equals(new Realm(collection, new Domain(domainName.toLowerCase(ConfigurationManager.LOCALE)))));
      
      //Combined letter casing. Mixed casing. Negative cases
      assertFalse(realm.equals(new Realm(collection.toUpperCase(ConfigurationManager.LOCALE), new Domain(domainName.toUpperCase(ConfigurationManager.LOCALE)))));
      assertFalse(realm.equals(new Realm(collection.toUpperCase(ConfigurationManager.LOCALE), new Domain(domainName.toLowerCase(ConfigurationManager.LOCALE)))));
      assertFalse(realm.equals(new Realm(collection.toLowerCase(ConfigurationManager.LOCALE), new Domain(domainName.toUpperCase(ConfigurationManager.LOCALE)))));
      assertFalse(realm.equals(new Realm(collection.toLowerCase(ConfigurationManager.LOCALE), new Domain(domainName.toLowerCase(ConfigurationManager.LOCALE)))));
      
   }
}
