#!/usr/bin/env bash

# this forces the bash script to stop processing as soon as it hits an error
set -e

# this gets the dir in which this bash script lives - it also works when
#   this script is executed via a symlink
SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

# create deploy nodes
./gradlew deployNodes

declare -a NODES=("Bank A" "Bank B" "Bank C" "Custodian" "Token Issuing Entity" "Notary")

DEPLOY_NODES_ROOT_PATH="ivno-collateral-token-deploynodes/build/nodes"

for NODE in "${NODES[@]}"
do

    THIS_NODE_ROOT_PATH="$SCRIPT_DIR/$DEPLOY_NODES_ROOT_PATH/$NODE"

    if [ "$NODE" == "Notary" ]; then
        gnome-terminal --title="$NODE" --window-with-profile=HoldOpen -- bash -c "cd \"$THIS_NODE_ROOT_PATH\" && java -jar corda.jar"
    else
        gnome-terminal --title="$NODE" --window-with-profile=HoldOpen -- bash -c "cd \"$THIS_NODE_ROOT_PATH\" && java -jar corda.jar run-migration-scripts --app-schemas && java -jar corda.jar --allow-hibernate-to-manage-app-schema"
    fi

done
