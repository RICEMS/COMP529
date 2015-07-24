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

package com.ericdaugherty.mail.server.configuration;

//Java imports
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.Policy;
import java.util.HashMap;
import java.util.Map;
import javax.net.SocketFactory;
import org.w3c.dom.Element;

//Log imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.security.PolicyHandler;
import com.ericdaugherty.mail.server.utils.IOUtils;

/**
 *
 * @author Andreas Kyrmegalos
 */
final class ConfigurationManagerAmavis implements ConfigurationParameterConstants {

   /**
    * Logger
    */
   private static final Log log = LogFactory.getLog(ConfigurationManager.class);
   private final ConfigurationManager cm = ConfigurationManager.getInstance();

   Map<String, String> getConfiguration() {

      Map<String, String> configuration = new HashMap<String, String>();

      configuration.put("dirRoot", cm.getRootDirectory());
      configuration.put("amavisSupportActive", Boolean.toString(amavisSupportActive) + RESTART);
      configuration.put("listenAddress", cm.getListenAddress().getHostAddress());
      configuration.put("amavisListenAddress", amavisListenAddress.getHostAddress() + RESTART);
      configuration.put("smtpPort", cm.getSMTPPort() + "");
      configuration.put("amavisSMTPPort", amavisSMTPPort + RESTART);
      configuration.put("amavisSMTPPortMax", "65535");
      configuration.put("amavisSMTPFilteredPort", amavisFilteredSMTPPort + RESTART);
      configuration.put("amavisSMTPFilteredPortMin", String.valueOf((!ConfigurationManager.isWin() && com.xlat4cast.mail.server.UnixUID.getUID() != 0) ? 1025 : 1));
      configuration.put("amavisSMTPFilteredPortMax", "65535");

      String dir = getAmavisSMTPDirectory();
      if (dir == null) {
         dir = "";
      } else if (dir.equals(cm.getRootDirectory() + File.separator + "amavisd")) {
         dir = "using default directory";
      }
      configuration.put("amavisSMTPFilteredDir", dir + RESTART);

      configuration.put("fileSeparator", File.separator);
      configuration.put("allowRemoteRestart", Boolean.toString(cm.isAllowRemoteRestart()));

      return configuration;
   }

   public ConfigurationManagerAmavis() {

      //The amavis directory property is always given a value to avoid possible security issues
      System.setProperty("jes.amavis.directory", cm.getSMTPDirectory());
   }

   void initTesting() {
      File testingDir = new File(cm.getRootDirectory(), "testing");
      if (!testingDir.exists()) {
         boolean dirMade;
         dirMade = testingDir.mkdir();
         if (!dirMade) {
            log.error("Unable to create the testing directory. Aborting...");
            throw new RuntimeException("Unable to create the testing directory. Aborting...");
         }
      }
      amavisSMTPDirectory = testingDir.getPath();
   }
   /**
    * A flag for amavisd-new support.
    */
   private boolean amavisSupportActive;
   /**
    * The local IP address amavisd-new uses to listen on. Null for all addresses
    */
   private InetAddress amavisListenAddress;
   /**
    * The port amavisd-new listens on.
    */
   private int amavisSMTPPort;
   /**
    * The port JES uses to listens on for amavisd-new filtered messages.
    */
   private int amavisFilteredSMTPPort;
   /**
    * The Transmitting MTA's SMTP directory.
    */
   private String amavisSMTPDirectory;

   public boolean isAmavisSupportActive() {
      return amavisSupportActive;
   }

   public InetAddress getAmavisListenAddress() {
      return amavisListenAddress;
   }

   public int getAmavisSMTPPort() {
      return amavisSMTPPort;
   }

   public int getAmavisFilteredSMTPPort() {
      return amavisFilteredSMTPPort;
   }

   public String getAmavisSMTPDirectory() {
      return amavisSMTPDirectory;
   }

   private void setAmavisSMTPDirectory(final String amavisSMTPDirectory) {
      this.amavisSMTPDirectory = amavisSMTPDirectory;
      File amavisDir = new File(amavisSMTPDirectory);
      // If the directory does not exist, create it.
      if (!amavisDir.exists()) {
         cm.requestDirCreation("amavis");
      }
      System.setProperty("jes.amavis.directory", amavisSMTPDirectory);
   }

   void amavisdNewConfiguration(Element element) {

      if (!cm.isFixed()) {

         amavisSupportActive = Boolean.parseBoolean(element.getAttribute("enable"));

         String amavisSMTPDirectory = ((Element) element.getElementsByTagName("TXDirectory").item(0)).getTextContent();
         if (amavisSMTPDirectory == null || amavisSMTPDirectory.trim().length() == 0) {

            amavisSMTPDirectory = cm.getRootDirectory() + File.separator + "amavisd";
         } else {
            amavisSMTPDirectory = amavisSMTPDirectory.trim();
         }

         setAmavisSMTPDirectory(amavisSMTPDirectory);

         //
         // Load the address JES will listen on for incoming traffic from amavisd-new
         //
         amavisListenAddress = Utils.getAllowedAddress("config.amavis-dnew.listenAddress", element.getAttribute("listenAddress"), true, true);
         
         amavisSMTPPort = Utils.parsePort(element.getAttribute("listenPort"), 10024);
         amavisFilteredSMTPPort = Utils.parsePort(((Element) element.getElementsByTagName("TXPort").item(0)).getFirstChild().getNodeValue(), 10025);

         if (!amavisSMTPDirectory.equals(cm.getSMTPDirectory()) && amavisSupportActive) {

            //Check that an instance of Amavis is actually running
            Socket socket = null;
            try {
               if (System.getSecurityManager()!=null) {
                  PolicyHandler.getInstance().updatePolicyEntries();
                  Policy.getPolicy().refresh();
               }
               socket = SocketFactory.getDefault().createSocket(amavisListenAddress, amavisSMTPPort);

               if (log.isDebugEnabled())
                  log.debug("Setting Transmitting MTA's directory to " + amavisSMTPDirectory);
               log.info("Successfully setup JES as a dual MTA. Amavis is listening on "
                     + (amavisListenAddress == null
                     ? "0.0.0.0"
                     : amavisListenAddress.getHostAddress())
                     + ":" + amavisSMTPPort + ", JES is listening on " + (cm.getListenAddress() == null
                     ? "0.0.0.0"
                     : cm.getListenAddress().getHostAddress()) + ":" + amavisFilteredSMTPPort);

            } catch (IOException ioe) {
               amavisSupportActive = false;
               cm.registerConfigDeviations("amavisSupportActive", "false");
               log.info("Amavis support requested, but no running instance of Amavis was detected. Ignoring...");
               
            } finally {
               IOUtils.close(socket);
            }
         }
         else {
            amavisSupportActive = false;
            cm.registerConfigDeviations("amavisSupportActive", "false");
            if (amavisSMTPDirectory.equals(cm.getSMTPDirectory()))
               log.error("The Transmiting MTA can't share the same directory with the Receiving MTA.");
            else
               log.info("Amavis support not requested.");
         }
      }
   }
}
