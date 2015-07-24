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

//Java imports
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.LoggingPermission;
import javax.security.auth.AuthPermission;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.auth.AuthContext;
import com.ericdaugherty.mail.server.configuration.*;
import com.ericdaugherty.mail.server.utils.IOUtils;
import com.xlat4cast.jes.dns.Config;

/**
 *
 * @author Andreas Kyrmegalos
 */
public final class PolicyHandler {

   private final static Log log = LogFactory.getLog(PolicyHandler.class);
   private static final PolicyHandler instance = new PolicyHandler();

   public static final PolicyHandler getInstance() {
      return instance;
   }

   private PolicyHandler() {
   }

   public static final class PolicyEntryWrapper {

      private final String entry;

      public PolicyEntryWrapper(String entry) {
         this.entry = entry;
      }

      private String getEntry() {
         return entry;
      }
   }

   public void updatePolicyEntries() {

      List<ProtectionDomain> domains = getPolicyEntries();
      if (domains == null) {
         return;
      }
      PrintWriter pw = null;
      boolean renamed;
      try {
         Permission permission;
         File policyFile = new File(System.getProperty("java.security.policy"));
         File previousFile = new File(ConfigurationManager.getInstance().getRootDirectory(), "previous.policy");
         boolean previousExists = false;
         if (previousFile.exists()) {
            previousExists = previousFile.renameTo(new File(ConfigurationManager.getInstance().getRootDirectory(), "previous.policy.old"));
         }
         renamed = policyFile.renameTo(previousFile);
         if (!renamed) {
            log.warn("Unable to rename to 'previous.policy' file.");
            if (previousExists) {
               renamed = new File(ConfigurationManager.getInstance().getRootDirectory(), "previous.policy.old").renameTo(previousFile);
               log.warn("Unable to restore older 'previous.polica' file.");
            }
         } else {
            File file = new File(ConfigurationManager.getInstance().getRootDirectory(), "previous.policy.old");
            if (!file.delete()) {
               file.deleteOnExit();
            }
         }
         pw = new PrintWriter(new File(System.getProperty("java.security.policy")));

         ProtectionDomain general = domains.remove(0);
         pw.println("grant {");
         Enumeration<Permission> permissions = general.getPermissions().elements();
         while (permissions.hasMoreElements()) {
            permission = permissions.nextElement();
            pw.print("permission");
            pw.print(" ");
            pw.print(permission.getClass().getName());
            pw.print(" \"");
            pw.print(permission.getName());
            pw.println("\";");

         }
         pw.println("};");

         URL url;

         for (ProtectionDomain domain : domains) {
            url = domain.getCodeSource().getLocation();
            pw.print("grant codeBase \"");
            pw.print(url.getProtocol());
            pw.print(":");
            pw.print(url.getFile());
            pw.println("\" {");
            permissions = domain.getPermissions().elements();
            while (permissions.hasMoreElements()) {
               permission = permissions.nextElement();
               pw.print("permission");
               pw.print(" ");
               pw.print(permission.getClass().getName());
               if (permission instanceof java.security.AllPermission) {
                  pw.println(";");
                  continue;
               }
               pw.print(" \"");
               pw.print(permission.getName());
               pw.print("\"");
               if ((permission instanceof java.net.SocketPermission) || (permission instanceof java.io.FilePermission)
                     || (permission instanceof java.util.PropertyPermission)) {
                  pw.print(", \"");
                  pw.print(permission.getActions());
                  pw.print("\"");
               }
               pw.println(";");

            }
            pw.println("};");
         }

         pw.flush();
      } catch (FileNotFoundException e) {
         log.error("Unable to locate the policy file.", e);
         File policyFile = new File(ConfigurationManager.getInstance().getRootDirectory(), "previous.policy");
         if (policyFile.exists()) {
            renamed = policyFile.renameTo(new File(System.getProperty("java.security.policy")));
            if (!renamed) {
               log.info("Unable to restore 'previous.policy' file.");
            }
         }
      } finally {
         IOUtils.close(pw);
      }

   }

