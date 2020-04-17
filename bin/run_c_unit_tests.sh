#!/usr/bin/env bash

cd $(git rev-parse --show-toplevel)/pepper/apps

if [ ! -f Makefile ]; then 
    cmake . 
fi
make test
#clang simple_contract_test.c -ldl -rdynamic -lcmocka -Ied25519 -std=c89 -o /tmp/simple_contract_test && /tmp/simple_contract_test && rm /tmp/simple_contract_test
