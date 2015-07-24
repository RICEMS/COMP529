apt-get install iptables lsof nano wget xl2tpd ppp openswan
ip addr add 168.7.138.107/26 dev ppp0
ip addr delete 168.7.138.107/32 dev ppp0
ip route delete 0.0.0.0/1
ip route add 0.0.0.0/1 via 168.7.138.126 dev ppp0 metric 1

/etc/sysctl.conf
net.ipv4.ip_forward=1

then sysctl -p

sudo /etc/init.d/ipsec restart
sudo /etc/init.d/xl2tpd restart

May also need:

interface "eth0" {
    prepend domain-name-servers 168.7.138.104;
}

in /etc/dhcp/dhclient.conf