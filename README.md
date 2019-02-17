# LibDepaums

[![](https://jitpack.io/v/EtchDroid/libdepaums.svg)](https://jitpack.io/#EtchDroid/libdepaums)

A library to access USB mass storage devices (pen drives, external HDDs, card readers) using the Android USB Host API. Currently it supports the SCSI command set and the FAT32 file system.

It's a fork of [libaums](https://github.com/magnusja/libaums). I forked it in order to have more control over the source code (my use case is slightly different than libaums').

### Key differences

- Removed unneeded modules such as `storageprovider`, `javafs` and `httpserver`
- Ported main classes to Kotlin to increase reliability and null safety (will port more over time, the idea is to port the whole thing to Kotlin)
- The BlockDevice implementations are public and accessible
- Fixed the tests so they build. Plan is to avoid downloading test assets at runtime
- Reworked logic to remove some side effects and make UsbMassStorageDevice Parcelable

## How to use

### Add to your project

In root `build.gradle`:

```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

In your module/app `build.gradle`:

```gradle
	dependencies {
	        implementation 'com.github.EtchDroid:libdepaums:v0.6.0-3'
	}
```

### Basics
#### Getting mass storage devices

##### Note
The following guide is from libaums, things may have changed with the fork. The readme will be updated soon.

```java
UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this /* Context or Activity */);

for(UsbMassStorageDevice device: devices) {
    
    // before interacting with a device you need to call init()!
    device.init();
    
    // Only uses the first partition on the device
    FileSystem currentFs = device.getPartitions().get(0).getFileSystem();
    Log.d(TAG, "Capacity: " + currentFs.getCapacity());
    Log.d(TAG, "Occupied Space: " + currentFs.getOccupiedSpace());
    Log.d(TAG, "Free Space: " + currentFs.getFreeSpace());
    Log.d(TAG, "Chunk size: " + currentFs.getChunkSize());
}
```

#### Permissions

Your app needs to get permission from the user at run time to be able to communicate the device. From a `UsbMassStorageDevice` you can get the underlying `android.usb.UsbDevice` to do so.

```java
PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Inten(ACTION_USB_PERMISSION), 0);
usbManager.requestPermission(device.getUsbDevice(), permissionIntent);
```

For more information regarding permissions please check out the Android documentation: https://developer.android.com/guide/topics/connectivity/usb/host.html#permission-d

#### Working with files and folders

```java
UsbFile root = currentFs.getRootDirectory();

UsbFile[] files = root.listFiles();
for(UsbFile file: files) {
    Log.d(TAG, file.getName());
    if(file.isDirectory()) {
        Log.d(TAG, file.getLength());
    }
}

UsbFile newDir = root.createDirectory("foo");
UsbFile file = newDir.createFile("bar.txt");

// write to a file
OutputStream os = new UsbFileOutputStream(file);

os.write("hello".getBytes());
os.close();

// read from a file
InputStream is = new UsbFileInputStream(file);
byte[] buffer = new byte[currentFs.getChunkSize()];
is.read(buffer);
```

#### Using buffered streams for more efficency

```java
OutputStream os = UsbFileStreamFactory.createBufferedOutputStream(file, currentFs);
InputStream is = UsbFileStreamFactory.createBufferedInputStream(file, currentFs);
```

#### Cleaning up

```java
// Don't forget to call UsbMassStorageDevice.close() when you are finished

device.close();
```

# Credits (libaums)

##### Thesis

Libaums - Library to access USB Mass Storage Devices  
License: Apache 2.0 (see license.txt for details)
Author: Magnus Jahnen, jahnen at in.tum.de  
Advisor: Nils Kannengießer, nils.kannengiesser at tum.de  
Supervisor: Prof. Uwe Baumgarten, baumgaru at in.tum.de  


Technische Universität München (TUM)  
Lehrstuhl/Fachgebiet für Betriebssysteme  
www.os.in.tum.de  

The library was developed by Mr. Jahnen as part of his bachelor's thesis in 2014. It's a sub-topic of the research topic "Secure Copy Protection for Mobile Apps" by Mr. Kannengießer. The full thesis document can be downloaded [here](https://www.os.in.tum.de/fileadmin/w00bdp/www/Lehre/Abschlussarbeiten/Jahnen-thesis.pdf).

We would appreciate an information email, when you plan to use the library in your projects.

