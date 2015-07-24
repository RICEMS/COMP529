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
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * This final class is used to setup the JAAS configuration system for
 * verifying Kerberos 5 credentials.
 *
 * @author Andreas Kyrmegalos
 */
public final class LoginConfig extends Configuration {
   
   private String name;
   private AppConfigurationEntry[] entry;

   public LoginConfig(String name, Map<String,String> options) {
      super();
      this.name = name;
      String principal = options.get("principal");
      if (principal.substring(0,principal.indexOf(':')).equals(principal.substring(principal.indexOf(':')+1,principal.indexOf('/')))) {
         principal = principal.substring(principal.indexOf(':')+1);
         options.put("principal", principal);
         this.entry = new AppConfigurationEntry[1];
         entry[0] = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
               AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
      }
      else {
         String smtp = principal.substring(0,principal.indexOf(':'));
         String pop3 = principal.substring(principal.indexOf(':')+1,principal.indexOf('/'));
         this.entry = new AppConfigurationEntry[2];
         String tempPrincipal = smtp+principal.substring(principal.indexOf('/'));
         options.put("principal", tempPrincipal);
         entry[0] = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
               AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
         tempPrincipal = pop3+principal.substring(principal.indexOf('/'));
         options = new HashMap<String,String>(options);
         options.put("principal", tempPrincipal);
         entry[1] = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
               AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);

      }

   }

   public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
      
      if (name.startsWith(this.name)) {
         if (entry.length==1) {
            return new AppConfigurationEntry[]{entry[0]};
         }
         else {
            String protocol = (String)entry[0].getOptions().get("principal");
            protocol = protocol.substring(0,protocol.indexOf('/'));
            if (name.endsWith(protocol)) {
               return new AppConfigurationEntry[]{entry[0]};
            }
            else {
               return new AppConfigurationEntry[]{entry[1]};
            }
         }
      }
      return null;
   }

   @Override
   public void refresh() {}

}
