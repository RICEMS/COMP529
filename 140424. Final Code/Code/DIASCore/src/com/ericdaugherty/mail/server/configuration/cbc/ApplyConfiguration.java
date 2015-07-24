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

package com.ericdaugherty.mail.server.configuration.cbc;

//Java imports
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

//Local imports
import com.ericdaugherty.mail.server.JSON.JSONException;
import com.ericdaugherty.mail.server.JSON.JSONObject;
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.*;
import com.ericdaugherty.mail.server.errors.ConfigurationUpdateException;

/**
 *
 * @author Andreas Kyrmegalos
 */
public final class ApplyConfiguration extends CBCResponseExecutor {

   private String response;
   
   public ApplyConfiguration(List<String> lines) {
      super(lines);
   }

   @Override
   public void processLines() throws CBCResponseException {

      if (lines.size() != 2)
         throw new CBCResponseException("\"ApplyConfiguration\" payload must consist of exactly two lines.");
      String configSection = ConfigType.getConfigSectionByLabel(lines.get(0));
      if (configSection == null)
         throw new CBCResponseException(lines.get(0) + " is not a recognized ConfigType.");
      if (configSection.equals(ConfigType.getConfigSectionByLabel(ConfigType.OTHER.getLabel())))
         throw new CBCResponseException(ConfigType.OTHER.getLabel() + " cannot be updated.");
      try {
         final ParentTreeNode valuesToPersist = new ParentTreeNode(configSection.toLowerCase(ConfigurationManager.LOCALE));
         String key, value;
         JSONObject config = new JSONObject(lines.get(1));
         Iterator<String> iterKeys = config.keys();
         while (iterKeys.hasNext()) {
            key = iterKeys.next();
            value = config.getString(key);
            if (value != null && value.length() > 0) {
               //This entry contains at least one boolean attribute
               if (key.equals("attributes")) {
                  Iterator<String> iter = Arrays.asList(value.split(",")).iterator();
                  while (iter.hasNext()) {
                     value = iter.next();
                     if (value.endsWith("$restart")) {
                        valuesToPersist.setRestart(true);
                        value = value.substring(0, value.length() - 8);
                     }
                     valuesToPersist.addAttribute(new TreeAttribute(value.substring(0, value.indexOf('$')), value.substring(value.indexOf('$') + 1)));
                  }
               } //This entry is the node's value
               else if (key.equals("value")) {
                  if (value.endsWith("$restart")) {
                     valuesToPersist.setRestart(true);
                     value = value.substring(0, value.length() - 8);
                  }
                  valuesToPersist.setValue(value);
               } //This entry is exactly one attribute
               else if (!key.contains("$")) {
                  if (value.endsWith("$restart")) {
                     valuesToPersist.setRestart(true);
                     value = value.substring(0, value.length() - 8);
                  }
                  valuesToPersist.addAttribute(new TreeAttribute(key, value));
               } else {
                  boolean isElement = key.endsWith("$");
                  if (isElement) {
                     key = key.substring(0, key.length() - 1);
                  }
                  String[] elementPath = key.split("\\$");
                  TreeNode currentNode = valuesToPersist, previousNode = valuesToPersist;
                  int lastIndex = elementPath.length - (isElement ? 0 : 1);
                  for (int i = 0; i < lastIndex; i++) {
                     currentNode = currentNode.getChild(elementPath[i]);
                     if (currentNode == null) {
                        if (i < (lastIndex - 1) || !isElement) {
                           currentNode = new TreeNode(elementPath[i], previousNode);
                           previousNode.addChild(currentNode);
                        } else {
                           break;
                        }
                     }
                     previousNode = currentNode;
                  }
                  //This entry is the node's value
                  if (isElement) {
                     if (value.endsWith("$restart")) {
                        valuesToPersist.setRestart(true);
                        value = value.substring(0, value.length() - 8);
                     }
                     if (currentNode == null) {
                        currentNode = new TreeNode(elementPath[lastIndex-1], previousNode);
                        previousNode.addChild(currentNode);
                     }
                     currentNode.setValue(value);
                  } else {
                     processEntry(valuesToPersist, currentNode, elementPath[elementPath.length - 1], value);
                  }
               }
            }
         }
         ConfigurationManager.getInstance().resetDeviations();
         ConfigurationManager.getInstance().updateConfigurationThroughConnection(valuesToPersist);
         if (valuesToPersist.isRestart()) {
            try {
               Thread.sleep(333);
            }
            catch (InterruptedException e) {}
            Object restartLock = Mail.getInstance().getRestartLock();
            synchronized(restartLock) {
               while(!Mail.isStarted() || Mail.getInstance().isShuttingDown()
                     || Mail.getInstance().isRestart() || Mail.getInstance().isRestarting()) {
                  try {
                     restartLock.wait(333);
                  } catch(InterruptedException e) {}
               }
            }
         }
         String deviations = ConfigurationManager.getInstance().getConfigDeviations();
         response = ("success" + (deviations.length() == 0 ? "" : "," + deviations));
      } catch (JSONException ex) {
         log.error(ex.getMessage(), ex);
         throw new CBCResponseException("Malformed JSON content.");
      } catch (ConfigurationUpdateException ex) {
         log.error(ex.getMessage(), ex);
         throw new CBCResponseException(ex.getLocalizedMessage());
      }
   }

   @Override
   public String getResponse() {
      return response;
   }

   private void processEntry(ParentTreeNode valuesToPersist, TreeNode treeNode, String key, String value) {
      
      //This entry contains at least one boolean attribute
      if (key.equals("attributes")) {
         Iterator<String> iter = Arrays.asList(value.split(",")).iterator();
         while (iter.hasNext()) {
            value = iter.next();
            if (value.endsWith("$restart")) {
               valuesToPersist.setRestart(true);
               value = value.substring(0, value.length() - 8);
            }
            treeNode.addAttribute(new TreeAttribute(value.substring(0, value.indexOf('$')), value.substring(value.indexOf('$') + 1)));
         }
      } //This entry is exactly one attribute
      else if (!key.contains("$")) {
         if (value.endsWith("$restart")) {
            valuesToPersist.setRestart(true);
            value = value.substring(0, value.length() - 8);
         }
         treeNode.addAttribute(new TreeAttribute(key, value));
      }
   }
}
