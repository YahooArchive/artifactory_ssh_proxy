#!/bin/sh
# This script is used to one time create the config directory for development setup.
ROOT=developer_config

# gack die ROOT die.
mkdir -p $ROOT/conf/sshd_proxy/
mkdir -p $ROOT/logs/sshd_proxy

# create the hostkey
ssh-keygen -t dsa -f $ROOT/conf/sshd_proxy/ssh_host_dsa_key -C '' -N ''

# copy the properties file
cp src/test/resources/developer_config/sshd_proxy.properties  $ROOT/conf/sshd_proxy/sshd_proxy.properties

# fix ROOT to be our root.
sed -i '' -e s/sshd.root=.*/sshd.root=$ROOT/g $ROOT/conf/sshd_proxy/sshd_proxy.properties
