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

/**
 * This interface defines the names for all configuration properties
 * loaded from the configuration file.  This interface also serves as
 * documentation for the possible parameters and their expected values.
 * <p>
 * Descriptions of the parameters are also available in the ConfigurationTool
 * java application included with this distribution.
 *
 * @author Eric Daugherty
 * @author Andreas Kyrmegalos (2.x branch)
 */
public interface ConfigurationParameterConstants {

    //***************************************************************
    // Configuration Sections
    //***************************************************************

   public static final String DIRECTORIES = "directories";
   public static final String GENERAL = "general";
   public static final String BACKEND = "backend";
   public static final String MAIL = "mail";
   public static final String AMAVISDNEW = "amavis-dnew";
   public static final String CBC = "cbc";
   public static final String JAVAWRAPPER = "javaWrapper";
   
   public static final String RESTART = "$restart";
   
   public static final String BACKEND_DB_DIRECTORY = "backend.db.directory";

   public static final String BACKEND_SECURE = "backend.secure";
    
   public static final String BACKEND_DB_HOST = "backend.db.host";

   public static final String BACKEND_DB_PORT = "backend.db.port";

   public static final String BACKEND_DB_USERNAME = "backend.db.username";

   public static final String GUI_DB_USERNAME = "gui.db.username";

   public static final String BACKEND_MINIMUM = "backend.minimum";

   public static final String BACKEND_MAXIMUM = "backend.maximum";

   public static final String MAIL_TRANSFER_MODE = "mail.transfer.mode";

   public static final String MAIL_RETRIEVAL_MODE = "mail.retrieval.mode";

    /**
     * The local IP address that the server will listen on.  If this
     * property is not set, or is invalid, the server will listen on all
     * addresses.
     */
    public static final String LISTEN_ADDRESS = "listen.address";

    /**
     * The directory used to store incoming messages.
     *
     */
    public static final String SMTPDIRECTORY = "incoming.directory";

    /**
     * The directory used to store the user accounts.
     *
     */
    public static final String USERSDIRECTORY = "users.directory";

    /**
     * The directory used to store failed incoming messages.
     *
     */
    public static final String FAILEDDIRECTORY = "failed.directory";
    
    public static final String CONFIG_ADDRESS = "configuration.address";
    public static final String CONFIG_PORT = "configuration.port";
    public static final String CONFIG_SECURE = "configuration.secure";

    /**
     * The server stores incoming SMTP messages on disk before attempting to deliver them.  This
     * setting determines how often (in seconds) the server checks the disk for new messages to deliver.  The
     * smaller the number, the faster message will be processed.  However, a smaller number will cause
     * the server to use more of your system's resources.
     */
    public static final String THROTTLING_STEP = "throttling.step";

    /**
     * The pop3port parameter defines the port to listen to incoming
     * Pop3 connection on.  By default, this value should be 110.
     */
    public static final String POP3PORT = "pop3port";

    /**
     * The smtpport parameter defines the port to listen to incoming
     * SMTP connection on.  By default, this value should be 25.
     */
    public static final String SMTPPORT = "smtpport";
    
    public static final String LOCALE_COUNTRY = "locale.country";
    
    public static final String LOCALE_LANGUAGE = "locale.language";
    
    public static final String FILE_ENCODING = "file.encoding";
    
    public static final String OS_NAME = "os.name";

    /**
     * The domains parameter defines the domain names that this server
     * will accept mail for.  All domains not listed here will will either
     * be relayed or ignored.  To configure mulitiple domains, just seperate
     * each domain with a comma.
     */
    public static final String DOMAINS = "domains";

    /**
     * The number of threads that will be allocated to each connection listener
     * for each port.
     */
    public static final String EXECUTE_THREADS = "threads";

    public static final String MIME8BIT = "8buitmime.enable";

    //***************************************************************
    // Mail Delivery Parameters
    //***************************************************************

    /**
     * The default user to deliver mail addressed to local unknown users.
     */
    public static final String DEFAULT_MAILBOX = "defaultMailbox";

