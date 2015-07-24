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

package com.ericdaugherty.mail.server.services.smtp.support;

//Java Imports
import java.util.*;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local Imports
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.ericdaugherty.mail.server.info.EmailAddress;

/**
 * SMTP Mail related common commands.
 *
 * @author Andreas Kyrmegalos
 */
public class Utils {

    /** Logger */
    private static final Log log = LogFactory.getLog( Utils.class );

    public static final String FILE_VERSION = "1.0";
   
    public static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * Converts a <code>List</code> of <code>EmailAddress</code>
     * instances into a comma delimited string.
     *
     * @param addresses Collection of Address instances.
     * @return Comma delimited String of the addresses.
     */
    public static String flattenAddresses( Collection<EmailAddress> addresses ) {
        StringBuilder toAddresses = new StringBuilder();
        EmailAddress address;
        Iterator<EmailAddress> addressIterator = addresses.iterator();
        while( addressIterator.hasNext() ) {
            address = addressIterator.next();
            toAddresses.append( address.toString() );
            toAddresses.append( "," );
        }

        // Remove the last comma.
        toAddresses.deleteCharAt( toAddresses.length() - 1 );

        return toAddresses.toString();
    }

    /**
     * Converts a comma delimited string of addresses into a
     * <code>List</code> of <code>EmailAddress</code> instances.
     *
     * @param addresses Comma delimited String of addresses.
     * @return List of Address instances.
     */
    public static List<EmailAddress> inflateAddresses( String addresses ) {
        StringTokenizer addressTokenizer = new StringTokenizer( addresses, "," );
        List<EmailAddress> addressList = new ArrayList<EmailAddress>();
        EmailAddress address;

        try {
            while( addressTokenizer.hasMoreTokens() ) {
                address = new EmailAddress( addressTokenizer.nextToken() );
                addressList.add( address );
            }

            return addressList;
        }
        catch( InvalidAddressException invalidAddressException ) {
            log.error( "Unable to parse to address read from database.  Full String is: " + addresses, invalidAddressException );
            throw new RuntimeException( "Error parsing address.  Message Delivery Failed." );
        }
    }
}
