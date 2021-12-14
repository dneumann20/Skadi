# Polar M600 Sensor Data App

This app consists of 2 different apps used by the Polar M600 smartwatch and a mobile phone, each of them will be referred here as companion apps.

## Prerequisites
* Linux OS
* Android JDK
* Android Studio or other preferred IDE (includes Android SDK and adb Manager)
* Wear OS Android app to pair smartphone and smartwatch
* Debugging activated in both devices

## Setup
The companion apps assume, that both 

Open the SDK, connect each device. When using Android Studio, use the dropdown with the options "wear" and "mobile" and choose the right device name to compile the app on the respective device type.

## Smartwatch App

### MainActivity

### SendThread

### AmbientCallback


## Mobile Phone App

### MainActivity

### PahoMQTTClient

## Trouble shooting

### Sensors are not reacting on Button click
Make sure that Bluetooth is activated on both devices during the use.

### No popup for giving permission over body sensors
adb devices permission (WIP)

### Connection to Hono is not working on the first try
Sadly it's a known issue from the MQttPahoClient library, it usually works on the 2nd try the latest.
