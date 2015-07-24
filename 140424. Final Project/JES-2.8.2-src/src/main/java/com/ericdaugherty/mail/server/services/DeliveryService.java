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

package com.ericdaugherty.mail.server.services;

import com.ericdaugherty.mail.server.info.EmailAddress;
import com.ericdaugherty.mail.server.configuration.ConfigurationParameterConstants;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.xlat4cast.jes.dns.internal.Domain;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Handles the evaluation of general mail delivery rules, including SMTP Relaying.
 *
 * @author Eric Daugherty
 *
 */
public class DeliveryService implements ConfigurationParameterConstants {

   /**
    * Logger Category for this class.
    */
   private static final Log log = LogFactory.getLog(DeliveryService.class);
   
   /**
    * Singleton Instance
    */
   private static DeliveryService instance;

    
   public static synchronized void instantiate(ConfigurationManager configurationManager) {
      if (instance == null)
         instance = new DeliveryService(configurationManager);
   }
   
   public static void shutdown() {
      instance = null;
   }

   /**
    * Retrieves the singleton instance of this class.
    *
    * @return
    */
   public static DeliveryService getInstance() {
      return instance;
   }
   
   private final ConfigurationManager configurationManager;

   /**
    * The IP Addresses that have logged into the POP3 server recently.
    */
   private final ConcurrentMap<String, Long> authenticatedIps;

   /**
    * The mailboxes that are currently locked.
    */
   private final ConcurrentSkipListSet<EmailAddress> lockedMailboxes;

   /**
    * The domains that are currently considered local.
    */
   private final Set<Domain> localDomains;

   private final ReentrantReadWriteLock domainsLock = new ReentrantReadWriteLock();

   private DeliveryService(ConfigurationManager configurationManager) {
      
      this.configurationManager = configurationManager;
      //Initialize the Map for tracking authenticated ip addresses.
      this.authenticatedIps = new ConcurrentHashMap<String, Long>(50, 0.75f,
            Runtime.getRuntime().availableProcessors() + 1);
      //Initialize the Map for tracking locked mailboxes
      this.lockedMailboxes = new ConcurrentSkipListSet<EmailAddress>();
      this.localDomains = new LinkedHashSet<Domain>();
   }

   /**
    * Checks an address to see if we should accept it for delivery. The address
    * parameter may be null.
    *
    * @param address
    * @param clientIp
    * @param clientFromAddress
    * @return
    */
   public boolean acceptAddress(EmailAddress address, String clientIp, EmailAddress clientFromAddress) {

      // Check to see if the email address should be accepted for delivery.
      boolean accept;

      // Accept EmailAddress if one of the rules matches.
      accept = (address == null ? false : configurationManager.isLocalDomain(address.getDomain().getDomainName()))
            || (configurationManager.isEnablePOPBeforeSMTP() && isAuthenticated(clientIp))
            || (isRelayApproved(clientIp, configurationManager.getRelayApprovedIPAddresses())
            || (isRelayApprovedForEmail(clientFromAddress, configurationManager.getRelayApprovedEmailAddresses())));
      return accept;
   }
   
   public <T extends Domain> void setLocalDomains(@NonNull Set<T> localDomains) {
      
      domainsLock.writeLock().lock();
      try {
         this.localDomains.clear();
         this.localDomains.addAll(localDomains);
      } finally {
         domainsLock.writeLock().unlock();
      }
   }
   
   private boolean isLocalDomain(Domain domain) {
      
      domainsLock.readLock().lock();
      try {
         return localDomains.contains(domain);
      } finally {
         domainsLock.readLock().unlock();
      }
   }
   
   public <T extends Domain> void removeLocalDomains(@NonNull Set<T> domains) {
      
      domainsLock.writeLock().lock();
      try {
         localDomains.removeAll(domains);
      } finally {
         domainsLock.writeLock().unlock();
      }
   }

   /**
    * This method locks a mailbox so that two clients can not access the same
    * mailbox at the same time.
    *
    * @param address
    * @return true if the mailbox is not already locked
    */
   public boolean lockMailbox(@NonNull EmailAddress address) {
      if (log.isDebugEnabled())
         log.debug("Locking Mailbox: " + address);
      if (!isLocalDomain(address.getDomain()))
            return false;
      return lockedMailboxes.add(address);
   }

