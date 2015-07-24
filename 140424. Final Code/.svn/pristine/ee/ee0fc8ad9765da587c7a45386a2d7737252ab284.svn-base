#!/bin/bash

#set failovers according to dns setup (overriding current entry regardless)

# These variables are for the use of the scripts run by run-parts
PPP_IFACE="$1"
PPP_TTY="$2"
PPP_SPEED="$3"
PPP_LOCAL="$4"
PPP_REMOTE="$5"
PPP_IPPARAM="$6"

echo $1 $2 $3 $4 $5 $6 > /root/dias-up-args

DOM="elec529.recg.rice.edu"
USERS="clay ellis adriana yanda haihua" #keep the order we had
KEYNAME=${DOM}.
KEY=aUWLBG+OIgWthWIcftfknw==
IPBASE="168.7.138."
IPSTART="105"
NSIP="104"

#have to declare functions first:
function updateDNS {
#       echo funcargs $1 $2
        echo "zone $DOM
update delete $1 A
update add $1 1 A $2
send
quit" | nsupdate -y ${KEYNAME}:${KEY}

}

for u in $USERS
do
#	if [[ "`dig +short ${u}.${DOM}`" == 127.* ]]
#	then
	#todo, make this a parameter...
	for f in `seq 1 4`
	do
		fip=`dig +short $f.${u}.${DOM} | tail -n 1`
        	if [[ "$fip" != 127.* ]]
	        then
	                updateDNS $u.$DOM. $fip
			break
	        fi
	done
#	fi
done
