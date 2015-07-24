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

package com.ericdaugherty.mail.server.auth;

//Java Imports
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;

//Log imports
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

//Encoding Imports
import org.apache.commons.codec.binary.Base64;

//Local imports
import com.ericdaugherty.mail.server.Mail;
import com.ericdaugherty.mail.server.configuration.ConfigurationManager;
import com.ericdaugherty.mail.server.errors.*;
import com.ericdaugherty.mail.server.info.*;
import com.ericdaugherty.mail.server.services.DeliveryService;
import com.ericdaugherty.mail.server.services.pop3.Pop3Processor;
import com.ericdaugherty.mail.server.utils.ByteUtils;
import com.xlat4cast.jes.crypto.digest.JESMessageDigest;
import com.xlat4cast.jes.dns.internal.Domain;

/**
 * Verify client authentication using SASL DIGEST-MD5. Possibly protect
 * the data stream using integrity/privacy wrapping.
 *
 * @author Andreas Kyrmegalos
 */
public class DigestMd5ServerMode extends AbstractSaslServerMode {

   /** Logger Category for this class. */
   private static final Log log = LogFactory.getLog( DigestMd5ServerMode.class );

   private Map<String,List<Integer>> nonces = new HashMap<String,List<Integer>>();

   private boolean integrity, privacy;
   
   private Set<Realm> realms;

   private int rcvMaxBuffer = 65536, sndMaxBuffer = 65536;

   private String sessionCipher;

   private String encoding = ISO_8859_1;

   private byte[] MD5DigestSessionKey;

   private DigestMd5ServerMode.Wrapper wrapper;

   private boolean firstEvaluation = true;
   
   private final Charset iso88591Charset = ConfigurationManager.getIso88591Charset();

   public DigestMd5ServerMode(boolean smtp) {
      if (smtp) {
         finalizeAuthentication = new DigestMd5ServerMode.FinalizeAuthenticationSMTP();
      }
      else {
         pop3 = true;
         finalizeAuthentication = new DigestMd5ServerMode.FinalizeAuthenticationPOP3();
      }
   }

   private String responseTokenProcessor(List<byte[]> splittedBytes, Map<String,byte[]> directives,
         String key, int position, int tokenCountMinus1) throws SaslException {
      byte[] temp = splittedBytes.get(position);
      byte[] value;
      int valueStart, valueFinish;
      if (directives.containsKey(key)) {
         throw new SaslException("Digest-Response can not contain multiple identical keys");
      }
      int lastCommaPos = byteArrayLastIndexOf(temp, 0x2c);
      if (lastCommaPos==-1) {
         if (position!=tokenCountMinus1) {
            throw new SaslException("Error encountered while parsing Digest-Response content");
         }
         lastCommaPos = temp.length;
      }
      if (lastCommaPos<byteArrayLastIndexOf(temp, 0x22)&&position!=tokenCountMinus1) {
         throw new SaslException("Inappropriate Digest-Response format");
      }
      //Get value while removing (any) surrounding white spaces
      valueStart = firstNonLWS(temp);
      valueFinish = lastNonLWS(temp, lastCommaPos-1);
      //Check for quoting inconsistency
      if (isImproperlyQuoted(temp, valueStart, valueFinish)) {
         throw new SaslException("Inappropriate Digest-Response format");
      }
      //Strip off the quotes (if any)
      if (temp[valueStart]==0x22) {
         valueStart++;
         valueFinish--;
      }
      value = new byte[valueFinish-valueStart+1];
      System.arraycopy(temp, valueStart, value, 0, valueFinish-valueStart+1);
      
      directives.put(key, value);
      //Get the new key for the next key-value pair (keys can be safely string encoded as US-ASCII)
      return position == tokenCountMinus1
            ? "" : new String(temp, lastCommaPos+1,temp.length-lastCommaPos-1,usAsciiCharset).toLowerCase(ConfigurationManager.LOCALE).trim();

   }

   private boolean isImproperlyQuoted(byte[] target, int first, int last) {
      return (target[first] != 0x22 && target[last] == 0x22)
            || (target[first] == 0x22 && target[last] != 0x22);
   }

   private boolean isLWS(int target) {
      return target == 0x0d || target == 0x0a || target == 0x20 || target == 0x09;
   }
   
   private int firstNonLWS(byte[] array) {
      int length = array.length;
      for (int i=0;i<length;i++) {
         if (!isLWS(array[i])) return i;
      }
      return 0;
   }
   
   private int lastNonLWS(byte[] array, int startAt) {
      for (int i=startAt;i>=0;i--) {
         if (!isLWS(array[i])) return i;
      }
      return startAt;
   }

   private int byteArrayLastIndexOf(byte[] array, int target) {
      for (int i=array.length-1;i>=0;i--) {
         if (array[i]==target) return i;
      }
      return -1;
   }

   private List<byte[]> splitByteArray(byte[] array, byte splitter) {
      List<byte[]> splitted = new ArrayList<byte[]>();
      byte[] current;
      int starting = 0, length = array.length;
      boolean comma = false, first = true;
      for(int i=starting;i<length;i++) {
         if (i==length-1) {
            current = new byte[i-starting+1];
            System.arraycopy(array, starting, current, 0, i-starting+1);
            splitted.add(current);
         }
         else if (array[i]==splitter) {
            if (!comma&&!first) continue;
            current = new byte[i-starting];
            System.arraycopy(array, starting, current, 0, i-starting);
            splitted.add(current);
            starting = ++i;
            comma = false;
            first = false;
         }
         if (array[i]==',') comma = true;
      }
      return splitted;
   }
   
