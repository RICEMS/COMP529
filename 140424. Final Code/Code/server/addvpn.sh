#!/bin/bash
for i in $1
do
   ip addr add 168.7.138.$i/26 broadcast 168.7.138.127 dev eth0
   iptables -t nat -A PREROUTING -d 168.7.138.$i -j DNAT --to-destination=192.168.29.$i
   iptables -t nat -A POSTROUTING -s 192.168.29.$i -j SNAT --to-source=168.7.138.$i
   #Do we need something like this to stop pings from responding? Or is that just something local?
   #iptables -A INPUT -p icmp -s 0/0 -d 168.7.138.$i -j DROP
done
