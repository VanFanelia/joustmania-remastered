## Einmalig
# sudo apt-get install --reinstall libc6-dev
# sudo apt-get install libdbus-1-3 libdbus-glib-1-2

./gradlew clean
java --version
./gradlew --version
./gradlew shadowJar

JAVA_PATH=$JAVA_HOME
CURRENT_PATH=$(dirname "$(realpath $0)")

sudo $JAVA_PATH/bin/java --version

sudo LD_LIBRARY_PATH=/lib:/usr/lib:/usr/lib/x86_64-linux-gnu:$JAVA_PATH/lib:$CURRENT_PATH/libs:$LD_LIBRARY_PATH \
    $JAVA_PATH/bin/java \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
    -Djava.library.path=/lib:/usr/lib:/usr/lib/x86_64-linux-gnu:$JAVA_PATH/lib:$CURRENT_PATH/libs -cp build/libs/joustmania-kotlin-all.jar de.vanfanel.joustmania.ApplicationKt
