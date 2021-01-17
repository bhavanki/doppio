#!/usr/bin/env bash

# To create a private key and self-signed cert using keytool:
#
# openssl req -newkey rsa:2048 -nodes -keyout clientkey.pem -x509 \
#   -days 365 -out clientcert.pem

set -e

if (( $# == 0 )); then
  echo "Usage: $0 <certificate file> <truststore file>"
  exit 0
fi

PEM_FILE=$1
TRUSTSTORE=$2

if [[ ! -r $PEM_FILE ]]; then
  echo "Cannot read $PEM_FILE"
  exit 1
fi

ALIAS=$(openssl x509 -in "$PEM_FILE" -noout -subject_hash)

keytool -importcert -file "$PEM_FILE" \
  -alias "$ALIAS" \
  -keystore "$TRUSTSTORE"
