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

package com.ericdaugherty.mail.server.services.smtp.server;

//Java Imports
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
public class RecipientPolicyHandlerTest extends TestCase {

    /**
     * The primary class responsible for executing the available tests
     *
     * @param testName
     * @throws java.io.IOException
     */
    public RecipientPolicyHandlerTest(String testName) throws IOException {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(RecipientPolicyHandlerTest.class);
    }

    public void testGlobal() throws Exception {

        String senderDomain = "a";
        Domain sender = new Domain(senderDomain);
        String rcptDomain1 = "b";
        String rcptDomain2 = "c";
        String rcptDomain3 = "d";
        RecipientPolicyHandler rpc = new RecipientPolicyHandler();
        Map<Object, RcptPolicy<String>> rcptPolicyMap;
        Properties rcptPolicyFileEntries = new Properties();

        System.out.println("case 1 GLOBAL");
        rcptPolicyFileEntries.clear();
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 2 GLOBAL");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("allow", senderDomain);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 3 GLOBAL");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("block", senderDomain);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 4 GLOBAL");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("allow", rcptDomain1);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 5 GLOBAL");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("block", rcptDomain1);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 6 GLOBAL");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("allow", rcptDomain1 + "," + rcptDomain2);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain3, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 7 GLOBAL");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("block", rcptDomain1 + "," + rcptDomain2);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain3, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
    }

    public void testDomainAndGlobal() throws Exception {

        String senderDomain = "a";
        Domain sender = new Domain(senderDomain);
        String senderDomain1 = "aa";
        Domain sender1 = new Domain(senderDomain1);
        String rcptDomain1 = "b";
        String rcptDomain2 = "c";
        String rcptDomain3 = "d";
        RecipientPolicyHandler rpc = new RecipientPolicyHandler();
        Map<Object, RcptPolicy<String>> rcptPolicyMap;
        Properties rcptPolicyFileEntries = new Properties();


        System.out.println("case 1 DOMAIN");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("allow." + senderDomain, senderDomain);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("block." + senderDomain, senderDomain);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 2 DOMAIN");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("allow", senderDomain);
        rcptPolicyFileEntries.put("allow." + senderDomain, senderDomain+","+rcptDomain1);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("allow", senderDomain);
        rcptPolicyFileEntries.put("block." + senderDomain, senderDomain+","+rcptDomain1);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 3 DOMAIN");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("block", senderDomain);
        rcptPolicyFileEntries.put("allow." + senderDomain, senderDomain+","+rcptDomain1);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("block", senderDomain);
        rcptPolicyFileEntries.put("block." + senderDomain, senderDomain+","+rcptDomain1);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));


        System.out.println("case 4 DOMAIN");
        rcptPolicyFileEntries.clear();
        rcptPolicyFileEntries.put("allow", rcptDomain1);
        rcptPolicyFileEntries.put("allow." + senderDomain, senderDomain+","+rcptDomain1);
        rcptPolicyMap = ConfigurationManager.getRcptPolicyMap(rcptPolicyFileEntries, ConfigurationManager.LOCALE, null);
        
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, senderDomain, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain, rcptDomain1, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain, rcptDomain2, rcptPolicyMap.get(sender), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain1, senderDomain, rcptPolicyMap.get(sender1), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertTrue(rpc.rcptPolicyActions(senderDomain1, rcptDomain1, rcptPolicyMap.get(sender1), rcptPolicyMap.get("#####")));
        System.out.println("Testing...");
        assertFalse(rpc.rcptPolicyActions(senderDomain1, rcptDomain2, rcptPolicyMap.get(sender1), rcptPolicyMap.get("#####")));

    }
}
