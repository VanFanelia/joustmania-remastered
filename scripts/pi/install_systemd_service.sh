#!/bin/bash

# name of the service
SERVICE_NAME="joustmania-start"

# name of executioner of this script
USER_NAME=$(whoami)
## get current path
CURRENT_PATH=$(dirname "$(realpath $0)")
# path of the execution
SCRIPT_PATH="$CURRENT_PATH/${SERVICE_NAME}.sh"

# path of the servie file
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"

# check if script file exists
if [ ! -f "$SCRIPT_PATH" ]; then
  echo "Error: $SCRIPT_PATH does not exists"
  exit 1
fi

# create service file
echo "Create systemd service-file: $SERVICE_FILE ..."
sudo bash -c "cat > $SERVICE_FILE" <<EOF
[Unit]
Description=Run on startup: ${SERVICE_NAME}
After=network.target

[Service]
Type=simple
ExecStart=${SCRIPT_PATH}
User=${USER_NAME}
WorkingDirectory=${HOME_DIR}
StandardOutput=journal
StandardError=journal
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# set file chmod
sudo chmod 644 "$SERVICE_FILE"

# reload systemd, activate service and start it
echo "Reload: restart systemd now and activate the deamon ..."
sudo systemctl daemon-reexec
sudo systemctl daemon-reload
sudo systemctl enable "${SERVICE_NAME}.service"
sudo systemctl start "${SERVICE_NAME}.service"

echo "Success: Deamon '${SERVICE_NAME}' was created, activated and started"