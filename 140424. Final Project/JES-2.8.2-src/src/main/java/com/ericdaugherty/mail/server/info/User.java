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

package com.ericdaugherty.mail.server.info;

import com.xlat4cast.jes.dns.internal.Domain;

/**
 * Represents a user object.  This class is responsible for providing all
 * information about a specific user and the corresponding mailbox.
 * 
 * @author Eric Daugherty
 */
public interface User extends Persistable {

    public String getUsername();
    
    public Domain getDomain();

    public String getUserAdress();

    public EmailAddress getEmailAddress();
    
    /**
     * Returns true if and only if the specified plain text password's hash
     * value matches the hashed password for this user.
     *
     * @param plainTextPassword the password to validate
     * @return true if it matches.
     */
    public boolean isPasswordValid( char[] plainTextPassword );
    
    /**
     * Returns true if and only if the specified plain text password's hash
     * value matches the hashed password for this user.
     *
     * @param plainTextPassword the password to validate
     * @param authenticationMechanism the authentication mechanism
     * @param authenticationData The possibly null additional data needed to validate the password
     * @return true if it matches.
     */
    public boolean isPasswordValid( char[] plainTextPassword, String authenticationMechanism, Object authenticationData );
    
    public byte[] getServerSignature();

    public void setPassword(char[] password);
    
    public EmailAddress[] getForwardAddresses();

    public void setForwardAddresses(EmailAddress[] forwardAddresses);

    /**
     * Returns an array of Strings that represent email addresses to deliver
     * email to this user.  If the forwardAddresses is not null or empty,
     * this will return the forwardAddresses array.  Otherwise, this will return
     * the user's email address.
     *
     * @return array of strings that represent email addresses.
     */
    public EmailAddress[] getDeliveryAddresses();
}
