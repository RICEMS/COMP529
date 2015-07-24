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
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

//Log imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local imports
import com.ericdaugherty.mail.server.errors.InvalidAddressException;
import com.xlat4cast.jes.dns.internal.Domain;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Represents a full email address, including username and domain.  This
 * class performs conversions between a full email address, and a username
 * and domain. Must be immutable.
 *
 * @author Eric Daugherty
 * @author Andreas Kyrmegalos (2.x branch)
 */
public final class EmailAddress implements Serializable, Comparable<EmailAddress> {
   
   private static final long serialVersionUID = -1000000008L;

   /** Logger */
   private static Log log = LogFactory.getLog(EmailAddress.class);
   
   private final static Domain nullDomain = Domain.getNullDomain();

   private String _username;
   private Domain _domain;

   private int hashCode = 0;
   
   private boolean NULL = true;

    /**
     * Creates an empty email address.  This is possible for
     * SMTP messages that have no MAIL FROM address.
     */
    public EmailAddress() {
       _username = "";
       _domain = nullDomain;
    }

   /**
    * Creates a new instance of this class using a single string that contains
    * the full email address (e.g. joe@mydomain.com). This constructor should
    * not be used by implementors of the ConfigurationManagerBackendIntf so as
    * to minimize memory usage.
    *
    * @param fullAddress
    * @throws com.ericdaugherty.mail.server.errors.InvalidAddressException
    */
    public EmailAddress( String fullAddress ) throws InvalidAddressException {

        int index = fullAddress.indexOf( '@' );
        if( index == -1 ) {
            throw new InvalidAddressException();
        }

        _username = fullAddress.substring( 0, index );
        parseLocalPartRFC5321(_username);
        _domain = new Domain(fullAddress.substring( index + 1 ));
        parseDomainRFC5321(_domain.getDomainName());

        NULL = false;
        
        hashCode = hashCode + _username.hashCode();
        hashCode = 17 * hashCode + _domain.hashCode();
    }

   /**
    * Creates a new instance of this class using a username {@link String} and a
    * domain {@link Domain}. This is the preferred constructor for implementors
    * of the ConfigurationManagerBackendIntf.
    *
    * @param username
    * @param domain
    * @throws com.ericdaugherty.mail.server.errors.InvalidAddressException
    */
    //TODO maximize the use of this constructor
    public EmailAddress( String username, Domain domain ) throws InvalidAddressException {
       
        _username = username;
        parseLocalPartRFC5321(_username);
        _domain = domain;
        parseDomainRFC5321(_domain.getDomainName());

        NULL = false;
        
        hashCode = hashCode + _username.hashCode();
        hashCode = 17*hashCode + _domain.hashCode();
    }

   /**
    * This is used to suppress the thrown exception when localPart and domain
    * have already been verified.
    *
    * @param localPart the local-part as defined in RFC5321
    * @param domain the domain or address-literal as defined in RFC5321
    * @return EmailAddress a syntactically valid e-mail address
    */
    public static EmailAddress getEmailAddress(String localPart, Domain domain) {
      try {
         return new EmailAddress(localPart, domain);
      } catch (InvalidAddressException ex) {
         throw new IllegalArgumentException("Either " + localPart + ", or " + domain
               + " cannot be parsed.");
      }
    }

   /**
    * This is used to suppress the thrown exception when username and domain
    * have already been verified.
    *
    * @param mailbox a mailbox as defined in RFC5321
    * @return EmailAddress a syntactically valid e-mail address
    */
    public static EmailAddress getEmailAddress(String mailbox) {
      try {
         return new EmailAddress(mailbox);
      } catch (InvalidAddressException ex) {
         //
         return null;
      }
    }

    public boolean isNULL() {
       return NULL;
    }

    public boolean isMailerDaemon() {
        return _username.toUpperCase(ConfigurationManager.LOCALE).equals("MAILER_DAEMON");
    }

    public String getUsername(){
        return _username;
    }

