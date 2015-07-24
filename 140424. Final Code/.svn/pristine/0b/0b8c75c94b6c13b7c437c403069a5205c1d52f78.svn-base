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
import com.ericdaugherty.mail.server.JSON.JSONArray;
import com.ericdaugherty.mail.server.JSON.JSONException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A single or multiple forward addresses to add to one more user ids. 
 * 
 * @author Andreas Kyrmegalos
 */
public final class AddForwardAddress extends CBCExecutor {

   private final List<NewUser> forwardAddresses = new ArrayList<NewUser>();

   public AddForwardAddress(List<String> lines) {
      super(lines);
   }

   @Override
   public void processLines() throws CBCResponseException {
      
      if (lines.size() % 2 != 0) {
         throw new CBCResponseException("Pairs of \"" + USER_ID + "\" and \"" + FORWARD_ADDRESS + "\" are required. All entries were ignored...");
      }
      List<NewUser> users = null;
      for (String line : lines) {
         if (line.startsWith(USER_ID)) {
            if (users != null)
               log.warn("Out of sequence " + USER_ID + " entry. Continuing...");
            line = line.substring(USER_ID.length()).trim();
            try {
               JSONArray jsonArray = new JSONArray(line);
               int length = jsonArray.length();
               users = new ArrayList<NewUser>(length);
               for (int index = 0;index < length;index++) {
                  NewUser newUser = new NewUser();
                  newUser.userId = jsonArray.getInt(index);
                  users.add(newUser);
               }
            } catch (JSONException je) {
               log.warn(line + " is an illegal JSONArray. Ignoring...", je);
               users = null;
            }
         } else if (line.startsWith(FORWARD_ADDRESS)) {
            line = line.substring(FORWARD_ADDRESS.length()).trim();
            if (users == null) {
               log.warn("Out of sequence " + FORWARD_ADDRESS + " entry. Continuing...");
               continue;
            }
            try {
               JSONArray jsonArray = new JSONArray(line);
               int length = jsonArray.length();
               if (length != users.size()) {
                  log.warn("ForwardAdresses (" + length + ") count does not match number of users (" + users.size() + "). Ignoring...");
                  users = null;
                  continue;
               }
               for (int index = 0;index < length;index++) {
                  JSONArray jsonArray1 = jsonArray.getJSONArray(index);
                  NewUser user = users.get(index);
                  int length1 = jsonArray1.length();
                  user.forwardAddresses = new LinkedHashSet<String>(length1);
                  for (int index1 = 0;index1 < length;index1++) {
                     user.forwardAddresses.add(jsonArray1.getString(index1));
                  }
               }
               forwardAddresses.addAll(users);
            } catch (JSONException je) {
               log.warn(line + " is or contains an illegal JSONArray. Ignoring...", je);
            }
            users = null;
         }
      }
   }

   @Override
   public void execute(PersistExecutor pe) throws PersistException {
      pe.addForwardAddress(forwardAddresses);
   }
}
