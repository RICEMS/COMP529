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

package com.ericdaugherty.mail.server.security;

//Java Imports
import java.net.SocketPermission;
import java.security.*;
import java.util.Map;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager.ProcessPortMapping;

/**
 * The JES extended Security Manager. Supports port listen permission checking.
 *
 * @author Andreas Kyrmegalos
 */
public final class JESSecurityManager extends SecurityManager{

   private static final Log log = LogFactory.getLog( JESSecurityManager.class );

   private AccessControlContext acc;
   
   private ProcessPortMapping ppm;

   public JESSecurityManager() {
      updateMappedPorts();
   }
   
   public synchronized final void getTemporaryMappedPort() {
      ppm = ConfigurationManager.getInstance().getTemporaryMappedPort();
   }
   
   public synchronized final void removeTemporaryPortMapping() {
      ppm = null;
   }

   public final void updateMappedPorts() {
      Map<String,Integer> mappedPorts = ConfigurationManager.getInstance().getMappedPorts();
      PermissionCollection pc = new Permissions();
      if (mappedPorts.containsKey("smtp")) {
         pc.add(new SocketPermission("localhost:"+mappedPorts.get("smtp"), "listen"));
      }
      if (mappedPorts.containsKey("pop3")) {
         pc.add(new SocketPermission("localhost:"+mappedPorts.get("pop3"), "listen"));
      }
      if (mappedPorts.containsKey("secureSmtp")) {
         pc.add(new SocketPermission("localhost:"+mappedPorts.get("secureSmtp"), "listen"));
      }
      if (mappedPorts.containsKey("securePop3")) {
         pc.add(new SocketPermission("localhost:"+mappedPorts.get("securePop3"), "listen"));
      }
      if (mappedPorts.containsKey("amavis")) {
         pc.add(new SocketPermission("localhost:"+mappedPorts.get("amavis"), "listen"));
      }
      if (mappedPorts.containsKey("testing")) {
         pc.add(new SocketPermission("localhost:"+mappedPorts.get("testing"), "listen"));
      }
      if (mappedPorts.containsKey("config")) {
         pc.add(new SocketPermission("localhost:"+mappedPorts.get("config"), "listen"));
      }
      
      synchronized (this) {
         acc = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, pc)});
      }
   }

   /**
    * Unfortunately there is a known bug when checking for a bind
    * permission to a local address other than IPv4 localhost. Please
    * see bug report at http://bugs.sun.com/view_bug.do?bug_id=6533031
    */
   public void checkListen(final int port) {
      if (log.isDebugEnabled()) {
         log.debug("Checking SocketPermission: localhost:"+port+" listen");
      }
      try{
         synchronized(this) {
            if (ppm!=null) {
               try {
                  PermissionCollection pc = new Permissions();
                  pc.add(new SocketPermission("localhost:"+ppm.getPort(), "listen"));
                  new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, pc)}).
                        checkPermission(new SocketPermission("localhost:"+port, "listen"));
               }
               catch (AccessControlException se) {
                  acc.checkPermission(new SocketPermission("localhost:"+port, "listen"));
               }
            }
            else {
               acc.checkPermission(new SocketPermission("localhost:"+port, "listen"));
            }
         }
      } catch(AccessControlException ace) {
         super.checkListen(port);
      }
   }
}
