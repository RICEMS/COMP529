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
 * $Rev: 334 $
 * $Date: 2014-01-04 02:56:07 +0100 (Sat, 04 Jan 2014) $
 *
 ******************************************************************************/

package com.ericdaugherty.mail.server.configuration.backEnd;

import com.ericdaugherty.mail.server.configuration.cbc.NewRealm;
import com.ericdaugherty.mail.server.configuration.cbc.NewUser;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Andreas Kyrmegalos
 */
public interface PersistExecutor {

   public void insertDomain(List<String> domains) throws PersistException;
   public void deleteDomain(Set<Integer> domainIds) throws PersistException;

   public void setDefaultDomain(int domainId) throws PersistException;

   public void insertUser(List<NewUser> newUsers) throws PersistException;
   public void deleteUser(Set<Integer> userIds) throws PersistException;
   
   public void setUserPassword(List<NewUser> users) throws PersistException;

   public void addForwardAddress(List<NewUser> forwardAddresses) throws PersistException;
   public void removeForwardAddress(List<NewUser> forwardAddresses) throws PersistException;

   public void setDefaultMailBox(Map<Integer, Integer> defaultMailBoxes) throws PersistException;

   public void insertRealm(List<NewRealm> newRealms) throws PersistException;
   public void removeRealm(Set<Integer> realmIds) throws PersistException;

   public void addUserToRealm(List<NewUser> users) throws PersistException;
   public void removeUserFromRealm(List<NewUser> users) throws PersistException;

}
