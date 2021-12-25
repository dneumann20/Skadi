# Polar M600 Sensor Data App - "Skadi"

## General info
The Skadi system consists of a wear app designed specifically for the Polar M600 smartwatch and a mobile app used by any mobile phone as well as an instance of the smad azure cloud by SMADDIS including Eclipse Hono (see https://github.com/smaddis/smad-deploy-azure). This is a first prototype of a system that feeds the smad cloud with data from an IoT device with a centralized control unit, in that case the mobile phone.

Both apps in combination will be referred as companion apps. The connection and messaging between the apps runs through the API Data layer. For further information check the following chapters of the official Android Wear OS documentation:
* [Send and sync data on Wear OS](https://developer.android.com/training/wearables/data/network-access)
* [Access the Wearable Data Layer](https://developer.android.com/training/wearables/data/accessing)

The connection between the mobile app and the cloud is handled by Eclipse Hono as the interface to connect the end points and the MQTT protocol for light weight message communication. The mobile app acts as the MQTT client and the Hono instance as the MQTT broker (see https://mqtt.org/ for more info).

## Prerequisites
* Linux OS
* A working Kubernetes cluster with the SMADDIS deployment
* Java JDK
* Android Studio or other preferred IDE (includes Android SDK and adb Manager)
* Wear OS Android app to pair smartphone and smartwatch
* USB debugging in mobile phone activated, ADB debugging in wear activated

### SMADIS deployment / Hono tenant
Before running the app, install the mentioned SMADIS deployment by following the instructions in the link above. After successful deployment, run the ``setup.sh`` script in ``/tests/honoscript`` to setup a MQTT broker (also known as a tenant) which will receive the incoming data and forward it to the cloud. The generated IP, tenant ID and client ID have to be used in the mobile app, exact usage of them is discussed in [Mobile MainActivity](#mqtt-client-and-credentials). Once it was succesfully generated, run the ``receiver.sh`` script everytime when using Skadi.

### Wear OS
**NOTE**: The companion apps assume, that the concerning devices are already paired beforehand.
Download the Wear OS App from the Google Playstore and follow the instructions to pair the devices.

## Setup

### Activating USB/ADB debugging
Before the apps can be installed on concerning devices, USB debugging in the mobile phone and ADB debugging in the wear has to be activated.

#### Wear
Activate the developer mode by going to Settings->System->Info. Click the field "build number" about 7 times until a popup message shows that the user is a developer now. Go into the developer settings and activate ADB debugging.

#### Mobile
Go to the Settings->About Device->About Phone and click "build number" 7 times. Type in PIN or password. Go in the developer settings and activate USB debugging.

### Install companion apps
Open the SDK, connect each device. When using Android Studio, use the dropdown with the options "wear" and "mobile". Choose the right device name to compile the app on the respective device type. 

## Mobile Phone App

### MainActivity

#### General
The app consists of a table view on top, a reset button to reset all sensors and a button toggling the connection to Hono. In the table, the buttons on the left toggle wear sensors, the text in the middle is merely static text and the right part shows either that the sensor is off (default value) or the latest received value of the concerning sensor. By default, the sensor buttons are disabled and are only enabled once the phone is successfully connected to Hono. Connecting and Disconnecting are handled in the MainActivity to ensure that the updating of button texts and activation/deactivation of sensor buttons only happens on a successful ``ActionCallback(...)``.

#### Hono connection
**NOTE:** Ensure that SMADDIS' ``receiver.sh`` script is running! To start the Connection to Hono, click the lowest button.

<img src="https://user-images.githubusercontent.com/70896815/146928959-22cd1cd9-abdb-4fe3-8cd9-8d09f8024180.jpg" width="30%">

The connection might take 2-3 seconds. Once the connection was successful the text view below the hono button shows the status "connected", the sensor buttons are enabled and ready to use.

<img src="https://user-images.githubusercontent.com/70896815/146928971-fc31a712-7047-4d51-82c1-c95f34845d6a.jpg" width="30%">

#### MQTT Client and Credentials
As mentioned, the generated IPs and IDs have to be added in the mobile app to register it as the MQTT client belonging to the generated MQTT broker. Following string are important to note:

|String name|Description|
|--------|----------|
|MQTT_ADAPTER_IP_URI|URI of the MQTT broker, has the format ``tcp://<ADAPTER_IP>:1883`` with 1883 as default port|
|TENANT_ID|Tenant ID is passed as an argument when connecting to the Hono server, corresponding to the broker ID| 
|CLIENT_DEVICE_ID|ID for the device that wants to act as the client connecting to the broker, also passed during connection handling|
|USERNAME|Hono requires any device to authenticate. User name is in the format ``CLIENT_DEVICE_ID@TENANT_ID``|
|PASSWORD|See in code. If password changes please contact the owners of the SMADDIS project|

#### Sensors and Activation / Deactivation
**NOTE:** Ensure that bluetooth is on! When in doubt, the Wear OS app shows whether the paired devices are connected.

Clicking a button makes the mobile app send a message to the wear app corresponding to the name of the sensor and toggles them on and off. For this prototype following sensors are available:
* Heart Rate
* Gyroscope
* Accelerator
* Light

Every sensor view has the initial value of a zero string "0" which is overwritten once the sensor sends its data. 

#### Reset Button
Pressing the Reset button the mobile app sends the string "reset" to turn off all sensors and reset the associated views and buttons.

#### Message Handling
The messages received by the wear are simply the current value of a sensor with a character as prefix to distinguish which sensor sent the message to the mobile app (more details in [Preprocession of Sensor Values and Prefixes](#preprocession-of-sensor-values-and-prefixes)).

Immediately after receiving a sensor value, the prefix is removed and forwarded to Hono via MQTT. To distinguish the type of messages for the MQTT broker, the message is sent (or published in terms of MQTT) with a topic as string. The SMADDIS deployment requires the topic to be in the format ``event/<TENANT_ID>/<CLIENT_DEVICE_ID>/topic``.

### PahoMQTTClient

Merely a helper class to bloat the MainActivity class a bit less. Holds additional connection and disconnection options as well as the method to publish received data to the MQTT broker. Subscribe/Unsubscribe functions of the MQTT protocol and therefore the corresponding methods are not needed for this application as the mobile phone doesn't receive messages from the MQTT broker.

### SendThread

This thread uses the Data Layer API, in which every device (including the mobile phone) that is connected via the Wear OS app is a Node. It gets all nodes connected to the current device and broadcasts the message via a uniquely identified path (``"/message_path"`` in this case). For this prototype it is assumed that the Polar m600 is the only node connected, so the broadcast does not matter.

## Wear App

### MainActivity

#### General
The app consists of a static table view with the name of each sensor on the left and its current values ("off" by default or when turned off by user input in the mobile app) as well as a basic info text. The app waits for the sensor buttons on the mobile app to be pushed.

![polar app_off](https://user-images.githubusercontent.com/70896815/146926377-4b4e64fc-8959-4f32-ac46-389b33f141c7.jpg)

After the app receives a message with the name of a sensor type ("heartRate","gyroscope","accelerator","light"), the associated sensor is toggled. On receiving "reset" as message the app is reset to default state.

(TODO screenshot with sensor data displayed)

#### Preprocession of Sensor Values and Prefixes

Some of the sensors need preprocession before the data is sent to the mobile app. In case of the heart rate and light sensor, both output values are one-dimensional and do not need preprocessing (heart rate value is casted as Integer as it does not have any decimal number anyway).

The gyroscope and accelerator sensors produce 3-dimensional values on the x,y and z axis. For the prototype only their x-values were used. These sensors are very sensitive and change the sensor value on the slightest movement. To prevent a massive overload on messages and freezing the mobile app due to it, the app subtracts the old and new sensor value and only updates as well as sends it only on a more signifant change. For testing purposes the gyroscope's threshold value to change is over 1 and the accelerator's threshold is 2.

As mentioned in [Message Handling](#message-handling), the value is sent as a message via the Data Layer API with a prefix character to distinguish which sensor the data is from.

### AmbientCallback

If the display wear device is not touched for a while, it goes into the start screen again. To prevent it making the ``MainActivity`` implement the ``AmbientModeSupport.AmbientCallbackProvider`` class and attaching an instance of the ``AmbientController`` to it is sufficient to prevent this and make the app be always on:

``AmbientModeSupport.AmbientController ambientController = AmbientModeSupport.attach(this);``

### SendThread

same as [Mobile SendThread](#sendthread).

## Trouble shooting

### No popup in wear for giving permission over body sensors
Plugin the wear via usb cable. Open a terminal and run ``adb devices``. The device list should show a message like "no permissions (...) are your udev rules wrong?".

* Run ``lsusb``
* and find the entry of the polar watch. The IDs might be in the form of ``4e02:c003`` whereas ``4e02`` is the ``idVendor`` and ``c003`` the ``idProduct``
* Open an editor with ``sudo vim /etc/udev/rules.d/51-android.rules``
* Add ``SUBSYSTEM=="usb", ATTR{idVendor}=="4e02", ATTR{idProduct}=="c003", MODE="0666", GROUP="plugdev"``, save file
* Run ``sudo udevadm control --reload-rules``
* Running ``adb devices`` again should now have the device listed properly

### Connection to Hono is not working on the first try
Sometimes the connection does not work on the first try and fail with an immediate timeout. This is sadly a known isue of the PahoMQTTClient library. The second time should usually work out.

### Mobile phone app cannot connect to Hono 
Make sure you run the ``receiver.sh`` script from the SMADIS deployment and the generated IDs and IP are rightly set in the mobile app.

### Sensors are not reacting on Button click
Make sure that Bluetooth is activated on both paired devices during the use.

## Future work
* Always active mobile phone app to prevent the data flow stop when the screen turns off
* Refinements like error handling in case of connection losses between phone+hono or phone+wear
* Extension of mobile app and testing with more IoT devices (earables, wristband, ...)
