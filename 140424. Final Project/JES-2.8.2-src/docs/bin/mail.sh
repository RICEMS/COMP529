#! /bin/sh
JAVA_EXEC="java"
JES_HOME="/usr/local/jes"

$JAVA_EXEC -client -Xmx512m -Ddns.simple=true -Ddns.mode=recursive -Dsun.net.spi.nameservice.provider.1=dns,dnsJES -Djava.security.policy=$JES_HOME/jes.policy -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dlog4j.configuration=file:$JES_HOME/conf/log4j.properties -Djava.library.path=$JES_HOME/lib -cp $JES_HOME/jes.jar:$JES_HOME/lib/log4j-1.2.17.jar com.ericdaugherty.mail.server.Mail $JES_HOME $1

# Use the next command, if you want to use the jdk14 logger
#$JAVA_EXEC -client -Xmx512m -Ddns.simple=true -Ddns.mode=recursive -Dsun.net.spi.nameservice.provider.1=dns,dnsJES -Djava.security.policy=$JES_HOME/jes.policy -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.class=com.ericdaugherty.mail.server.logging.jdk14.LoggingConfigLoader -Djava.util.logging.config.file=$JES_HOME/conf/jdk14.properties -Djes.install.directory=$JES_HOME -Djava.library.path=$JES_HOME/lib -cp $JES_HOME/jes.jar com.ericdaugherty.mail.server.Mail $JES_HOME $1