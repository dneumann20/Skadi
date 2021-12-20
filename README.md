# Polar M600 Sensor Data App - Work title "Skadi"

The Skadi project consists of a wear app and a mobile app used by the Polar M600 smartwatch and a mobile phone. Both apps in combination will be referred as companion apps.

## Prerequisites
* Linux OS
* A working Kubernetes cluster with the SMADIS deployment (see https://github.com/smaddis/smad-deploy-azure)
* Android JDK
* Android Studio or other preferred IDE (includes Android SDK and adb Manager)
* Wear OS Android app to pair smartphone and smartwatch
* USB debugging in mobile phone activated, ADB debugging in smartphone activated

## Setup

### SMADIS deployment
Before running the app, install the mentioned SMADIS deployment by following the instructions in the link above. After successful deployment, run the ``setup.sh`` script in ``/tests/honoscript`` to setup a tenant which will receive the incoming data. The generated IP, tenant ID and client ID have to be used in the mobile app, exact usage of them is discussed in [Mobile MainActivity](#mainactivity-1).

### Wear OS
**NOTE**: The companion apps assume, that the concerning devices are already paired beforehand.
Download the Wear OS App from the Google Playstore and follow the instructions to pair the devices.

### Skadi apps
Open the SDK, connect each device. When using Android Studio, use the dropdown with the options "wear" and "mobile".Choose the right device name to compile the app on the respective device type. 


## Smartwatch App
Before the app can be installed, activate the developer mode by going to Settings->System->Info. Click the field "build number" 7 times. Go into the developer settings and activate ADB debugging.

### MainActivity


### SendThread

### AmbientCallback


## Mobile Phone App

### MainActivity

### PahoMQTTClient

## Trouble shooting

### No popup for giving permission over body sensors
adb devices permission (WIP)

### Connection to Hono is not working on the first try
Sadly it's a known issue from the MQttPahoClient library, it usually works on the 2nd try.

### Mobile phone app cannot connect to Hono 
Make sure you run the ``receiver.sh`` script from the SMADIS deployment and the generated IDs and IP are rightly set in the mobile app.

### Sensors are not reacting on Button click
Make sure that Bluetooth is activated on both paired devices during the use.

## Future work
* Always active mobile phone app to prevent the data flow stop when the screen turns off
* Error messages in mobile App UI
* Refinement (handling of deactivated Bluetooth connection, better message handling)
* Extension of mobile app and testing with more IoT devices (earables, wristband, ...)
* Dynamic UI elements adjusting to mobile phone height?