   private void decodeMixed(Map rawDirectives) {
      
      CharsetDecoder decoder = iso88591Charset.newDecoder();
      decoder.onMalformedInput(CodingErrorAction.REPORT);
      decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
      List<String> keys = new ArrayList<String>(rawDirectives.keySet());
      Iterator<String> iter = keys.iterator();
      String key, stringValue;
      byte[] value;
      while(iter.hasNext()) {
         key = iter.next();
         value = (byte[])rawDirectives.remove(key);
         if (key.equals("authzid")){
            stringValue = new String(value, utf8Charset);
         }
         else if (key.equals("realm")||key.equals("username")) {
            try {
               ByteBuffer in = ByteBuffer.wrap(value.clone());
               CharBuffer cb = decoder.decode(in);
               char[] charValue = new char[cb.remaining()];
               cb.get(charValue);
               stringValue = new String(charValue);
            }
            catch (CharacterCodingException uee) {
               stringValue = new String(value, utf8Charset);
            }
         }
         else {
            stringValue = new String(value, iso88591Charset);
         }
         rawDirectives.put(key, stringValue);
      }
      
   }

   private void decodeAllAs8859(Map rawDirectives) {
      List<String> keys = new ArrayList<String>(rawDirectives.keySet());
      Iterator<String> iter = keys.iterator();
      String key, stringValue;
      byte[] value;
      while(iter.hasNext()) {
         key = iter.next();
         value = (byte[])rawDirectives.remove(key);
         if (key.equals("authzid")){
            stringValue = new String(value, utf8Charset);
         }
         else {
            stringValue = new String(value, iso88591Charset);
         }
         rawDirectives.put(key, stringValue);
      }
   }
   
   /**
    * This method is devoid of purpose since the actions associated
    * with it are already carried out in the {@link #evaluateResponse(byte[])}
    * method.
    * @return 
    * @throws javax.security.sasl.SaslException
    */
   @Override
   protected String getValidAuthenticationID(String authenticationID) throws SaslException{
      return authenticationID;
   }
   
   private String getRealmsForResponse(Set<Realm> realms) {
      
      StringBuilder sb = new StringBuilder(100);
      for(Realm realm:realms) {
         sb.append("realm=\"").append(realm.getFullRealmName()).append("\"").append(',');
      }
      return sb.toString();
   }

