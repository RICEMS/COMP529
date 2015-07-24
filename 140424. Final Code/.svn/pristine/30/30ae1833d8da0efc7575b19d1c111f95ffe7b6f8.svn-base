#!/bin/bash

DOM="elec529.recg.rice.edu"
KEYNAME=${DOM}.
KEY=aUWLBG+OIgWthWIcftfknw==

#have to declare functions first:
function updateDNS {
#       echo funcargs $1 $2
        echo "zone $DOM
update delete $1 A
update add $1 1 A $2
send
quit" | nsupdate -y ${KEYNAME}:${KEY}

}

updateDNS $1 $2
