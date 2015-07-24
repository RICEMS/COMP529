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

package com.ericdaugherty.mail.server.configuration;

import com.ericdaugherty.mail.server.configuration.cbc.CBCExecutor;
import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.info.Realm;
import com.ericdaugherty.mail.server.info.User;
import com.xlat4cast.jes.dns.internal.Domain;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.w3c.dom.Element;


/**
 *
 * @author Andreas Kyrmegalos
 */
public final class ConfigurationManagerBackEndLDAP implements ConfigurationManagerBackEnd{
@Override public void init(Element element) {}
@Override public void shutdown() {}
/* Not implemented in a LDAP scope */
@Override public void restore(String backupDirectory) throws IOException {}
/* Not implemented in a LDAP scope */
@Override public void doBackup(String backupDirectory) throws IOException {}
/* Not implemented in a LDAP scope */
@Override public void doWeeklyBackup(String backupDirectory) throws IOException {}
@Override public void persistUsersAndRealms() {}
@Override public List<String> updateThroughConnection(List<CBCExecutor> cbcExecutors) {return null;}
@Override public boolean persistUserUpdate() {return false;}
@Override public char[] getRealmPassword(Realm realmName, EmailAddress username) {return null;}
@Override public void loadUsersAndRealms() {}
@Override public void updateUsersAndRealmPasswords() {}
@Override public Set<Domain> getDomains(){return null;}
@Override public Set<Realm> getRealms() {return null;}
public void updateDomains(String domains, String defaultMailboxes) {}
@Override public boolean isLocalDomain(String domain) {return false;}
@Override public boolean isSingleDomainMode() {return false;}
@Override public Domain getSingleDomain() {return null;}
@Override public Domain getDefaultDomain() {return null;}
@Override public void updateDefaultDomain() {}
@Override public EmailAddress getDefaultMailbox(String domain) {return null;}
@Override public User getUser(EmailAddress address) {return null;}
@Override public Realm getRealm(String realmName) {return null;}
public boolean isUserARealmMember(Realm realm, String username_lower_case) {return false;}
}
