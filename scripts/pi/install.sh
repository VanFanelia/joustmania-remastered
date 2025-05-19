#!/bin/bash

sudo apt-get update
sudo apt install -y default-jre-headless jq

java --version

FILE="/etc/bluetooth/input.conf"
LINE="ClassicBondedOnly=false"

# Check if the file exists
if [ ! -f "$FILE" ]; then
  echo "File $FILE does not exist. cannot fix bluetooth settings. We need to add $LINE to allow psmove controllers to pair."
  exit 1
fi

# Check if the line already exists
echo "Check for bluetooth Fix in $FILE..."
if grep -Fxq "$LINE" "$FILE"; then
  echo "Line already exists in $FILE. No changes made."
else
  echo -e "\n\n## fix for PSMOVE controller" | sudo tee -a "$FILE" > /dev/null
  echo "$LINE" | sudo tee -a "$FILE" > /dev/null
  echo "Line added to $FILE."
fi

## restart bluetooth
echo "Restart bluetooth to enable line"
sudo systemctl restart bluetooth

### create symlink for libpsmove_java.so
# Detect system architecture
ARCH=$(uname -m)

# Set target based on architecture
case "$ARCH" in
    x86_64)
        TARGET="x86_64"
        ;;
    armv7l)
        TARGET="arm32"
        ;;
    aarch64)
        TARGET="arm64"
        ;;
    *)
        echo "Unsupported architecture: $ARCH. cannot create symlink"
        exit 1
        ;;
esac

# Create symlink
CURRENT_PATH=$(dirname "$(realpath $0)")
SYMLINK_NAME="lib/libpsmove_java.so"

# Remove existing symlink if it exists
[ -L "$SYMLINK_NAME" ] && rm "$SYMLINK_NAME"

# Create new symlink
SYMLINK_SOURCE="$CURRENT_PATH/lib/linux/$TARGET/libpsmove_java.so"
SYMLINK_TARGET="$CURRENT_PATH/$SYMLINK_NAME"
ln -s "$SYMLINK_SOURCE" "$SYMLINK_TARGET"
echo "Created symlink '$SYMLINK_SOURCE' -> '$SYMLINK_TARGET'"

# disable internal bluetooth
./disableInternalBluetooth.sh