#!/bin/bash

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
