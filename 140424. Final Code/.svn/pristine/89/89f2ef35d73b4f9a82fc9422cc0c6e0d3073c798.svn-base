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

package com.ericdaugherty.mail.server.services.smtp.server.support;

//Java Imports
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//Logging Imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Local Imports
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.VerifyIPConfigurator;
import com.ericdaugherty.mail.server.configuration.VerifyIPConfigurator.DnsSubList;
import com.xlat4cast.jes.dns.ClientRequest;
import com.xlat4cast.jes.dns.ClientRequest.IPRequest.Version;
import com.xlat4cast.jes.dns.DNSUtil;
import com.xlat4cast.jes.dns.Resolver;
import com.xlat4cast.jes.dns.client.IPReply;
import com.xlat4cast.jes.dns.internal.DNSException;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 * The factory that supplies info concerning opening a connection or dropping one.
 *
 * @author Andreas Kyrmegalos
 */
public class VerifyIPFactory {

   /** Logger Category for this class. */
   private static final Log log = LogFactory.getLog( VerifyIPFactory.class );

   public static synchronized VerifyIP getNewVerifyIPInstance(boolean useDummy, boolean useAmavisSMTPDirectory) {

      if (useDummy)
         return new Dummy(useAmavisSMTPDirectory);
      return ConfigurationManager.getInstance().isAmavisSupportActive()
            ? new WithAmavisdInstance()
            : new NoAmavisdInstance();
   }

   private static class Dummy implements VerifyIP {
      
      private final boolean useAmavisSMTPDirectory;
      
      private Dummy(boolean useAmavisSMTPDirectory) {
         this.useAmavisSMTPDirectory = useAmavisSMTPDirectory;
      }
      
      @Override
      public boolean blockIP(InetAddress address) {
         return false;
      }
   
      @Override
      public boolean getUseAmavisDirectory() {
         return useAmavisSMTPDirectory;
      }
   }

   static final String sendToAmavis = "sendTo";
   static final String bypassAmavis = "bypass";

   private static class NoAmavisdInstance implements VerifyIP {

      protected String result;

