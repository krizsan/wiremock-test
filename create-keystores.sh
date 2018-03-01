#!/bin/bash

# This script creates client and server keystores and truststores
# for the WireMock example project.
# All certificates are self-signed.
#
# Author: Ivan Krizsan

rm -r client

mkdir -p client/server

# Create the client keystore containing the client public and private keys.
keytool -genkey -keyalg RSA -keysize 2048 -alias client \
    -keypass secret -storepass secret -keystore client/client_keystore.jks \
    -dname "CN=client.ivankrizsan.se,OU=Client Company,O=Client Organization,L=Client City,ST=Client State,C=SE"

# Export the client certificate (public key).
keytool -export -alias client -keystore client/client_keystore.jks -storepass secret -file client/client.cer

# Create the server truststore containing the client certificate (public key).
keytool -importcert -v -trustcacerts -alias client -keystore client/server/server_cacerts.jks -keypass secret -file client/client.cer

# Create the server keystore containing the server public and private keys.
keytool -genkey -keyalg RSA -keysize 2048 -alias server \
    -keypass secret -storepass secret -keystore client/server/server_keystore.jks \
    -dname "CN=server.ivankrizsan.se,OU=Server Company,O=Server Organization,L=Server City,ST=Server State,C=SE"

# Export the server certificate (public key).
keytool -export -alias server -keystore client/server/server_keystore.jks -storepass secret -file client/server/server.cer

# Create the client truststore containing the server certificate (public key).
keytool -importcert -v -trustcacerts -alias server -keystore client/client_cacerts.jks -storepass secret -keypass secret -file client/server/server.cer

