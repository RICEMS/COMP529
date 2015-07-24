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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andreas Kyrmegalos
 */
public enum ConfigType {
   
   GENERAL("ConfigGeneral") {

      @Override
      public Map<String, String> getConfigurationMap() {
         if (log.isDebugEnabled())
            log.debug("General settings requested");
         return ConfigurationManager.getInstance().getGeneralConfiguration();
      }
      
   },
   BACKEND("ConfigBackend") {

      @Override
      public Map<String, String> getConfigurationMap() {
         if (log.isDebugEnabled())
            log.debug("Backend settings requested");
         return ConfigurationManager.getInstance().getBackEndConfiguration();
      }
      
   },
   MAIL("ConfigMail") {

      @Override
      public Map<String, String> getConfigurationMap() {
         if (log.isDebugEnabled())
            log.debug("Mail settings requested");
         return ConfigurationManager.getInstance().getMailConfiguration();
      }
      
   },
   DIR("ConfigDirectories") {

      @Override
      public Map<String, String> getConfigurationMap() {
         if (log.isDebugEnabled())
            log.debug("Directory settings requested");
         return ConfigurationManager.getInstance().getDirConfiguration();
      }
      
   },
   AMAVIS("ConfigAmavis-dnew") {

      @Override
      public Map<String, String> getConfigurationMap() {
         if (log.isDebugEnabled())
            log.debug("Amavis settings requested");
         return ConfigurationManager.getInstance().getAmavisConfiguration();
      }
      
   },
   OTHER("ConfigOther") {

      @Override
      public Map<String, String> getConfigurationMap() {
         Map<String, String> configurationMap = new HashMap<String,String>(4);
         configurationMap.put("jes.fileSystem", ConfigurationManager.isWin() ? "Win" : "Linux");
         configurationMap.put("jes.version", System.getProperty("jes.version"));
         if (log.isDebugEnabled())
            log.debug("Other settings requested");
         return configurationMap;
      }
      
   };
   
   private static final Log log = LogFactory.getLog(ConfigType.class);
   
   public abstract Map<String, String> getConfigurationMap();
    
   private final String label;

   private ConfigType(String label) {
      this.label = label;
   }

   public String getLabel() {
      return label;
   }

   public static ConfigType getByLabel(String label) {
      if (label == null) {
         return null;
      }
      for (ConfigType configType : values()) {
         if (configType.getLabel().equals(label)) {
            return configType;
         }
      }
      return null;
   }
   
   public static Map<String, String> getConfigurationMapByLabel(String label) {
      ConfigType configType = getByLabel(label);
      if (configType == null)
         return null;
      return configType.getConfigurationMap();
   }
   
   public static String getConfigSectionByLabel(String label) {
      ConfigType configType = getByLabel(label);
      if (configType == null)
         return null;
      return configType.getLabel().substring(6);
   }
}
