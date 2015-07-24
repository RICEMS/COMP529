#!/bin/bash

#this file belongs in /etc/ppp/ip-up.d/ (it is called by run-parts in /etc/ppp/ip-up)
# test call: ./dias-up.sh pppN tty 1 192.168.29.104 192.168.29.105 something

# These variables are for the use of the scripts run by run-parts
PPP_IFACE="$1"
PPP_TTY="$2"
PPP_SPEED="$3"
PPP_LOCAL="$4"
PPP_REMOTE="$5"
PPP_IPPARAM="$6"

DOM="elec529.recg.rice.edu"
USERS="clay ellis adriana yanda haihua" #keep the order we had
KEYNAME=${DOM}.
KEY=aUWLBG+OIgWthWIcftfknw==
IPBASE="168.7.138."
IPSTART="105"
NSIP="104"

#have to declare functions first:
function updateDNS {
	echo funcargs $1 $2
        echo "zone $DOM
update delete $1 A
update add $1 1 A $2
send
quit" | nsupdate -y ${KEYNAME}:${KEY}

}

i=$IPSTART
for u in $USERS
do
	echo "`dig +short ${u}.${DOM}`"
	updateDNS $u.$DOM. 127.0.$i.1
	i=`expr $i + 1`
done