   @Override
   public byte[] evaluateResponse(byte[] responseBytes) throws SaslException {

      if (completed) throw new SaslException("Authentication already completed.");
      if (failed) throw new SaslException("Authentication already tried and failed.");
      
      if (firstEvaluation) {
         firstEvaluation = false;
         StringBuilder challenge = new StringBuilder(100);
         realms = new HashSet<Realm>();
         realms.addAll(configurationManager.getRealms());
         challenge.append(getRealmsForResponse(realms));
         String nonceUUID = UUID.randomUUID().toString();
         String nonce = null;
         try {
            nonce = new String(Base64.encodeBase64(MD5Digest(String.valueOf(System.nanoTime()+":"+nonceUUID))),usAsciiCharset);
         } catch (GeneralSecurityException uee) {
            throw new SaslException(uee.getMessage(),uee);
         }
         nonces.put(nonce, new ArrayList<Integer>());
         nonces.get(nonce).add(Integer.valueOf(1));
         challenge.append("nonce=\"").append(nonce).append("\"");
         challenge.append(",");
         challenge.append("qop=\"").append(configurationManager.getSaslQOP()).append("\"");
         challenge.append(",");
         challenge.append("maxbuf=\"65536\"");
         challenge.append(",");
         challenge.append("charset=\"utf-8\"");
         challenge.append(",");
         challenge.append("algorithm=\"md5-sess\"");
         if (configurationManager.getSaslQOP().indexOf("auth-conf")!=-1) {
            challenge.append(",");
            challenge.append("cipher-opts=\"").append(configurationManager.getDigestMD5Ciphers()).append("\"");
         }
         return Base64.encodeBase64(challenge.toString().getBytes(usAsciiCharset));
      }
      else {

         String nonce;
         //The decoded digest-response may contain a mix of iso-8859-1 and utf-8 encoded data
         //Must process at byte level, keys can be decoded safely as ASCII
         if (!Base64.isArrayByteBase64(responseBytes)) {
            throw new SaslException("Can not decode Base64 Content",new MalformedBase64ContentException());
         }
         responseBytes = Base64.decodeBase64(responseBytes);

         //Process the response

         List<byte[]> splittedBytes = splitByteArray(responseBytes,(byte)0x3d);
         //Last token can only be a value
         int tokenCountMinus1 = splittedBytes.size()-1, lastCommaPos;
         Map rawDirectives = new HashMap();
         //First token can only be a key
         String key;
         Map<String,String> directives;
         key = new String(splittedBytes.get(0),usAsciiCharset);
         for (int i=1;i<tokenCountMinus1;i++) {
            key = responseTokenProcessor(splittedBytes, rawDirectives, key, i, tokenCountMinus1);
         }
         responseTokenProcessor(splittedBytes, rawDirectives, key, tokenCountMinus1, tokenCountMinus1);
         //If Digest-Response contains a charset directive and its value is utf-8
         //the directives username and realm must be decoded as utf-8.
         //This also applies to the username, passsword and realm when retreived
         //locally to verify the response directive
         if (rawDirectives.containsKey("charset")) {
            String value = new String((byte[])rawDirectives.get("charset"),usAsciiCharset).toLowerCase(ConfigurationManager.LOCALE);
            if (value.equals("utf-8")) {
               encoding = UTF_8;
            }
            else {
               throw new SaslException("The charset directive is only allowed the utf-8 value.");
            }
         }

         if (encoding.equals(ISO_8859_1)) {
            decodeAllAs8859(rawDirectives);
         }
         else {
            decodeMixed(rawDirectives);
         }
         directives = rawDirectives;

         if (!directives.containsKey("username") || !directives.containsKey("nonce") || !directives.containsKey("nc")||
               !directives.containsKey("cnonce") || !directives.containsKey("response")) {
            throw new SaslException("Digest-Response lacks at least one neccesery key-value pair");
         }
         
         //There is ambiguity concerning the digest-uri value. Although it is specified as
         //"This directive may appear at most once;" which implies that it is legal to be
         //omitted, there are no instructions concerning its absence during A2 computation.
         //To avoid interoperability issues, the empty string is used in its absence.
         String digestURI = directives.get("digest-uri");
         if (digestURI==null) {
            digestURI = "";
         }
         else {
            int slashPos = digestURI.indexOf('/');
            if (slashPos==-1) {
               throw new SaslException("Malformed digest-uri entry.");
            }
            if (!digestURI.substring(0, slashPos).toLowerCase(ConfigurationManager.LOCALE).equals("smtp")) {
               throw new SaslException("Serv-type mismatch. Should be smtp. "+digestURI.substring(0, slashPos)+" found.");
            }
            int lastSlashPos = digestURI.lastIndexOf('/');
            String host, servName;
            if (lastSlashPos!=slashPos) {
               host = digestURI.substring(slashPos+1, lastSlashPos);
               servName = digestURI.substring(lastSlashPos+1);
            }
            else {
               host = digestURI.substring(slashPos+1);
               servName = null;
            }
            if (servName!=null) {
               if (new Domain(servName).equals(new Domain(host))) {
                  throw new SaslException("If the service is not replicated, or the serv-name is identical to the "+
                                          "host, then the serv-name component MUST be omitted. Aborting...");
               }
               if (!host.toLowerCase(ConfigurationManager.LOCALE).endsWith(servName.toLowerCase(ConfigurationManager.LOCALE))) {
                  throw new SaslException("Host / serv-name mismatch. Aborting...");
               }
               if (!configurationManager.isLocalDomain(servName)) {
                  throw new SaslException(host+" is not among the list of hosted domains. Aborting...");
               }
            }
            if (!configurationManager.isLocalMXDomain(host)) {
               throw new SaslException(host+" is not a replication of a member of the list of hosted domains. Aborting...");
            }
         }
         
         if (directives.get("username").indexOf('@')!=-1) {
            throw new SaslException("Digest-response username field must not include domain name");
         }
         if (!directives.containsKey("qop")) {
            directives.put("qop", QOP_AUTH);
         }
         
         String realm = directives.get("realm");
         if (realm == null) {
            if (configurationManager.isSingleDomainMode()) {
               realm = configurationManager.getSingleDomain().getDomainName();
            }
            else {
               realm = "null";
            }
         }
         else if (realm.equals("")) {
            realm = "null";
         }
         else {
            Realm aRealm;
            int atPos = realm.indexOf('@');
            if (atPos == -1) {
               aRealm = new Realm(new Domain(realm));
            } else {
               aRealm = new Realm(realm.substring(0, atPos), new Domain(realm.substring(atPos+1)));
            }
            if (!realms.contains(aRealm)){
               throw new SaslException("Realm value not in server realm list");
            }
         }
         //The realm at this point can be:
         //    a. a String representing the null realm
         //    b. a String representing the single JES domain or one of the JES domains
         //    d. a String representing one of the JES realms
         
         nonce = (String)directives.get("nonce");
         if (!nonces.containsKey(nonce)) {
            throw new SaslException("Illegal nonce value");
         }
         List<Integer> nonceListInMap = nonces.get(nonce);
         int nc = Integer.parseInt((String)directives.get("nc"), 16);
         if (nonceListInMap.get(nonceListInMap.size()-1).equals(Integer.valueOf(nc))) {
            nonceListInMap.add(Integer.valueOf(++nc));
         }
         else {
            throw new SaslException("Illegal nc value");
         }

         if (directives.get("qop").equals(QOP_AUTH_INT)) integrity = true;
         else if (directives.get("qop").equals(QOP_AUTH_CONF)) privacy = true;

         if (privacy) {
            if (!directives.containsKey("cipher")) {
               throw new SaslException("Message confidentially required but cipher entry is missing");
            }
            sessionCipher = directives.get("cipher").toLowerCase(ConfigurationManager.LOCALE);
            if ("3des,des,rc4-40,rc4,rc4-56".indexOf(sessionCipher)==-1) {
               throw new SaslException("Unsupported cipher for message confidentiality");
            }

         }
         if ((privacy||integrity)&&directives.containsKey("maxbuf")) {
            try {
               sndMaxBuffer = Math.min(sndMaxBuffer, Integer.parseInt(directives.get("maxbuf")));
            }
            catch(NumberFormatException nfe) {}
         }

         String authenticationID = directives.get("username");
         
         //The method local authorizationID variable MUST NOT change
         //Although the class' authorizationID field could reference the authenticationID
         final String authorizationID = directives.get("authzid");
         
         //Since the authorizationID is application protocol specific
         //it is expected to represent a fully defined mailbox.
         if (authorizationID != null && authorizationID.indexOf('@') == -1) {
            throw new SaslException("AuthorizationID not a fully defined mailbox.");
         }
         
         //If both the realm and the authorizationID are defined and
         //their respected domains differ then fail the authentication.
         if (!realm.equals("null")) {
            int atPos = realm.indexOf('@');
            if (atPos == -1) {
               if (authorizationID != null) {
                  if (!realm.equalsIgnoreCase(authorizationID.substring(authorizationID.indexOf('@')+1))) {
                     throw new SaslException("Domain miss-match in credentials.");
                  }
               }
               authenticationID += "@" + realm;
            }
            else {
               if (authorizationID != null) {
                  if (!realm.substring(atPos+1).equalsIgnoreCase(authorizationID.substring(authorizationID.indexOf('@')+1))) {
                     throw new SaslException("Domain miss-match in credentials.");
                  }
               }
               authenticationID += realm.substring(atPos);
            }
         }
         else if (authorizationID != null) {
            authenticationID += authorizationID.substring(authorizationID.indexOf('@'));
         }
         //The authenticationID at this point can be:
         //    a. a String representing a username sans a domain
         //    b. a String representing a username with a domain
         
         //If there is no domain present fail the authentication
         if (authenticationID.indexOf('@')==-1) {
            throw new SaslException("Invalid credentials. Unable to deduce domain name (required).");
         }
         if (directives.get("realm")==null) {
            realm = "null";
         }
         
         //SASL DIGEST-MD5 rules
         //authzid
            //The "authorization ID" as per RFC 2222, encoded in UTF-8. This
            //directive is optional. If present, and the authenticating user has
            //sufficient privilege, and the server supports it, then after
            //authentication the server will use this identity for making all
            //accesses and access checks. If the client specifies it, and the
            //server does not support it, then the response-value will be
            //incorrect, and authentication will fail.
         
         //SASL DIGEST-MD5 rules
         //charset (digest-challenge)
            //This directive, if present, specifies that the server supports
            //UTF-8 encoding for the username and password. If not present, the
            //username and password must be encoded in ISO 8859-1 (of which
            //US-ASCII is a subset). The directive is needed for backwards
            //compatibility with HTTP Digest, which only supports ISO 8859-1.
            //This directive may appear at most once; if multiple instances are
            //present, the client should abort the authentication exchange.
         
         //SASL DIGEST-MD5 rules
         //charset (digest-response)
            //This directive, if present, specifies that the client has used
            //UTF-8 encoding for the username and password. If not present, the
            //username and password must be encoded in ISO 8859-1 (of which
            //US-ASCII is a subset). The client should send this directive only
            //if the server has indicated it supports UTF-8. The directive is
            //needed for backwards compatibility with HTTP Digest, which only
            //supports ISO 8859-1.
         
         //DIGEST rules The realm value (case-sensitive)
         
         //DIGEST rules
         //realm
            //A string to be displayed to users so they know which username and
            //password to use. This string should contain at least the name of
            //the host performing the authentication and might additionally
            //indicate the collection of users who might have access. An example
            //might be "registered_users@gotham.news.com".
         
         //DIGEST rules
         //username
            //The user's name in the specified realm.
         
         //SASL DIGEST-MD5 rules
         //realm
            //Mechanistically, a string which can enable users to know which
            //username and password to use, in case they might have different
            //ones for different servers. Conceptually, it is the name of a
            //collection of accounts that might include the user's account. This
            //string should contain at least the name of the host performing the
            //authentication and might additionally indicate the collection of
            //users who might have access. An example might be
            //"registered_users@gotham.news.example.com".  This directive is
            //optional; if not present, the client SHOULD solicit it from the
            //user or be able to compute a default; a plausible default might be
            //the realm supplied by the user when they logged in to the client
            //system. Multiple realm directives are allowed, in which case the
            //user or client must choose one as the realm for which to supply to
            //username and password.
         
         //SASL DIGEST-MD5 rules
         //username
            //The user's name in the specified realm, encoded according to the
            //value of the "charset" directive. This directive is required and
            //MUST be present exactly once; otherwise, authentication fails.
         
         //SASL DIGEST-MD5 rules
         //realm
            //The realm containing the user's account. This directive is
            //required if the server provided any realms in the
            //"digest-challenge", in which case it may appear exactly once and
            //its value SHOULD be one of those realms. If the directive is
            //missing, "realm-value" will set to the empty string when computing
            //A1 (see below for details).
         
         //SASL DIGEST-MD5 rules
         //The "username-value", "realm-value" and "passwd" are encoded
         //according to the value of the "charset" directive. If "charset=UTF-8"
         //is present, and all the characters of either "username-value" or
         //"passwd" are in the ISO 8859-1 character set, then it must be
         //converted to ISO 8859-1 before being hashed. This is so that
         //authentication databases that store the hashed username, realm and
         //password (which is common) can be shared compatibly with HTTP, which
         //specifies ISO 8859-1. A sample implementation of this conversion is
         //in section 8.
         
         
         Realm requestRealm = realm.equals("null")?Realm.getNullRealm():ConfigurationManager.getInstance().getRealm(realm);
         //Could be a domain
         if (requestRealm==null&&!realm.equals("null")) {
            requestRealm = new Realm(new Domain(realm));
         }
         
         char[] password = configurationManager.getRealmPassword(requestRealm, EmailAddress.getEmailAddress(authenticationID));
         if (password==null) {
            
            //Compatibility mode with previous versions
            String compAuthenticationID = authenticationID.substring(0, authenticationID.indexOf('@')+1)+authenticationID.substring(authenticationID.indexOf('@'));

            password = configurationManager.getRealmPassword(requestRealm, EmailAddress.getEmailAddress(compAuthenticationID));
            
            if (password==null) {
               log.warn("The supplied username/password and/or realm do(es) not match a registered entry");
               throw new SaslException("User "+authenticationID+" not authenticated",
                     pop3?new AuthenticationException(Pop3Processor.MESSAGE_INVALID_LOGIN):new AuthenticationException());
            }
         }

            String[] hexResponseHash = getHexResponseHash(directives, digestURI, password);
            //Compatibility mode with previous versions
            if (!hexResponseHash[0].equals(directives.get("response"))) {
               
               String compAuthenticationID = authenticationID.substring(0, authenticationID.indexOf('@')+1)+authenticationID.substring(authenticationID.indexOf('@'));
               Realm compRequestRealm = requestRealm.isNullRealm()
                     ? requestRealm
                     : requestRealm.isDomainRealm()
                     ? requestRealm
                     : new Realm(requestRealm.getCollection().toLowerCase(ConfigurationManager.LOCALE), requestRealm.getDomain());
               
               password = configurationManager.getRealmPassword(compRequestRealm, EmailAddress.getEmailAddress(compAuthenticationID));
               hexResponseHash = getHexResponseHash(directives, digestURI, password);
            }
            if (hexResponseHash[0].equals(directives.get("response"))) {
               
               MessageDigest md = null;
               try {
                  md = JESMessageDigest.getInstance("MD5");
               }
               catch (NoSuchAlgorithmException gse) {
                  throw new SaslException(gse.getMessage());
               }
               final Charset iso = iso88591Charset;
               
               md.update(":".getBytes(iso));
               md.update(digestURI.getBytes(iso));
               if (!directives.get("qop").equals(QOP_AUTH)) {
                  md.update(":".getBytes(iso));
                  md.update("00000000000000000000000000000000".getBytes(iso));
               }
               byte[] HA2 = md.digest();
               String HA2HEX = new String(ByteUtils.toHex(HA2));
               md.update(hexResponseHash[1].getBytes(iso));
               md.update(":".getBytes());
               md.update((directives.get("nonce")).getBytes(iso));
               md.update(":".getBytes());
               md.update((directives.get("nc")).getBytes(iso));
               md.update(":".getBytes());
               md.update((directives.get("cnonce")).getBytes(iso));
               md.update(":".getBytes());
               md.update((directives.get("qop")).getBytes(iso));
               md.update(":".getBytes());
               md.update(HA2HEX.getBytes(iso));
               byte[] responseHash = md.digest();
               finalizeAuthentication.finalize(authenticationID, authorizationID, null);

               if (integrity) {
                   wrapper = new DigestMd5ServerMode.Wrapper();
               } else if (privacy) {
                   wrapper = new DigestMd5ServerMode.ConfidentWrapper();
               }

               log.info( "User: " + authenticationID + " logged in successfully.");
               return Base64.encodeBase64(("rspauth="+new String(ByteUtils.toHex(responseHash))).getBytes(usAsciiCharset));
            } else {
               log.warn("Could not verify response hash for user: "+authenticationID);
               throw new SaslException("User "+authenticationID+" not authenticated",
                     pop3?new AuthenticationException(Pop3Processor.MESSAGE_INVALID_LOGIN):new AuthenticationException());
            }
      }
   }
   
