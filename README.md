# joustmania-kotlin

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                               | Description                                                 |
| ----------------------------------------------------|------------------------------------------------------------- |
| [Routing](https://start.ktor.io/p/routing-default) | Allows to define structured routes and associated handlers. |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

### Stuff to put into install script 

sudo apt-get update
sudo apt-get install libc6-dev

mkdir -p ./opencv-libs
cd ./opencv-libs
ln -s /usr/lib/x86_64-linux-gnu/libdl.so.2 libdl.so



- do stuff
sudo usermod -aG bluetooth $USER

- do not use pin ?
- sudo nano /etc/bluetooth/input.conf
Neeed for Install:

```
Changing /etc/bluetooth/input.cfg on my arch machine to use "ClassicBondedOnly=false" instead of the default of true does (after restarting bluez) eliminate this issue, making it possible to connect controllers without issue, and without compiling my own bluez!
```

## next games:
- Ninja Bomb (Random Time between 2 and 3 minutes. Sounds get faster and faster)
- Zombie (Max Time 3 Minutes)
- Protect the King (one is the king, if he dies, the hole team loses, all other are knights. If Knights get beaten, they need to press the Z Button and Shake the stick to get color again, the king cannot move fast, is very sensitve)
- Shake or Fail (alle leuchten orange, 1,5-2 mal so viele controller wie spieler. Dann werden die Controller verteilt und scharf geschaltet (werden dann grün) jetzt müssen sie immer dort wo sie waren gerüttelt werden bevor sie wieder Rot sind.)

## todos:
- on new player connected via usb, change duration to 3-4 seconds of white to wait for bluetooth restarting
- add multi-language support via config
- add german language
- add players death sound (cancelable)
- add lobby background sound
- add sound if player has a warning (cancelable)
- fixing bug of dying players are red with touch of black instead of complete black
- play example file in audio output
- logging into file
- add context menu on Hardware tab -> On click open new sub page with fullscreen controller stats. Or popup maybe, -> Add rumble and color blink option for this controller to identfiy

### nice to have
- colored logs
- fix select audio output


### thanks and credits 
- Thanks to https://openmoji.org/ for the awesome open source icons