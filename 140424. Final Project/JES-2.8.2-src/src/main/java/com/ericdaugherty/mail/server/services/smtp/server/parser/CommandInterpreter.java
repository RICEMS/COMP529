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

package com.ericdaugherty.mail.server.services.smtp.server.parser;

//Local imports
import com.ericdaugherty.mail.server.errors.SMTPReplyException;
import com.ericdaugherty.mail.server.services.smtp.server.CommandVerb;

/**
 * This class will drive the action forward
 *
 * @author Andreas Kyrmegalos
 */
public interface CommandInterpreter {

   String[] parseCommand(String line) throws SMTPReplyException;

   String handleArgument(String argument) throws SMTPReplyException;

   String[] handleParameters(String parameters) throws SMTPReplyException;

   void reset();
   
   String POSTMASTER = "postmaster";
   String WSP = "(\\ |\\t)";
   String ctext = "[A-Za-z0-9!\"#$%&'*+,\\-./:;<=>?@\\[\\]\\\\^_`{|}~]";
   String atext = "[A-Za-z0-9!#$%&'*+\\-/=?\\\\^_`{|}~]";
   String VCHAR = "[ A-Za-z0-9!\"#$%&'()*+,\\-./:;<=>?@\\[\\\\]\\\\^_`{|}~]";
   String letDigRegex = "[A-Za-z0-9]";
   String ldhStrRegex = "[A-Za-z0-9\\-]*" + letDigRegex;
   String subDomainRegex = letDigRegex + "(?:" + ldhStrRegex + ")?";
   String domainRegex = subDomainRegex + "(?:\\." + subDomainRegex + ")*";
   String atDomainRegex = "@" + domainRegex;
   String adlRegex = atDomainRegex + "(?:," + atDomainRegex + ")*";
   //TODO the literals
   String addressLiteralRegex = "\\[.+]";
   String atomRegex = atext + "+";
   String dotStringRegex = atomRegex + "(?:\\." + atomRegex + ")*";
   String qtextSMTPRegex = "[ A-Za-z0-9!#$%&'()*+,\\-./:;<=>?@\\[\\]\\\\^_`{|}~]";
   String quotedPairSMTPRegex = "\\\\[ A-Za-z0-9!\"#$%&'()*+,\\-./:;<=>?@\\[\\\\\\]\\\\^_`{|}~]";
   String qcontentSMTPRegex = "(?:" + quotedPairSMTPRegex + "|" + qtextSMTPRegex + ")";
   String quotedStringRegex = "\"" + qcontentSMTPRegex + "*\"";
   String localPartRegex = "(?:(?:" + dotStringRegex + ")|(?:" + quotedStringRegex + "))";
   String mailboxRegex = localPartRegex + "@" + "(?:(?:" + domainRegex + ")|" + addressLiteralRegex + ")";
   String pathRegex = "<(?:" + adlRegex + ":)?" + mailboxRegex + ">";
   String forwardPathRegex = "(?:<(?i:)" + POSTMASTER + "(?-i:)@" + domainRegex + ">|<(?i:)" + POSTMASTER + "(?-i:)>|" + pathRegex + ")";
   String reversePathRegex = "(?:" + pathRegex + ")|<>";
   String esmtpvalueRegex = "[A-Za-z0-9!\"#$%&'()*+,\\-./:;<>?@\\[\\\\\\]\\\\^_`{|}~]+";
   String esmtpKeywordRegex = "[A-Za-z0-9][A-Za-z0-9\\-]*";
   String esmtpParamRegex = esmtpKeywordRegex + "(?:=" + esmtpvalueRegex + ")?";
   String mailParametersRegex = esmtpParamRegex;
   String rcptParametersRegex = mailParametersRegex;
   String stringRegex = atomRegex + "|" + quotedStringRegex;
   String saslMechRegex = "[A-Z0-9\\-_]{1,20}";
   String base64CharRegex = "[A-Za-z0-9+/]";
   String base64TerminalRegex = "(?:" + base64CharRegex + "{2}==|" + base64CharRegex + "{3}=)";
   String base64Regex = "(?:" + base64TerminalRegex + "|" + base64CharRegex + "{4,}(?:==|=)?)";
   
   String EHLORegex = "(?i)(" + CommandVerb.EHLO.getLiteral() + ")(?-i) (" + domainRegex + "|" + addressLiteralRegex + ")(?:\\s?" + ctext + "*)?\\s*";
   String HELORegex = "(?i)(" + CommandVerb.HELO.getLiteral() + ")(?-i) (" + domainRegex + ")\\s*";
   String MAILRegex = "(?i)(" + CommandVerb.MAIL.getLiteral() + ")(?-i)(" + reversePathRegex + ")(?: (" + mailParametersRegex + ")*)?\\s*";
   String RCPTRegex = "(?i)(" + CommandVerb.RCPT.getLiteral() + ")(?-i)(" + forwardPathRegex + ")(?: (" + rcptParametersRegex + ")*)?\\s*";
   String DATARegex = "(?i)(" + CommandVerb.DATA.getLiteral() + ")(?-i)\\s*";
   String RSETRegex = "(?i)(" + CommandVerb.RSET.getLiteral() + ")(?-i)\\s*";
   String VRFYRegex = "(?i)(" + CommandVerb.VRFY.getLiteral() + ")(?-i) (" + stringRegex + ")\\s*";
   String EXPNRegex = "(?i)(" + CommandVerb.EXPN.getLiteral() + ")(?-i) (" + stringRegex + ")\\s*";
   String NOOPRegex = "(?i)(" + CommandVerb.NOOP.getLiteral() + ")(?-i)(?: (?:" + stringRegex + ")*)?\\s*";
   String QUITRegex = "(?i)(" + CommandVerb.QUIT.getLiteral() + ")(?-i)\\s*";
   String STLSRegex = "(?i)(" + CommandVerb.STLS.getLiteral() + ")(?-i)\\s*";
   String AUTHRegex = "(?i)(" + CommandVerb.AUTH.getLiteral() + ")(?-i) (" + saslMechRegex + ")(?: (" + base64Regex + "|=))?\\s*";
}
