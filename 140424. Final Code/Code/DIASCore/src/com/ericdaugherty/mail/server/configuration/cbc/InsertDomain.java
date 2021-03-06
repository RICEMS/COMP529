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
 * $Rev: 292 $
 * $Date: 2013-03-01 05:55:36 +0100 (Fr, 01 Mrz 2013) $
 *
 ******************************************************************************/

package com.ericdaugherty.mail.server.configuration.cbc;

import com.ericdaugherty.mail.server.configuration.backEnd.PersistException;
import com.ericdaugherty.mail.server.configuration.backEnd.PersistExecutor;
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.JSON.JSONArray;
import com.ericdaugherty.mail.server.JSON.JSONException;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import java.util.ArrayList;
import java.util.List;

/**
 * A single domain or a comma separated list of domains can be added
 * 
 * @author Andreas Kyrmegalos
 */
public final class InsertDomain extends CBCExecutor {

   private final List<String> domains = new ArrayList<String>();

   public InsertDomain(List<String> lines) {
      super(lines);
   }

   @Override
   public void processLines() {
      
      for (String line : lines) {
         if (line.startsWith(DOMAIN)) {
            line = line.substring(DOMAIN.length());
            try {
               JSONArray jsonArray = new JSONArray(line);
               int length = jsonArray.length();
               List<String> domainsToAdd = new ArrayList<String>(length);
               for (int index = 0;index < length;index++) {
                  String domain = jsonArray.getString(index);
                  try {
                     EmailAddress.parseDomain(domain.toLowerCase(ConfigurationManager.LOCALE));
                  } catch (InvalidAddressException iae) {
                     log.warn(domain + " is an illegal domain name. Ignoring...", iae);
                     continue;
                  }
                  domainsToAdd.add(domain);
               }
               domains.addAll(domainsToAdd);
            } catch (JSONException je) {
               log.warn(line + " is an illegal JSONArray. Ignoring...", je);
            }
         }
      }
   }

   @Override
   public void execute(PersistExecutor pe) throws PersistException {
      pe.insertDomain(domains);
   }
}
