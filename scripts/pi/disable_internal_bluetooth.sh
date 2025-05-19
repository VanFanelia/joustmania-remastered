#!/bin/bash

LINE="dtoverlay=disable-bt"
FILE="/boot/firmware/config.txt"

#this will disable on-board bluetooth
echo "Disable internal bluetooth"
sudo echo "$LINE" | sudo tee -a "$FILE"


# Check if the line already exists
echo "Check if internal bluetooth is disabled in file: $FILE ..."
if grep -Fxq "$LINE" "$FILE"; then
  echo "Line already exists in $FILE. No changes made."
else
  echo "Disable internal bluetooth"
  echo -e "\n\n## disable internal bluetooth" | sudo tee -a "$FILE" > /dev/null
  sudo echo "$LINE" | sudo tee -a "$FILE"
fi

#remove onboard bluetooth folders
echo "delete all paired devices"
sudo rm -rf /var/lib/bluetooth/*

echo "Finish cleanup, reboot now"
sudo reboot