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

//Java imports
import java.security.GeneralSecurityException;

//Local imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.PasswordFactory;

/**
 * Represents a user object.  This class is responsible for providing all
 * information about a specific user and their mailbox.
 * 
 * @author Eric Daugherty
 */
public class UserFile extends AbstractUser {

    /**
     * Creates a new user with the full username (user and domain).
     * The username and domain fields are not allowed to have their
     * values changed for the entire life-cycle of an instance of this
     * class.
     *
     * @param address User's full email address
     */
    public UserFile( EmailAddress address ){
         super(address);
    }

    /**
     * Returns true if and only if the specified plain text password's
     * value matches the password for this user.
     *
     * @param plainTextPassword the password to validate.
     * @return true if it matches.
     */
    @Override
    public boolean isPasswordValid(char[] plainTextPassword) {
        if(log.isDebugEnabled())
           log.debug("Authenticating User: " + getUserAdress());
        boolean result = PasswordFactory.getInstance().getPasswordHasher().passwordMatches(password, plainTextPassword);
        if(log.isDebugEnabled() && !result)
           log.debug("Authentication Failed for User: " + getUserAdress());

        return result;
    }
    
    public char[] getEncryptedPassword() {
       try {
          return ConfigurationManager.getInstance().encryptUserPassword(password);
       }
       finally {
          char[] newPass = new char[password.length+5];
          System.arraycopy(ENC_C, 0, newPass, 0, 5);
          System.arraycopy(password, 0, newPass, 5, password.length);
          password = newPass;
       }
    }

    /**
     * 
     * @deprecated New passwords are encrypted using scrypt, not simply hashed.
     * @return char[] 
     */
    public char[] getHashedPassword() {
       return PasswordFactory.getInstance().getPasswordHasher().hashPassword(password);
    }
    
    /**
     * Creates the A1 Hash for the SASL DIGEST-MD5 authentication mechanism.
     * The username is case-sensitive, and creation time casing is preserved. The
     * {@link String} representation of the domain, this {@link Realm} is a member
     * of, must be in lower case.
     * 
     * @param realm a realm the user belongs to
     * @return the hashed Realm password
     * @throws GeneralSecurityException 
     */
    public char[] getHashedRealmPassword(Realm realm) throws GeneralSecurityException {
       
       return PasswordFactory.getInstance().getPasswordHasher().hashRealmPassword(emailAddress.getUsername(), realm, password);
    }
    
    public boolean isClearTextPassword() {
       
       if (password.length<5) return true;
       String key = new String(password, 0, 5);
       
       return !(key.equals(SHA_S)||key.equals(ENC_S));
    }
}
