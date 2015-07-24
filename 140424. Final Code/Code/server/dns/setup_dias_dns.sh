#!/bin/bash

#apt-get purge bind9
#apt-get install -y bind9 apparmor-utils dnsutils

DOM="elec529.recg.rice.edu"

#USERS="adriana clay ellis haihua yanda"
USERS="clay ellis adriana yanda haihua azalia erg" #keep the order we had

IPBASE="168.7.138."
IPSTART="105"
NSIP="104"

#apparmor really doesn't like it if you put this in /etc/bind/
DIR="/var/lib/bind/"
mkdir -p $DIR
chown bind:bind $DIR
FILE="${DIR}db.$DOM"
REVFILE="${DIR}db.${IPBASE:0:3}"
#FILE="/etc/bind/db.$DOM"

rm $FILE

echo "\$TTL 1"  >> $FILE
echo "\$ORIGIN ${DOM}."  >> $FILE
echo -e "@      IN      SOA     ns.${DOM}. root.${DOM}. ( \n\t\t\t\t`date +%s` \n\t\t\t\t5 \n\t\t\t\t1 \n\t\t\t\t28800 \n\t\t\t\t60 )" >> $FILE
echo "" >> $FILE
echo "IN      NS      ns.${DOM}." >> $FILE
echo "ns        IN      A       ${IPBASE}${NSIP}"  >> $FILE
echo "@			IN      A       ${IPBASE}${NSIP}"  >> $FILE
echo "" >> $FILE

rm $REVFILE
echo "\$TTL 2"  >> $REVFILE
echo -e "@      IN      SOA     ns.${DOM}. root.${DOM}. ( \n\t\t\t\t`date +%s` \n\t\t\t\t5 \n\t\t\t\t1 \n\t\t\t\t28800 \n\t\t\t\t60 )" >> $REVFILE
echo "" >> $REVFILE
echo "@	IN	NS	ns." >> $REVFILE
echo "${NSIP}	IN	PTR	ns.${DOM}." >> $REVFILE
echo "" >> $FILE

i=$IPSTART
for user in $USERS
do
        echo "\$ORIGIN ${user}.${DOM}."  >> $FILE #origin is really unnecessary since we are using the FQDNs below -- it does make for a nice section header though.
		#echo "@ IN      A       ${IPBASE}${i}"  >> $FILE
		#echo "${user}.${DOM}.	IN      A       ${IPBASE}${i}"  >> $FILE
		echo "${user}.${DOM}.	IN      A       127.0.${i}.1"  >> $FILE #by default, no hosts are connected...
		echo "m.${user}.${DOM}.	IN      A       ${IPBASE}${i}"  >> $FILE
		echo "0.${user}.${DOM}.	IN      CNAME       m.${user}.${DOM}."  >> $FILE
		echo "${i}	IN	PTR	${user}.${DOM}." >> $REVFILE
        echo "${user}.${DOM}.   IN      MX       1      ${user}.${DOM}."  >> $FILE
        j=1
        #might want to use `shuf` to randomize this or something
        for u in $USERS
        do
                if [ $u != $user ]
                then                        
                        echo "$j.${user}.${DOM}.        IN      CNAME   ${u}.${DOM}."  >> $FILE
                        echo "${user}.${DOM}.   IN      MX       ${j}0    ${u}.${DOM}."  >> $FILE
                        j=`expr $j + 1`
                fi
        done
        echo "" >> $FILE
        i=`expr $i + 1`
done

if [ "$1" == "setconf" ]
then
        echo "Setting conf...  (You only need to do this once per domain!!!)"
        echo "zone \"${DOM}\" {
	type master;
	file \"$FILE\";
	notify yes;
	allow-update { key ${DOM}.; };
	allow-transfer { any; };
	allow-query { any; };		
};"  >> /etc/bind/named.conf.local

        echo "zone \"`echo $IPBASE | awk -F. '{print $3 "." $2 "." $1}'`.in-addr.arpa\" {
	type master;
	file \"$REVFILE\";
	notify yes;
	allow-update { key ${DOM}.; };
	allow-transfer { any; };
	allow-query { any; };		
};"  >> /etc/bind/named.conf.local


	#setup nsupdate
	echo "Generating dnssec keys, this may take a moment..."
	rm K${DOM}* #clear the old keys out
	dnssec-keygen -a HMAC-MD5 -b 128 -n HOST ${DOM}
	#mv ${DOM}*.key ${DOM}.key
	#mv ${DOM}*.private ${DOM}.private
	echo "key ${DOM}. {
	algorithm HMAC-MD5;
	secret \"`awk '{print $(NF);}' K${DOM}*.key`\";
};" > /etc/bind/keys.conf
	echo "include \"/etc/bind/rndc.key\";" >> /etc/bind/named.conf
	echo "include \"/etc/bind/keys.conf\";" >> /etc/bind/named.conf
	#aa-complain /usr/sbin/named #disable apparmor

	#also had to run rndc-confgen and save it to /etc/rndc.conf...
	echo "Running rndc-confgen, this may take a moment..."
	rndc-confgen > /etc/rndc.conf	
	
	echo "You may still need to add listen-on { ${IPBASE}${NSIP}; }; to the /etc/bind/named.conf.options file."
fi
#chown bind:bind $FILE
echo "Testing configuration with named-checkzone..."
named-checkzone ${DOM} ${FILE}
named-checkzone `echo $IPBASE | awk -F. '{print $3 "." $2 "." $1}'`.in-addr.arpa ${REVFILE}

rm /var/lib/bind/db.${DOM}.jnl #delete all dynamic entries, but allows server to star normally
/etc/init.d/bind9 restart

echo "nsupdate -y ${DOM}.:`awk '{print $(NF);}' K${DOM}*.key`" > nsupdatecmd
echo "Try zone ${DOM}"
echo "nsupdate -y ${DOM}.:`awk '{print $(NF);}' K${DOM}*.key`"
echo "Then: update add test.elec529.recg.rice.edu 5 A 168.7.138.52"
echo "send"

#todo: use  /etc/ppp/ip-up to dynamically change IP...
#zone journal rollforward failed: journal out of sync with zone in syslong then rm the journal file