   private String[] getHexResponseHash(Map<String,String> directives, String digestURI, char[] password) throws SaslException {
      byte[] HA1 = ByteUtils.toByteArray(password);
         for (int i=password.length-1;i>=0;i--) {
            password[i] = 0xff;
         }

         MessageDigest md = null;
         try {
            md = JESMessageDigest.getInstance("MD5");
         }
         catch (NoSuchAlgorithmException gse) {
            throw new SaslException(gse.getMessage());
         }
         
         final Charset iso = iso88591Charset;
         md.update(HA1);
         md.update(":".getBytes());
         md.update((directives.get("nonce")).getBytes(iso));
         md.update(":".getBytes());
         md.update((directives.get("cnonce")).getBytes(iso));
         if (authorizationID!=null) {
            md.update(":".getBytes());
            md.update((directives.get("authzid")).getBytes(utf8Charset));
         }
         MD5DigestSessionKey = HA1 = md.digest();
         String MD5DigestSessionKeyToHex = new String(ByteUtils.toHex(HA1));
         md.update("AUTHENTICATE".getBytes(iso));
         md.update(":".getBytes(iso));
         md.update(digestURI.getBytes(iso));
         if (!directives.get("qop").equals(QOP_AUTH)) {
            md.update(":".getBytes(iso));
            md.update("00000000000000000000000000000000".getBytes(iso));
         }
         byte[] HA2 = md.digest();
         String HA2HEX = new String(ByteUtils.toHex(HA2));
         md.update(MD5DigestSessionKeyToHex.getBytes(iso));
         md.update(":".getBytes());
         md.update((directives.get("nonce")).getBytes(iso));
         md.update(":".getBytes());
         md.update((directives.get("nc")).getBytes(iso));
         md.update(":".getBytes());
         md.update((directives.get("cnonce")).getBytes(iso));
         md.update(":".getBytes());
         md.update((directives.get("qop")).getBytes(iso));
         md.update(":".getBytes());
         md.update(HA2HEX.getBytes(iso));
         byte[] responseHash = md.digest();
         
         return new String[]{new String(ByteUtils.toHex(responseHash)), MD5DigestSessionKeyToHex};
   }
   