   /**
    * Checks to see if a user currently has the specified mailbox locked.
    *
    * @param address
    * @return
    */
   public boolean isMailboxLocked(@NonNull EmailAddress address) {
      if (log.isDebugEnabled())
         log.debug("Querying Mailbox lock status: " + address);
      return lockedMailboxes.contains(address);
   }

    /**
    * Unlocks an mailbox.
    *
    * @param address
    * @return 
    */
   public boolean unlockMailbox(@NonNull EmailAddress address) {
      if (log.isDebugEnabled())
         log.debug("Unlocking Mailbox: " + address);
      return lockedMailboxes.remove(address);
   }
   
   /**
    * This method should be called whenever a client authenticates itself.
    *
    * @param clientIp
    */
   public void addAuthenticated(String clientIp) {
      if (log.isDebugEnabled())
         log.debug("Adding authenticated IP address: " + clientIp);
      authenticatedIps.put(clientIp, new Date().getTime() + configurationManager.getAuthenticationTimeoutMilliseconds());
   }

   /**
    * Checks the current state to determine if a user from this IP address has
    * authenticated with the POP3 server with the timeout length.
    */
   private boolean isAuthenticated(String clientIp) {
      
      Long expires = authenticatedIps.get(clientIp);
      if (expires == null)
         return false;

      long currentTime = System.currentTimeMillis();
      if (expires.longValue() > currentTime) {
         return true;
      } else {
         //If the IP address has timed out, remove it from the hashtable.
         authenticatedIps.remove(clientIp);
         return false;
      }
   }
   
   public void cleanup() {
      
      long currentTime = System.currentTimeMillis();
      for (Iterator<Long> iter = authenticatedIps.values().iterator();iter.hasNext();) {
         Long expires = iter.next();
         if (expires == null)
            continue;
         if (expires.longValue() > currentTime)
            iter.remove();
      }
   }

   /**
    * Returns true if the client IP address matches an IP address in the
    * approvedAddresses array.
    *
    * @param clientIp The IP address to test.
    * @param approvedAddresses The approved list.
    * @return true if the address is approved.
    */
   private boolean isRelayApproved(String clientIp, String[] approvedAddresses) {

      for (String approvedAddress : approvedAddresses) {
         // Check for an exact match.
         if (clientIp.equals(approvedAddress))
            return true;
         
         int wildcardIndex = approvedAddress.indexOf("*");
         if (wildcardIndex == -1)
            continue;
         boolean matched = true;
         StringTokenizer clientIpTokenizer = new StringTokenizer(clientIp, ".");
         StringTokenizer approvedAddressTokenizer = new StringTokenizer(approvedAddress, ".");
         while (clientIpTokenizer.hasMoreTokens()) {
            try {
               String clientIpToken = clientIpTokenizer.nextToken().trim();
               String approvedAddressToken = approvedAddressTokenizer.nextToken().trim();
               if (!clientIpToken.equals(approvedAddressToken) && !approvedAddressToken.equals("*")) {
                  matched = false;
                  break;
               }
            } catch (NoSuchElementException nsee) {
               log.warn("Invalid ApprovedAddress found: " + approvedAddress + ".  Skipping.");
               matched = false;
               break;
            }
         }
         // Return true if there was a match.
         if (matched)
            return true;
      }
      return false;
   }

   /**
    * Returns true if the client email address matches an email address in the
    * approvedEmailAddresses array.
    *
    * @param clientFromEmail The email address to test.
    * @param approvedEmailAddresses The approved list.
    * @return true if the email address is approved.
    */
   private boolean isRelayApprovedForEmail(EmailAddress clientFromEmail, String[] approvedEmailAddresses) {

      for (String approvedEmailAddress : approvedEmailAddresses) {
         approvedEmailAddress = approvedEmailAddress.trim();
         // Check for an exact match (case insensitive).
         if (clientFromEmail.getAddress().equalsIgnoreCase(approvedEmailAddress)) {
            return true;
         } else if (approvedEmailAddress.startsWith("@")) {
            // Check for a domain
            String domain = approvedEmailAddress.substring(1);
            if (clientFromEmail.getDomain().getUniqueName().endsWith(domain))
               return true;
         }
      }
      return false;
   }
}