    /**
     * The default server to deliver mail addressed to remote users.
     */
    public static final String DEFAULT_SMTP_SERVERS = "defaultsmtpservers";

    /**
     * Enables the POP3 login as a valid address for SMTP Relaying.
     */
    public static final String RELAY_POP_BEFORE_SMTP = "relay.popbeforesmtp";

    /**
     * This setting only applies when RELAY_POP_BEFORE_SMTP is set to true.
     * This setting defines the timeout period (in minutes) between when a
     * user (ip address) last authenticated with the POP3 server and when they will
     * no longer be able to send SMTP mail to remote domains. This option defaults
     * to 10 minutes.
     */
    public static final String RELAY_POP_BEFORE_SMTP_TIMEOUT = "relay.popbeforesmtp.timeout";

    /**
     * This is the label for the UI
     */
    public static final String RELAY_ADDRESSLIST = "relay.ipaddresses";

    /**
     * Enables some email addresses for SMTP Relaying.
     */
    public static final String RELAY_EMAILSLIST = "relay.emailaddresses";

    /**
     * The server picks the messages from the disk in order to deliver them.  If some message
     * cannot be delivered to remote SMTP server at that moment, because of some error, then the message
     * will be kept on the disk for later delivery attempt. However server can't retry delivery
     * indefinitely, therefore following config entry will set maximum number of retries before the server
     * gives up on the message and moves it from smtp spool directory to failed directory.
     */
    public static final String SMTP_DELIVERY_THRESHOLD = "smtpdelivery.threshold";

    /**
     * This setting limits the size of incoming SMTP messages.  This setting
     * (in megabytes) will cause emails over the max size to be rejected.
     */
    public static final String SMTP_MAX_MESSAGE_SIZE = "smtp.messagesize";

    //***************************************************************
    // User Parameters
    //***************************************************************

    /**
     * Defines the prefix to usernames stored in the properties file.
     * A username should be stored as:
     * USER_DEF_PREFIX&lt;user@domain.com&gt;=&lt;password&gt;
     */
    public static final String USER_DEF_PREFIX = "user.";
    
    /**
     * Defines the prefix to realms stored in the properties file.
     * 
     */
    public static final String REALM_DEF_PREFIX = "realm.";

    /**
     * Defines the prefix for user properties usernames stored in the
     * properties file.
     * A user property should be stored as:
     * USER_PROPERTY_PREFIX&lt;user@domain.com&gt;.&lt;property name&gt;=&lt;value&gt;
     */
    public static final String USER_PROPERTY_PREFIX = "userprop.";

   /**
    * The USER_PROPERTY_PREFIX&lt;user@domain.com&gt;.&lt;forwardAddresses&gt;=&lt;value&gt;
    * property defines a comma separated
    * list of addresses that mail to this user will be forwarded to.
    */
    public static final String USER_FILE_FORWARDS = ".forwardAddresses";

    /**
     * A parameter that toggles TLS/SSL use on standard ports.
     */
    public static final String STANDARDSMTPSECURE = "standardSMTPsecure";
    public static final String STANDARDPOP3SECURE = "standardPOP3secure";
        
    /** Controls whether or not the secure server modules will be activated */
    public static final String SECUREACTIVE = "secureactive";
    
    /** Controls whether or not clear text passwords are allowed */
    public static final String ALLOWCLEARTEXT = "allowClearText";
    
    public enum ClearText {
       NEVER("never"), 
       ENCRYPTED_ONLY("encryptedOnly"),
       ALWAYS("always");
       
       private final String literal;
       
       ClearText(String literal) {
          this.literal = literal;
       }
       
       public String getLiteral() {
          return literal;
       }
    }
    
    /**
     * The pop3port parameter defines the port to listen to incoming
     * Pop3 connection on.  By default, this value should be 995.
     */
    public static final String SECUREPOP3PORT = "securepop3port";