   protected class FinalizeAuthenticationSMTP extends AbstractSaslServerMode.FinalizeAuthentication {

      @Override
      public byte[] finalize(String authenticationID, String authorizationID, char[] password) throws SaslException{

            user = configurationManager.getUser(EmailAddress.getEmailAddress(authenticationID));
            if (authorizationID == null) {
               authorizationID = authenticationID;
            }
            DigestMd5ServerMode.this.authorizationID = authorizationID;
            log.info( "User " + authenticationID + " logged in successfully and authorized as "+authorizationID);
            completed = true;
            return null;
      }
   }
   
   protected class FinalizeAuthenticationPOP3 extends AbstractSaslServerMode.FinalizeAuthentication {

      @Override
      public byte[] finalize(String authenticationID, String authorizationID, char[] password) throws SaslException {
         
         EmailAddress emailAddress = EmailAddress.getEmailAddress(authenticationID);
         
         DeliveryService deliveryService = DeliveryService.getInstance();
         if(deliveryService.lockMailbox(emailAddress)) {
            deliveryService.addAuthenticated(clientIp);
            log.info( "User: " + authenticationID + " logged in successfully and authorized as "+authorizationID);
            completed = true;
            return null;
         }
         else {
            user = null;
            throw new SaslException(authenticationID+" mailbox is locked",
                  new AuthenticationException(Pop3Processor.MESSAGE_USER_MAILBOX_LOCKED));
         }
      }
   }

