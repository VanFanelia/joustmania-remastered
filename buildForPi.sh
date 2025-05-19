PACKAGE_FOLDER="./packages"
if [ -d "$PACKAGE_FOLDER" ]; then
  echo "found old packages. delete it"
  rm -rf "$PACKAGE_FOLDER"
fi

## build web package
node --version
npm --prefix joustmania-web-app run build

## copy dist to static resource of java project
rm -rf ./src/main/resources/static/*
cp -r ./joustmania-web-app/dist/* ./src/main/resources/static

## build jar
./gradlew clean
java --version
./gradlew --version
./gradlew shadowJar

## build pi package
mkdir -p "$PACKAGE_FOLDER/pi/lib/linux"
mkdir -p "$PACKAGE_FOLDER/pi/apfiles"
cp -r ./lib/psmoveapi.jar "$PACKAGE_FOLDER/pi/lib"
cp -r ./lib/linux "$PACKAGE_FOLDER/pi/lib"

## copy install and start script for pi
cp ./scripts/pi/*.sh "$PACKAGE_FOLDER/pi"
cp ./scripts/pi/settings.json "$PACKAGE_FOLDER/pi"
cp ./scripts/pi/apfiles/* "$PACKAGE_FOLDER/pi/apfiles"

## copy finished jar
cp ./build/libs/joustmania-kotlin-all.jar "$PACKAGE_FOLDER/pi/joustmania.jar"

