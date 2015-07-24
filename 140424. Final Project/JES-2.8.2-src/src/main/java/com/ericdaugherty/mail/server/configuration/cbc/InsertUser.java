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

import com.ericdaugherty.mail.server.JSON.JSONArray;
import com.ericdaugherty.mail.server.JSON.JSONException;
import com.ericdaugherty.mail.server.JSON.JSONObject;
import com.ericdaugherty.mail.server.configuration.backEnd.PersistException;
import com.ericdaugherty.mail.server.configuration.backEnd.PersistExecutor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A single or multiple users to be added along with their passwords and (optionally) the realms ids that each user is to be attached to
 * 
 * @author Andreas Kyrmegalos
 */
public final class InsertUser extends CBCExecutor {

   private List<NewUser> users;

   public InsertUser(List<String> lines) {
      super(lines);
   }

   @Override
   public void processLines() throws CBCResponseException {
      
      users = new ArrayList<NewUser>(lines.size());
      
      int count = 0;
      for (String line : lines) {
         try {
            JSONObject jsonObject = new JSONObject(line.trim());
            NewUser newUser = new NewUser();
            newUser.username = jsonObject.getString(USERNAME);
            newUser.domainId = jsonObject.getInt(DOMAIN_ID);
            newUser.password = jsonObject.getString(PASSWORD).toCharArray();
            if (jsonObject.has(REALM)) {
               JSONArray jsonArray = jsonObject.getJSONArray(REALM);
               int length = jsonArray.length();
               newUser.realms = new LinkedHashSet<String>(length);
               for (int index = 0;index < length;index++) {
                  newUser.realms.add(jsonArray.getString(index));
               }
            }
            users.add(newUser);
            count++;
         } catch (JSONException je) {
            log.warn("Line " + count + " is an illegal JSONObject. Ignoring...");
         }
      }
   }

   @Override
   public void execute(PersistExecutor pe) throws PersistException {
      pe.insertUser(users);
   }
}