   public boolean isProtected() {
      return integrity||privacy;
   }

   @Override
   public User getUser() {
      return user;
   }

   private byte[] MD5Digest(String input) throws GeneralSecurityException {
      MessageDigest md = JESMessageDigest.getInstance("MD5");
      md.update(input.getBytes(usAsciiCharset));
      return md.digest();
   }

   private static final String QOP_AUTH = "auth";
   private static final String QOP_AUTH_INT = "auth-int";
   private static final String QOP_AUTH_CONF = "auth-conf";

   private static final String[] CIPHERSUITS = { "rc4-40", "rc4-56", "rc4", "des", "3des"};
   private static final int RC4_40 = 0;
   private static final int RC4_56 = 1;
   private static final int RC4 = 2;
   private static final int DES = 3;
   private static final int DES3 = 4;

   private static final byte[] EMPTY = new byte[0];

   @Override
   public String getMechanismName() {
       return "DIGEST-MD5";
   }

   @Override
   public byte[] unwrap(byte[] incoming, int offset, int length) throws SaslException {

      if (wrapper == null) {
         throw new IllegalStateException("Neither integrity nor privacy was negotiated");
      }
      if (!completed) {
         throw new IllegalStateException("Authentication still in progress");
      }

      return wrapper.unwrap(incoming, offset, length);
   }

   @Override
   public byte[] wrap(byte[] outgoing, int offset, int length) throws SaslException {

      if (wrapper == null) {
         throw new IllegalStateException("Neither integrity nor privacy was negotiated");
      }
      if (!completed) {
         throw new IllegalStateException("Authentication still in progress");
      }

      return wrapper.wrap(outgoing, offset, length);
   }

   @Override

   public void dispose() throws SaslException {
      clientIp = null;
      Iterator<List<Integer>> noncesEntries = nonces.values().iterator();
      while(noncesEntries.hasNext()) {
         noncesEntries.next().clear();
      }
      nonces.clear();
      nonces = null;
      user = null;
   }

   @Override
   public Object getNegotiatedProperty(String propName) {
      if(!completed) {
         throw new IllegalStateException("Authentication still in progress");
      }
      if (Sasl.QOP.equals(propName)) {
         if (privacy) {
            return "auth-conf";
         } else if (integrity) {
            return "auth-int";
         } else {
            return "auth";
         }
      }
      if (Sasl.MAX_BUFFER.equals(propName)) {
         return rcvMaxBuffer;
      }
      if (Sasl.RAW_SEND_SIZE.equals(propName)) {
         return sndMaxBuffer;
      }
      return null;
   }

   @Override
   public boolean isComplete() {
      return completed;
   }

    private class Wrapper {

        protected byte[] kis = new byte[16], kic = new byte[16];

        protected int serverSeqNum,  clientSeqNum;

        protected final byte[] sequenceNum = new byte[4];
        protected final byte[] messageTypeNBO = new byte[2];

        private Wrapper() throws SaslException {

            try {

               byte[] serverSalt = "Digest session key to server-to-client signing key magic constant".getBytes(iso88591Charset);
               byte[] clientSalt = "Digest session key to client-to-server signing key magic constant".getBytes(iso88591Charset);

               MessageDigest md5 = JESMessageDigest.getInstance("MD5");

               md5.update(MD5DigestSessionKey);
               md5.update(serverSalt);
               md5.digest(kis, 0, 16);
               
               md5.update(MD5DigestSessionKey);
               md5.update(clientSalt);
               md5.digest(kic, 0, 16);

               ByteUtils.getNetworkByteOrderFromInt(1, messageTypeNBO, 0, 2);
               
               sndMaxBuffer -= 16;

            } catch (GeneralSecurityException e) {
               throw new SaslException(e.getMessage(),e);
            }

        }

        public byte[] wrap(byte[] outgoing, int offset, int length)
            throws SaslException {

            if (length == 0) {
                return EMPTY;
            }

            byte[] wrapped = new byte[length+16];

            System.arraycopy(outgoing, offset, wrapped, 0, length);

            ByteUtils.getNetworkByteOrderFromInt(serverSeqNum++, sequenceNum,0,4);
            byte[] mac = computeHMAC(kis, sequenceNum, outgoing, offset, length);

            System.arraycopy(mac, 0, wrapped, length, 10);
            System.arraycopy(messageTypeNBO, 0, wrapped, length+10, 2);
            System.arraycopy(sequenceNum, 0, wrapped, length+12, 4);

            return wrapped;
        }

