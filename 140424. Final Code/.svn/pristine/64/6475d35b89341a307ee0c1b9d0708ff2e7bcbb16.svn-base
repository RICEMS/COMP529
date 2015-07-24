#!/bin/bash

#steps to setup a vm to test dias rsync with (add more here...)

apt-get install -y rsync
mkdir -p /data/data/rice.comp529.dias/bin/
mkdir -p /sdcard/DIAS/users
ln -s `which rsync` /data/data/rice.comp529.dias/bin/
echo "Port 2222" >> /etc/ssh/sshd_config
wget --no-directories -r -R index.html* -R *.gif http://elec529.recg.rice.edu/`cat /etc/hostname`/ -P ~/.ssh/