   private List<ProtectionDomain> getPolicyEntries() {

      try {

         List<ProtectionDomain> domains = new ArrayList<ProtectionDomain>();

         PermissionCollection socketHandlerPermissions = new Permissions();
         boolean socketHandler = false;

         ConfigurationManager cm = ConfigurationManager.getInstance();
         URL url;

         PermissionCollection pc = new Permissions();
         pc.add(new RuntimePermission("accessClassInPackage.sun.*"));
         pc.add(new RuntimePermission("accessClassInPackage.sun.security.*"));
         domains.add(new ProtectionDomain(new CodeSource(null, (Certificate[]) null), pc));

         pc = new Permissions();

         url = new URL("file", null, "${jes.install.directory}/jes.jar");

         pc.add(new RuntimePermission("shutdownHooks"));
         pc.add(new RuntimePermission("modifyThread"));
         pc.add(new RuntimePermission("loadLibrary.UnixUID"));
         pc.add(new RuntimePermission("readFileDescriptor"));
         pc.add(new RuntimePermission("writeFileDescriptor"));
         pc.add(new RuntimePermission("getenv.COMPUTERNAME"));
         pc.add(new RuntimePermission("getenv.HOSTNAME"));

         pc.add(new PropertyPermission("file.encoding", "read"));
         pc.add(new PropertyPermission("jes.*", "read, write"));
         pc.add(new PropertyPermission("jes.install.directory", "read"));
         pc.add(new PropertyPermission("derby.*", "read"));
         pc.add(new PropertyPermission("dns.*", "read"));
         pc.add(new PropertyPermission("log4j.configuration", "read"));
         pc.add(new PropertyPermission("org.apache.commons.logging.Log", "read"));
         pc.add(new PropertyPermission("maxOccurLimit", "read"));
         pc.add(new PropertyPermission("elementAttributeLimit", "read"));
         pc.add(new PropertyPermission("entityExpansionLimit", "read"));
         pc.add(new PropertyPermission("java.class.path", "read"));
         pc.add(new PropertyPermission("java.security.policy", "read"));
         pc.add(new PropertyPermission("java.net.preferIPv6Addresses", "read"));
         pc.add(new PropertyPermission("javax.net.debug", "read"));

         pc.add(new FilePermission("${jes.install.directory}", "read"));
         pc.add(new FilePermission("${jes.install.directory}${/}jes.policy", "read,write"));
         pc.add(new FilePermission("${jes.install.directory}${/}previous.policy", "read,write"));
         pc.add(new FilePermission("${jes.install.directory}${/}previous.policy.old", "read,write"));
         pc.add(new FilePermission("${jes.install.directory}${/}*", "read"));
         pc.add(new FilePermission("${jes.install.directory}${/}backup${/}*", "read,write,delete"));
         pc.add(new FilePermission("${jes.install.directory}${/}conf${/}*", "read,write"));
         pc.add(new FilePermission("${jes.install.directory}${/}external", "read"));
         pc.add(new FilePermission("${jes.install.directory}${/}external${/}*", "read"));
         pc.add(new FilePermission("${jes.install.directory}${/}logs${/}*", "read,write,delete"));
         pc.add(new FilePermission("${jes.install.directory}${/}security", "read"));
         pc.add(new FilePermission(cm.getKeystoreLocation().getEntry(), "read"));
         pc.add(new FilePermission(cm.getTruststoreLocation().getEntry(), "read"));
         pc.add(new FilePermission("${jes.install.directory}${/}security${/}realms.dat", "read,write"));
         pc.add(new FilePermission("${jes.users.directory}", "read,write,delete"));
         pc.add(new FilePermission("${jes.users.directory}${/}-", "read,write,delete"));
         pc.add(new FilePermission("${jes.incoming.directory}", "read,write,delete"));
         pc.add(new FilePermission("${jes.incoming.directory}${/}-", "read,write,delete"));
         pc.add(new FilePermission("${jes.failed.directory}", "read,write,delete"));
         pc.add(new FilePermission("${jes.failed.directory}${/}-", "read,write,delete"));
         pc.add(new FilePermission("${jes.testing.directory}", "read,write,delete"));
         pc.add(new FilePermission("${jes.testing.directory}${/}-", "read,write,delete"));

         pc.add(new LoggingPermission("control", null));

         pc.add(new SecurityPermission("getPolicy"));

         //any incoming connections
         pc.add(new SocketPermission("*", "accept"));
         //outgoing public smtp connections
         pc.add(new SocketPermission("*:25", "connect"));
         //outgoing public smtp secure connections
         pc.add(new SocketPermission("*:465", "connect"));
         //amavis
         if (cm.isAmavisSupportActive()) {
            boolean noLookup = false;
            InetAddress amavis = cm.getAmavisListenAddress();
            if (amavis.isAnyLocalAddress()||amavis.isLoopbackAddress()||amavis.isSiteLocalAddress()) {
               noLookup = true;
            }
            pc.add(new SocketPermission((noLookup?amavis.getHostAddress():amavis.getHostName())+":"+cm.getAmavisSMTPPort(), "connect"));
         }

         //access to mail.xsd
         pc.add(new SocketPermission("javaemailserver.sourceforge.net:80", "connect"));
         //The ISP provided SMTP relay servers should the outbound port 25 be blocked
         if (cm.isDefaultSmtpServerEnabled()) {
            for (DefaultSMTPServer entry : cm.getDefaultSmtpServers()) {
               pc.add(new SocketPermission(entry.getHost() + ":" + entry.getPort(), "connect"));
            }
         }
         //all the nameservers JES-DNS could use
         if (Config.getInstance().isAnyRecursiveMode()) {
            for (InetAddress server : Config.getInstance().getDefaultServers()) {
               pc.add(new SocketPermission(server.getHostAddress() + ":53", "connect"));
            }
         } else {
            pc.add(new SocketPermission("*:53", "connect"));
         }

         //The log4J network stream logging facility
         if (System.getProperty("org.apache.commons.logging.Log").equals("org.apache.commons.logging.impl.Log4JLogger")) {

            String log4jString = System.getProperty("log4j.configuration");
            File log4j = new File(log4jString.substring(log4jString.indexOf(':') + 1));
            if (log4j.exists()) {
               BufferedReader br = null;
               try {
                  String line;
                  String remoteHost = null;
                  int remotePort = 4650;
                  br = new BufferedReader(new FileReader(log4j));
                  while ((line = br.readLine()) != null) {
                     if (line.startsWith("log4j.appender.CHAINSAW_CLIENT")) {
                        socketHandler = true;
                        continue;
                     } else if (socketHandler) {
                        if (line.startsWith("log4j.appender.CHAINSAW_CLIENT.RemoteHost")) {
                           int indexOf = line.indexOf(".");
                           if (indexOf == line.length() - 1) {
                              continue;
                           } else {
                              remoteHost = line.substring(indexOf + 1);
                           }
                        } else if (line.startsWith("log4j.appender.CHAINSAW_CLIENT.Port")) {
                           int indexOf = line.indexOf(".");
                           if (indexOf == line.length() - 1) {
                              continue;
                           } else {
                              try {
                                 remotePort = Integer.parseInt(line.substring(indexOf + 1));
                              } catch (NumberFormatException nfe) {
                                 log.error("The chainsaw listening port is incorrectly specified.");
                              }
                           }
                        }
                     }
                  }
                  if (socketHandler) {
                     if (remoteHost == null) {
                        remoteHost = "localhost";
                     }
                     Permission permission;
                     permission = new SocketPermission(remoteHost + ":" + remotePort, "connect");
                     socketHandlerPermissions.add(permission);
                     pc.add(permission);
                     if (remoteHost.equals("localhost")) {
                        permission = new SocketPermission("127.0.0.1" + ":" + remotePort, "connect");
                        socketHandlerPermissions.add(permission);
                        pc.add(permission);
                     } else if (remoteHost.equals("127.0.0.1")) {
                        permission = new SocketPermission("localhost" + ":" + remotePort, "connect");
                        socketHandlerPermissions.add(permission);
                        pc.add(permission);
                     }
                  }
               } catch (IOException e) {
                  log.info("log4j configuration file not found. Policy entries will not be recorded.");
               } finally {
                  IOUtils.close(br);
               }
            }
         } else if (System.getProperty("org.apache.commons.logging.Log").equals("org.apache.commons.logging.impl.Jdk14Logger")) {

            String jdk14String = System.getProperty("java.util.logging.config.file");
            File jdk14 = new File(jdk14String);
            if (jdk14.exists()) {
               BufferedReader br = null;
               try {
                  String line;
                  String remoteHost = null;
                  int remotePort = 4650;
                  br = new BufferedReader(new FileReader(jdk14));
                  while ((line = br.readLine()) != null) {
                     if (line.startsWith("java.util.logging.SocketHandler")) {
                        socketHandler = true;
                        continue;
                     } else if (socketHandler) {
                        if (line.startsWith("java.util.logging.SocketHandler.host")) {
                           int indexOf = line.indexOf(".");
                           if (indexOf == line.length() - 1) {
                              continue;
                           } else {
                              remoteHost = line.substring(indexOf + 1);
                           }
                        } else if (line.startsWith("java.util.logging.SocketHandler.port")) {
                           int indexOf = line.indexOf(".");
                           if (indexOf == line.length() - 1) {
                              continue;
                           } else {
                              try {
                                 remotePort = Integer.parseInt(line.substring(indexOf + 1));
                              } catch (NumberFormatException nfe) {
                                 log.error("The socketHandler listening port is incorrectly specified.");
                              }
                           }
                        }
                     }
                  }
                  if (socketHandler) {
                     if (remoteHost == null) {
                        remoteHost = "localhost";
                     }
                     Permission permission;
                     permission = new SocketPermission(remoteHost + ":" + remotePort, "connect");
                     socketHandlerPermissions.add(permission);
                     pc.add(permission);
                     if (remoteHost.equals("localhost")) {
                        permission = new SocketPermission("127.0.0.1" + ":" + remotePort, "connect");
                        socketHandlerPermissions.add(permission);
                        pc.add(permission);
                     } else if (remoteHost.equals("127.0.0.1")) {
                        permission = new SocketPermission("localhost" + ":" + remotePort, "connect");
                        socketHandlerPermissions.add(permission);
                        pc.add(permission);
                     }
                  }
               } catch (IOException e) {
                  log.info("Jdk14Logger configuration file not found. Policy entries will not be recorded.");
               } finally {
                  IOUtils.close(br);
               }
            }
         }

         //access to the user/domain/realm database
         if (cm.getBackEndType() == BackEndType.RDBM) {
            pc.add(new PropertyPermission("derby.drda.host", "read"));
            pc.add(new PropertyPermission("derby.drda.portNumber", "read"));
            pc.add(new SocketPermission(System.getProperty("derby.drda.host") + ":" + System.getProperty("derby.drda.portNumber"), "connect"));

         }

         pc.add(new AuthPermission("doAsPrivileged"));
         pc.add(new AuthPermission("setLoginConfiguration"));

         if (cm.isGSSEnabled()) {

            pc.add(new PropertyPermission("java.security.krb5.realm", "read,write"));
            pc.add(new PropertyPermission("java.security.krb5.kdc", "read,write"));
            pc.add(new PropertyPermission("java.security.auth.login.config", "read,write"));
            pc.add(new PropertyPermission("sun.security.krb5.principal", "read,write"));
            pc.add(new AuthPermission("createLoginContext.com.ericdaugherty.mail.server.auth.GSSServerMode"));
            pc.add(new SocketPermission("*." + System.getProperty("java.security.krb5.realm") + ":88", "connect"));
            pc.add(new javax.security.auth.kerberos.ServicePermission(AuthContext.getInstance().getSubjectName(), "accept"));
            if (AuthContext.getInstance().getOtherSubjectName() != null) {
               pc.add(new javax.security.auth.kerberos.ServicePermission(AuthContext.getInstance().getOtherSubjectName(), "accept"));
            }
         }

         if (cm.isAmavisSupportActive()) {
            pc.add(new FilePermission("${jes.amavis.directory}", "read,write,delete"));
            pc.add(new FilePermission("${jes.amavis.directory}${/}-", "read,write,delete"));
            pc.add(new SocketPermission(cm.getAmavisListenAddress().getHostAddress() + cm.getAmavisSMTPPort(), "connect"));
         }

         domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));
         
