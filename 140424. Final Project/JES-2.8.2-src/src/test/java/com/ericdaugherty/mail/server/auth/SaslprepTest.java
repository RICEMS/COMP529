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

package com.ericdaugherty.mail.server.auth;

import com.ericdaugherty.mail.server.auth.Saslprep.StringType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class SaslprepTest {
   
   private static final boolean checkBidi = true;
   private static final Saslprep.StringType stringType = StringType.STORED_STRING;
   private static final boolean includeUnassigned = false;
     
   @Test
   public void testPrepareString() throws SaslprepException {
      System.out.println("prepareString");
      
      //SOFT HYPHEN mapped to nothing (rfc 4013)
      String expResult = "IX";
      char[] input = "I\u00adX".toCharArray();
      char[] result = Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
      assertArrayEquals(String.format("Expected \"%s\", got \"%s\"", expResult, new String(result)).toString(), expResult.toCharArray(), result);
      
      //no transformation (rfc 4013)
      expResult = "user";
      input = "user".toCharArray();
      result = Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
      assertArrayEquals(String.format("Expected \"%s\", got \"%s\"", expResult, new String(result)), expResult.toCharArray(), result);
      
      //case preserved, will not match #2 (rfc 4013)
      expResult = "USER";
      input = "USER".toCharArray();
      result = Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
      assertArrayEquals(String.format("Expected \"%s\", got \"%s\"", expResult, new String(result)), expResult.toCharArray(), result);
      
      //output is NFKC, input in ISO 8859-1 (rfc 4013)
      expResult = "a";
      input = "\u00aa".toCharArray();
      result = Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
      assertArrayEquals(String.format("Expected \"%s\", got \"%s\"", expResult, new String(result)), expResult.toCharArray(), result);
      
      //output is NFKC, will match #1 (rfc 4013)
      expResult = "IX";
      input = "\u2168".toCharArray();
      result = Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
      assertArrayEquals(String.format("Expected \"%s\", got \"%s\"", expResult, new String(result)), expResult.toCharArray(), result);
      
      //output is correctly created when input containts non latin characters
      expResult = "\u4F60\u597D";//Ni hao (hello in simplified chinese)
      input = "\u4F60\u597D".toCharArray();
      result = Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
      assertArrayEquals(String.format("Expected \"%s\", got \"%s\"\n", expResult, new String(result)), expResult.toCharArray(), result);
      
   }
   
   @Test
   public void testPrepareStringSurrogates() throws SaslprepException {
      System.out.println("prepareStringSurrogates");
      
      //output is correctly created when input containts surrogates (all characters have bidirectional property "L")
      String expResult = "\u0041\uD800\uDF20\u0042";
      char[] input = "\u0041\uD800\uDF20\u0042".toCharArray();
      char[] result = Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
      assertArrayEquals(String.format("Expected \"%s\", got \"%s\"", expResult, new String(result)).toString(), expResult.toCharArray(), result);
      
      //output is correctly created when input containts surrogates (unassigned code points)
      expResult = "\u4F60\u597D";
      input = "\u4F60\uD800\uDC00\u597D".toCharArray();
      result = Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
      assertArrayEquals(String.format("Expected \"%s\", got \"%s\"", expResult, new String(result)).toString(), expResult.toCharArray(), result);
      
   }
   
   @Test(expected=SaslprepException.class)
   public void testPrepareStringExceptionCategories() throws SaslprepException {
      System.out.println("prepareStringExceptionCategories");
      
      //Error - check for mixed RandALCat and LCat characters
      char[] input = "\u06DD\u0042".toCharArray();
      Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
   }
   
   @Test(expected=SaslprepException.class)
   public void testPrepareStringExceptionProhibited() throws SaslprepException {
      System.out.println("prepareStringExceptionProhibited");
      
      //Error - prohibited character
      char[] input = "\u0007".toCharArray();
      Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
   }
   
   @Test(expected=SaslprepException.class)
   public void testPrepareStringExceptionBidiCheck() throws SaslprepException {
      System.out.println("prepareStringExceptionBidiCheck");
      
      //Error - bidirectional check
      char[] input = "\u0627\u0031".toCharArray();
      Saslprep.prepareString(input, checkBidi, stringType, includeUnassigned);
   }
}