      @Override
      public boolean blockIP(InetAddress address) {
         if (log.isDebugEnabled())
            log.debug("verifying IP " + address);
         VerifyIPConfigurator vIP = VerifyIPConfigurator.getInstance();

         String reversedIP = DNSUtil.ipAddressToStringReverse(address.getAddress(), false);

         //WhiteList Stage (mandatory)
         DnsSubList list = vIP.getWhiteList();
         String hostname = list.getHostName();
         Resolver dnsResolver = Mail.getInstance().getDNSResolver();
         try {
            if (log.isDebugEnabled())
               log.debug("looking up " + (reversedIP + hostname));
            IPReply reply = dnsResolver.submitRequest(new ClientRequest.IPRequest(new Domain(reversedIP + hostname), Version.V4));
            List<InetAddress> ipAddresses = reply.getIPAddresses();
            if (!ipAddresses.isEmpty()) {
               //The array is not empty, simply check if the first entry is a
               //loopback address
               InetAddress verify = ipAddresses.get(0);
               if (verify.isLoopbackAddress()) {
                  if (log.isDebugEnabled())
                     log.debug("whitelisted " + address);
                  result = bypassAmavis;
                  return false;
               }
               //Not a loopback address (perhaps the list hostname is wrong)
               log.error("Please check if " + hostname + " is a valid list hostname.");
            } else {
               //Since this a whiteList and the lookup concerned A type records
               //if the array is empty then treat the ip as not listed and
               //continue with the process.
               if (log.isDebugEnabled())
                  log.debug("VerifyIP lookup for " + (reversedIP + hostname)
                        + " returned an empty response of " + reply.getResponseType());
            }
         } catch (DNSException ex) {
            log.error("", ex);
            result = bypassAmavis;
            return false;
         }
         
         //MixedList Stage (optional)
         if ((list = vIP.getMixedList()) != null) {
            try {
               if (log.isDebugEnabled())
                  log.debug("looking up " + (reversedIP + hostname));
               IPReply reply = dnsResolver.submitRequest(new ClientRequest.IPRequest(new Domain(reversedIP + hostname), Version.V4));
               List<InetAddress> ipAddresses = reply.getIPAddresses();
               if (!ipAddresses.isEmpty()) {
                  //The array is not empty, validate the answers.
                  //Note that since this is a mixedList a reply can be either black
                  //or white but all replies must be of the same "color".
                  String recordSeverity, severity = null;
                  for (InetAddress ipAddress : ipAddresses) {
                     String recordReply = ipAddress.getHostAddress();
                     if (!recordReply.startsWith("127")) {
                        log.error("A lookup reply is not a loopback address,"
                              + " check that hostname " + hostname + " is correct");
                        break;
                     }
                     boolean isIPv4 = ipAddress instanceof java.net.Inet4Address;
                     if (list.getGeneral().containsKey(recordReply)
                           || isListed(isIPv4, list.getGeneral(), recordReply.split(isIPv4 ? "\\." : ":"))) {
                        recordSeverity = list.getGeneral().get(recordReply);
                        if (severity == null)
                           severity = recordSeverity;
                        else {
                           if ((severity.startsWith("W") && recordSeverity.startsWith("B"))
                                 || (severity.startsWith("B") && recordSeverity.startsWith("W"))) {
                              log.error("mixedList returned both black and white replies");
                              break;
                           }
                           else
                              severity = recordSeverity;
                        }
                     }
                  }
                  //Nothing listed or blacklisted, continue to the next step
                  if (severity != null && !severity.startsWith("B")) {
                     //Whitelisted, don't block the clientIP address
                     if (log.isDebugEnabled())
                        log.debug("whitelisted " + address);
                     result = bypassAmavis;
                     return false;
                  }
               } else {
                  //Since this a mixedList and the lookup concerned A type records
                  //if the array is empty then treat the ip as not listed and
                  //continue with the process.
                  if (log.isDebugEnabled())
                     log.debug("VerifyIP lookup for " + (reversedIP + hostname)
                           + " returned an empty response of " + reply.getResponseType());
               }
            } catch (DNSException ex) {
               log.error("", ex);
               result = bypassAmavis;
               return false;
            }
         }

         //BlackList Stage
         list = vIP.getBlackList();
         hostname = list.getHostName();
         boolean useStrict = vIP.isStrictUsed();
         boolean blockIP = vIP.isBlockingIP();
         boolean relayAmavis = vIP.isAmavisRelayed();
         try {
            if (log.isDebugEnabled())
               log.debug("looking up " + (reversedIP + hostname));
            IPReply reply = dnsResolver.submitRequest(new ClientRequest.IPRequest(new Domain(reversedIP + hostname), Version.V4));
            List<InetAddress> ipAddresses = reply.getIPAddresses();
            if (!ipAddresses.isEmpty()) {
               //The array is not empty, validate the answers
               String recordSeverity, severity = null;
               for (InetAddress ipAddress : ipAddresses) {
                  String recordReply = ipAddress.getHostAddress();
                  if (!recordReply.startsWith("127")) {
                     log.error("A lookup reply is not a loopback address,"
                           + " check that hostname " + hostname + " is correct.");
                     result = relayAmavis ? sendToAmavis : bypassAmavis;
                     return false;
                  }
                  boolean isIPv4 = ipAddress instanceof java.net.Inet4Address;
                  if (list.getGeneral().containsKey(recordReply)
                        || isListed(isIPv4, list.getGeneral(), recordReply.split(isIPv4 ? "\\." : ":"))) {
                     recordSeverity = list.getGeneral().get(recordReply);
                     if (severity == null)
                        severity = recordSeverity;
                     else if (recordSeverity.length() > severity.length())
                        severity = recordSeverity;
                  }
                  if (useStrict) {
                     if (list.getStrict().containsKey(recordReply)
                           || isListed(isIPv4, list.getStrict(), recordReply.split(isIPv4 ? " \\." : ":"))) {
                        recordSeverity = list.getGeneral().get(recordReply);
                        if (severity == null)
                           severity = recordSeverity;
                        else if (recordSeverity.length() > severity.length())
                           severity = recordSeverity;
                     }
                  }
               }
               if (severity != null) {
                  //If highest severity and admin requests IPs to be blocked
                  //then return true
                  if (severity.equals("BBB") && blockIP) {
                     if (log.isDebugEnabled())
                        log.debug("clientIP's " + address
                              + " reply has a maximum severity and"
                              + " the admin has requested that the IP be blocked.");
                     result = relayAmavis ? sendToAmavis : bypassAmavis;
                     return true;
                  }
                  //Blacklisted
                  if (log.isDebugEnabled())
                     log.debug("blacklisted " + address);
                  result = relayAmavis ? sendToAmavis : bypassAmavis;
                  return false;
               }
               //Not listed
               if (log.isDebugEnabled())
                  log.debug("not listed " + address + " and "+ (relayAmavis
                        ? "the admin has requested that all messages are filtered through amavis."
                        : "allowed to bypass amavisd-new."));
               result = relayAmavis ? sendToAmavis : bypassAmavis;
               return false;
            } else {
               //Since this a blackList and the lookup concerned A type records
               //if the array is empty then treat the ip as not listed and
               //conclude the process.
               if (log.isDebugEnabled()) {
                  log.debug("VerifyIP lookup for " + (reversedIP + hostname)
                        + " returned an empty response of " + reply.getResponseType());
                  log.debug("not blacklisted " + address);
               }
               result = relayAmavis ? sendToAmavis : bypassAmavis;
               return false;
            }
         } catch (DNSException ex) {
            log.error("", ex);
            result = bypassAmavis;
            return false;
         }
      }

      private boolean isListed(boolean isIPv4, Map<String,String> list, String[] recordReplyParts) {

         Iterator<String> iter = list.keySet().iterator();
         while (iter.hasNext()) {
            String reply = iter.next();
            if ((reply.indexOf(':') == -1) != isIPv4)
               continue;
            String[] replyParts = reply.split(isIPv4 ? "\\." : ":");
            if (isIPv4)
               if (replyParts[1].equals("*") || replyParts[1].equals(recordReplyParts[1]))
                  if (replyParts[2].equals("*") || replyParts[2].equals(recordReplyParts[2]))
                     if (replyParts[3].equals(recordReplyParts[3]))
                        return true;
            //TO-DO check IPv6 addresses
         }
         return false;
      }

      //With no running instance of amavis and if the sender doesn't get blocked this can only be
      //saved in incoming.directory
      @Override
      public boolean getUseAmavisDirectory() {
         return false;
      }
   }

   private static class WithAmavisdInstance extends NoAmavisdInstance {

      //Having a running instance of amavis and had the sender not been blocked, then based on
      //the result of the client IP's check, either save the message in amavis.incoming.directory
      //(effectively bypassing amavis) or in incoming.directory to be picked up by amavis.
      @Override
      public boolean getUseAmavisDirectory() {
         return !result.equals(sendToAmavis);
      }
   }
}
