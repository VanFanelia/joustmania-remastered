## Einmalig
# sudo apt-get install --reinstall libc6-dev
# sudo apt-get install libdbus-1-3 libdbus-glib-1-2

PACKAGE_FOLDER="./packages"
if [ -d "$PACKAGE_FOLDER" ]; then
  echo "found old packages. delete it"
  rm -rf "$PACKAGE_FOLDER"
fi

## build jar
./gradlew clean
java --version
./gradlew --version
./gradlew shadowJar

## build pi package
mkdir -p "$PACKAGE_FOLDER/pi/lib/linux"
cp -r ./lib/psmoveapi.jar "$PACKAGE_FOLDER/pi/lib"
cp -r ./lib/linux "$PACKAGE_FOLDER/pi/lib"

## copy install and start script for pi
cp ./scripts/pi/*.sh "$PACKAGE_FOLDER/pi"

## copy finished jar
cp ./build/libs/joustmania-kotlin-all.jar "$PACKAGE_FOLDER/pi/joustmania.jar"

