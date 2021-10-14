# Skadi

## Build Android Virtual Machine

Follow Tutorial here:

https://github.com/google/android-emulator-container-scripts

### Notes

Used Emulator and Image für Virtual Android Env

* SYSIMG Android 86_64 - Version 30-x.zip
* EMU stable Emulator-30-x.zip

After downloading each, run this command:

``emu-docker create <emulator-zip> <system-image-zip>``

Afterwards, build with:

``emu-docker build ./src/``

