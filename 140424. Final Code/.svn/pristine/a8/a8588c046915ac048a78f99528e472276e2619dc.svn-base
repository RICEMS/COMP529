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

//Java Imports
import java.security.PrivilegedAction;
import javax.security.auth.Subject;
import javax.security.sasl.SaslException;

/**
 * This class is used in a GSS-API context to envelope a SMTP/POP server session
 * in a privileged block bound by (a) jgss subject(s).
 * 
 * @author Andreas Kyrmegalos
 */
public class AuthContext {

   private static volatile AuthContext instance;

   private final Subject[] subjects;

   private AuthContext(Subject[] subjects) {
      this.subjects = subjects;
   }

   public static AuthContext getInstance() {
      return instance;
   }

   public static AuthContext initialize(Subject[] subjects) {
      if (instance == null) {
         //this method is never called from more than one Threads, no worries.
         instance = new AuthContext(subjects);
      }
      return instance;
   }

   public synchronized GSSServerMode getGSSServerMode(final boolean smtp,
      final String clientIp) throws SaslException{

      //doAsPrivileged with null acc block in order to avoid security exception for "getClassLoader" 
      final ClassLoader cl = (ClassLoader)Subject.doAsPrivileged(
            ((smtp||subjects.length==1)?subjects[0]:subjects[1]),
            new PrivilegedAction<ClassLoader>(){
               
               @Override
               public ClassLoader run(){
                  ClassLoader cl = Thread.currentThread().getContextClassLoader();
                  return cl;
               }
            },
            null);
      try {
         @SuppressWarnings("unchecked")
         Class<GSSServerMode> c = (Class<GSSServerMode>)Class.forName("com.ericdaugherty.mail.server.auth.GSSServerMode", true, cl);
         GSSServerMode gssServerMode = c.getConstructor(Boolean.class).newInstance(smtp);
         java.lang.reflect.Method mainMethod = c.getMethod("negotiateGSSAuthenticationContext");
         mainMethod.invoke(gssServerMode);
         return gssServerMode;
      }
      catch(Exception e) {
         throw new SaslException(e.getLocalizedMessage());
      }
   }
   
   public String getSubjectName() {
      try {
         return subjects[0].getPrincipals().iterator().next().getName();
      }
      catch (NullPointerException npe) {
         return null;
      }
   }
   
   public String getOtherSubjectName() {
      try {
         if (subjects.length==2) {
            return subjects[1].getPrincipals().iterator().next().getName();
         }
         return null;
      }
      catch (NullPointerException npe) {
         return null;
      }
   }
}
