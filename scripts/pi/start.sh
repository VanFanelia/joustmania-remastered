#!/bin/bash

### before starting the application, check if we need to enabel / disable the ap
json_file="settings.json"
if jq -e '.enableAP' "$json_file" > /dev/null; then
    # Key existiert
    enable_ap=$(jq -r '.enableAP' "$json_file")
    if [[ "$enable_ap" == "true" ]]; then
        echo "Access Point is activated"
        source ./enable_ap.sh
    else
        echo "Access Point is deactivated"
        source ./disable_ap.sh
    fi
else
    echo "Key 'enableAP' not found in settings. Do nothing."
fi

## get current path
CURRENT_PATH=$(dirname "$(realpath $0)")

## check for blocked bluetooth devices
rfkill list bluetooth
## force unblock them
sudo rfkill unblock bluetooth
## check again
rfkill list bluetooth

## start program with
sudo -E java -Djava.library.path=$CURRENT_PATH/lib -cp joustmania.jar de.vanfanel.joustmania.ApplicationKt