    /**
     * 
     * @param localPart the local-part segment of an e-mail address
     * @return 
     * @throws com.ericdaugherty.mail.server.errors.InvalidAddressException
     */
    public String parseLocalPartRFC5321(String localPart) throws InvalidAddressException{

       if (localPart.length()==0) {
          throw new InvalidAddressException("Zero length local-part");
       }
       //Quoted-string
       if (localPart.charAt(0)=='"') {
          if (localPart.charAt(localPart.length()-1)!='"'||
                (localPart.length()>=4&&localPart.charAt(localPart.length()-2)=='\\'&&
                 localPart.charAt(localPart.length()-3)!='\\')) {
             throw new InvalidAddressException("Malformed local-part Quoted-string");
          }
          if (localPart.length()==3&&localPart.charAt(1)=='\\') {
             throw new InvalidAddressException("Illegal local-part quoted-pairSMTP character "+localPart.charAt(1));
          }
          int length = localPart.length()-1;
          if (localPart.charAt(length-1)=='\\'&&localPart.charAt(length-2)!='\\') {
             throw new InvalidAddressException("Malformed local-part quoted-pairSMTP");
          }
          StringBuilder sb = new StringBuilder(length-1);
          char aChar;
          for (int i=1;i<length;i++) {
             aChar = localPart.charAt(i);
             if (aChar=='\\') {
                i++;
                if (i==length) break;
                aChar = localPart.charAt(i);
                if (aChar<32||aChar>126) {
                   throw new InvalidAddressException("quoted-pairSMTP illegal character: "+aChar);
                }
                sb.append(aChar);
             }
             else {
                if (aChar<32||aChar>126||aChar==34||aChar==92) {
                   throw new InvalidAddressException("qtextSMTP illegal character: "+aChar);
                }
                sb.append(aChar);
             }
          }
          return sb.toString();
       }
       //Dot-string
       else {
          String check;
          check = localPart.toLowerCase(ConfigurationManager.LOCALE);
          int length = check.length();
          if (check.charAt(0)=='.') {
             throw new InvalidAddressException("Illegal local-part Dot-string character (first character can not be a dot)");
          }
          if (!charInAtextLowerCaseOnly(check.charAt(0))) {
             throw new InvalidAddressException("Illegal local-part Dot-string character (character "+localPart.charAt(0)+" not an atext)");
          }
          boolean wasADot = false;
          for (int i=1;i<length;i++) {
             if (!charInAtextLowerCaseOnly(check.charAt(i)) && check.charAt(i)!='.') {
                throw new InvalidAddressException("Illegal local-part Dot-string character (character "+localPart.charAt(i)+" not an atext or a dot)");
             }
             else if (check.charAt(i)=='.') {
                if (wasADot) {
                   throw new InvalidAddressException("Illegal local-part Dot-string character sequence (Consecutive dots not allowed)");
                }
                else {
                   wasADot = true;
                }
             }
             else {
                wasADot = false;
             }
          }
          return localPart;
       }

    }
    
    private boolean charInAtextLowerCaseOnly(char aChar) {
       
       for (int i=0;i<55;i++) {
          if (aChar==ATEXT_LOWERCASE_ONLY[i]) return true;
       }
       return false;
    }

    private static final char[] ATEXT_LOWERCASE_ONLY = new char[] {
       '!', '#', '$', '%', '&', '\'', '*', '+', '-', '/',
       '0', '1', '2', '3', '4', '5' , '6', '7', '8', '9',
       '=', '?',
       'a', 'b', 'c', 'd', 'e', 'f' , 'g', 'h', 'i', 'j',
       'k', 'l', 'm', 'n', 'o', 'p' , 'q', 'r', 's', 't',
       'u', 'v', 'w', 'x', 'y', 'z' ,
       '^', '_', '`',
       '{', '|', '}', '~'
    };

    public Domain getDomain(){
        return _domain;
    }

    public String parseDomainRFC5321(String domain) throws InvalidAddressException{

       //address-literal (it is explicitly assumed that if an address-literal is used
       //that such an address-literal will not fail a DNS lookup. Since use of address-
       //literal is somewhat limited this doesn't place considerable burden upon a
       //nameservice)
       if (domain.charAt(0)=='[') {
          if (domain.charAt(domain.length()-1)!=']') {
             throw new InvalidAddressException(domain+" starts with a bracket but doesn't finish with one.");
          }
          String check = domain;
          if (check.indexOf('.')!=-1) {
             //Get rid of the brackets, otherwise will be mistaken for a IPv6
             check = domain.substring(1, domain.length()-1);
          }
          try {
             InetAddress.getByName(check);
          }
          catch (UnknownHostException uhe) {
             throw new InvalidAddressException(domain+" is not a valid address-literal.");
          }
       }
       //domain (unlike in the address-literal case use of the InetAddress facility
       //is avoided and the domain is parsed as simple text)
       else {
          parseDomain(domain.toLowerCase(ConfigurationManager.LOCALE));
       }
       return domain;
    }
   
