#!/usr/bin/env bash
#
# Copyright (C) 2021 Bill Havanki
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

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
