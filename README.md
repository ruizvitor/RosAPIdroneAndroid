# Ros API drone Android
This project is intended as a bridge between the DJI Mobile SDK and the Robot Operating System(ROS), allowing broader use of the DJI Phantom 3 Standard drone capabilities.
<p align="center">
<img src="./docs/RosAPI.svg">
</p>

## Necessary Software:
* Python 2.7
* [Ros melodic](http://wiki.ros.org/melodic)
* [Rospy](http://wiki.ros.org/rospy)
* Android Studio
* [Numpy](http://www.numpy.org/) #Optional, only necessary for image streaming
* [OpenCV 2](https://opencv.org/) #Optional, only necessary for image streaming
* OpenCV-Python #Optional, only necessary for image streaming

## Recommend Software
* [Terminator](https://terminator-gtk3.readthedocs.io/en/latest/)

## Getting Started

[Releases](https://github.com/ruizvitor/RosAPIdroneAndroid/releases)

```
git clone https://github.com/ruizvitor/RosAPIdroneAndroid.git
git clone https://github.com/ruizvitor/rosHostApi.git
```
or compile your own apk in the Android Studio:
* Build Project
* Sync Project with Graddle Files
* Run

# Running the full system:
In the host machine:
* Open several terminal windows, one for the roscore, and one for each rostopic application 
* Connect to the drone internal wifi network
* Check your wifi ip address using:
```
ip address
```


Illustrative layout of multiple terminal windows:
<img src="./docs/exampleTerminal.png">

* your wifi ip on the drone internal wifi network should be used on the shell variable ROS_HOSTNAME for each bash terminal, so in each terminal run:
```
export ROS_HOSTNAME=yourHostWifiIPhere
```
for example:
```
export ROS_HOSTNAME=192.168.1.20
```
* in only one of the terminals run:
```
roscore
```
The roscore output should show you your ROS_MASTER_URI, which will be used by the android application.

In the Android device:

**If it is your first time launching the app, connect to a reliable wifi connection, accept the permissions, wait for the DJI registration process to finish. In case no toast message appeared, close the app and repeat. If registration has been succeeded a toast message will appear.**
After the initial setup or in the following launches do:
* Connect to the drone internal wifi network
* Check if the name Phantom 3 Standard appears
* **Set the ROS_MASTER_URI accordingly to your roscore setup**
* Click the Open button
* If your wifi configuration is rightly setup and the roscore is running in the correct ip, your ROSJAVA nodes should be operational at this point already.
* To kill the ROSJAVA nodes exit the SimpleActivity by pressing the back button

## To receive the video streaming:
in a different terminal run 
```
chmod u+x img_listener.py 
./img_listener.py 
```
or
```
python img_listener.py 
```


## To send commands to the drone / publish a message in the ros env:
in a different terminal run 
```
chmod u+x talker.py 
./talker.py
```
or
```
python talker.py 
```

## To listen messages:
in a different terminal run 
```
chmod u+x listener.py 
./listener.py
```
or
```
python listener.py 
```
# Known Issues:
"Mobile SDK 4.7 and later versions are incompatible with x86 devices: Since v4.7, Mobile
SDK has included FFMpeg lib to provide the transcoded video feed, but FFMpeg x86 so files will lead
to the runtime crash when the target API of APP is larger than 23(included)." [See DJI Mobile SDK release notes](https://developer.dji.com/mobile-sdk/downloads/)

# Useful Links:

* [Overview presentation](https://github.com/ruizvitor/RosAPIdroneAndroid/tree/master/docs/overview_rosdrone_api.pdf)
* [Build and run your app in android studio](https://developer.android.com/studio/run)
* [DJI Developer Documentation](https://developer.dji.com/mobile-sdk/documentation/introduction/index.html)
* [Mobile SDK android API reference](https://developer.dji.com/api-reference/android-api/Components/SDKManager/DJISDKManager.html)

Tested on:
* Ubuntu 18.04.1 LTS
* Android Marshmallow 6.0.1
* Android Pie 9
* DJI Phantom 3 Standard