   /**
    * Parses the domain. Case is respected.
    * Based on the specifications as set by rfc 952, rfc 1035,
    * rfc1123, rfc 4343 and rfc 5321. The last rfc is Internet
    * Mail specific. The rest pertain to Internet Hosts/DNS.
    * 
    * @param domain
    * @return String true if conforms to specs
    * @throws com.ericdaugherty.mail.server.errors.InvalidAddressException
    */
   public static String parseDomain(@NonNull String domain) throws InvalidAddressException {
      //drop trailing dot, if present
      if (domain.length() > 0 && domain.charAt(domain.length() - 1) == '.') {
         domain = domain.substring(0, domain.length() - 1);
      }

      if (domain.length() == 0) {
         throw new InvalidAddressException("Domain can not be zero length.");
      } //rfc 1035, 5321
      else if (domain.length() > 255) {
         throw new InvalidAddressException("Domain can not have length greater than 255 octets.");
      }
      
      //rfc 952, 1035, 4343
      String testDomain = domain.toLowerCase(ConfigurationManager.LOCALE);
      
      String[] labels = testDomain.split("\\.");
      if (labels.length == 0) {
         throw new InvalidAddressException("There must be at least one domain component(label).");
      } else if (labels.length == 1) {
         if (!labels[0].toLowerCase(ConfigurationManager.LOCALE).equals("localhost")) {
            throw new InvalidAddressException("Top-level domains are not acceptable.");
         }else {
            log.warn("The localhost is used as a mail domain.");
         }
      }

      for (String label : labels) {
         //rfc 952
         if (label.length() <= 1) {
            throw new InvalidAddressException("A sub domain must be at least two characters long");
         } //rfc 952, 1035
         else if (label.length() > 63) {
            throw new InvalidAddressException("A sub domain must be at most sixty three characters long");
         }
         //rfc 952, 1035, 1123, 5321
         if (label.charAt(0) == '-' || label.charAt(label.length() - 1) == '-') {
            throw new InvalidAddressException("The first or last character of a sub domain can not be a hyphen.");
         }
         //rfc 952, 1035, 5321
         for (char c : label.toCharArray()) {
            if (!charInAlphaDigitHyphenLowerCaseOnly(c)) {
               throw new InvalidAddressException("Illegal sub domain ALPHA/DIGIT/- (ldh) character ' " + c + " '.");
            }
         }
      }
      return domain;
   }
   
   private static boolean charInAlphaDigitHyphenLowerCaseOnly(char aChar) {

      for (char c : ALPHA_DIGIT_HYPHEN_LOWERCASE_ONLY) {
         if (aChar == c) {
            return true;
         }
      }
      return false;
   }

   private static final char[] ALPHA_DIGIT_HYPHEN_LOWERCASE_ONLY = new char[]{
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
      'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
      'u', 'v', 'w', 'x', 'y', 'z', '-'
   };

    public String getAddress() {
        return getFullAddress( getUsername(), getDomain().getDomainName() );
    }

    //***************************************************************
    // Private Interface
    //***************************************************************

    /**
     * Combines a username and domain into a single email address.
     */
    private String getFullAddress( String username, String domain ) {

        if( NULL ) {
            return "";
        }
        else {
            StringBuilder fullAddress = new StringBuilder( username.length()+1+domain.length() );
            fullAddress.append(username).append( '@' ).append( domain );

            return fullAddress.toString();
        }
    }

   @Override
   public int compareTo(EmailAddress o) {
      return (this.hashCode<o.hashCode ? -1 : (this.hashCode==o.hashCode ? 0 : 1));
   }

   /**
    *
    * @return the hashCode
    */
   @Override
   public final int hashCode() {
      return hashCode;
   }

   /**
    * Two EmailAddress Objects are treated as representing the same
    * EmailAddresss if the case-sensitive {@link String} depiction of the
    * local-part and the case-insensitive {@link String} depiction of the domain
    * are equal for both objects.
    *
    * @param object the object to consider whether it is meaningfully equivalent
    * to this instance
    * @return true if this instance of EmailAddress is equivalent to the
    * supplied object parameter
    */
   @Override
   public final boolean equals(Object object) {
      if (!(object instanceof EmailAddress)) {
         return false;
      }
      EmailAddress that = (EmailAddress) object;
      if ((this._username == null && that._username != null) || (this._username != null && !this._username.equals(that._username))) {
         if (log.isTraceEnabled())
            log.trace(this + " " + hashCode + " " + that + " " + that.hashCode);
         return false;
      }
      if ((this._domain == null && that._domain != null) || (this._domain != null && !this._domain.equals(that._domain))) {
         if (log.isTraceEnabled())
            log.trace(this + " " + hashCode + " " + that + " " + that.hashCode);
         return false;
      }
      return true;
   }

   /**
    * Override toString to return the full address.
    */
   @Override
   public String toString() {
      return getAddress();
   }
}