        public byte[] unwrap(byte[] incoming, int offset, int length)
            throws SaslException {

            if (length == 0) {
                return EMPTY;
            }
            int messageSize = length - 16;

            byte[] message = new byte[messageSize];
            byte[] seqNum = new byte[4];

            System.arraycopy(incoming, offset, message, 0, messageSize);
            System.arraycopy(incoming, offset+messageSize+12, seqNum, 0, 4);

            int messageType = ByteUtils.getIntegerFromNetworkByteOrder(incoming,offset+messageSize+10,2);
            if (messageType != 1) {
                throw new SaslException("Invalid message type: " +messageType);
            }

            int clientSeqNum = ByteUtils.getIntegerFromNetworkByteOrder(seqNum,0,4);
            if (clientSeqNum != this.clientSeqNum) {
                throw new SaslException("A message segment was received out of order. Expected: " +
                    this.clientSeqNum +" Received: "+clientSeqNum);
            }

            this.clientSeqNum++;

            byte[] mac = new byte[10];
            System.arraycopy(incoming, offset+messageSize, mac, 0, 10);

            byte[] expectedMac = computeHMAC(kic, seqNum, message, 0, messageSize);

            if (!Arrays.equals(mac, expectedMac)) {
                return EMPTY;
            }
            return message;
        }

        protected byte[] computeHMAC(byte[] kic, byte[] seqNum, byte[] message,
              int offset, int length) throws SaslException {

            byte[] completeMessage = new byte[4+length];
            System.arraycopy(seqNum, 0, completeMessage, 0, 4);
            System.arraycopy(message, offset, completeMessage, 4, length);

            try {
                SecretKey key = new SecretKeySpec(kic, "HmacMD5");
                Mac m = Mac.getInstance("HmacMD5");
                m.init(key);
                byte[] hmac = m.doFinal(completeMessage);

                byte mac[] = new byte[10];
                System.arraycopy(hmac, 0, mac, 0, 10);

                return mac;

            } catch (InvalidKeyException e) {
                throw new SaslException(e.getMessage(),e);
            } catch (NoSuchAlgorithmException e) {
                throw new SaslException(e.getMessage(),e);
            }
        }
    }

    private final class ConfidentWrapper extends DigestMd5ServerMode.Wrapper {

        private Cipher encodingCipher, decodingCipher;

        private ConfidentWrapper() throws SaslException {

           super();

           try {

               byte[] serverSalt = "Digest H(A1) to server-to-client sealing key magic constant".getBytes(iso88591Charset);
               byte[] clientSalt = "Digest H(A1) to client-to-server sealing key magic constant".getBytes(iso88591Charset);

               int n;
               if (sessionCipher.equals(CIPHERSUITS[RC4_40])) {
                   n = 5;
               } else if (sessionCipher.equals(CIPHERSUITS[RC4_56])) {
                   n = 7;
               } else {
                   n = 16;
               }

               MessageDigest messageDigest = JESMessageDigest.getInstance("MD5");

               byte[] temp =  new byte[n + serverSalt.length];
               System.arraycopy(MD5DigestSessionKey, 0, temp, 0, n);

               System.arraycopy(serverSalt, 0, temp, n, serverSalt.length);
               byte[] kcs = messageDigest.digest(temp);

               System.arraycopy(clientSalt, 0, temp, n, clientSalt.length);
               byte[] kcc = messageDigest.digest(temp);

               SecretKey encodingKey;
               SecretKey decodingKey;
               if ((sessionCipher.equals(CIPHERSUITS[DES])) ||
                  (sessionCipher.equals(CIPHERSUITS[DES3]))) {

                  String cipherName;

                  if (sessionCipher.equals(CIPHERSUITS[DES])) {
                     cipherName = "DES/CBC/NoPadding";
                     encodingKey = createDesKey(kcs);
                     decodingKey = createDesKey(kcc);
                  } else {
                     cipherName = "DESede/CBC/NoPadding";
                     encodingKey = createDesedeKey(kcs);
                     decodingKey = createDesedeKey(kcc);
                  }

                  encodingCipher = Cipher.getInstance(cipherName);
                  IvParameterSpec encodingIV = new IvParameterSpec(kcs, 8, 8);
                  encodingCipher.init(Cipher.ENCRYPT_MODE, encodingKey, encodingIV);

                  decodingCipher = Cipher.getInstance(cipherName);
                  IvParameterSpec decodingIV = new IvParameterSpec(kcc, 8, 8);
                  decodingCipher.init(Cipher.DECRYPT_MODE, decodingKey, decodingIV);

               }
               else {
                  encodingCipher = Cipher.getInstance("RC4");
                  encodingKey = new SecretKeySpec(kcs, "RC4");
                  encodingCipher.init(Cipher.ENCRYPT_MODE, encodingKey);

                  decodingCipher = Cipher.getInstance("RC4");
                  decodingKey = new SecretKeySpec(kcc, "RC4");
                  decodingCipher.init(Cipher.DECRYPT_MODE, decodingKey);

               }
               
               //Due to variable message padding reduce the sndMaxBuffer by 16 bytes
               //and the maximum padding length
               sndMaxBuffer -= 26;

            } catch (GeneralSecurityException e) {
               throw new SaslException(e.getMessage());
            }

        }

