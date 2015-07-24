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

//Java imports
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

//Local imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andreas Kyrmegalos
 */
public final class Utils {

   /**
    * Logger
    */
   private static Log log = LogFactory.getLog(Utils.class);

   /**
    * Converts the string into a valid port number.
    *
    * @param stringValue the string value to parse
    * @param defaultValue the default value to return if parsing fails.
    * @return a valid int.
    */
   static int parsePort(String stringValue, int defaultValue) {
      int value = defaultValue;
      if (stringValue != null && stringValue.length() > 0) {
         try {
            value = Integer.parseInt(stringValue);
         } catch (NumberFormatException e) {
            log.warn("Error parsing port string: " + stringValue + " using default value: " + defaultValue);
         }
      }
      return value;
   }

   /**
    * Loads the values of the specified key from the configuration file. This method parses the
    * value into a String array using the specified string as a delimiter. This method returns an
    * empty array if the the value string was null or empty.
    *
    * @param value the string to tokenize into an array.
    * @param trim trim each tokens leading and trailing whitespaces (if any)
    * @param delimiter the delimiter used to separate the individual values
    * @return a String[] of the values, or an empty array if the key could not be found.
    */
   static String[] tokenize(String value, boolean trim, String delimiter) {

      if (value == null || value.trim().equals("")) {
         return new String[0];
      } else {
         String[] result = value.split(delimiter);
         if (trim) {
            for (int i = 0; i < result.length; i++) {
               result[i] = result[i].trim();
            }
         }
         return result;
      }
   }

   /**
    * Converts an array of Strings to a String containing the delimited values
    *
    * @param tokens an array containing the String tokens
    * @param delimiter the separator to use to delimit the value
    * @return a String containing the delimited tokens
    */
   static String stringify(String[] tokens, String delimiter) {

      if (tokens == null || tokens.length == 0) {
         return "";
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < tokens.length - 1; i++) {
         sb.append(tokens[i]);
         sb.append(delimiter);
      }
      sb.append(tokens[tokens.length - 1]);
      return sb.toString();
   }

   /**
    * Converts a list of Strings to a String containing the delimited values
    *
    * @param tokens a list containing the String tokens
    * @param delimiter the separator to use to delimit the value
    * @return a String containing the delimited tokens
    */
   static String stringifyString(List<String> tokens, String delimiter) {

      if (tokens == null || tokens.isEmpty()) {
         return "";
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < tokens.size() - 1; i++) {
         sb.append(tokens.get(i));
         sb.append(delimiter);
      }
      sb.append(tokens.get(tokens.size() - 1));
      return sb.toString();
   }

   /**
    * Converts a list of Strings to a String containing the delimited values
    *
    * @param tokens a list containing the String tokens
    * @param delimiter the separator to use to delimit the value
    * @return a String containing the delimited tokens
    */
   static String stringifyDefaultSMTPServer(List<DefaultSMTPServer> tokens, String delimiter) {

      if (tokens == null || tokens.isEmpty()) {
         return "";
      }
      StringBuilder sb = new StringBuilder();
      for (DefaultSMTPServer token : tokens) {
         sb.append(token.stringify());
         sb.append(delimiter);
      }
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
   }
   
   static InetAddress getAllowedAddress(String propertyName, String requestAddress, boolean optionalClause, boolean allowZero) {
      
      ConfigurationManager cm = ConfigurationManager.getInstance();
      InetAddress allowedAddress;
      
         requestAddress = requestAddress.trim().toLowerCase(cm.LOCALE);
         if (requestAddress.equals("localhost") || requestAddress.equals("127.0.0.1")
               || requestAddress.equals("0:0:0:0:0:0:0:1") || requestAddress.equals("::1")) {

            requestAddress = null;
         }
         try {
            if (requestAddress != null && (requestAddress.equals("0.0.0.0") || requestAddress.equals("::"))) {
               if (!allowZero) {
                  String message = "Address is not a "
                        + (allowZero ? "zero, " : "")
                        + "loopback or site local address: "
                        + requestAddress;
                  log.error(message);
                  throw new RuntimeException(message);
               }
               allowedAddress = cm.isIPv6Preferred()
                     ? InetAddress.getByAddress("::", new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0})
                     : InetAddress.getByAddress("0.0.0.0.0", new byte[]{0,0,0,0});
               return allowedAddress;
            } else {
               allowedAddress = InetAddress.getByName(requestAddress);
               if (optionalClause || allowedAddress.isLoopbackAddress() || allowedAddress.isSiteLocalAddress()
                     || cm.isExternalDelegated()) {
                  return allowedAddress;
               }
               else {
                  String message = "Address is not a "
                        + (allowZero ? "zero, " : "")
                        + "loopback or site local address: "
                        + requestAddress;
                  log.error(message);
                  throw new RuntimeException(message);
               }
            }
         } catch (UnknownHostException unknownHostException) {
            String message = "Invalid value for property " + propertyName + ".";
            log.error(message, unknownHostException);
            throw new RuntimeException(message, unknownHostException);
         }
   }
    
    static final String ENC_S = "{ENC}";
    
    static final char[] ENC_C = ENC_S.toCharArray();
    
    static final String SHA_S = "{SHA}";
    
    static final char[] SHA_C = SHA_S.toCharArray();
}
