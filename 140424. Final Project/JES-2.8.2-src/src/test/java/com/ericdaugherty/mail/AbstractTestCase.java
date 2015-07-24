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
 * $Rev: $
 * $Date: $
 *
 ******************************************************************************/

package com.ericdaugherty.mail;

//Java imports
import java.io.*;
import java.util.*;

/**
 *
 * @author Andreas Kyrmegalos
 */
public abstract class AbstractTestCase implements TestCase{
   
   protected final Object lock = new Object();
   
   protected final String name;
   protected final String server;
   protected final Properties userInfo;
   protected final Properties configurationProperties;
   protected final File tempJESDir;
   protected final PasswordAuthenticator senderCredentials;
   
   protected TestInstance testInstance;
   
   private static final String RANDOM_POOL = "qwertzuiopasdfghjklyxcvbnm0987612345MNBVCXYLKJHGFDSAPOIUZTREWQ";
   
   private final Random random = new Random();
   
   AbstractTestCase(String name, AbstractTestInstance testInstance) {
      this.testInstance = testInstance;
      
      this.name = name;
      this.server = testInstance.server;
      this.userInfo = testInstance.userInfo;
      this.configurationProperties = testInstance.configurationProperties;
      this.tempJESDir = testInstance.tempJESDir;
      this.senderCredentials = testInstance.senderCredentials;
   }
   
   protected String getRandomSequence() {
      StringBuilder sb = new StringBuilder(8);
      int index = 0;
      do {
         sb.append(RANDOM_POOL.charAt(random.nextInt(62)));
      }while(++index<8);
      return sb.toString();
   }
}