    /**
     * The smtpport parameter defines the port to listen to incoming
     * SMTP connection on.  By default, this value should be 465.
     */
    public static final String SECURESMTPPORT = "securesmtpport";

    /**
     * Use TLS/SSL if the server being contacted when sending messages
     * supports the "STARTTLS" extension (RFC 3207)
     */
    public static final String OUTGOINGSECURE = "outgoingSecure";

    /**
     * The number of threads that will be allocated to each secure connection listener
     * for each port.
     */
    public static final String SECURE_EXECUTE_THREADS = "securethreads";

    /** The keystore complete path */
    public static final String KEYSTORELOCATION = "keystore.location";

    /** The keystore provider */
    public static final String KEYSTOREPROVIDER = "keystore.provider";

    /** The keystore type */
    public static final String KEYSTORETYPE = "keystore.type";

    /** The truststore type */
    public static final String TRUSTSTORETYPE = "truststore.type";

    /** The truststore complete path */
    public static final String TRUSTSTORELOCATION = "truststore.location";

    /** The truststore provider */
    public static final String TRUSTSTOREPROVIDER = "truststore.provider";

    public static final String CRYPTOGRAPHY = "cryptography";

    public static final String ENABLE_SECURITYMANAGER = "enable.SecurityManager";
    
    public static final String PERSIST_MASTER = "persist.master";

    public static final String ENABLE_HELO = "enable.HELO";

    public static final String NOTIFY_DEFAULT = "notify.default";

    public static final String DISABLE_WARNING = "disable.warning";

    public static final String EXTERNAL_DELEGATED = "external.delegated";

    public static final String REJECT_NON_EXISTENT_LOCAL = "reject.nonexistent.local";

    /**
     * The maximum total number of errors before the connection is forcibly
     * closed
     */
    public static final String MAX_ERROR_COUNT = "max.error.count";

    public static final String MAX_VALID_RCPT = "max.valid.rcpt";

    public static final String ADD_PCT_RCPT = "added.pct.rcpt";

    public static final String MIN_TOT_FAIL_RCPT = "min.tot.fail.rcpt";

    public static final String MIN_PCT_FAIL_RCPT = "min.pct.fail.rcpt";

    public static final String MAX_PASS_ATTEMPTS = "max.pass.attempts";

    public static final String VERIFY_IP = "verify.ip";

    public static final String SASL_QOP = "sasl.qop";

    public static final String SASL_CRAM_MEMBERS = "sasl.cram.members";
    
    public static final String SASL_CRAM_TRUNCATE = "sasl.cram.truncate";

    public static final String SASL_DIGEST_MD5_ENABLE  = "sasl.digest-md5.enable";

    public static final String SASL_DIGEST_MD5_CIPHERS = "sasl.digest-md5.ciphers";

    public static final String SASL_SCRAM_MEMBERS = "sasl.scram.members";

    public static final String SASL_GSS_ENABLE  = "sasl.GSS.enable";

    public static final String SASL_GSS_REALM = "sasl.GSS.realm";

    public static final String SASL_GSS_KDC = "sasl.GSS.kdc";

    public static final String SASL_GSS_PRINCIPAL = "sasl.GSS.principal";

    public static final String SASL_GSS_STOREKEY = "sasl.GSS.storeKey";

    public static final String SASL_GSS_USEKEYTAB = "sasl.GSS.useKeyTab";

    public static final String SASL_GSS_KEYTAB = "sasl.GSS.keyTab";

    //***************************************************************
    // amavisd-new parameters
    //***************************************************************

    public static final String ACTIVATE_AMAVISD = "activate.amavisd";

    public static final String AMAVISD_INCOMING_DIRECTORY = "amavisd.incoming.directory";

    public static final String AMAVISD_LISTEN_ADDRESS = "amavisd.listen.address";

    public static final String AMAVISD_SMTPPORT = "amavisd.smtpport";

    public static final String AMAVISD_FILTERED_SMTPPORT = "amavisd.filtered.smtpport";

