# Polar M600 Sensor Data App - Work title "Skadi"

The Skadi project consists of a wear app and a mobile app used by the Polar M600 smartwatch and a mobile phone. Both apps in combination will be referred as companion apps.

## Prerequisites
* Linux OS
* A working Kubernetes cluster with the SMADIS deployment (see https://github.com/smaddis/smad-deploy-azure)
* Android JDK
* Android Studio or other preferred IDE (includes Android SDK and adb Manager)
* Wear OS Android app to pair smartphone and smartwatch
* USB debugging in mobile phone activated, ADB debugging in wear activated

### SMADIS deployment
Before running the app, install the mentioned SMADIS deployment by following the instructions in the link above. After successful deployment, run the ``setup.sh`` script in ``/tests/honoscript`` to setup a tenant which will receive the incoming data. The generated IP, tenant ID and client ID have to be used in the mobile app, exact usage of them is discussed in [Mobile MainActivity](#mainactivity-1). Once it was succesfully generated, run the ``receiver.sh`` script 

### Wear OS
**NOTE**: The companion apps assume, that the concerning devices are already paired beforehand.
Download the Wear OS App from the Google Playstore and follow the instructions to pair the devices.

## Setup

### Activating USB/ADB debugging

#### Wear
Before the app can be installed, activate the developer mode by going to Settings->System->Info. Click the field "build number" 7 times. Go into the developer settings and activate ADB debugging.

#### Mobile

### Skadi apps
Open the SDK, connect each device. When using Android Studio, use the dropdown with the options "wear" and "mobile".Choose the right device name to compile the app on the respective device type. 

## Mobile Phone App

### MainActivity
The app consists of a table view on top, a reset button to reset all sensors and a button toggling the connection to Hono. In the table, the buttons on the left toggle wear sensors, the text in the middle is merely static text and the right part shows either that the sensor is off (default value) or the latest received value of the concerning sensor. By default, the sensor buttons are disabled and are only enabled once the phone is successfully connected to Hono.

To start the Connection to Hono, click the button . [PahoMQTTClient](#pahomqttclient)

![mobile1](https://user-images.githubusercontent.com/70896815/146928959-22cd1cd9-abdb-4fe3-8cd9-8d09f8024180.jpg)

![mobile2](https://user-images.githubusercontent.com/70896815/146928971-fc31a712-7047-4d51-82c1-c95f34845d6a.jpg)

### PahoMQTTClient

## Wear App

### MainActivity

The app consists of a static table view with the name of each sensor on the left and its current values ("off" by default or when turned off by user input in the mobile app). The wear app waits for the buttons in the mobile app to be pushed.

![polar app_off](https://user-images.githubusercontent.com/70896815/146926377-4b4e64fc-8959-4f32-ac46-389b33f141c7.jpg)

### SendThread

### AmbientCallback

If the display wear device is not touched for a while, it goes into the start screen again. To prevent it making the MainActivity implement the ``AmbientModeSupport.AmbientCallbackProvider`` class and attaching an instance of the ``AmbientController`` to it is sufficient to prevent this and make the app be always on.

``AmbientModeSupport.AmbientController ambientController = AmbientModeSupport.attach(this);``

## Trouble shooting

### No popup in wear for giving permission over body sensors
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
* Refinements like error handling in case of connection losses between phone+hono or phone+wear, handling of deactivated Bluetooth
* Extension of mobile app and testing with more IoT devices (earables, wristband, ...)