         pc = new Permissions();
         pc.add(new RuntimePermission("modifyThread"));
         url = new URL("file", null, "${jes.install.directory}/lib/JES-DNS-1.0.jar");
         domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));

         if (cm.getBackEndType() == BackEndType.RDBM) {
            pc = new Permissions();

            pc.add(new SocketPermission(System.getProperty("derby.drda.host") + ":" + System.getProperty("derby.drda.portNumber"), "connect"));
            pc.add(new java.io.FilePermission("${jes.install.directory}", "read"));

            url = new URL("file", null, "${jes.install.directory}/lib/commons-dbcp-1.3.jar");
            domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));

            url = new URL("file", null, "${jes.install.directory}/lib/commons-dbcp-1.4.jar");
            domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));

            url = new URL("file", null, "${jes.install.directory}/lib/commons-pool-1.5.4.jar");
            domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));
         }

         pc = new Permissions();

         url = new URL("file", null, "${jes.install.directory}/lib/commons-logging-1.1.1.jar");

         pc.add(new FilePermission("${jes.install.directory}${/}*", "read"));
         pc.add(new FilePermission("${jes.install.directory}${/}logs${/}*", "read,write,delete"));

         domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));

         //----------------------------------------- log4J -----------------------------------------

         //Log4J class path not hardcoded, need to find it         
         String[] libs = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
         String fileSeparator = System.getProperty("file.separator");
         String log4jLib = null, temp;
         for (String lib : libs) {
            temp = lib.toLowerCase();
            if (temp.endsWith(".jar")) {
               temp = temp.substring(temp.lastIndexOf(fileSeparator)+1);
               if (temp.startsWith("log4j-")) {
                  log4jLib = lib.substring(lib.lastIndexOf(fileSeparator)+1);
                  break;
               }
            }
         }

         if (log4jLib!=null) {
            pc = new Permissions();

            url = new URL("file", null, "${jes.install.directory}/lib/"+log4jLib);

            pc.add(new FilePermission("${jes.install.directory}${/}*", "read"));
            pc.add(new FilePermission("${jes.install.directory}${/}conf${/}log4j.properties", "read"));
            pc.add(new FilePermission("${jes.install.directory}${/}logs${/}*", "read,write,delete"));
            if (socketHandler) {
               Enumeration<Permission> enums = socketHandlerPermissions.elements();
               while (enums.hasMoreElements()) {
                  pc.add(enums.nextElement());
               }
            }
            //all the nameservers DNS-JES could use
            if (Config.getInstance().isAnyRecursiveMode()) {
               for (InetAddress server : Config.getInstance().getDefaultServers()) {
                  pc.add(new SocketPermission(server.getHostAddress() + ":53", "connect"));
               }
            } else {
               pc.add(new SocketPermission("*:53", "connect"));
            }

            domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));
         }
         //----------------------------------------- log4J -----------------------------------------

         pc = new Permissions();

         url = new URL("file", null, "${jes.install.directory}/lib/unixUID-1.0.jar");

         pc.add(new RuntimePermission("loadLibrary.UnixUID"));

         domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));

         pc = new Permissions();

         url = new URL("file", null, "${jes.install.directory}/lib/wrapper.jar");

         pc.add(new AllPermission());

         domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));

         if (cm.getBackEndType() == BackEndType.RDBM) {

            pc = new Permissions();

            url = new URL("file", null, "${jes.install.directory}/lib/derby-10.10.1.1.jar");

            pc.add(new SocketPermission("127.0.0.1:*", "listen"));

            pc.add(new java.io.FilePermission("${jes.install.directory}", "read"));

            pc.add(new RuntimePermission("getenv.APPDATA"));
            pc.add(new RuntimePermission("createClassLoader"));
            pc.add(new PropertyPermission("derby.*", "read"));
            pc.add(new PropertyPermission("user.dir", "read"));
            pc.add(new PropertyPermission("derby.storage.jvmInstanceId", "write"));
            pc.add(new FilePermission("${derby.system.home}${/}-", "read,write,delete"));

            pc.add(new PropertyPermission("user.*", "read"));
            pc.add(new PropertyPermission("java.home", "read"));
            pc.add(new PropertyPermission("java.class.path", "read"));
            pc.add(new RuntimePermission("getProtectionDomain"));

            domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));

            pc = new Permissions();

            url = new URL("file", null, "${jes.install.directory}/lib/derbynet-10.10.1.1.jar");


            pc.add(new SocketPermission(System.getProperty("derby.drda.host") + ":*", "accept"));
            try {
               if (InetAddress.getByName(System.getProperty("derby.drda.host")).isLoopbackAddress()) {
                  pc.add(new SocketPermission("localhost:*", "accept"));
               }
            } catch (UnknownHostException e) {
               log.warn("Will not add the localhost to derbynet's Protection Domain.");
            }
            pc.add(new SocketPermission(System.getProperty("derby.drda.host") + ":" + System.getProperty("derby.drda.portNumber"), "connect"));
            //all the nameservers JES-DNS could use
            if (Config.getInstance().isAnyRecursiveMode()) {
               for (InetAddress server : Config.getInstance().getDefaultServers()) {
                  pc.add(new SocketPermission(server.getHostAddress() + ":53", "connect"));
               }
            } else {
               pc.add(new SocketPermission("*:53", "connect"));
            }
            pc.add(new RuntimePermission("getenv.APPDATA"));
            pc.add(new FilePermission("${derby.system.home}${/}-", "read,write,delete"));

            pc.add(new PropertyPermission("user.*", "read"));
            pc.add(new PropertyPermission("java.home", "read"));
            pc.add(new PropertyPermission("java.class.path", "read"));
            pc.add(new RuntimePermission("getProtectionDomain"));

            domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));

            pc = new Permissions();

            url = new URL("file", null, "${jes.install.directory}/lib/derbyclient-10.10.1.1.jar");

            pc.add(new SocketPermission(System.getProperty("derby.drda.host") + ":" + System.getProperty("derby.drda.portNumber"), "connect"));

            pc.add(new FilePermission("${derby.system.home}${/}-", "read,write,delete"));
            pc.add(new java.io.FilePermission("${jes.install.directory}", "read"));

            pc.add(new PropertyPermission("user.*", "read"));
            pc.add(new PropertyPermission("java.home", "read"));
            pc.add(new PropertyPermission("java.class.path", "read"));
            pc.add(new RuntimePermission("getProtectionDomain"));

            domains.add(new ProtectionDomain(new CodeSource(url, (Certificate[]) null), pc));
         }

         return domains;
      } catch (MalformedURLException e) {
         log.error(e.getLocalizedMessage(), e);
         return null;
      }
   }
}