        @Override
        public byte[] wrap(byte[] outgoing, int offset, int length) throws SaslException {

            if (length == 0) {
                return EMPTY;
            }

            ByteUtils.getNetworkByteOrderFromInt(serverSeqNum++, sequenceNum,0,4);
            byte[] mac = computeHMAC(kis, sequenceNum, outgoing, offset, length);

            int blockSize = encodingCipher.getBlockSize();
            byte paddingSize = blockSize==1?0:(byte)((blockSize - ((length + 10) % blockSize))&0xff);

            byte[] toBeEncrypted = new byte[length+paddingSize+10];

            System.arraycopy(outgoing, offset, toBeEncrypted, 0, length);
            for (int i=0;i<paddingSize;i++) {
               toBeEncrypted[length+i] = paddingSize;
            }
            System.arraycopy(mac, 0, toBeEncrypted, length+paddingSize, 10);

            byte[] encryptedMessage = encodingCipher.update(toBeEncrypted);
            if (encryptedMessage == null) {
               throw new SaslException("Error encrypting outgoing message");
            }
            int encryptedMessageSize = encryptedMessage.length;

            byte[] wrapped = new byte[encryptedMessageSize+6];
            System.arraycopy(encryptedMessage, 0, wrapped, 0, encryptedMessageSize);
            System.arraycopy(messageTypeNBO, 0, wrapped, encryptedMessageSize, 2);
            System.arraycopy(sequenceNum, 0, wrapped, encryptedMessageSize+2, 4);

            return wrapped;
        }

        @Override
        public byte[] unwrap(byte[] incoming, int offset, int length) throws SaslException {

            if (length == 0) {
                return EMPTY;
            }
            int toBeDecryptedSize = length - 6;

            byte[] toBeDecrypted = new byte[toBeDecryptedSize];
            byte[] seqNum = new byte[4];

            System.arraycopy(incoming, offset,toBeDecrypted, 0, toBeDecryptedSize);
            System.arraycopy(incoming, offset+toBeDecryptedSize+2,seqNum, 0, 4);

            int messageType = ByteUtils.getIntegerFromNetworkByteOrder(incoming,offset+toBeDecryptedSize+10,2);
            if (messageType != 1) {
                throw new SaslException("Invalid message type: "+messageType);
            }

            int clientSeqNum = ByteUtils.getIntegerFromNetworkByteOrder(seqNum,0,4);
            if (clientSeqNum != this.clientSeqNum) {
                throw new SaslException("A message segment was received out of order. Expected: " +
                    this.clientSeqNum +" Received: "+clientSeqNum);
            }

            this.clientSeqNum++;

            byte[] decryptedMessage = decodingCipher.update(toBeDecrypted);
            if (decryptedMessage == null) {
               throw new SaslException("Error decrypting incoming message");
            }
            int paddedMessageSize = decryptedMessage.length - 10;

            byte[] paddedMessage = new byte[paddedMessageSize];
            System.arraycopy(decryptedMessage, 0, paddedMessage, 0, paddedMessageSize);

            byte[] mac = new byte[10];
            System.arraycopy(decryptedMessage, paddedMessageSize, mac, 0, 10);

            int blockSize = decodingCipher.getBlockSize();
            if (blockSize > 1) {

                paddedMessageSize -= paddedMessage[paddedMessageSize - 1];
                if (paddedMessageSize < 0) {
                    return EMPTY;
                }
            }

            byte[] expectedMac = computeHMAC(kic, seqNum, paddedMessage, 0, paddedMessageSize);

            if (!Arrays.equals(mac, expectedMac)) {
                return EMPTY;
            }

            if (paddedMessageSize == paddedMessage.length) {
                return paddedMessage;
            } else {
                byte[] message = new byte[paddedMessageSize];
                System.arraycopy(paddedMessage, 0, message, 0, paddedMessageSize);
                return message;
            }
        }
    }

    private static SecretKey createDesKey(byte[] rawKey) throws GeneralSecurityException{

       byte[] edeKeyBaseBytes = ByteUtils.convert8bitTo7bit(rawKey, 0, true);
       ByteUtils.computeAndSetParityBit(edeKeyBaseBytes);
       SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("des");
       KeySpec keySpec = new DESKeySpec(edeKeyBaseBytes, 0);
       return secretKeyFactory.generateSecret(keySpec);

    }

    private static SecretKey createDesedeKey(byte[] rawKey) throws GeneralSecurityException{

       byte[] edeKeyBaseBytes = ByteUtils.convert8bitTo7bit(rawKey, 0, true);
       ByteUtils.computeAndSetParityBit(edeKeyBaseBytes);
       byte[] edeKeyBaseBytes2 = ByteUtils.convert8bitTo7bit(rawKey, 7, true);
       ByteUtils.computeAndSetParityBit(edeKeyBaseBytes2);
       byte[] desedeKeyBaseBytes = new byte[24];
       System.arraycopy(edeKeyBaseBytes, 0, desedeKeyBaseBytes, 0, 8);
       System.arraycopy(edeKeyBaseBytes2, 0, desedeKeyBaseBytes, 8, 8);
       System.arraycopy(edeKeyBaseBytes, 0, desedeKeyBaseBytes, 16, 8);
       SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("desede");
       KeySpec keySpec = new DESedeKeySpec(desedeKeyBaseBytes, 0);
       return secretKeyFactory.generateSecret(keySpec);
       
    }
    
    private static final String ISO_8859_1 = "ISO-8859-1";
    private static final String UTF_8 = "UTF-8";
}
