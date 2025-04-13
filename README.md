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


## todos:
- add different colors to ffa game
- add winner sounds to ffa game
- add players death sound
- fixing lobby when game was over


- colored logs
- save variables in external file
- select audio output
- play example file in audio output