/******************************************************************************
 * This program is a 100% Java Email Server.
 ******************************************************************************
 * Copyright (c) 2001-2013, Eric Daugherty (http://www.ericdaugherty.com)
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

package com.ericdaugherty.mail.server.utils;

//Java Imports
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

//JUnit Imports
import junit.framework.*;

//Local Imports
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.configuration.RcptPolicy;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class JESPropertiesTest extends TestCase {

   private static final byte[] textCRLF = ("##################### Recipient Policy #####################\r\n"
         + "# This file is used by the recipient policy mechanism\r\n"
         + "# to identify whether a recipient should be accepted/\r\n"
         + "# reject during a mail session. The policy is enforced when\r\n"
         + "# a message from a user of a locally administered domain is\r\n"
         + "# being received by the server. Both a global acceptance/\r\n"
         + "# rejection list and a per jes-domain list are offered.\r\n"
         + "# Per jes-domain entries supersede the global ones. That\r\n"
         + "# is, any domain listed in the jes-domain list will always\r\n"
         + "# take precedence over the opposite global entry during a\r\n"
         + "# session with a sender from the said domain. E.g. for a\r\n"
         + "# jes-domain named mydomain.com with entries in this file:\r\n"
         + "# allow=domainABC.com, block.mydomain.com=domainABC.com\r\n"
         + "# JES will always reject recipients at domainABC.com from\r\n"
         + "# any sender at the mydomain.com jes-domain and allow its\r\n"
         + "# use by all other jes-domains. If both block and allow\r\n"
         + "# entries exist, the block entries take precedence and the\r\n"
         + "# allow list is ignored. This applies to both global and\r\n"
         + "# per jes-domain entries. A valid explicit allow(block) entry\r\n"
         + "# instantly places an implicit  block(allow) setting for all\r\n"
         + "# other domains. E.g. for a policy\r\n"
         + "# allow.mydomain.com=domainABC.com JES will block all traffic\r\n"
         + "# from domain mydomain.com except messages intended for\r\n"
         + "# recipients at domainABC.com. A global block on domainABC.com\r\n"
         + "# will be ignored as well as any global allow entry (for the\r\n"
         + "# jes-domain in this example).\r\n"
         + "# Take note that in the case of a jes-domain entry the jes-domain\r\n"
         + "# itself is treated like any other recipient domain. Therefore\r\n"
         + "# if it is not defined in the list of allowed(blocked) domains\r\n"
         + "# any recipient belonging to the said jes-domain will be blocked\r\n"
         + "# (allowed).\r\n"
         + "# The entries in this file have the following format:\r\n"
         + "# allow=\r\n"
         + "# block=\r\n"
         + "# for global entries and\r\n"
         + "# allow.mydomain.com=\r\n"
         + "# block.mydomain.com=\r\n"
         + "# for a jes-domain called mydomain.com.\r\n"
         + "allow=domain1, domain2\r\n"
         + "# comment1\r\n"
         + "block=domain3, domain4\r\n"
         + "allow.local1=local1, domain1\r\n"
         + "# comment2\r\n"
         + "block.local2=local2, domain2").getBytes();
   
   private static final byte[] textLF = ("##################### Recipient Policy #####################\n"
         + "# This file is used by the recipient policy mechanism\n"
         + "# to identify whether a recipient should be accepted/\n"
         + "# reject during a mail session. The policy is enforced when\n"
         + "# a message from a user of a locally administered domain is\n"
         + "# being received by the server. Both a global acceptance/\n"
         + "# rejection list and a per jes-domain list are offered.\n"
         + "# Per jes-domain entries supersede the global ones. That\n"
         + "# is, any domain listed in the jes-domain list will always\n"
         + "# take precedence over the opposite global entry during a\n"
         + "# session with a sender from the said domain. E.g. for a\n"
         + "# jes-domain named mydomain.com with entries in this file:\n"
         + "# allow=domainABC.com, block.mydomain.com=domainABC.com\n"
         + "# JES will always reject recipients at domainABC.com from\n"
         + "# any sender at the mydomain.com jes-domain and allow its\n"
         + "# use by all other jes-domains. If both block and allow\n"
         + "# entries exist, the block entries take precedence and the\n"
         + "# allow list is ignored. This applies to both global and\n"
         + "# per jes-domain entries. A valid explicit allow(block) entry\n"
         + "# instantly places an implicit  block(allow) setting for all\n"
         + "# other domains. E.g. for a policy\n"
         + "# allow.mydomain.com=domainABC.com JES will block all traffic\n"
         + "# from domain mydomain.com except messages intended for\n"
         + "# recipients at domainABC.com. A global block on domainABC.com\n"
         + "# will be ignored as well as any global allow entry (for the\n"
         + "# jes-domain in this example).\n"
         + "# Take note that in the case of a jes-domain entry the jes-domain\n"
         + "# itself is treated like any other recipient domain. Therefore\n"
         + "# if it is not defined in the list of allowed(blocked) domains\n"
         + "# any recipient belonging to the said jes-domain will be blocked\n"
         + "# (allowed).\n"
         + "# The entries in this file have the following format:\n"
         + "# allow=\n"
         + "# block=\n"
         + "# for global entries and\n"
         + "# allow.mydomain.com=\n"
         + "# block.mydomain.com=\n"
         + "# for a jes-domain called mydomain.com.\n"
         + "allow=domain1, domain2\n"
         + "# comment1\r\n"
         + "block=domain3, domain4\n"
         + "allow.local1=local1, domain1\n"
         + "# comment2\n"
         + "block.local2=local2, domain2").getBytes();

   /**
    * The primary class responsible for executing the available tests
    *
    * @param testName
    * @throws java.io.IOException
    */
   public JESPropertiesTest(String testName) throws IOException {
      super(testName);
   }

   public static Test suite() {
      return new TestSuite(JESPropertiesTest.class);
   }

   public void test() throws IOException {

      Properties properties;
      DelimitedInputStream dis = null;

      try {
         dis = new DelimitedInputStream(new ByteArrayInputStream(textCRLF), 2048, "\r\n".getBytes());
         JESProperties jesProperties = new JESProperties(dis);
         jesProperties.load();
         properties = jesProperties.getProperties();
      } finally {
         IOUtils.close(dis);
      }
      assertTrue(properties.getProperty("allow") != null);
      assertTrue(properties.getProperty("block") != null);
      assertTrue(properties.getProperty("allow").equals("domain1, domain2"));
      assertTrue(properties.getProperty("block").equals("domain3, domain4"));

      assertTrue(properties.getProperty("allow.local1") != null);
      assertTrue(properties.getProperty("block.local2") != null);
      assertTrue(properties.getProperty("allow.local1").equals("local1, domain1"));
      assertTrue(properties.getProperty("block.local2").equals("local2, domain2"));

      try {
         dis = new DelimitedInputStream(new ByteArrayInputStream(textLF), 2048, "\n".getBytes());
         JESProperties jesProperties = new JESProperties(dis);
         jesProperties.load();
         properties = jesProperties.getProperties();
      } catch (IOException e) {
         assertTrue(false);
      } finally {
         IOUtils.close(dis);
      }
      assertTrue(properties.getProperty("allow") != null);
      assertTrue(properties.getProperty("block") != null);
      assertTrue(properties.getProperty("allow").equals("domain1, domain2"));
      assertTrue(properties.getProperty("block").equals("domain3, domain4"));

      assertTrue(properties.getProperty("allow.local1") != null);
      assertTrue(properties.getProperty("block.local2") != null);
      assertTrue(properties.getProperty("allow.local1").equals("local1, domain1"));
      assertTrue(properties.getProperty("block.local2").equals("local2, domain2"));

      Map<Object, RcptPolicy<String>> rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(properties, ConfigurationManager.LOCALE, null);

      assertTrue(rcptPolicyMap.get(new Domain("local1")) != null);
      assertTrue(rcptPolicyMap.get(new Domain("local1")).getRcptPolicyList().contains("local1"));
      assertTrue(rcptPolicyMap.get(new Domain("local1")).getRcptPolicyList().contains("domain1"));

      assertTrue(rcptPolicyMap.get(new Domain("local2")) != null);
      assertTrue(rcptPolicyMap.get(new Domain("local2")).getRcptPolicyList().contains("local2"));
      assertTrue(rcptPolicyMap.get(new Domain("local2")).getRcptPolicyList().contains("domain2"));
   }
}
