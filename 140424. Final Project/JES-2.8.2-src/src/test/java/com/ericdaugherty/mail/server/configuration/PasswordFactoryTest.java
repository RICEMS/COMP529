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
import java.io.IOException;
import java.util.*;

//JUnit imports
import junit.framework.*;

//Local imports
import com.ericdaugherty.mail.server.info.Realm;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class PasswordFactoryTest extends TestCase {
   
   /**
    * The primary class responsible for executing the available tests
    *
    * @param testName
    * @throws java.io.IOException
    */
   public PasswordFactoryTest( String testName ) throws IOException{
      super( testName );
   }

   public static Test suite() {
      return new TestSuite( PasswordFactoryTest.class );
   }
   
   /**
    * Standard password test facility
    *
    * @throws java.lang.Exception
    */
   public void test1() throws Exception {
      
      Random random = new Random();
      char[] password = new char[10];
      for (int i=0;i<10;i++) {
         password[i] = (char)(65+random.nextInt(25));
      }
      
      PasswordFactory.shutdown();
      PasswordFactory.instantiate(BackEndType.FILE);
      
      char[] hashedPassword = PasswordFactory.getInstance().getPasswordHasher().hashPassword(password);
      
      char[] testPassword = password.clone();
      
      assertTrue(PasswordFactory.getInstance().getPasswordHasher().passwordMatches(hashedPassword, testPassword));
      assertFalse(PasswordFactory.getInstance().getPasswordHasher().passwordMatches(password, testPassword));
      
      char[] encPassword = new char[15];
      System.arraycopy("{ENC}".toCharArray(), 0, encPassword, 0, 5);
      System.arraycopy(password, 0, encPassword,5, 10);
      assertTrue(PasswordFactory.getInstance().getPasswordHasher().passwordMatches(encPassword, testPassword));
      
      testPassword[5] = Character.toLowerCase(testPassword[5]);
      
      assertFalse(PasswordFactory.getInstance().getPasswordHasher().passwordMatches(hashedPassword, testPassword));
      assertFalse(PasswordFactory.getInstance().getPasswordHasher().passwordMatches(password, testPassword));
      
      encPassword = new char[15];
      System.arraycopy("{ENC}".toCharArray(), 0, encPassword, 0, 5);
      System.arraycopy(password, 0, encPassword,5, 10);
      assertFalse(PasswordFactory.getInstance().getPasswordHasher().passwordMatches(encPassword, testPassword));
      
      for (int i=0;i<10;i++) {
         password[i] = (char)(65+random.nextInt(25));
      }
      
      PasswordFactory.shutdown();
      PasswordFactory.instantiate(BackEndType.RDBM);
      
      PasswordHasher ph = PasswordFactory.getInstance().getPasswordHasher();
      hashedPassword = ph.hashPassword(password);
      //A salt is generated after the previous call
      
      testPassword = password.clone();
      
      assertTrue(ph.passwordMatches(hashedPassword, testPassword));
      assertTrue(ph.passwordMatches(password, testPassword));
      
      testPassword[5] = Character.toLowerCase(testPassword[5]);
      
      assertFalse(ph.passwordMatches(hashedPassword, testPassword));
      assertFalse(ph.passwordMatches(password, testPassword));
      
      PasswordFactory.shutdown();
   }
   
   /** 
    * Realm password test facility
    *
    * @throws java.lang.Exception
    */
   public void test2() throws Exception {
      
      String username = "useRName";
      
      String domainName = "exaMPle.com";
      Domain domain = new Domain(domainName);
      
      String collection = "usERs";
      Realm realm = new Realm(collection, domain);
      
      Random random = new Random();
      char[] password = new char[10];
      for (int i=0;i<10;i++) {
         password[i] = (char)(65+random.nextInt(25));
      }
      
      PasswordFactory.shutdown();
      //The backend is irrelevant
      PasswordFactory.instantiate(BackEndType.FILE);
      
      char[] realmPassword = PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(username, realm, password);
      
      String testUsername = username;
      
      String testCollection = collection;
      Realm testRealm = new Realm(testCollection, domain);
      
      char[] testRealmPAssword = PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(testUsername, testRealm, password);
      
      assertTrue(Arrays.equals(realmPassword, testRealmPAssword));
      
      String compUsername = username.toLowerCase(ConfigurationManager.LOCALE);
      
      Domain compDomain = new Domain(domainName.toLowerCase(ConfigurationManager.LOCALE));
      
      String compCollection = collection.toLowerCase(ConfigurationManager.LOCALE);
      Realm compRealm = new Realm(compCollection, compDomain);
      
      //This is how the realm password was generated in prior versions.
      char[] compRealmPassword = PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(compUsername, compRealm, password);
      
      assertFalse(Arrays.equals(realmPassword, compRealmPassword));
      
      testUsername = username.toLowerCase(ConfigurationManager.LOCALE);
      
      testCollection = collection.toLowerCase(ConfigurationManager.LOCALE);
      testRealm = new Realm(testCollection, domain);
      
      testRealmPAssword = PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(testUsername, testRealm, password);
      
      assertTrue(Arrays.equals(compRealmPassword, testRealmPAssword));
      
   }
}