    //***************************************************************
    // Testing Grounds parameters
    //***************************************************************

    public static final String TESTING_DESTINATION = "testing.destination";


    static final String LINE_SEPARATOR = System.getProperty("line.separator");

    static final String USER_PROPERTIES_HEADER =
        "# Java Email Server (JES) User Configuration" + LINE_SEPARATOR +
        "#" + LINE_SEPARATOR +
        "# All users are defined in this file.  To add a user, follow" + LINE_SEPARATOR +
        "# the succeeding pattern:" + LINE_SEPARATOR +
        "# user.<username@domain>=<plain text password>" + LINE_SEPARATOR +
        "#" + LINE_SEPARATOR +
        "# The plain text password will be converted to a hash when the file" + LINE_SEPARATOR +
        "# is first loaded by the server." + LINE_SEPARATOR +
        "#" + LINE_SEPARATOR +
        "# Additional configuration such as forward addresses can be specified as:" + LINE_SEPARATOR +
        "# userprop.<username@domain>.forwardAddresses=<Comma list of forward addresses>" + LINE_SEPARATOR +
        "#" + LINE_SEPARATOR +
        "# When a message is received for a local user, the user's address will be replaced" + LINE_SEPARATOR +
        "# with the addresses in the forwardAddresses property.  If you also wish to have" + LINE_SEPARATOR +
        "# a copy delivered to the local user, you may add the user's local address to" + LINE_SEPARATOR +
        "# the forwardAddresses property." + LINE_SEPARATOR +
        "# Adding a user while the server is running allows a user to be a member" + LINE_SEPARATOR +
        "# of the null realm alone. If it is desired that this user be a part of any" + LINE_SEPARATOR +
        "# number of realms, the server has to go offline and the user respecified in this" + LINE_SEPARATOR +
        "# file. Then add the user to the desired realms and restart the server." + LINE_SEPARATOR +
        "# Alternatively, if the purpose is to add(delete) a user without restarting the" + LINE_SEPARATOR +
        "# server, then first add(delete) the user to(from) the realms file and subsequently" + LINE_SEPARATOR +
        "# from the users file. Please note that in order to save a user's realm password," + LINE_SEPARATOR +
        "# either the user has to be newly added, or the user's password in the users file" + LINE_SEPARATOR +
        "# has to be reentered. If an existing user is added to a realm and the password's" + LINE_SEPARATOR +
        "# hash is stored in the users file rather than the original plaintext password," + LINE_SEPARATOR +
        "# then the no realm password will be recorded for the existing user. This applies" + LINE_SEPARATOR +
        "# whether or not the realm is a new or existing one.";

    static final String REALMS_PROPERTIES_HEADER =
        "# Java Email Server (JES) Realm Configuration"+ LINE_SEPARATOR +
        "#" + LINE_SEPARATOR +
        "# All realms are defined in this file.  To add a realm, follow" + LINE_SEPARATOR +
        "# the following pattern:" + LINE_SEPARATOR +
        "# realm.<realm-name@domain>=<user@domain>,<user@domain>,<user@domain>" + LINE_SEPARATOR+
        "#" + LINE_SEPARATOR +
        "# Multiple realms can be specified for a given domain." + LINE_SEPARATOR +
        "# e.g realmA@example.com, realmB@example.com." + LINE_SEPARATOR +
        "# A user can belong to any number of realms for a specified domain." + LINE_SEPARATOR +
        "# There can be only one instance of <realm-name@domain>." + LINE_SEPARATOR +
        "# A realm-name is unique only for a specific domain." + LINE_SEPARATOR +
        "# Thus start@example.com and start@example.net are allowed." + LINE_SEPARATOR +
        "# For a realm to be considered valid the domain has to be defined" + LINE_SEPARATOR +
        "# as a 'local' one in mail.xml." + LINE_SEPARATOR +
        "# For instructions on updates to this file while there's a running" + LINE_SEPARATOR +
        "# instance see user.conf.";

}
//EOF
