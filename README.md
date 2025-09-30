# joustmania-kotlin

## What is JoustMania?

- JoustMania is a multiplayer party game inspired by the indie game [“Johann Sebastian Joust.”](http://jsjoust.com/) 
- The goal is to keep your controller as still as possible while trying to knock out your opponents' controllers by moving around.
- Different game modes are available, including FFA, Teams, Werewolves and Zombies (more to come).  
- JoustMania is perfect for parties, events, or as a fun group game.

## What do I need to run this game?
- A lot of [PlayStation Move controllers.](https://en.wikipedia.org/wiki/PlayStation_Move)
- Some bluetooth dongle (for every 6–8 controllers you need a minimum of one)
- A device running java (currently only tested on linux 64bit and raspberry pi 5)
- (optional) a device with a browser to open the web interface

## how do I install it?
- (full install description coming soon)

### Rasberry Pi 5
- setup the hardware
- run the buildForPi.sh script
- copy the content of `packages/pi` to your fresh installed raspberry pi 5 into `~/joustmania`
- run `sudo ./install.sh`
- reboot your raspberry pi 5
- connect to the new spawning WiFi network named `JoustMania` with password `joustpass`
- open the web interface: [http://joust.mania](http://joust.mania)
- enjoy the game


### What is the difference between the GitHub Project [JoustMania by adangert](https://github.com/adangert/JoustMania)?
Only a small one – This project focus is: **easy to use and install.** 
The old project is complex to install and sometimes buggy on new hardware.

So the top priorities are:
- One Button press and ready to play
- easy to install and easy to rebuild the hardware

My personal goal was to learn multi-threading in Kotlin. 
So I have rewritten the whole project to learn and polish the game experience.



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

## next Todos
- cleanup repository for publishing
  - cleanup Readme
  - add first draft of manual

## next games:
- Zombie (Max Time 3 Minutes)
- Ninja Bomb (Random Time between 2 and 3 minutes. Sounds get faster and faster)
- Shake or Fail (alle leuchten orange, 1,5-2 mal so viele controller wie spieler. Dann werden die Controller verteilt und scharf geschaltet (werden dann grün) jetzt müssen sie immer dort wo sie waren gerüttelt werden bevor sie wieder Rot sind.)
- Protect the King (one is the king, if he dies, the hole team loses, all other are knights. If Knights get beaten, they need to press the Z Button and Shake the stick to get color again, the king cannot move fast, is very sensitve)

## todos:
- on new player connected via usb, change duration to 3-4 seconds of white to wait for bluetooth restarting
- add multi-language support via config
- add german language
- logging into file

### nice to have
- colored logs
- fix select audio output

### thanks and credits 
- Many, many thanks to [adangert](https://github.com/adangert/) for the original project
- Without the driver [psmoveapi](https://github.com/thp/psmoveapi) from [thp](https://github.com/thp/) this project would not be possible
- Thanks to https://openmoji.org/ for the awesome open source icons