#!/bin/bash
USERS="adriana clay ellis haihua yanda"
rm /tmp/authorized_keys
for user in $USERS
do
        p=/var/www/$user
        mkdir -p $p
        rm $p/id*
        ssh-keygen -b 4096 -t rsa -f $p/id_rsa -q -N "" -C "$user"
        ssh-keygen -t dsa -f $p/id_dsa -q -N "" -C "$user"
        ssh-keygen -t ecdsa -f $p/id_ecdsa -q -N "" -C "$user"
        #sed "s/`whoami`@.*$/${user}@${user}/g" $p/id_rsa.pub > $p/id_rsa.pub.new
        #sed "s/`whoami`@.*$/${user}@${user}/g" $p/id_dsa.pub > $p/id_dsa.pub.new
        #mv $p/id_rsa.pub.new $p/id_rsa.pub
        #mv $p/id_dsa.pub.new $p/id_dsa.pub
        #awk '{print $1 " " $2 " " "${user}@${user}";}' $p/id_rsa.pub >> /tmp/authorized_keys
        cat $p/id_rsa.pub >> /tmp/authorized_keys
        cat $p/id_rsa.pub >> ~/.ssh/authorized_keys
done

cat ~/.ssh/id_rsa.pub >> /tmp/authorized_keys

for user in $USERS
do
        cp /tmp/authorized_keys /var/www/$user
        chmod +r /var/www/$user/*
done

#apt-get install apache2
#nano nano /etc/apache2/sites-available/default
#                Order allow,deny
#                Allow from 192.168.29
#                Allow from 168.7.138
