---
title: Media Capture - Open Camera
description: Capture audio, video, and images.
---
<!--
# license: Licensed to the Apache Software Foundation (ASF) under one
#         or more contributor license agreements.  See the NOTICE file
#         distributed with this work for additional information
#         regarding copyright ownership.  The ASF licenses this file
#         to you under the Apache License, Version 2.0 (the
#         "License"); you may not use this file except in compliance
#         with the License.  You may obtain a copy of the License at
#
#           http://www.apache.org/licenses/LICENSE-2.0
#
#         Unless required by applicable law or agreed to in writing,
#         software distributed under the License is distributed on an
#         "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#         KIND, either express or implied.  See the License for the
#         specific language governing permissions and limitations
#         under the License.
-->

# cordova-plugin-open-camera

This plugin provides access to the device's audio, image, and video capture capabilities using [open camera](https://sourceforge.net/projects/opencamera/).
We can use [cordova-plugin-media-capture](https://github.com/apache/cordova-plugin-media-capture) orign plugin for video and image in Ionic project.
But in some android device, we can only get 3gp format for video. And we can't control video quality/resolution in this plugin for android.
So I tried to implement a new plugin using open camera project on android for Ionic 


## Installation

    cordova plugin add cordova-plugin-open-camera

__Or__

    cordova plugin add https://github.com/yyc93/cordova-plugin-open-camera.git
    

## Supported Platforms

- Amazon Fire OS
- Android
- BlackBerry 10
- Browser
- iOS
- Windows Phone 7 and 8
- Windows 8
- Windows

## Objects

- Capture
- CaptureAudioOptions
- CaptureImageOptions
- CaptureVideoOptions
- CaptureCallback
- CaptureErrorCB
- ConfigurationData
- MediaFile
- MediaFileData

## Methods

- capture.captureAudio
- capture.captureImage
- capture.captureVideo
- MediaFile.getFormatData

## Properties

- __supportedAudioModes__: The audio recording formats supported by the device. (ConfigurationData[])

- __supportedImageModes__: The recording image sizes and formats supported by the device. (ConfigurationData[])

- __supportedVideoModes__: The recording video resolutions and formats supported by the device. (ConfigurationData[])

## capture.captureAudio

> Start the audio recorder application and return information about captured audio clip files.

    navigator.device.capture.captureAudio(
        CaptureCB captureSuccess, CaptureErrorCB captureError,  [CaptureAudioOptions options]
    );

### Description

Starts an asynchronous operation to capture audio recordings using the
device's default audio recording application.  The operation allows
the device user to capture multiple recordings in a single session.

The capture operation ends when either the user exits the audio
recording application, or the maximum number of recordings specified
by `CaptureAudioOptions.limit` is reached.  If no `limit` parameter
value is specified, it defaults to one (1), and the capture operation
terminates after the user records a single audio clip.

When the capture operation finishes, the `CaptureCallback` executes
with an array of `MediaFile` objects describing each captured audio
clip file.  If the user terminates the operation before an audio clip
is captured, the `CaptureErrorCallback` executes with a `CaptureError`
object, featuring the `CaptureError.CAPTURE_NO_MEDIA_FILES` error
code.

### Supported Platforms

- Amazon Fire OS
- Android
- BlackBerry 10
- iOS
- Windows Phone 7 and 8
- Windows 8
- Windows

### Example

    // capture callback
    var captureSuccess = function(mediaFiles) {
        var i, path, len;
        for (i = 0, len = mediaFiles.length; i < len; i += 1) {
            path = mediaFiles[i].fullPath;
            // do something interesting with the file
        }
    };

    // capture error callback
    var captureError = function(error) {
        navigator.notification.alert('Error code: ' + error.code, null, 'Capture Error');
    };

    // start audio capture
    navigator.device.capture.captureAudio(captureSuccess, captureError, {limit:2});

[see more](https://github.com/apache/cordova-plugin-media-capture) about cordova-plugin-media-capture plugin


