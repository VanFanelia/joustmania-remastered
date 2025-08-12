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

## convert all mp3 to wav
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
input_dir="$script_dir/src/main/resources/sound/en/mp3"
output_dir="$script_dir/src/main/resources/sound/en/wav"

mkdir -p "$output_dir"

for file in "$input_dir"/*.mp3; do
  filename=$(basename "$file" .mp3)
  target="$output_dir/$filename.wav"

  if [ -f "$target" ]; then
    echo "file $target exists. skip..."
    continue
  fi

  ffmpeg -i "$file" "$target"
done


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
cp ./scripts/pi/apfiles/* "$PACKAGE_FOLDER/pi/apfiles"

## copy finished jar
cp ./build/libs/joustmania-kotlin-all.jar "$PACKAGE_FOLDER/pi/joustmania.jar"

