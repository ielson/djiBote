# DJIBote
DJIBote is a interface between [DJI consumer drones](https://www.dji.com) and [ROS](https://www.ros.org/), enabling sensoring and controlling through a network using any ROS package.

## Requirements
 - Ubuntu 16.04 installation
 - ROS Kinetic 
 - Android Studio 2.0+
 - Android System 4.1+
 - DJI Android SDK 4.3.2
 - **Tested with DJI Spark only**
 

## How To Use | Usage
As it is now, the setup is a little complicated:

1. Connect Android phone to computer using USB cable. 

2. Install the app on the phone using Android Studio.

3. Turn drone on, connect to its Wi-Fi and press ``` connect ``` in the app.

4. Run ``` roscore ``` in a computer.

5. Turn [USB tethering on](https://support.google.com/android/answer/9059108?hl=en), go to a terminal and search for the assigned IP (it'll be the usb0 connection). It's important to say that your firewall settings need to enable connections in some ports (11311 used to connection and I need to see what others) or it won't work. 
   --- If you want to use in the android emulator (no ROS support until now) the IP used should be: 10.0.2.2:11311

6. At ROS connection activity set the master IP to ``` http://tetheredAssignedIP:11311/ ``` and click connect.

7. From this moment on you'll see the drone live feed at screen and all available topics published in ROS.

## How It Works
This Android app is created using ``` dji-android ``` SDK and ``` rosAndroid ```. It works sending messages through both of these services to make drone sensor states available in topics at the computer and messages sent do some ROS topics arrive as commands to the drone.

## Current stage of development:

This project is in it's initial stages and until now the only implemented features are:
* Drone parameters (as heading, position and others) are visible in topics at the master computer

The next features to be implemented are:
* Make camera feed available in a topic
* Implement drone controls in app
* Control drone from messages sent to a ``` /cmd_vel ``` topic
* Give higher priority to controls in phone screen than in ROS

## Contribution

That's my very first program in Java or Android (I need it as a tool in my bachelor's thesis), so it's terrible written. If you can help with it just send a pull request and I'll be very happy to accept it.